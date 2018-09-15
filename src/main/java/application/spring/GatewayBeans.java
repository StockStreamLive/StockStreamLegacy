package application.spring;


import network.gateway.information.NewsAPI;
import network.gateway.information.StockTwits;
import network.gateway.information.Twitter;
import network.gateway.localhost.StreamLabels;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Import({UtilBeans.class,
         LogicBeans.class,
         ComputerBeans.class})
@Configuration
public class GatewayBeans {

    @Autowired
    private UtilBeans utilBeans;

    @Autowired
    private LogicBeans logicBeans;

    @Autowired
    private ComputerBeans computerBeans;

    @Bean
    public StockTwits stockTwits() {
        return new StockTwits();
    }

    @Bean
    public Twitter twitterBot() {
        return new Twitter();
    }

    @Bean
    public NewsAPI newsAPI() {
        return new NewsAPI();
    }

    @Bean
    public StreamLabels streamLabels() {
        return new StreamLabels();
    }

}
