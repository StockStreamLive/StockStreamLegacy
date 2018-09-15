package utils;


import cache.BrokerCache;
import cache.SortedInstrumentCache;
import com.google.common.collect.ImmutableList;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class GameUtil {

    private static Collection<String> errorMessages =
            ImmutableList.of("ERROR: {\"message\": \"kappa_rainbow.gif does not exist!\"}",
                             "ERROR: {\"message\": \"cash me outside, how bout dah?\"}",
                             "ERROR: {\"message\": \"PC Load Letter\"}");

    @Autowired
    private BrokerCache brokerCache;

    @Autowired
    private SortedInstrumentCache sortedInstrumentCache;

    public GameUtil() {
    }

    public String getGenericErrorMessage() {
        return RandomUtil.randomChoice(errorMessages).orElse(errorMessages.iterator().next());
    }

    public Collection<String> findPopularSymbols(final int count) {

        final Set<String> topics = new HashSet<>();

        this.brokerCache.getAssets().forEach(asset -> topics.add(asset.getSymbol()));

        for (final SortedInstrumentCache.ScoredInstrument token : this.sortedInstrumentCache.getCandidateTokens()) {
            if (topics.size() >= count) {
                break;
            }
            topics.add(token.getInstrument().getSymbol());
        }

        return topics;
    }

}
