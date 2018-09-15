package network.http;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;


@Slf4j
public class HTTPUtil {

    private static final int BACKOFF_HOURS = 24;
    private static final Map<String, DateTime> LAST_ERRORED_QUERY = new ConcurrentHashMap<>();

    @Autowired
    private MetricRegistry metricRegistry;

    public HTTPUtil() {
    }

    public Optional<HTTPResult> executeHTTPPostRequest(final HTTPQuery httpQuery) {
        HttpPost httpPost = new HttpPost(httpQuery.getUrl());

        List<NameValuePair> params = new ArrayList<>();
        for (final Map.Entry<String, String> paramEntry : httpQuery.getParameters().entrySet()) {
            params.add(new BasicNameValuePair(paramEntry.getKey(), paramEntry.getValue()));
        }

        try {
            httpPost.setEntity(new UrlEncodedFormEntity(params, "utf-8"));
        } catch (final UnsupportedEncodingException e) {
            log.warn(e.getMessage(), e);
        }

        return executeHTTPRequest(httpPost, httpQuery.getHeaders());
    }


    public Optional<HTTPResult> executeHTTPGetRequest(final HTTPQuery httpQuery) {
        String paramString = "";
        if (httpQuery.getParameters().size() > 0) {
            List<NameValuePair> params = new ArrayList<>();
            for (final Map.Entry<String, String> paramEntry : httpQuery.getParameters().entrySet()) {
                params.add(new BasicNameValuePair(paramEntry.getKey(), paramEntry.getValue()));
            }
            paramString = "?" + URLEncodedUtils.format(params, "utf-8");
        }
        HttpGet httpGet = new HttpGet(httpQuery.getUrl() + paramString);

        return executeHTTPRequest(httpGet, httpQuery.getHeaders());
    }

    private Optional<HTTPResult> executeHTTPRequest(final HttpUriRequest httpRequest,
                                                    final Map<String, String> headers) {
        final Timer latencyTimer = this.metricRegistry.timer(name(httpRequest.getURI().getHost(), "latency"));
        Timer.Context timerContext = latencyTimer.time();

        final String url = httpRequest.getURI().toString();
        if (isURLThrottled(url)) {
            return Optional.empty();
        }

        final List<Header> headerList = new ArrayList<>();
        headers.entrySet().forEach(header -> headerList.add(new BasicHeader(header.getKey(), header.getValue())));

        final HttpClient httpClient = HttpClients.custom()
                                                 .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
                                                 .setDefaultHeaders(headerList).setConnectionTimeToLive(2000, TimeUnit.MILLISECONDS).build();

        try {
            HttpResponse response = httpClient.execute(httpRequest);
            HttpEntity entity = response.getEntity();

            final Map<String, String> responseHeader = new HashMap<>();
            for (final Header header : response.getAllHeaders()) {
                responseHeader.put(header.getName(), header.getValue());
            }

            final String pageContents = IOUtils.toString(entity.getContent(), Charset.defaultCharset());

            HTTPResult result = new HTTPResult(response.getStatusLine().getStatusCode(), responseHeader, pageContents);

            return Optional.of(result);
        } catch (final IOException e) {
            log.warn(e.getMessage(), e);
        } finally {
            timerContext.stop();
        }

        /*if (responseCode != 200) {
            final DateTime now = new DateTime();
            LAST_ERRORED_QUERY.put(url, now);
            log.warn("At {} got response of {} from url {}, so updating throttle rule.", now, response, url);
            return Optional.empty();
        }*/

        return Optional.empty();
    }

    private boolean isURLThrottled(final String url) {
        if (!LAST_ERRORED_QUERY.containsKey(url)) {
            return false;
        }

        final DateTime lastErroredQuery = LAST_ERRORED_QUERY.get(url);
        boolean isThrottled = lastErroredQuery.isAfter(new DateTime().minusHours(BACKOFF_HOURS));

        if (isThrottled) {
            log.warn("Last errored query is {} so URL is throttled: {}", lastErroredQuery, url);
        }

        return isThrottled;
    }
}
