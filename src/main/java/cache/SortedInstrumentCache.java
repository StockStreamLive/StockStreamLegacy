package cache;


import data.Instrument;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;
import org.springframework.beans.factory.annotation.Autowired;
import utils.TextUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class SortedInstrumentCache extends ListenerAdapter {

    @Data
    @AllArgsConstructor
    public class ScoredInstrument {
        private Instrument instrument;

        // symbol = 1, word = .05 * wordlen
        private Double score;

        @Override
        public int hashCode() {
            int hashcode = new HashCodeBuilder().append(instrument.getSymbol()).toHashCode();
            return hashcode;
        }

        @Override
        public boolean equals(final Object object) {
            if (!(object instanceof Instrument)) {
                return false;
            }
            final Instrument otherInstrument = (Instrument) object;

            return instrument.getSymbol().equals(otherInstrument.getSymbol());
        }
    }

    static class ScoredInstrumentComparator implements Comparator<ScoredInstrument> {

        @Override
        public int compare(final ScoredInstrument scoredInstrument1, final ScoredInstrument scoredInstrument2) {
            if (scoredInstrument1.getInstrument().getSymbol().equals(scoredInstrument2.getInstrument().getSymbol())) {
                return 0;
            }

            if (scoredInstrument2.getScore() > scoredInstrument1.getScore()) {
                return 1;
            } else {
                return -1;
            }
        }

    }

    @Autowired
    private InstrumentCache instrumentCache;

    private final Map<String, ScoredInstrument> candidateTokenMap = new ConcurrentHashMap<>();
    private final Set<ScoredInstrument> candidateTokens = new TreeSet<>(new ScoredInstrumentComparator());

    public SortedInstrumentCache() {
    }

    public Set<ScoredInstrument> getCandidateTokens() {
        return Collections.unmodifiableSet(candidateTokens);
    }

    @Override
    public void onMessage(final MessageEvent event) {
        log.debug("processing message {}", event.getMessage());

        final String token = event.getMessage();

        final Map<Instrument, MutableDouble> additionalScoreValues = new HashMap<>();

        final String[] items = token.split(" ");
        for (final String item : items) {
            if (item.equalsIgnoreCase("!buy") || item.equalsIgnoreCase("!sell") || item.equalsIgnoreCase("!hold")) {
                continue;
            }

            final String lowerCase = TextUtil.stripNormalPunctuation(item.toLowerCase());

            final Instrument instrument = this.instrumentCache.getSymbolToInstrument().get(lowerCase.toUpperCase());
            if (instrument != null) {
                additionalScoreValues.computeIfAbsent(instrument, doub -> new MutableDouble(0)).add(1);
                continue;
            }

            final Set<Instrument> relatedInstruments = this.instrumentCache.getWordToInstrument().get(lowerCase);
            if (relatedInstruments == null) {
                continue;
            }

            relatedInstruments.forEach(instr -> additionalScoreValues.computeIfAbsent(instr, doub -> new MutableDouble(0)).add(.05 * lowerCase.length()));
        }

        additionalScoreValues.forEach((key, value) -> updateInstrumentScore(key, value.getValue()));
    }

    private synchronized void updateInstrumentScore(final Instrument instrument, final double additionalScore) {
        ScoredInstrument candidateToken =
                candidateTokenMap.computeIfAbsent(instrument.getSymbol(), comm -> new ScoredInstrument(instrument, 0d));
        candidateTokens.removeIf(cc -> cc.getInstrument().getSymbol().equals(instrument.getSymbol()));
        candidateToken.setScore(candidateToken.getScore() + additionalScore);
        candidateTokens.add(candidateToken);
    }


    public synchronized void reset() {
        candidateTokens.clear();
        candidateTokenMap.clear();
    }

}
