package application.spring;

import cache.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Import({MetricBeans.class,
         LogicBeans.class})
@Configuration
public class CacheBeans {

    @Autowired
    private MetricBeans metricBeans;

    @Autowired
    private UtilBeans utilBeans;

    @Autowired
    private LogicBeans logicBeans;

    @Autowired
    private FinanceBeans financeBeans;

    @Autowired
    private ComputerBeans computerBeans;

    @Bean
    public BrokerCache brokerCache() {
        return new BrokerCache();
    }

    @Bean
    public InstrumentCache instrumentCache() {
        return new InstrumentCache();
    }

    @Bean
    public MusicCache musicCache() {
        return new MusicCache();
    }

    @Bean
    public InfoMessageCache infoMessageCache() {
        return new InfoMessageCache();
    }

    @Bean
    public SortedInstrumentCache sortedInstrumentCache() {
        return new SortedInstrumentCache();
    }

    @Bean
    public QuotesCache quotesCache() {
        return new QuotesCache();
    }

    @Bean
    public PlayerCommandCache playerCommandCache() {
        return new PlayerCommandCache();
    }

    @Bean
    public PlayerScoreCache playerScoreCache() {
        return new PlayerScoreCache();
    }

}
