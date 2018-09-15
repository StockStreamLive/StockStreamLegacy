package application.spring;

import application.Config;
import application.Stage;
import com.google.common.collect.ImmutableMap;
import network.gateway.broker.Broker;
import network.gateway.broker.Robinhood;
import network.gateway.broker.VirtualFund;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Import({MetricBeans.class,
         LogicBeans.class})
@Configuration
public class FinanceBeans {

    @Autowired
    private MetricBeans metricBeans;

    @Autowired
    private UtilBeans utilBeans;

    @Autowired
    private LogicBeans logicBeans;

    @Bean
    public Robinhood robinhood() {
        return new Robinhood(Config.RH_UN, Config.RH_PW);
    }

    @Bean
    public VirtualFund virtualFund() {
        return new VirtualFund();
    }

    @Bean
    public ImmutableMap<Stage, Broker> brokers() {
        return ImmutableMap.of(Stage.TEST, virtualFund(),
                               Stage.LOCAL, virtualFund(),
                               Stage.GAMMA, robinhood(),
                               Stage.PROD, robinhood());
    }

    @Bean
    public Broker broker() {
        return brokers().get(Config.stage);
    }

}
