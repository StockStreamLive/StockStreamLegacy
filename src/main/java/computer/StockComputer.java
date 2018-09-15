package computer;

import cache.InstrumentCache;
import data.Quote;
import exception.RobinhoodException;
import network.gateway.broker.Broker;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class StockComputer {

    @Autowired
    private Broker broker;

    @Autowired
    private InstrumentCache instrumentCache;

    public StockComputer() {
    }

    public boolean isSymbol(final String token) {
        String tokenToCheck = token;
        if (tokenToCheck.startsWith("$")) {
            tokenToCheck = tokenToCheck.substring(1);
        }

        return this.instrumentCache.getValidSymbols().contains(tokenToCheck);
    }

    public Map<String, Quote> loadSymbolToQuote(final Set<String> symbols) throws RobinhoodException {
        final Map<String, Quote> symbolToQuote = new ConcurrentHashMap<>();

        final List<Map<String, String>> quotes = this.broker.getQuotes(symbols);
        quotes.forEach(entry -> {
            final Quote quote = new Quote(entry);
            symbolToQuote.put(quote.getSymbol(), quote);
        });

        return symbolToQuote;
    }

}
