package application.spring;

import network.gateway.aws.AssetPublisher;
import network.gateway.aws.ScoreUploader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Import({LogicBeans.class,
         CacheBeans.class,
         ComputerBeans.class,
         FinanceBeans.class})
@Configuration
public class AWSBeans {

    @Autowired
    private LogicBeans logicBeans;

    @Autowired
    private CacheBeans cacheBeans;

    @Autowired
    private ComputerBeans computerBeans;

    @Autowired
    private FinanceBeans financeBeans;

    @Bean
    public AssetPublisher assetPublisher() {
        return new AssetPublisher();
    }

    @Bean
    public ScoreUploader scoreUploader() {
        return new ScoreUploader();
    }

}
