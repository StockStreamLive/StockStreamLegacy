package application.spring;

import computer.CommandComputer;
import computer.StockComputer;
import computer.TimeComputer;
import logic.score.ScoreComputer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Import({CacheBeans.class,
         FinanceBeans.class})
@Configuration
public class ComputerBeans {

    @Autowired
    private CacheBeans cacheBeans;

    @Autowired
    private FinanceBeans financeBeans;

    @Bean
    public TimeComputer timeComputer() {
        return new TimeComputer();
    }

    @Bean
    public StockComputer stockComputer() {
        return new StockComputer();
    }

    @Bean
    public CommandComputer commandComputer() {
        return new CommandComputer();
    }

    @Bean
    public ScoreComputer scoreComputer() {
        return new ScoreComputer();
    }

}
