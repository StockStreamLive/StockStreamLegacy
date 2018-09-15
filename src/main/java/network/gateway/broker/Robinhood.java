package network.gateway.broker;

import application.Config;
import cache.InstrumentCache;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import computer.TimeComputer;
import data.*;
import exception.RobinhoodException;
import logic.Scheduler;
import lombok.extern.slf4j.Slf4j;
import network.http.HTTPQuery;
import network.http.HTTPResult;
import network.http.HTTPUtil;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import spark.utils.CollectionUtils;
import spark.utils.StringUtils;
import utils.JSONUtil;
import utils.MathUtil;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;

@Slf4j
public class Robinhood implements Broker {
    private static final List<String> REQUIRED_ENDPOINT_KEYS = ImmutableList.of("positions", "portfolio", "account");

    private final HashMap<String, String> headers = new HashMap<String, String>() {{
        put("Accept", "*/*");
        put("Accept-Encoding", "gzip, deflate");
        put("Accept-Language", "en;q=1, fr;q=0.9, de;q=0.8, ja;q=0.7, nl;q=0.6, it;q=0.5");
        put("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
        put("X-Robinhood-API-Version", "1.70.0");
        put("Connection", "keep-alive");
        put("User-Agent", "Robinhood/823 (iPhone; iOS 7.1.2; Scale/2.00)");
    }};

    private final HashMap<String, String> endpoints = new HashMap<String, String>() {{
        put("login", "https://api.robinhood.com/api-token-auth/");
        put("investment_profile", "https://api.robinhood.com/user/investment_profile/");
        put("accounts", "https://api.robinhood.com/accounts/");
        put("ach_iav_auth", "https://api.robinhood.com/ach/iav/auth/");
        put("ach_relationships", "https://api.robinhood.com/ach/relationships/");
        put("ach_transfers", "https://api.robinhood.com/ach/transfers/");
        put("applications", "https://api.robinhood.com/applications/");
        put("dividends", "https://api.robinhood.com/dividends/");
        put("edocuments", "https://api.robinhood.com/documents/");
        put("instruments", "https://api.robinhood.com/instruments/");
        put("margin_upgrades", "https://api.robinhood.com/margin/upgrades/");
        put("markets", "https://api.robinhood.com/markets/");
        put("notifications", "https://api.robinhood.com/notifications/");
        put("orders", "https://api.robinhood.com/orders/");
        put("password_reset", "https://api.robinhood.com/password_reset/request/");
        put("quotes", "https://api.robinhood.com/quotes/");
        put("document_requests", "https://api.robinhood.com/upload/document_requests/");
        put("user", "https://api.robinhood.com/user/");
        put("user/additional_info", "https://api.robinhood.com/user/additional_info/");
        put("user/basic_info", "https://api.robinhood.com/user/basic_info/");
        put("user/employment", "https://api.robinhood.com/user/employment/");
        put("user/investment_profile", "https://api.robinhood.com/user/investment_profile/");
        put("watchlists", "https://api.robinhood.com/watchlists/");
    }};

    @Autowired
    private InstrumentCache instrumentCache;

    @Autowired
    private HTTPUtil httpUtil;

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private TimeComputer timeComputer;

    private String username;
    private String password;

    public Robinhood(final String username, final String password) {
        this.username = username;
        this.password = password;
    }

    private boolean isLoggedIn() {
        if (!headers.containsKey("Authorization")) {
            return false;
        }

        for (final String key : REQUIRED_ENDPOINT_KEYS) {
            if (!endpoints.containsKey(key)) {
                return false;
            }
        }
        return true;
    }

    private void doLoginRoutine() {
        try {
            this.doLogin();
            this.acquireAccountInfo();
            log.info("Robinhood logged in.");
        } catch (final Exception e) {
            log.warn("Unable to log into Robinhood because " + e.getMessage(), e);
        }
    }

    private void verifyLoginStatus() throws RobinhoodException {
        if (!isLoggedIn()) {
            doLoginRoutine();
        }
        if (!isLoggedIn()) {
            throw new RobinhoodException("Login failed!");
        }
    }

    private void doLogin() throws RobinhoodException {
        log.info("Logging in to Robinhood.");

        Optional<HTTPResult> httpResult =
                this.httpUtil.executeHTTPPostRequest(new HTTPQuery(endpoints.get("login"), ImmutableMap.of("username", username, "password", password), headers));

        if (!httpResult.isPresent()) {
            throw new RobinhoodException("Bad response from Robinhood.");
        }

        final String jsonData = httpResult.get().getBody();

        Optional<Map<String, String>> responseMap =
                JSONUtil.deserializeString(jsonData, new TypeReference<Map<String, String>>() {});

        if (!responseMap.isPresent()) {
            throw new RobinhoodException(String.format("Unable to deserialize response of [%s]", httpResult));
        }

        headers.put("Authorization", "Token " + responseMap.get().get("token"));
    }

    private void acquireAccountInfo() throws RobinhoodException {
        log.info("Acquiring account info.");

        final Optional<HTTPResult> getResponse = this.httpUtil.executeHTTPGetRequest(new HTTPQuery(endpoints.get("accounts"), ImmutableMap.of(), headers));

        if (!getResponse.isPresent()) {
            throw new RobinhoodException("Bad response from Robinhood.");
        }

        Optional<Map<String, List<Map<String, Object>>>> callResults =
                JSONUtil.deserializeString(getResponse.get().getBody(), new TypeReference<Map<String, List<Map<String, Object>>>>() {});

        if (!callResults.isPresent()) {
            throw new RobinhoodException(String.format("Unable to deserialize response of [%s]", getResponse));
        }

        Map<String, List<Map<String, Object>>> accountsMap = callResults.get();

        final String positionsUrlStr = (String) accountsMap.get("results").get(0).get("positions");
        final String portfolioUrlStr = (String) accountsMap.get("results").get(0).get("portfolio");
        final String accountUrlStr = (String) accountsMap.get("results").get(0).get("url");

        final String accountNumber = (String) accountsMap.get("results").get(0).get("account_number");
        final String portfolioHistoricals = "https://api.robinhood.com/portfolios/historicals/" + accountNumber;

        endpoints.put("positions", positionsUrlStr);
        endpoints.put("portfolio", portfolioUrlStr);
        endpoints.put("portfolios_historicals", portfolioHistoricals);
        endpoints.put("account", accountUrlStr);
    }

    @VisibleForTesting
    protected double calculateBuyOrderCeiling(final Quote quote) throws RobinhoodException {
        // https://archive.fo/hf0rt
        float mostRecentPrice = quote.getMostRecentPrice();

        double ceilingPercentage = .05d;
        if (timeComputer.isAfterHours()) {
            ceilingPercentage = .01d;
        }

        return mostRecentPrice + (mostRecentPrice * ceilingPercentage);
    }

    @VisibleForTesting
    protected double calculateSellOrderFloor(final Quote quote) throws RobinhoodException {
        float mostRecentPrice = quote.getMostRecentPrice();

        double floorPercentage = .03d;
        if (timeComputer.isAfterHours()) {
            floorPercentage = .01d;
        }

        return mostRecentPrice - (mostRecentPrice * floorPercentage);
    }

    private OrderStatus placeOrder(final String symbol, final int shares, final String side) throws RobinhoodException {
        final Instrument instrument = this.instrumentCache.getSymbolToInstrument().get(symbol);

        final Map<String, String> rhQuote = getQuote(symbol);
        final Quote quote = new Quote(rhQuote);

        String orderType = "market";

        double orderLimit = quote.getMostRecentPrice();
        if ("buy".equalsIgnoreCase(side)) {
            orderLimit = calculateBuyOrderCeiling(quote);
            orderType = "limit";
        } else if ("sell".equalsIgnoreCase(side)) {
            orderLimit = calculateSellOrderFloor(quote);
            orderType = "market";
        }

        final double cashBalance = getBuyingPower();
        if ("buy".equalsIgnoreCase(side) && orderLimit > cashBalance) {
            return OrderStatus.CANT_AFFORD;
        }

        orderLimit = MathUtil.roundToTick(orderLimit, instrument.getMin_tick_size());


        DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.CEILING);
        String maxPriceStr = df.format(orderLimit);

        log.info("Placing limit order of {} with a limit of {} and a cash balance of {}", side, maxPriceStr, cashBalance);

        Map<String, String> params =
                ImmutableMap.<String, String>builder().put("account", endpoints.get("account"))
                                                      .put("extended_hours", "true")
                                                      .put("instrument", instrument.getUrl())
                                                      .put("side", side)
                                                      .put("quantity", String.valueOf(shares))
                                                      .put("symbol", symbol)
                                                      .put("time_in_force", "gfd")
                                                      .put("trigger", "immediate")
                                                      .put("price", maxPriceStr)
                                                      .put("type", orderType)
                                                      .build();

        log.info("Placing order {} {} {}", endpoints.get("orders"), params, headers);

        Optional<HTTPResult> httpResult =
                this.httpUtil.executeHTTPPostRequest(new HTTPQuery(endpoints.get("orders"), params, headers));

        if (!httpResult.isPresent()) {
            throw new RobinhoodException("Bad response from Robinhood.");
        }

        final String jsonData = httpResult.get().getBody();

        log.info("From order got response {}", jsonData);

        JSONObject jsonObj = new JSONObject(jsonData);

        if (jsonObj.has("non_field_errors")) {
            log.warn("Got error when executing command {} : {}",
                     jsonObj.get("non_field_errors"), side + " -> " + symbol);
            throw new RobinhoodException("Bad response from Robinhood.");
        }

        if (jsonObj.has("detail")) {
            if (jsonObj.getString("detail").startsWith("You can only purchase")) {
                return OrderStatus.CANT_AFFORD;
            }
            throw new RobinhoodException(String.format("Got unexpected response from Robinhood [%s]", jsonObj));
        }

        if (!jsonObj.isNull("reject_reason")) {
            throw new RobinhoodException(String.format("Got unexpected response from Robinhood [%s]", jsonObj));
        }

        this.scheduler.notifyEvent(Event.ORDER_PLACED);

        return OrderStatus.OK;
    }

    @Override
    public OrderStatus buyShares(final String symbol, final int shares) throws RobinhoodException {
        verifyLoginStatus();
        return placeOrder(symbol, shares, "buy");
    }

    @Override
    public OrderStatus sellShares(final String symbol, final int shares) throws RobinhoodException {
        verifyLoginStatus();
        return placeOrder(symbol, shares, "sell");
    }

    public List<EquityHistorical> getHistoricalValues() throws RobinhoodException {
        verifyLoginStatus();

        final List<EquityHistorical> historicalValues = new ArrayList<>();

        Optional<HTTPResult> httpResult =
                this.httpUtil.executeHTTPGetRequest(new HTTPQuery(endpoints.get("portfolios_historicals"),
                                                                  ImmutableMap.of("span", "day", "interval", "5minute", "bounds", "extended"), headers));

        final JSONObject jsonObj = new JSONObject(httpResult.get().getBody());

        final JSONArray equitiesListObj = jsonObj.getJSONArray("equity_historicals");

        for (final Object historicalObj : equitiesListObj) {
            if (!(historicalObj instanceof JSONObject)) {
                continue;
            }

            final Optional<EquityHistorical> equityOptional = JSONUtil.deserializeObject(historicalObj.toString(), EquityHistorical.class);

            if (!equityOptional.isPresent()) {
                continue;
            }

            historicalValues.add(equityOptional.get());
        }

        return historicalValues;
    }

    @Override
    public List<Map<String, String>> getQuotes(final Collection<String> symbols) throws RobinhoodException {
        verifyLoginStatus();

        if (CollectionUtils.isEmpty(symbols)) {
            return Collections.emptyList();
        }

        Map<String, String> params = ImmutableMap.of("symbols", Joiner.on(',').join(symbols));
        Optional<HTTPResult> httpResult = this.httpUtil.executeHTTPGetRequest(new HTTPQuery(endpoints.get("quotes"), params, headers));

        if (!httpResult.isPresent()) {
            throw new RobinhoodException("Bad response from Robinhood.");
        }

        final String jsonData = httpResult.get().getBody();

        final Optional<Map<String, List<Map<String, String>>>> mapResult =
                JSONUtil.deserializeString(jsonData, new TypeReference<Map<String, List<Map<String, String>>>>() {});

        if (!mapResult.isPresent()) {
            throw new RobinhoodException(String.format("Unable to deserialize response of [%s]", jsonData));
        }

        final List<Map<String, String>> quotes = mapResult.get().get("results");

        quotes.removeIf(Objects::isNull);

        return quotes;
    }

    @Override
    public Map<String, String> getQuote(final String symbol) throws RobinhoodException {
        verifyLoginStatus();

        final Optional<HTTPResult> httpResult =
                this.httpUtil.executeHTTPGetRequest(new HTTPQuery(endpoints.get("quotes"), ImmutableMap.of("symbols", symbol), headers));

        if (!httpResult.isPresent()) {
            throw new RobinhoodException("Bad response when getting quotes!");
        }

        final String jsonData = httpResult.get().getBody();

        final Optional<Map<String, List<Map<String, String>>>> mapResult =
                JSONUtil.deserializeString(jsonData, new TypeReference<Map<String, List<Map<String, String>>>>() {});

        if (!mapResult.isPresent()) {
            throw new RobinhoodException(String.format("Unable to deserialize response of [%s]", jsonData));
        }

        return mapResult.get().get("results").get(0);
    }

    private Optional<String> getInstrumentFromURL(final String instrumentURL) {
        /*final Optional<Map<String, String>> instrumentInfo =
                HTTPUtil.retrieveEndpointInfo(instrumentURL, headers, new TypeReference<Map<String, String>>() {});

        if (!instrumentInfo.isPresent()) {
            return Optional.empty();
        }

        return Optional.ofNullable(instrumentInfo.get().get("symbol"));*/
        return Optional.empty();
    }

    @Override
    public float getBuyingPower() throws RobinhoodException {
        verifyLoginStatus();

        final Optional<HTTPResult> httpResult =
                this.httpUtil.executeHTTPGetRequest(new HTTPQuery(endpoints.get("account"), ImmutableMap.of(), headers));

        if (!httpResult.isPresent()) {
            throw new RobinhoodException("Bad response from Robinhood.");
        }

        JSONObject jsonObj = new JSONObject(httpResult.get().getBody());

        if (jsonObj.isNull("buying_power")) {
            throw new RobinhoodException(String.format("buying_power not in Robinhood response of [%s]", jsonObj));
        }

        //final String buyingPowerStr = jsonObj.getString("buying_power");

        float marginCash = 0f;

        if (!jsonObj.isNull("margin_balances")) {
            final JSONObject marginBalances = jsonObj.getJSONObject("margin_balances");

            if (!marginBalances.isNull("unallocated_margin_cash")) {
                final String unallocatedMarginCash = marginBalances.getString("unallocated_margin_cash");
                marginCash += Float.parseFloat(unallocatedMarginCash);
            }

        }

        //final Float buyingPower = Float.parseFloat(buyingPowerStr) + marginCash;

        log.info("Got buying power of {}", marginCash);

        return marginCash - Config.GOLD_POWER;
    }

    @Override
    public Set<Instrument> getAllInstruments() {

        String instrumentsURL = endpoints.get("instruments");

        final Set<Instrument> allInstruments = new HashSet<>();

        while (instrumentsURL != null) {
            log.info("Getting all instruments, have {} so far.", allInstruments.size());

            final Optional<HTTPResult> callResults =
                    this.httpUtil.executeHTTPGetRequest(new HTTPQuery(instrumentsURL, ImmutableMap.of(), headers));

            if (!callResults.isPresent()) {
                break;
            }

            final JSONObject callResult = new JSONObject(callResults.get().getBody());

            final JSONArray instrumentsListObj = callResult.getJSONArray("results");

            for (final Object instrumentObj : instrumentsListObj) {
                if (!(instrumentObj instanceof JSONObject)) {
                    continue;
                }

                Optional<Instrument> instrumentOptional = JSONUtil.deserializeObject(instrumentObj.toString(), Instrument.class);

                if (!instrumentOptional.isPresent()) {
                    log.warn("Could not deserialize instrument {}", instrumentObj);
                    continue;
                }

                allInstruments.add(instrumentOptional.get());
            }

            instrumentsURL = String.valueOf(callResult.get("next"));
        }

        return allInstruments;
    }

    @Override
    public Collection<Asset> getOwnedAssets() throws RobinhoodException {
        verifyLoginStatus();

        log.info("Getting owned assets.");

        final Optional<HTTPResult> httpResult =
                this.httpUtil.executeHTTPGetRequest(new HTTPQuery(endpoints.get("positions"), ImmutableMap.of(), headers));

        if (!httpResult.isPresent()) {
            throw new RobinhoodException("Bad response from Robinhood.");
        }

        final String jsonData = httpResult.get().getBody();

        final Optional<Map<String, List<Map<String, String>>>> responseObject =
                JSONUtil.deserializeString(jsonData, new TypeReference<Map<String, List<Map<String, String>>>>() {});

        final List<Asset> ownedAssets = new ArrayList<>();

        if (!responseObject.isPresent()) {
            return ownedAssets;
        }

        final List<Map<String, String>> ownedInstruments = responseObject.get().get("results");

        Collection<String> symbols = new ArrayList<>();
        for (final Map<String, String> ownedInstrument : ownedInstruments) {
            if (!ownedInstrument.containsKey("instrument")) {
                continue;
            }
            final Instrument instrument = this.instrumentCache.getUrlToInstrument().get(ownedInstrument.get("instrument"));
            if (instrument == null || StringUtils.isEmpty(instrument.getSymbol())) {
                continue;
            }
            symbols.add(instrument.getSymbol());
        }

        List<Map<String,String>> quotes = getQuotes(symbols);

        Map<String, Map<String, String>> instrumentURLToQuote = new HashMap<>();
        quotes.forEach(quote -> instrumentURLToQuote.put(quote.get("instrument"), quote));

        for (final Map<String, String> ownedInstrument : ownedInstruments) {
            final double shares = Double.parseDouble(ownedInstrument.get("quantity"));
            final double avgBuyPrice = Double.parseDouble(ownedInstrument.get("average_buy_price"));

            if (shares <= 0) {
                continue;
            }

            final String instrumentURL = ownedInstrument.get("instrument");
            final Instrument instrument = this.instrumentCache.getUrlToInstrument().get(instrumentURL);

            if (instrument == null) {
                continue;
            }

            if (!instrumentURLToQuote.containsKey(instrumentURL)) {
                continue;
            }

            final String symbol = instrument.getSymbol();

            final Quote quote = new Quote(instrumentURLToQuote.get(instrumentURL));

            ownedAssets.add(new Asset(symbol, Math.toIntExact((long) shares), avgBuyPrice, quote));
        }

        log.info("Got {} assets from Robinhood.", ownedAssets.size());

        return ownedAssets;
    }

    @Override
    public Collection<Order> getRecentOrders() throws RobinhoodException {
        final Optional<HTTPResult> getResult = this.httpUtil.executeHTTPGetRequest(new HTTPQuery(endpoints.get("orders"), ImmutableMap.of(), headers));

        if (!getResult.isPresent()) {
            throw new RobinhoodException("Exception getting order history!");
        }

        final Collection<Order> allOrders = new ConcurrentHashSet<>();

        final JSONObject callResult = new JSONObject(getResult.get().getBody());
        final JSONArray ordersListObj = callResult.getJSONArray("results");

        for (final Object ordersObj : ordersListObj) {
            if (!(ordersObj instanceof JSONObject)) {
                continue;
            }

            Optional<Order> order = JSONUtil.deserializeObject(ordersObj.toString(), Order.class);

            if (!order.isPresent()) {
                log.warn("Could not deserialize order {}", ordersObj);
                continue;
            }

            allOrders.add(order.get());
        }

        return allOrders;
    }

    public MarketState getMarketStateForDate(final DateTime dateTime) throws RobinhoodException {
        verifyLoginStatus();

        final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd");
        final String dateString = formatter.print(dateTime);
        final String url = String.format("%s%s%s", endpoints.get("markets"), "XNAS/hours/", dateString);

        final Optional<HTTPResult> getResult = this.httpUtil.executeHTTPGetRequest(new HTTPQuery(url, ImmutableMap.of(), headers));

        if (!getResult.isPresent()) {
            throw new RobinhoodException("Bad response from Robinhood.");
        }

        final Optional<Map<String, String>> responseObject =
                JSONUtil.deserializeString(getResult.get().getBody(), new TypeReference<Map<String, String>>() {});

        if (!responseObject.isPresent()) {
            throw new RobinhoodException(String.format("Unable to deserialize response of [%s]", getResult));
        }

        return new MarketState(responseObject.get());
    }

}
