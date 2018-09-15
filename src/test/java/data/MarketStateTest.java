package data;


import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.junit.Test;
import test.MarketStateTestUtils;

import static org.junit.Assert.assertEquals;

public class MarketStateTest {

    @Test
    public void testIsOpenNow_noDataPresent_expectMarketClosed() {
        final MarketState marketState = new MarketState(ImmutableMap.of());
        assertEquals(marketState.isOpenNow(), false);
        assertEquals(marketState.isOpenThisDay(), false);
        assertEquals(marketState.getExtendedOpenTime().isPresent(), false);
        assertEquals(marketState.getExtendedCloseTime().isPresent(), false);
    }

    @Test
    public void testIsOpenNow_marketDataPresentMarketClosed_expectMarketClosedDataOk() {
        final MarketState marketState = MarketStateTestUtils.createMarketState(new DateTime(), false);
        assertEquals(marketState.isOpenNow(), false);
        assertEquals(marketState.isOpenThisDay(), false);
        assertEquals(marketState.getExtendedOpenTime().isPresent(), false);
        assertEquals(marketState.getExtendedCloseTime().isPresent(), false);
    }

    @Test
    public void testIsOpenNow_marketDataPresentMarketOpen_expectMarketOpenDataOk() {
        final MarketState marketState = MarketStateTestUtils.createMarketState(new DateTime(), true);
        assertEquals(marketState.isOpenNow(), true);
        assertEquals(marketState.isOpenThisDay(), true);
        assertEquals(marketState.getExtendedOpenTime().isPresent(), true);
        assertEquals(marketState.getExtendedCloseTime().isPresent(), true);
    }

}
