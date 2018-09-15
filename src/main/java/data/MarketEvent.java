package data;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Objects;

@Data
@AllArgsConstructor
public class MarketEvent {

    public static final MarketEvent MARKET_OPEN_EVENT = new MarketEvent(Status.OPEN, 0, 0);
    public static final MarketEvent MARKET_CLOSE_EVENT = new MarketEvent(Status.CLOSE, 0, 0);

    public enum Status {
        OPEN,
        CLOSE
    }

    private final Status status;
    private final int offsetMinutes;
    private final int offsetSeconds;


    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(status).append(offsetMinutes).build();
    }

    @Override
    public boolean equals(final Object otherObject) {
        if (!(otherObject instanceof MarketEvent)) {
            return false;
        }
        final MarketEvent otherEvent = (MarketEvent) otherObject;
        return Objects.equals(otherEvent.getOffsetMinutes(), otherEvent.getOffsetMinutes()) &&
                Objects.equals(otherEvent.getStatus(), otherEvent.getStatus());
    }

    @Override
    public String toString() {
        return String.format("%s.%s.%s", status, offsetMinutes, offsetSeconds);
    }
}
