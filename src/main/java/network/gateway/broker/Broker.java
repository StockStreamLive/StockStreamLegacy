package network.gateway.broker;


import data.*;
import exception.RobinhoodException;
import org.joda.time.DateTime;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Broker {

    Set<Instrument> getAllInstruments();

    float getBuyingPower() throws RobinhoodException;

    Collection<Asset> getOwnedAssets() throws RobinhoodException;

    Collection<Order> getRecentOrders() throws RobinhoodException;

    List<EquityHistorical> getHistoricalValues() throws RobinhoodException;

    List<Map<String, String>> getQuotes(final Collection<String> symbols) throws RobinhoodException;

    Map<String, String> getQuote(final String symbol) throws RobinhoodException;

    OrderStatus buyShares(final String symbol, final int shares) throws RobinhoodException;

    OrderStatus sellShares(final String symbol, final int shares) throws RobinhoodException;

    MarketState getMarketStateForDate(final DateTime dateTime) throws RobinhoodException;
}
