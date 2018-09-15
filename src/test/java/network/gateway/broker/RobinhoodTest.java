package network.gateway.broker;

import cache.InstrumentCache;
import com.google.common.collect.ImmutableMap;
import computer.TimeComputer;
import data.Quote;
import exception.RobinhoodException;
import logic.Scheduler;
import network.http.HTTPUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class RobinhoodTest {

    @Mock
    private InstrumentCache instrumentCache;

    @Mock
    private HTTPUtil httpUtil;

    @Mock
    private Scheduler scheduler;

    @Mock
    private TimeComputer timeComputer;

    @InjectMocks
    private Robinhood robinhood = new Robinhood("UN", "PW");

    @Before
    public void setupTest() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testCalculateBuyOrderCeiling_somePriceNotAfterHours_expectLessThan6Percent() throws RobinhoodException {
        final Quote quote = new Quote(ImmutableMap.of("last_trade_price", "4.20"));

        when(timeComputer.isAfterHours()).thenReturn(false);

        final double ceiling = robinhood.calculateBuyOrderCeiling(quote);

        assertTrue(ceiling > 4.20d && ceiling < 4.45d);
    }

    @Test
    public void testCalculateBuyOrderCeiling_somePriceIsAfterHours_expectLessThan6Percent() throws RobinhoodException {
        final Quote quote = new Quote(ImmutableMap.of("last_trade_price", "4.20"));

        when(timeComputer.isAfterHours()).thenReturn(true);

        final double ceiling = robinhood.calculateBuyOrderCeiling(quote);

        assertTrue(ceiling > 4.20d && ceiling < 4.25d);
    }

}
