package network.gateway.information;

import com.google.common.collect.ImmutableList;
import data.InfoMessage;
import data.JobInterval;
import data.factory.InfoMessageFactory;
import logic.PubSub;
import logic.Scheduler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import twitter4j.*;
import utils.GameUtil;
import utils.RandomUtil;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class Twitter {

    /*public class TweetProcessor implements StatusListener {

        @Override
        public void onStatus(final Status status) {
            Optional<InfoMessage> infoMessageOptional = Singletons.infoMessageFactory.constructInfoMessageFromTweet(status, false, true);

            if (!infoMessageOptional.isPresent()) {
                return;
            }

            Singletons.pubSub.publish(InfoMessage.class, infoMessageOptional.get());
        }

        @Override
        public void onDeletionNotice(final StatusDeletionNotice statusDeletionNotice) {

        }

        @Override
        public void onTrackLimitationNotice(final int numberOfLimitedStatuses) {

        }

        @Override
        public void onScrubGeo(final long userId, final long upToStatusId) {

        }

        @Override
        public void onStallWarning(final StallWarning warning) {

        }

        @Override
        public void onException(final Exception ex) {

        }
    }*/

    private static final int MIN_FAVS = 3;

    private static final JobInterval DELAYED_REFRESH = new JobInterval(10, 600, TimeUnit.SECONDS);

    //private final TwitterStream twitterStream = new TwitterStreamFactory().getInstance();
    private final Collection<String> twitterUsers =
            ImmutableList.of("realdonaldtrump", "twitch", "jimcramer", "SEC_News", "Robinhood", "seanspicer",
                             "JeffBezos", "jonsteinberg", "Cheddar", "WisdomyQuotes");

    @Autowired
    private InfoMessageFactory infoMessageFactory;

    @Autowired
    private PubSub pubSub;

    @Autowired
    private GameUtil gameUtil;

    @Autowired
    private Scheduler scheduler;

    //private final StatusListener tweetProcessor = new TweetProcessor();

    public Twitter() {
        //twitterStream.addListener(tweetProcessor);
        //Singletons.scheduler.scheduleJob(this::updateSubscriptions, DELAYED_REFRESH);
        //Singletons.scheduler.scheduleJob(this::findAssetTweets, DELAYED_REFRESH);
    }

    @PostConstruct
    public void init() {
        this.scheduler.scheduleJob(this::findFollowTweets, DELAYED_REFRESH);
    }

    /*private void updateSubscriptions() {
        twitterStream.cleanUp();

        final List<String> topics = Singletons.brokerCache.getAssets().stream().map(Asset::getSymbol).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(topics)) {
            return;
        }

        twitterStream.filter(topics.toArray(new String[topics.size()]));
    }*/

    /*public void tweetResult(final OrderResult orderResult) {
        if (!orderResult.isOk()) {
            return;
        }

        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(orderResult.getCommandExecuted());

        twitter4j.Twitter twitter = TwitterFactory.getSingleton();

        try {
            Status status = twitter.updateStatus(new StatusUpdate(stringBuilder.toString()));
            log.info("Successfully updated the status to [" + status.getText() + "].");
        } catch (final TwitterException e) {
            log.error(e.getMessage(), e);
            return;
        }
    }*/

    private void findAssetTweets() {
        twitter4j.Twitter twitter = TwitterFactory.getSingleton();

        Collection<String> symbols = this.gameUtil.findPopularSymbols(10);
        symbols = symbols.stream().map(symbol -> "$" + symbol).collect(Collectors.toList());

        if (symbols.size() == 0) {
            return;
        }

        final List<Status> tweets = new ArrayList<>();

        String queryString = StringUtils.join(symbols, " or ");

        try {
            QueryResult queryResult = twitter.search(new Query(queryString));
            tweets.addAll(queryResult.getTweets());
        } catch (TwitterException e) {
            log.warn(e.getMessage(), e);
        }

        publishTweets(tweets);
    }

    private void findFollowTweets() {
        twitter4j.Twitter twitter = TwitterFactory.getSingleton();

        String user = RandomUtil.randomChoice(twitterUsers).orElse(twitterUsers.iterator().next());

        List<Status> statuses = new ArrayList<>();

        try {
            statuses = twitter.getUserTimeline(user);
        } catch (TwitterException e) {
            log.warn(e.getMessage(), e);
        }

        publishTweets(statuses);
    }

    private void publishTweets(final Collection<Status> tweets) {
        final Set<InfoMessage> infoMessages = new HashSet<>();

        for (final Status status : tweets) {

            if (status.isRetweet() || status.getFavoriteCount() < MIN_FAVS) {
                continue;
            }

            final Optional<InfoMessage> infoMessage = this.infoMessageFactory.constructInfoMessageFromTweet(status);
            if (!infoMessage.isPresent()) {
                continue;
            }
            infoMessages.add(infoMessage.get());
        }

        log.info("Got {} InfoMessages out of {} tweets.", infoMessages.size(), tweets.size());

        infoMessages.forEach(infoMessage -> pubSub.publish(InfoMessage.class, infoMessage));
    }

}
