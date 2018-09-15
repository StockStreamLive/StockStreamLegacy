package data;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Instrument {
    private final String url;
    private final String symbol;
    private final String name;
    private final float day_trade_ratio;
    private final boolean tradeable;
    private final float min_tick_size;
}
