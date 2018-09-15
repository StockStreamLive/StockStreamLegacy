package data;


import lombok.AllArgsConstructor;
import lombok.Data;
import utils.MathUtil;

@Data
@AllArgsConstructor
public class Asset {
    private String symbol;
    private int shares;
    private double avgBuyPrice;
    private Quote quote;

    public Asset(final Asset otherAsset) {
        symbol = otherAsset.symbol;
        shares = otherAsset.shares;
        avgBuyPrice = otherAsset.avgBuyPrice;
        quote = new Quote(quote);
    }

    public double getPctReturn() {
        if (avgBuyPrice <= 0) {
            return 100;
        }

        final float mostRecentPrice = quote.getMostRecentPrice();

        return MathUtil.computePercentChange(avgBuyPrice, mostRecentPrice);
    }

    public double getAssetValue() {
        final double mostRecentPrice = quote.getMostRecentPrice();

        return shares * mostRecentPrice;
    }

    @Override
    public String toString() {
        return String.valueOf(shares) + " x " + "$" + symbol + " @ " + String.valueOf(avgBuyPrice);
    }
}
