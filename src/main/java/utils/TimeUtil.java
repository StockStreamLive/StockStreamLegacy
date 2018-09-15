package utils;

import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;

@Slf4j
public class TimeUtil {

    public static Optional<DateTime> createDateFromStr(final String format, final String strDate) {
        DateFormat dateFormat = new SimpleDateFormat(format, Locale.ENGLISH);

        try {
            return Optional.of(new DateTime(dateFormat.parse(strDate)));
        } catch (ParseException e) {
            log.warn(e.getMessage(), e);
        }

        return Optional.empty();
    }

    public static Optional<DateTime> createDateFromStr(final String format, final String strDate, final String timezone) {
        DateFormat dateFormat = new SimpleDateFormat(format, Locale.ENGLISH);
        dateFormat.setTimeZone(TimeZone.getTimeZone(timezone));

        try {
            return Optional.of(new DateTime(dateFormat.parse(strDate)));
        } catch (ParseException e) {
            log.warn(e.getMessage(), e);
        }

        return Optional.empty();
    }

}
