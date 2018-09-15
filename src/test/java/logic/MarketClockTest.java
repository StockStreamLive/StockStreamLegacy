package logic;

import cache.BrokerCache;
import com.codahale.metrics.MetricRegistry;
import computer.TimeComputer;
import data.MarketEvent;
import data.MarketState;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import test.MarketStateTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class MarketClockTest {

    @Mock
    private MetricRegistry metricRegistry;

    @Mock
    private Scheduler scheduler;

    @Mock
    private BrokerCache brokerCache;

    @Mock
    private TimeComputer timeComputer;

    @InjectMocks
    private MarketClock marketClock;

    @Before
    public void setupTest() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testFindNextEvent_marketOpenPastOpenEvent_expectOpenNextDay() {
        final DateTime today = new DateTime();
        final DateTime tomorrow = today.plusDays(1);

        final MarketState marketStateToday = MarketStateTestUtils.createMarketState(today, true);
        final MarketState marketStateTomorrow = MarketStateTestUtils.createMarketState(tomorrow, true);

        when(brokerCache.getMarketState(any())).thenReturn(marketStateToday);
        when(timeComputer.findNextBusinessDay(any())).thenReturn(marketStateTomorrow);

        final DateTime dateTime = marketClock.findEventTime(new MarketEvent(MarketEvent.Status.OPEN, 0, 0), true);

        assertTrue(marketStateTomorrow.getExtendedOpenTime().isPresent());
        assertEquals(dateTime, marketStateTomorrow.getExtendedOpenTime().get());
    }


}
