package data;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
@AllArgsConstructor
public class Quote {

    private Map<String, String> robinhoodQuote;

    public Quote(final Quote otherQuote) {
        robinhoodQuote = new ConcurrentHashMap<>(otherQuote.getRobinhoodQuote());
    }

    public float getMostRecentPrice() {
        final String lastTradeAfterHoursStr = robinhoodQuote.getOrDefault("last_extended_hours_trade_price", "0.0");
        final String lastTradeStr = robinhoodQuote.getOrDefault("last_trade_price", "0.0");

        Float mostRecentPrice = Float.parseFloat(lastTradeStr);
        if (lastTradeAfterHoursStr != null && !("0.0".equals(lastTradeAfterHoursStr))) {
            mostRecentPrice = Float.parseFloat(lastTradeAfterHoursStr);
        }

        return mostRecentPrice;
    }

    public float getPercentChange() {
        final String prevCloseStr = robinhoodQuote.getOrDefault("previous_close", "0.0");

        final float prevClose = Float.valueOf(prevCloseStr);
        final float lastTrade = getMostRecentPrice();


        final float change = lastTrade - prevClose;
        final float percentReturn = change / lastTrade * 100;

        return percentReturn;
    }


    public String getSymbol() {
        return robinhoodQuote.get("symbol");
    }
}
