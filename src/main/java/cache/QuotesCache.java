package cache;


import data.Quote;
import exception.RobinhoodException;
import logic.Scheduler;
import lombok.extern.slf4j.Slf4j;
import network.gateway.broker.Broker;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.springframework.beans.factory.annotation.Autowired;
import utils.GameUtil;
import utils.RandomUtil;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
public class QuotesCache {
    @Autowired
    private Broker broker;

    @Autowired
    private GameUtil gameUtil;

    @Autowired
    private Scheduler scheduler;

    private Set<Quote> quotes = new ConcurrentHashSet<>();

    public QuotesCache() {
    }

    @PostConstruct
    public void init() {
        scheduler.scheduleJob(this::updateQuotes, 25L, 300L, TimeUnit.SECONDS);
    }

    public Set<Quote> getQuotes() {
        return Collections.unmodifiableSet(quotes);
    }

    public Set<Quote> getRandomQuotes(int n) {
        Collection<Quote> rand = RandomUtil.nRandomChoices(n, quotes);
        return new HashSet<>(rand);
    }

    private synchronized void updateQuotes() {
        final Collection<String> popularSymbols = this.gameUtil.findPopularSymbols(100);

        final List<Map<String, String>> rhQuotes;

        try {
            rhQuotes = this.broker.getQuotes(popularSymbols);
        } catch (final RobinhoodException e) {
            log.warn(e.getMessage(), e);
            return;
        }

        final Set<Quote> newQuotes = new ConcurrentHashSet<>();
        rhQuotes.forEach(quote -> newQuotes.add(new Quote(quote)));
        quotes = newQuotes;
    }

}
