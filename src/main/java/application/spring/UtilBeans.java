package application.spring;

import network.http.HTTPUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import utils.GameUtil;

@Import({MetricBeans.class, CacheBeans.class})
@Configuration
public class UtilBeans {

    @Autowired
    private MetricBeans metricBeans;

    @Autowired
    private CacheBeans cacheBeans;

    @Bean
    public HTTPUtil httpUtil() {
        return new HTTPUtil();
    }

    @Bean
    public GameUtil gameUtil() {
        return new GameUtil();
    }

}
