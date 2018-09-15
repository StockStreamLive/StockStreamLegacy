package logic;

import cache.BrokerCache;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.annotations.VisibleForTesting;
import computer.TimeComputer;
import data.JobInterval;
import data.MarketEvent;
import data.MarketState;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;

@Slf4j
public class MarketClock {

    @Autowired
    private MetricRegistry metricsRegistry;

    @Autowired
    private BrokerCache brokerCache;

    @Autowired
    private TimeComputer timeComputer;

    @Autowired
    private Scheduler scheduler;

    private final Map<MarketEvent, Set<Runnable>> eventRunnables = new ConcurrentHashMap<>();
    private final Map<MarketEvent, DateTime> nextEventDates = new ConcurrentHashMap<>();

    public MarketClock() {
    }

    @PostConstruct
    public void init() {
        scheduler.scheduleJob(this::tickClock, new JobInterval(10, 10, TimeUnit.SECONDS));

        subscribe(() -> {}, MarketEvent.MARKET_OPEN_EVENT);
        subscribe(() -> {}, MarketEvent.MARKET_CLOSE_EVENT);
    }

    public void subscribe(final Runnable job, final MarketEvent marketEvent) {
        final DateTime nextEventDate = findEventTime(marketEvent, true);
        log.info("For event {} got next date of {}", marketEvent, nextEventDate);
        nextEventDates.putIfAbsent(marketEvent, nextEventDate);
        eventRunnables.computeIfAbsent(marketEvent, set -> new ConcurrentHashSet<>()).add(job);
    }

    public Optional<DateTime> getNextEventDate(final MarketEvent event) {
        return Optional.ofNullable(nextEventDates.get(event));
    }

    @VisibleForTesting
    protected DateTime findEventTime(final MarketEvent marketEvent, final boolean includeToday) {
        final DateTime now = new DateTime();

        final MarketState marketStateToday = this.brokerCache.getMarketState(now);
        final boolean isOpenToday = marketStateToday.isOpenThisDay();
        final Optional<DateTime> openTimeTodayOptional = marketStateToday.getExtendedOpenTime();
        final Optional<DateTime> closeTimeTodayOptional = marketStateToday.getExtendedCloseTime();

        if (isOpenToday && (!openTimeTodayOptional.isPresent() || !closeTimeTodayOptional.isPresent())) {
            log.warn("Could not find open or close time for date {}, includeToday? {}", now, includeToday);
            return now.plusMinutes(5);
        }

        MarketState marketStateToUse = marketStateToday;

        final boolean openEventAndIsAfterOpen = isOpenToday &&
                                                now.isAfter(marketStateToday.getExtendedOpenTime().get()) &&
                                                MarketEvent.Status.OPEN.equals(marketEvent.getStatus());

        final boolean closeEventAndIsAfterClose = isOpenToday &&
                                                  now.isAfter(marketStateToday.getExtendedCloseTime().get()) &&
                                                  MarketEvent.Status.CLOSE.equals(marketEvent.getStatus());

        if (openEventAndIsAfterOpen || closeEventAndIsAfterClose || !includeToday || !marketStateToday.isOpenThisDay()) {
            marketStateToUse = this.timeComputer.findNextBusinessDay(now);
        }

        Optional<DateTime> openTimeOptional = marketStateToUse.getExtendedOpenTime();
        Optional<DateTime> closeTimeOptional = marketStateToUse.getExtendedCloseTime();

        if (!openTimeOptional.isPresent() || !closeTimeOptional.isPresent()) {
            log.warn("Could not find open or close time for date {}, includeToday? {}", now, includeToday);
            return now.plusMinutes(5);
        }

        final DateTime dateTimeOfNextEvent = MarketEvent.Status.OPEN.equals(marketEvent.getStatus()) ? marketStateToUse.getExtendedOpenTime().get() : closeTimeOptional.get();

        return dateTimeOfNextEvent.plusMinutes(marketEvent.getOffsetMinutes()).plusSeconds(marketEvent.getOffsetMinutes());
    }

    private void tickClock() {
        final Map<MarketEvent, DateTime> newEventDates = new ConcurrentHashMap<>();

        nextEventDates.entrySet().forEach(entry -> {
            final MarketEvent event = entry.getKey();
            final DateTime nextUpdate = entry.getValue();

            if (nextUpdate.isBeforeNow()) {
                final Timer latencyTimer = this.metricsRegistry.timer(name(event.toString(), "duration"));
                Timer.Context timerContext = latencyTimer.time();

                try {
                    eventRunnables.getOrDefault(event, Collections.emptySet()).forEach(Runnable::run);
                } catch (final Exception ex) {
                    log.warn("tickClock() issue {} {}", event, entry, ex);
                }

                Optional<DateTime> nextDate = Optional.empty();

                try {
                    nextDate = Optional.of(findEventTime(event, false));
                } catch (final Throwable throwable) {
                    log.warn(throwable.getMessage(), throwable);
                }

                nextDate.ifPresent(dateTime -> newEventDates.put(event, dateTime));

                timerContext.stop();
            }
        });

        newEventDates.entrySet().forEach(event -> {
            log.info("Next date for event {} is {}", event.getKey(), event.getValue());
            nextEventDates.put(event.getKey(), event.getValue());
        });
    }

}
