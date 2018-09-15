package application.spring;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricBeans {

    @Bean
    public MetricRegistry metricsRegistry() {
        return new MetricRegistry();
    }

    @Bean
    public JmxReporter jmxReporter() {
        final JmxReporter jmxReporter = JmxReporter.forRegistry(metricsRegistry()).build();
        jmxReporter.start();
        return jmxReporter;
    }

}
