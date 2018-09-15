package logic;


import application.Config;
import data.Event;
import data.JobInterval;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Scheduler {
    private final ScheduledThreadPoolExecutor threadPoolExecutor = new ScheduledThreadPoolExecutor(Config.CPU_CORE_COUNT);
    private final Map<Event, Set<Runnable>> eventRunnableMap = new ConcurrentHashMap<>();

    public void scheduleJob(final Runnable runnable, final JobInterval jobInterval) {
        scheduleJob(runnable, jobInterval.getInitialDelay(), jobInterval.getIntervalDelay(), jobInterval.getTimeUnit());
    }

    public void cancelJob(final Runnable job) {
        threadPoolExecutor.remove(job);
    }

    public void scheduleJob(final Runnable job,
                            final long initialDelay,
                            final long period,
                            final TimeUnit unit) {
        threadPoolExecutor.scheduleAtFixedRate(() -> {
            try {
                job.run();
            } catch (Exception ex) {
                log.warn(ex.getMessage(), ex);
            }
        }, initialDelay, period, unit);
    }

    public void scheduleJob(final Runnable job, final DateTime atDate) {
        final DateTime now = new DateTime();
        final long timeDifferenceMilliseconds = atDate.getMillis() - now.getMillis();
        threadPoolExecutor.schedule(() -> {
            try {
                job.run();
            } catch (final Exception ex) {
                log.warn(ex.getMessage(), ex);
            }
        }, timeDifferenceMilliseconds, TimeUnit.MILLISECONDS);
    }

    public void scheduleJob(final Runnable job, final Event event) {
        eventRunnableMap.computeIfAbsent(event, set -> new HashSet<>()).add(() -> {
            try {
                job.run();
            } catch (final Exception ex) {
                log.warn("Scheduler job issue {} {} {} {}", job, event, ex.getMessage(), ex);
            }
        });
    }

    public void notifyEvent(final Event event) {
        eventRunnableMap.getOrDefault(event, Collections.emptySet()).forEach(Runnable::run);
    }
}
