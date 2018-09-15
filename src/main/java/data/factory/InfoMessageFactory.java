package data.factory;


import application.Config;
import cache.BrokerCache;
import cache.SortedInstrumentCache;
import com.google.common.collect.ImmutableSet;
import data.InfoMessage;
import data.OrderResult;
import data.command.ActionType;
import logic.Scheduler;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import twitter4j.Status;
import utils.GameUtil;

import javax.annotation.PostConstruct;
import java.util.Optional;
import java.util.Set;

public class InfoMessageFactory {
    @Autowired
    private SortedInstrumentCache sortedInstrumentCache;

    @Autowired
    private BrokerCache brokerCache;

    @Autowired
    private GameUtil gameUtil;

    @Autowired
    private Scheduler scheduler;

    private static final int MAX_SYMBOLS = 50;

    private Set<String> topSymbols = new ConcurrentHashSet<>();

    public InfoMessageFactory() {
    }

    @PostConstruct
    public void init() {
        this.scheduler.scheduleJob(this::updateTopSymbols, Config.MEDIA_REFRESH);
    }

    private void updateTopSymbols() {
        final Set<String> topics = new ConcurrentHashSet<>();

        this.brokerCache.getAssets().forEach(asset -> topics.add(asset.getSymbol()));

        for (final SortedInstrumentCache.ScoredInstrument token : this.sortedInstrumentCache.getCandidateTokens()) {
            if (topics.size() >= MAX_SYMBOLS) {
                break;
            }
            topics.add(token.getInstrument().getSymbol());
        }

        topSymbols = topics;
    }

    private boolean containsSymbol(final String text) {
        for (final String symbol : topSymbols) {
            if (text.contains("$" + symbol)) {
                return true;
            }
        }
        return false;
    }

    public InfoMessage constructInfoMessageFromOrderResult(final OrderResult orderResult) {
        final String commandString = String.valueOf(orderResult.getAction()).toLowerCase();
        String orderMessage = this.gameUtil.getGenericErrorMessage();

        switch (orderResult.getOrderStatus()) {
            case OK: {
                if (ActionType.HOLD.equals(orderResult.getAction())) {
                    orderMessage = String.format("The winning vote is %s, with %s votes. Skipping a round!",
                                                 commandString, orderResult.getVotesReceived());
                } else {
                    orderMessage = String.format("Placed order to %s a share of %s after receiving %s votes.",
                                                 commandString, orderResult.getSymbol(), orderResult.getVotesReceived());
                }
                break;
            } case CANT_AFFORD: {
                final float balance = this.brokerCache.getAccountBalance();
                orderMessage = String.format("Received %s votes to %s %s, but cannot afford because there is only $%s of spendable cash.",
                                             orderResult.getVotesReceived(), commandString, orderResult.getSymbol(), balance);
                break;
            } case NET_WORTH_TOO_LOW: {
                orderMessage = String.format("Trading with a net worth of less than %s is not permitted due to rules imposed by FINRA. " +
                "In order to continue trading please either donate some funds or petition the appropriate authorities to remove these rules.",
                        Config.MIN_NET_WORTH);
            } case NO_SHARES: {
                orderMessage = String.format("Received %s votes to %s %s, but could not because there are no shares of %s to %s.",
                                             orderResult.getVotesReceived(), commandString, orderResult.getSymbol(), orderResult.getSymbol(), commandString);
                break;
            } case NOT_ENOUGH_VOTES: {
                orderMessage = String.format("Not enough votes cast, so not buying or selling anything. Go to %s to cast your vote!", Config.PROMO_URL);
                break;
            } case MARKET_CLOSED: {
                orderMessage = String.format("Received %s votes to %s %s, but unfortunately the market closed before the order could be placed!",
                                             orderResult.getVotesReceived(), commandString, orderResult.getSymbol());
                break;
            } case BROKER_EXCEPTION: {
                orderMessage = String.format("Received %s votes to %s %s, but ran into an snafu. See if you can make some sense of it: %s",
                                             orderResult.getVotesReceived(), commandString, orderResult.getSymbol(), this.gameUtil.getGenericErrorMessage());
                break;
            } case UNKNOWN: {
                orderMessage = String.format("Not sure exactly what is going on anymore. %s {} %s () %s [] %s @" +
                                             this.gameUtil.getGenericErrorMessage(), commandString, orderResult.toString(), orderResult.getVotesReceived());
                break;
            }
        }

        return new InfoMessage("Stock Stream", orderMessage,  new DateTime().getMillis(), "twitchinvests", orderMessage);
    }

    private static final Set<String> SPECIAL_HANDLES = ImmutableSet.of("cheddar","realdonaldtrump");

    public Optional<InfoMessage> constructInfoMessageFromTweet(final Status tweet) {

        final String text = tweet.getText();

        final String handle = tweet.getUser().getScreenName();

        final String url= "https://twitter.com/" + handle + "/status/" + tweet.getId();

        final String platform = (SPECIAL_HANDLES.contains(handle.toLowerCase())) ? handle.toLowerCase() : "twitter";

        final InfoMessage infoMessage = new InfoMessage("@" + handle, text,
                                                        tweet.getCreatedAt().getTime(), platform, url);


        return Optional.of(infoMessage);
    }

}
