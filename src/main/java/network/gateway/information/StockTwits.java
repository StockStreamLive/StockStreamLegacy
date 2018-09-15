package network.gateway.information;

import application.Config;
import com.google.common.collect.ImmutableMap;
import computer.TimeComputer;
import data.InfoMessage;
import logic.PubSub;
import logic.Scheduler;
import lombok.extern.slf4j.Slf4j;
import network.http.HTTPQuery;
import network.http.HTTPResult;
import network.http.HTTPUtil;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import utils.GameUtil;
import utils.TextUtil;
import utils.TimeUtil;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.Optional;

@Slf4j
public class StockTwits {

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private PubSub pubSub;

    @Autowired
    private HTTPUtil httpUtil;

    @Autowired
    private GameUtil gameUtil;

    @Autowired
    private TimeComputer timeComputer;

    private static final int DEFAULT_QUOTA = 200;
    private static final int MINIMUM_LIKES = 3;
    private static final String SYMBOL_URL = "https://api.stocktwits.com/api/2/streams/symbol/";

    private long nextQuotaReset = 0;
    private int queriesRemaining = DEFAULT_QUOTA;

    public StockTwits() {
    }

    @PostConstruct
    public void init() {
        this.scheduler.scheduleJob(this::refreshInfo, Config.MEDIA_REFRESH);
    }

    private void refreshInfo() {
        final Collection<String> symbols = this.gameUtil.findPopularSymbols(10);
        for (final String symbol : symbols) {
            final String url = SYMBOL_URL + symbol.toUpperCase() + ".json";

            if (queriesRemaining <= 0) {
                scheduleQuotaUpdate();
                return;
            }

            Optional<HTTPResult> httpResultOptional = this.httpUtil.executeHTTPGetRequest(new HTTPQuery(url, ImmutableMap.of(), ImmutableMap.of()));

            if (!httpResultOptional.isPresent()) {
                continue;
            }

            final HTTPResult httpResult = httpResultOptional.get();

            updateRateLimitingInformation(httpResult);

            if (!httpResult.isOk()) {
                log.warn("Bad response: {}", httpResult);
                continue;
            }

            final String jsonData = httpResult.getBody();
            final JSONObject jsonObj = new JSONObject(jsonData);
            final JSONArray messages = jsonObj.getJSONArray("messages");

            log.debug("Got {} items from stocktwits for symbol {}", messages.length(), symbol);

            for (final Object message : messages) {
                if (!(message instanceof JSONObject)) {
                    continue;
                }

                JSONObject messageObj = (JSONObject) message;

                final String text = StringEscapeUtils.unescapeHtml4(messageObj.getString("body"));

                if (TextUtil.containsURL(text)) {
                    continue;
                }

                if (!messageObj.has("id")) {
                    continue;
                }

                final int messageId = messageObj.getInt("id");
                final String createdAt = messageObj.getString("created_at");
                final String username = messageObj.getJSONObject("user").getString("username");

                Optional<DateTime> date = TimeUtil.createDateFromStr("yyyy-MM-dd'T'HH:mm:ss'Z'", createdAt, "UTC");

                if (!date.isPresent()) {
                    continue;
                }

                // Timestamps are ambiguous due to lack of timezone data.
                DateTime dateToUse = date.get();
                if (dateToUse.isAfter(new Instant())) {
                    dateToUse = new DateTime().minusMinutes(10);
                }

                int likes = 0;
                if (messageObj.has("likes")) {
                    likes = messageObj.getJSONObject("likes").getInt("total");
                }

                if (likes <= MINIMUM_LIKES) {
                    continue;
                }

                final String statusUrl = "https://stocktwits.com/" + username + "/message/" + String.valueOf(messageId);

                this.pubSub.publish(InfoMessage.class, new InfoMessage(username, text, dateToUse.getMillis(), "stocktwits", statusUrl));
            }
        }
    }

    private void scheduleQuotaUpdate() {
        this.scheduler.scheduleJob(() -> queriesRemaining = DEFAULT_QUOTA, new DateTime(nextQuotaReset));
    }

    private void updateRateLimitingInformation(final HTTPResult httpResult) {
        if (httpResult.getHeaders() == null) {
            return;
        }
        final String strLimit = httpResult.getHeaders().get("X-RateLimit-Remaining");
        if (!StringUtils.isEmpty(strLimit)) {
            queriesRemaining = Integer.parseInt(strLimit);
        }

        final String strNextReset = httpResult.getHeaders().get("X-RateLimit-Reset");
        if (!StringUtils.isEmpty(strNextReset)) {
            nextQuotaReset = Long.parseLong(strNextReset);
        }
    }

}
