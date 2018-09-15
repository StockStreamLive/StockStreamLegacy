package cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import data.*;
import exception.RobinhoodException;
import logic.PubSub;
import logic.Scheduler;
import lombok.extern.slf4j.Slf4j;
import network.gateway.broker.Broker;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class BrokerCache {

    private class UpdateAccountRunnable implements Runnable {

        @Override
        public void run() {
            try {
                updateAssets();
                updateAccountBalance();
                updateHistoricalValue();
            } catch (final Exception e) {
                log.warn(e.getMessage(), e);
            }
        }
    }

    // Day of year -> DayObject
    private LoadingCache<Integer, MarketState> marketStateCache =
            CacheBuilder.newBuilder().build(new CacheLoader<Integer, MarketState>() {
                @Override
                public MarketState load(final Integer key) throws Exception {
                    DateTime thisDay = new DateTime(0).withYear(new DateTime().getYear()).withDayOfYear(key);
                    MarketState day = broker.getMarketStateForDate(thisDay);
                    return day;
                }
            });

    private final UpdateAccountRunnable updateAccountRunnable = new UpdateAccountRunnable();

    private float accountBalance = 0f;
    private Collection<Order> recentOrders = new ArrayList<>();
    private Collection<Asset> assets = new ConcurrentHashSet<>();
    private Set<String> ownedSymbols = new ConcurrentHashSet<>();
    private List<EquityHistorical> historicalEquityValue = new ArrayList<>();

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private PubSub pubSub;

    @Autowired
    private Broker broker;

    public BrokerCache() {
    }

    @PostConstruct
    public void init() {
        scheduler.scheduleJob(updateAccountRunnable, 10L, 60, TimeUnit.SECONDS);
        scheduler.scheduleJob(updateAccountRunnable, Event.ORDER_PLACED);
        pubSub.subscribeRunnable(updateAccountRunnable, OrderResult.class);
    }

    public Collection<Order> getRecentOrders() {
        return Collections.unmodifiableCollection(recentOrders);
    }

    public Collection<Asset> getAssets() {
        return Collections.unmodifiableCollection(assets);
    }

    public Set<String> getOwnedSymbols() {
        return Collections.unmodifiableSet(ownedSymbols);
    }

    public List<EquityHistorical> getHistoricalAccountValue() {
        return Collections.unmodifiableList(historicalEquityValue);
    }

    public float getAccountBalance() {
        return accountBalance;
    }

    public MarketState getMarketState(final DateTime forDate) {
        try {
            return marketStateCache.get(forDate.getDayOfYear());
        } catch (final Exception ex) {
            log.warn(ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

    public synchronized double getAccountNetWorth() {
        double netWorth = (double) accountBalance;
        netWorth += assets.stream().mapToDouble(Asset::getAssetValue).sum();
        return netWorth;
    }

    private synchronized void updateAccountBalance() {
        try {
            accountBalance = this.broker.getBuyingPower();
        } catch (final RobinhoodException e) {
            log.warn("Could not update account balance due to {}", e.getMessage(), e);
        }
    }

    private synchronized void updateAssets() {
        try {
            assets = this.broker.getOwnedAssets();
            ownedSymbols = assets.stream().map(Asset::getSymbol).collect(Collectors.toSet());
        } catch (final RobinhoodException e) {
            log.warn("Could not update account assets due to {}", e.getMessage(), e);
        }
    }

    private synchronized void updateHistoricalValue() {
        try {
            historicalEquityValue = this.broker.getHistoricalValues();
        } catch (final RobinhoodException e) {
            log.warn("Could not update historical values due to {}", e.getMessage(), e);
        }
    }

    private synchronized void updateRecentOrders() {
        try {
            recentOrders = this.broker.getRecentOrders();
        } catch (final RobinhoodException e) {
            log.warn("Could not update recent orders due to {}", e.getMessage(), e);
        }
    }


}
