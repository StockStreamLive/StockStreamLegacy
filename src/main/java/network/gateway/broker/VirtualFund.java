package network.gateway.broker;

import com.google.common.collect.ImmutableMap;
import data.*;
import exception.RobinhoodException;
import logic.Scheduler;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;


@Slf4j
public class VirtualFund implements Broker {
    private static final int BUFFER = 50;
    private static final int MIN_BALANCE = 25000;

    @Autowired
    private Robinhood robinhood;

    @Autowired
    private Scheduler scheduler;

    private final Random random = new Random();
    private final Map<String, Asset> assets = new HashMap<>();
    private double balance = 50000.0;

    public VirtualFund() {
        //buyShares("SPY", 1);
        //buyShares("FB", 1);
        //buyShares("GOOG", 1);
        //buyShares("MSFT", 1);
        //buyShares("AMZN", 1);
        //buyShares("NFLX", 1);
        //buyShares("SGYP", 1);
        //buyShares("AAPL", 1);
        //buyShares("JNUG", 1);
    }

    @Override
    public float getBuyingPower() {
        return (float) balance;
    }

    @Override
    public Collection<Asset> getOwnedAssets() {
        assets.values().forEach(asset -> asset.setAvgBuyPrice(random.nextFloat() * 250));
        return Collections.unmodifiableCollection(assets.values());
    }

    @Override
    public Collection<Order> getRecentOrders() throws RobinhoodException {
        return Collections.emptyList();
    }

    @Override
    public List<EquityHistorical> getHistoricalValues() throws RobinhoodException {
        return robinhood.getHistoricalValues();
    }

    @Override
    public List<Map<String, String>> getQuotes(final Collection<String> symbols) throws RobinhoodException {
        final List<Map<String, String>> quotes = robinhood.getQuotes(symbols);

        for (final Map<String, String> robinhoodQuote : quotes) {
            final float quote = Float.parseFloat(robinhoodQuote.get("last_trade_price"));
            final float newQuote = quote + random.nextFloat();
            robinhoodQuote.put("last_trade_price", String.valueOf(newQuote));
            robinhoodQuote.put("last_extended_hours_trade_price", String.valueOf(newQuote));
        }

        return quotes;
    }

    @Override
    public Map<String, String> getQuote(final String symbol) throws RobinhoodException {
        return robinhood.getQuote(symbol);
    }

    @Override
    public OrderStatus buyShares(final String symbol, final int shares) throws RobinhoodException {
        final String commandExecuted = "sell " + symbol;

        float price = random.nextFloat() * 500;

        //////////////////////////
        if ((balance - price) <= MIN_BALANCE + BUFFER) {
            return OrderStatus.CANT_AFFORD;
        }
        //////////////////////////

        Map<String, String> rhQuote = robinhood.getQuote(symbol);

        final Quote quote = new Quote(rhQuote);

        Asset asset = assets.computeIfAbsent(symbol,
                newAsset -> new Asset(symbol, 0, 0.0f, quote));

        double currentTotalPrice = asset.getShares() * asset.getAvgBuyPrice();

        asset.setShares(asset.getShares() + shares);
        this.balance -= (price * shares);

        double newTotalPrice = (price * shares) + currentTotalPrice;
        double newAverage = newTotalPrice / asset.getShares();

        asset.setAvgBuyPrice(newAverage);

        this.scheduler.notifyEvent(Event.ORDER_PLACED);

        return OrderStatus.OK;
    }

    @Override
    public OrderStatus sellShares(final String symbol, final int shares) {
        float price = random.nextFloat() * 100;

        if (!assets.containsKey(symbol)) {
            return OrderStatus.NO_SHARES;
        }

        Asset asset = assets.get(symbol);
        asset.setShares(asset.getShares() - 1);

        if (asset.getShares() <= 0) {
            assets.remove(symbol);
        }

        this.balance += price;

        this.scheduler.notifyEvent(Event.ORDER_PLACED);

        return OrderStatus.OK;
    }

    @Override
    public MarketState getMarketStateForDate(final DateTime inputTime) throws RobinhoodException {
        final boolean openThisDay = true;/*
                !(inputTime.getDayOfWeek() == DateTimeConstants.SATURDAY || inputTime.getDayOfWeek() == DateTimeConstants.SUNDAY);*/
        final DateTime marketOpen = new DateTime().withDayOfYear(inputTime.getDayOfYear())
                .withHourOfDay(0).withMinuteOfHour(0);
        final DateTime marketClose = new DateTime().withDayOfYear(inputTime.getDayOfYear())
                .withHourOfDay(23).withMinuteOfHour(55);

        final MarketState marketState =
                new MarketState(ImmutableMap.of("is_open", String.valueOf(openThisDay),
                        "extended_opens_at", marketOpen.toString(ISODateTimeFormat.dateTimeNoMillis()),
                        "extended_closes_at", marketClose.toString(ISODateTimeFormat.dateTimeNoMillis())));
        return marketState;
    }

    public Set<Instrument> getAllInstruments() {
        return robinhood.getAllInstruments();
    }
}