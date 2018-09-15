package application.spring;

import application.SparkServer;
import logic.game.GameEngine;
import logic.score.ScoreEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Import({MetricBeans.class,
         ClockBeans.class,
         LogicBeans.class,
         UtilBeans.class,
         LogicBeans.class,
         FinanceBeans.class,
         FactoryBeans.class,
         DispatcherBeans.class,
         CacheBeans.class,
         GatewayBeans.class,
         TwitchBeans.class,
         AWSBeans.class})
@Configuration
public class ApplicationContext {

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

    @Autowired
    private CacheBeans cacheBeans;

    @Autowired
    private ClockBeans clockBeans;

    @Autowired
    private GatewayBeans gatewayBeans;

    @Bean
    public ScoreEngine scoreEngine() {
        return new ScoreEngine();
    }

    @Bean
    public GameEngine gameEngine() {
        return new GameEngine();
    }

    @Bean
    public SparkServer sparkServer() {
        return new SparkServer();
    }
}
