package application.spring;


import data.factory.AffiliateURLFactory;
import data.factory.CommandFactory;
import data.factory.InfoMessageFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Import({LogicBeans.class,
         ComputerBeans.class,
         CacheBeans.class,
         UtilBeans.class})
@Configuration
public class FactoryBeans {

    @Autowired
    private LogicBeans logicBeans;

    @Autowired
    private ComputerBeans computerBeans;

    @Autowired
    private CacheBeans cacheBeans;

    @Autowired
    private UtilBeans utilBeans;

    @Bean
    public CommandFactory commandFactory() {
        return new CommandFactory();
    }

    @Bean
    public InfoMessageFactory infoMessageFactory() {
        return new InfoMessageFactory();
    }

    @Bean
    public AffiliateURLFactory affiliateURLFactory() {
        return new AffiliateURLFactory();
    }

}
