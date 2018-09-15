package application;

import application.spring.ApplicationContext;
import com.google.common.collect.ImmutableMap;
import javazoom.jl.decoder.JavaLayerException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import java.io.IOException;

@Slf4j
public class Application {

    public static final ImmutableMap<String, Stage> stages =
            ImmutableMap.of("-t", Stage.TEST, "-l", Stage.LOCAL, "-g", Stage.GAMMA, "-p", Stage.PROD);

    public static AnnotationConfigWebApplicationContext initApplicationContext() {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.register(ApplicationContext.class);
        context.refresh();

        SparkServer sparkServer = context.getBean(SparkServer.class);
        sparkServer.startServer();
        return context;
    }

    /**
     * !!! Config.stage MUST BE APPLIED BEFORE BEAN INIT !!!
     */
    public static void main(final String[] args) throws JavaLayerException, IOException {
        log.info("StockStream initialized");

        if (args.length != 1) {
            log.info("Must define stage? -t[est] -l[ocal] -g[amma] -p[prod] ?.");
            System.exit(1);
        }

        final String strStage = args[0];

        if (!stages.keySet().contains(strStage)) {
            log.info("Unknown stage {}", strStage);
            System.exit(1);
        }

        final Stage stage = stages.get(strStage);

        Config.stage = stage;

        //////////////////////////////////////////////////////////////////

        AnnotationConfigWebApplicationContext context = initApplicationContext();

        context.close();
    }

}
