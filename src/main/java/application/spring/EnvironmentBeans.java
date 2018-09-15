package application.spring;

import environment.OBSManager;
import environment.android.AndroidManager;
import environment.jukebox.Jukebox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Import({ClockBeans.class,
         LogicBeans.class,
         ComputerBeans.class})
@Configuration
public class EnvironmentBeans {

    @Autowired
    private ClockBeans clockBeans;

    @Autowired
    private LogicBeans logicBeans;

    @Autowired
    private ComputerBeans computerBeans;

    @Bean
    public AndroidManager androidManager() {
        return new AndroidManager();
    }

    @Bean
    public Jukebox jukeBox() {
        return new Jukebox();
    }

    @Bean
    public OBSManager obsManager() {
        return new OBSManager();
    }

    //public static final RobinhoodAppMonitor robinhoodAppMonitor = new RobinhoodAppMonitor();

}
