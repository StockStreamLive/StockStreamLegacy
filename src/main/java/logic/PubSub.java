package logic;


import application.Config;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

import static com.codahale.metrics.MetricRegistry.name;

@Slf4j
public class PubSub {
    @Autowired
    private MetricRegistry metricsRegistry;

    private final ExecutorService executorService = Executors.newFixedThreadPool(Config.CPU_CORE_COUNT);

    private Map<Class<?>, Set<Function>> functionSubscriptions = new ConcurrentHashMap<>();
    private Map<Class<?>, Set<Runnable>> runnableSubscriptions = new ConcurrentHashMap<>();

    public PubSub() {
    }

    public void publishAsync(final Class<?> type, final Object object) {
        new Thread(() -> publish(type, object)).start();
    }

    public void publish(final Class<?> type, final Object object) {
        final Timer latencyTimer = this.metricsRegistry.timer(name(type.getCanonicalName(), "Pubsub.publish"));
        Timer.Context timerContext = latencyTimer.time();

        final Set<Function> functions = functionSubscriptions.getOrDefault(type, Collections.emptySet());
        final Set<Runnable> runnables = runnableSubscriptions.getOrDefault(type, Collections.emptySet());

        final int jobCount = Math.min(Config.CPU_CORE_COUNT, functions.size() + runnables.size());

        final List<Future<?>> executorFutures = new ArrayList<>(jobCount);

        final ConcurrentLinkedQueue<Function> functionQueue = new ConcurrentLinkedQueue<>(functions);
        final ConcurrentLinkedQueue<Runnable> runnableQueue = new ConcurrentLinkedQueue<>(runnables);

        for (int i = 0; i < jobCount; ++i) {
            executorFutures.add(executorService.submit(() -> {
                while (!functionQueue.isEmpty()) {
                    final Function function = functionQueue.poll();
                    if (function == null) {
                        continue;
                    }
                    try {
                        function.apply(object);
                    } catch (Exception e) {
                        log.warn(e.getMessage(), e);
                    }
                }
                while (!runnableQueue.isEmpty()) {
                    final Runnable runnable = runnableQueue.poll();
                    if (runnable == null) {
                        continue;
                    }
                    try {
                        runnable.run();
                    } catch (final Exception e) {
                        log.warn(e.getMessage(), e);
                    }
                }
            }));
        }

        try {
            for (final Future<?> demandScoreFuture : executorFutures) {
                demandScoreFuture.get();
            }
        } catch (final CancellationException | InterruptedException | ExecutionException e) {
            log.warn(e.getMessage(), e);
        }

        timerContext.stop();
    }

    public <T> void subscribe(final Function<T, Void> onReceive, final Class<?> type) {
        functionSubscriptions.computeIfAbsent(type, set -> new ConcurrentHashSet<>()).add(onReceive);
    }

    public void subscribeRunnable(final Runnable runnable, final Class<?> type) {
        runnableSubscriptions.computeIfAbsent(type, set -> new ConcurrentHashSet<>()).add(runnable);
    }
}
