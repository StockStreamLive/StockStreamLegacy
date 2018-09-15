package application.spring;

import logic.MarketClock;
import logic.game.GameClock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Import({MetricBeans.class,
         LogicBeans.class,
         FinanceBeans.class,
         ComputerBeans.class,
         CacheBeans.class})
@Configuration
public class ClockBeans {

    @Bean
    public MarketClock marketClock() {
        return new MarketClock();
    }

    @Bean
    public GameClock gameClock() {
        return new GameClock();
    }

}
