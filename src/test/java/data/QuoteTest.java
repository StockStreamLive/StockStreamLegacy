package data;


import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class QuoteTest {

    @Test
    public void testGetMostRecentPrice_noAfterHours_expectMarketClosePrice() {
        final Quote quote = new Quote(ImmutableMap.of("last_trade_price", "2.49"));
        assertEquals(2.49f, quote.getMostRecentPrice(), .01);
    }

    @Test
    public void testGetMostRecentPrice_afterHours_expectAfterHoursPrice() {
        final Quote quote = new Quote(ImmutableMap.of("last_trade_price", "2.49",
                                                      "last_extended_hours_trade_price", "5.25"));
        assertEquals(5.25f, quote.getMostRecentPrice(), .01);
    }

}
