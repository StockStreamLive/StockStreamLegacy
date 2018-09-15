package test;


import com.google.common.collect.ImmutableMap;
import data.MarketState;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import java.util.Map;

public class MarketStateTestUtils {

    public static MarketState createMarketState(final DateTime dateTime, boolean marketOpen) {
        return new MarketState(createRobinhoodDay(dateTime, marketOpen));
    }

    public static Map<String, String> createRobinhoodDay(final DateTime inputTime, final boolean openToday) {
        final DateTime marketOpen = new DateTime().withDayOfYear(inputTime.getDayOfYear())
                                                  .withHourOfDay(0).withMinuteOfHour(0);
        final DateTime marketClose = new DateTime().withDayOfYear(inputTime.getDayOfYear())
                                                   .withHourOfDay(23).withMinuteOfHour(59);

        return ImmutableMap.of("is_open", String.valueOf(openToday),
                               "extended_opens_at", marketOpen.toString(ISODateTimeFormat.dateTimeNoMillis()),
                               "extended_closes_at", marketClose.toString(ISODateTimeFormat.dateTimeNoMillis()));
    }


}
