package logic.game;

import application.Config;
import application.Stage;
import cache.BrokerCache;
import cache.PlayerCommandCache;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import computer.CommandComputer;
import computer.TimeComputer;
import data.Event;
import data.MarketState;
import data.RoundResult;
import logic.PubSub;
import logic.Scheduler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.base.BaseDateTime;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GameClock {
    @Autowired
    private GameEngine gameEngine;

    @Autowired
    private BrokerCache brokerCache;

    @Autowired
    private TimeComputer timeComputer;

    @Autowired
    private CommandComputer commandComputer;

    @Autowired
    private PlayerCommandCache playerCommandCache;

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private PubSub pubSub;

    public static final ImmutableMap<Stage, Long> INTERVALS =
            ImmutableMap.of(Stage.TEST, 5L,
                            Stage.LOCAL, 20L,
                            Stage.GAMMA, 20L,
                            Stage.PROD, 300L);

    @Getter
    private GameEvent nextGameEvent;

    public GameClock() {
    }

    @PostConstruct
    public void init() {
        this.scheduler.scheduleJob(this::doApplicationTick, 10, 1, TimeUnit.SECONDS);
        setNextEvent(new DateTime());
    }

    private void doApplicationTick() {
        final DateTime now = new DateTime();

        if (now.isAfter(nextGameEvent.getNextEvent())) {
            log.info("Now {} is after next event {}.", now, nextGameEvent);

            final RoundResult roundResult = new RoundResult(new HashMap<>(playerCommandCache.getPlayerToCommand()),
                                                            commandComputer.computeRankedCommands());

            this.pubSub.publish(RoundResult.class, roundResult);
            this.scheduler.notifyEvent(Event.GAME_TICK);

            setNextEvent(now);
        }
    }

    @VisibleForTesting
    protected void setNextEvent(final DateTime now) {
        final MarketState marketStateToday = this.brokerCache.getMarketState(now);

        final long nextGameTick = System.currentTimeMillis() + (INTERVALS.get(Config.stage) * 1000);
        if (marketStateToday.isOpenNow()) {
            nextGameEvent = new GameEvent(nextGameTick, GameEvent.Type.GAME_TICK);
            return;
        }

        if (marketStateToday.isOpenThisDay() &&
                now.isBefore(marketStateToday.getExtendedOpenTime().get()) &&
                now.isBefore(marketStateToday.getExtendedCloseTime().get())) {
            final long openTimeToday = marketStateToday.getExtendedOpenTime().map(BaseDateTime::getMillis).orElse(nextGameTick);
            nextGameEvent = new GameEvent(openTimeToday, GameEvent.Type.MARKET_OPEN);
            return;
        }

        final MarketState nextBusinessDay = this.timeComputer.findNextBusinessDay(now);
        final long nextBusinessDayOpenTime = nextBusinessDay.getExtendedOpenTime().map(BaseDateTime::getMillis).orElse(nextGameTick);
        nextGameEvent = new GameEvent(nextBusinessDayOpenTime, GameEvent.Type.MARKET_OPEN);
    }

}
