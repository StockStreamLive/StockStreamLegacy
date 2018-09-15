package application.spring;


import logic.PubSub;
import logic.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Import(MetricBeans.class)
@Configuration
public class LogicBeans {

    @Autowired
    private MetricBeans metricBeans;

    @Bean
    public PubSub pubSub() {
        return new PubSub();
    }

    @Bean
    public Scheduler scheduler() {
        return new Scheduler();
    }
}
