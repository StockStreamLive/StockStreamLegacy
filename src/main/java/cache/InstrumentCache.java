package cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import data.Instrument;
import logic.Scheduler;
import lombok.extern.slf4j.Slf4j;
import network.gateway.broker.Broker;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import spark.utils.StringUtils;
import utils.JSONUtil;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class InstrumentCache {
    private static final String INSTRUMENTS_FILE = "instruments.json";

    private static final Set<String> RESTRICTED_SYMBOLS = ImmutableSet.of();

    private final ConcurrentHashSet<String> validSymbols = new ConcurrentHashSet<>();
    private final ConcurrentHashMap<String, Instrument> urlToInstrument = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Instrument> symbolToInstrument = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<Instrument>> wordToInstrument = new ConcurrentHashMap<>();

    @Autowired
    private Broker broker;

    @Autowired
    private Scheduler scheduler;

    public InstrumentCache() {
    }

    @PostConstruct
    public void init() {
        new Thread(this::loadLocalInstruments).start();
        scheduler.scheduleJob(this::loadRemoteInstruments, 1, 1, TimeUnit.DAYS);
    }

    private void loadLocalInstruments() {
        String jsonInstruments = null;

        try {
            jsonInstruments = new Scanner(new File(INSTRUMENTS_FILE)).useDelimiter("\\Z").next();
        } catch (final Exception e) {
            log.warn(e.getMessage(), e);
        }

        if (StringUtils.isEmpty(jsonInstruments)) {
            loadRemoteInstruments();
            return;
        }

        Optional<Map<Long,List<Instrument>>> instrumentMapOptional =
                JSONUtil.deserializeString(jsonInstruments, new TypeReference<Map<Long, List<Instrument>>>() { });

        if (!instrumentMapOptional.isPresent()) {
            loadRemoteInstruments();
            return;
        }

        Map<Long,List<Instrument>> dateToInstruments = instrumentMapOptional.get();

        updateSymbolMaps(dateToInstruments.values().iterator().next());

        final DateTime refreshDate = new DateTime(dateToInstruments.keySet().iterator().next());
        if (refreshDate.plusDays(1).isBeforeNow()) {
            loadRemoteInstruments();
            return;
        }
    }

    private void loadRemoteInstruments() {
        final Set<Instrument> instruments = this.broker.getAllInstruments();

        JSONUtil.saveObjectToFile(INSTRUMENTS_FILE, ImmutableMap.of(new DateTime().getMillis(), instruments));
        updateSymbolMaps(instruments);

        log.info("InstrumentsCache constructed.");
    }

    private synchronized void updateSymbolMaps(final Collection<Instrument> withInstruments) {
        validSymbols.clear();
        urlToInstrument.clear();
        symbolToInstrument.clear();
        wordToInstrument.clear();

        withInstruments.forEach(instrument -> {
            if (RESTRICTED_SYMBOLS.contains(instrument.getSymbol())) {
                log.debug("Ignoring instrument {} because it's restricted.", instrument);
                return;
            }
            if (instrument.getDay_trade_ratio() > .25f) {
                log.debug("Ignoring instrument {} because it's too risky.", instrument);
                return;
            }
            if (!instrument.isTradeable()) {
                log.debug("Ignoring instrument {} because it's not tradeable.", instrument);
                return;
            }
            validSymbols.add(instrument.getSymbol());
            urlToInstrument.put(instrument.getUrl(), instrument);
            symbolToInstrument.put(instrument.getSymbol(), instrument);
            final String[] words = instrument.getName().split(" ");
            for (final String word : words) {
                wordToInstrument.computeIfAbsent(word.toLowerCase(), set -> new ConcurrentHashSet<>()).add(instrument);
            }
        });
    }

    public synchronized Set<String> getValidSymbols() {
        return Collections.unmodifiableSet(validSymbols);
    }

    public synchronized Map<String, Instrument> getUrlToInstrument() {
        return Collections.unmodifiableMap(urlToInstrument);
    }

    public synchronized Map<String, Instrument> getSymbolToInstrument() {
        return Collections.unmodifiableMap(symbolToInstrument);
    }

    public synchronized Map<String, Set<Instrument>> getWordToInstrument() {
        return Collections.unmodifiableMap(wordToInstrument);
    }
}
