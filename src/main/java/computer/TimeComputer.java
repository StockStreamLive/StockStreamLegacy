package computer;

import cache.BrokerCache;
import com.google.common.collect.ImmutableSet;
import data.MarketState;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

public class TimeComputer {

    public static final Set<Integer> WEEKDAYS =
            ImmutableSet.of(DateTimeConstants.MONDAY, DateTimeConstants.TUESDAY, DateTimeConstants.WEDNESDAY,
                            DateTimeConstants.THURSDAY, DateTimeConstants.FRIDAY);

    @Autowired
    private BrokerCache brokerCache;

    public TimeComputer() {
    }

    public boolean isMarketOpenNow() {
        final DateTime now = new DateTime();
        final MarketState marketState = this.brokerCache.getMarketState(now);
        return marketState.isOpenNow();
    }

    public boolean isAfterHours() {
        final DateTime now = new DateTime();
        final MarketState marketState = this.brokerCache.getMarketState(now);
        return marketState.isAfterHoursNow();
    }

    public boolean isMarketOpenToday() {
        final DateTime now = new DateTime();
        final MarketState marketState;

        marketState = this.brokerCache.getMarketState(now);

        return marketState.isOpenThisDay();
    }

    public MarketState findNextBusinessDay(final DateTime fromDate) {
        DateTime dayIterator = fromDate.plusDays(1);

        while (true) {
            final MarketState marketState;

            marketState = this.brokerCache.getMarketState(dayIterator);

            if (marketState.isOpenThisDay()) {
                return marketState;
            }

            dayIterator = dayIterator.plusDays(1);
        }
    }

}
