package application.spring;

import network.gateway.localhost.LiveCommands;
import network.gateway.twitch.Responder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Import({ClockBeans.class,
         LogicBeans.class,
         ComputerBeans.class,
         EnvironmentBeans.class,
         FactoryBeans.class,
         CacheBeans.class})
@Configuration
public class DispatcherBeans {

    @Autowired
    private ClockBeans clockBeans;

    @Autowired
    private LogicBeans logicBeans;

    @Autowired
    private ComputerBeans computerBeans;

    @Autowired
    private EnvironmentBeans environmentBeans;

    @Autowired
    private FactoryBeans factoryBeans;

    @Bean
    public Responder responder() {
        return new Responder();
    }

    @Bean
    public LiveCommands liveCommands() {
        return new LiveCommands();
    }


}
