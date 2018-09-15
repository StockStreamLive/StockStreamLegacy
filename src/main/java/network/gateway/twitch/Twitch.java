package network.gateway.twitch;

import application.Config;
import application.Stage;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;

@Slf4j
public class Twitch {

    @Autowired
    private ImmutableMap<Stage, Configuration> configurations;

    private PircBotX activeBot;

    public void sendMessage(final String message) {
        try {
            activeBot.sendRaw().rawLine(message);
        } catch (final Throwable t) {
            log.warn(t.getMessage(), t);
        }
    }

    @PostConstruct
    public void init() {

        final Configuration configuration = configurations.get(Config.stage);

        final Thread runThread = new Thread(() -> {
            while (true) {
                log.info("Starting Twitch chat (IRC) listener.");

                final PircBotX bot = new PircBotX(configuration);
                activeBot = bot;

                try {
                    bot.startBot();
                } catch (final Exception e) {
                    log.error(e.getMessage(), e);
                }

                log.info("Twitch chat (IRC) listener stopped for some reason, sleeping for 60 seconds.");

                try {
                    Thread.sleep(60000L);
                } catch (final InterruptedException e) {
                    log.error(e.getMessage(), e);
                    log.info("Failed to sleep for 60 seconds (WTF?).");
                }
            }
        });

        runThread.start();
    }

}
