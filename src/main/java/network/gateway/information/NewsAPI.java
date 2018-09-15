package network.gateway.information;


import application.Config;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mdimension.jchronic.Chronic;
import com.mdimension.jchronic.utils.Span;
import data.InfoMessage;
import logic.PubSub;
import logic.Scheduler;
import lombok.extern.slf4j.Slf4j;
import network.http.HTTPQuery;
import network.http.HTTPResult;
import network.http.HTTPUtil;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import utils.RandomUtil;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class NewsAPI {

    private static final Set<HTTPQuery> blacklistedRequests = new ConcurrentHashSet<>();

    private static final String API_KEY = "71af2b5d0caa4f3383df59e94ea069ea";

    private static final List<String> PREFERRED_PLATFORMS =
            ImmutableList.of("cnn", "bloomberg", "associated-press", "business-insider", "cnbc",
                             "the-economist", "the-new-york-times", "the-wall-street-journal", "time", "bbc-news",
                             "fortune", "techcrunch", "usa-today", "reuters", "recode", "breitbart-news", "the-verge");

    private static final Set<String> IGNORE_TITLE_PLATFORMS =
            ImmutableSet.of("associated-press", "cnn", "reuters", "the-wall-street-journal", "bloomberg");

    private static final List<String> SORT_KEYS = ImmutableList.of("top", "latest", "popular");

    @Autowired
    private PubSub pubSub;

    @Autowired
    private HTTPUtil httpUtil;

    @Autowired
    private Scheduler scheduler;

    public NewsAPI() {
    }

    @PostConstruct
    public void init() {
        this.scheduler.scheduleJob(this::refreshInfo, Config.MEDIA_REFRESH);
    }

    private String constructURL(final String source, final String sortOrder) {
        return String.format("https://newsapi.org/v1/articles?source=%s&sortBy=%s&apiKey=%s", source, sortOrder, API_KEY);
    }

    private void refreshInfo() {
        for (final String platform : PREFERRED_PLATFORMS) {
            try {
                refreshPlatform(platform, RandomUtil.randomChoice(SORT_KEYS).orElse(SORT_KEYS.iterator().next()));
            } catch (Exception ex) {
                log.warn(ex.getMessage(), ex);
            }
        }
    }

    private void refreshPlatform(final String platform, final String sortOrder) {
        final String url = constructURL(platform, sortOrder);

        final HTTPQuery httpQuery = new HTTPQuery(url, ImmutableMap.of(), ImmutableMap.of());

        if (blacklistedRequests.contains(httpQuery)) {
            return;
        }

        final Optional<HTTPResult> httpResultOptional = this.httpUtil.executeHTTPGetRequest(httpQuery);

        if (!httpResultOptional.isPresent()) {
            log.warn("Bad request {}", httpResultOptional);
            return;
        }

        final HTTPResult httpResult = httpResultOptional.get();

        if (!httpResult.isOk()) {
            processBadResponse(httpQuery, httpResult);
            return;
        }

        final String jsonData = httpResult.getBody();
        final JSONObject jsonObj = new JSONObject(jsonData);

        if (jsonObj.isNull("articles")) {
            return;
        }

        final JSONArray articles = jsonObj.getJSONArray("articles");

        for (final Object article : articles) {
            if (!(article instanceof JSONObject)) {
                continue;
            }

            JSONObject articleObj = (JSONObject) article;

            if (articleObj.isNull("description") || articleObj.isNull("author") || articleObj.isNull("publishedAt")) {
                continue;
            }

            final String info = extractContent(platform, articleObj);

            if (StringUtils.isEmpty(info)) {
                continue;
            }

            String author = articleObj.getString("author");
            String strTimestamp = articleObj.getString("publishedAt");
            String storyUrl = articleObj.getString("url");

            // Timestamps are ambiguous due to lack of timezone data.
            DateTime dateToUse = new DateTime();

            if (strTimestamp != null) {
                Span dateSpan = Chronic.parse(strTimestamp);
                if (dateSpan != null) {
                    dateToUse = new DateTime(dateSpan.getBegin());
                }
            }

            if (dateToUse.isAfterNow()) {
                dateToUse = new DateTime();
            }

            final long messageTimestamp = dateToUse.getMillis();

            this.pubSub.publish(InfoMessage.class, new InfoMessage(author, info, messageTimestamp, platform, storyUrl));
        }
    }

    private void processBadResponse(final HTTPQuery httpQuery,
                                    final HTTPResult httpResult) {
        if (httpResult.getStatusCode() == 400) {
            final JSONObject jsonObject = new JSONObject(httpResult.getBody());

            if (jsonObject.has("code") && "sourceUnavailableSortedBy".equals(jsonObject.getString("code"))) {
                blacklistedRequests.add(httpQuery);
                return;
            }
        }
        log.warn("Unknown bad request {} -> {}", httpQuery, httpResult);
    }


    private String extractContent(final String platform, final JSONObject articleObject) {
        final StringBuilder builder = new StringBuilder();

        if (!IGNORE_TITLE_PLATFORMS.contains(platform)) {
            String title = articleObject.getString("title");
            title = (title.endsWith(".") ? title : title + ".");

            builder.append(title);
        }

        builder.append(" ");

        final String text = articleObject.getString("description");

        if (!StringUtils.isEmpty(text)) {
            builder.append(text);
        }

        return builder.toString();
    }
}
