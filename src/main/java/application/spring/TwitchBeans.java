package application.spring;

import application.Stage;
import com.google.common.collect.ImmutableMap;
import network.gateway.twitch.Twitch;
import org.pircbotx.cap.EnableCapHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Import({CacheBeans.class,
         DispatcherBeans.class})
@Configuration
public class TwitchBeans {

    @Autowired
    private CacheBeans cacheBeans;

    @Autowired
    private DispatcherBeans dispatcherBeans;

    // TEST CONFIG
    @Bean
    public org.pircbotx.Configuration testChannelConfiguration() {
        return new org.pircbotx.Configuration.Builder().setAutoNickChange(false)
                                                       .setOnJoinWhoEnabled(false)
                                                       .setName("stockstream")
                                                       .addServer("localhost")
                                                       .addAutoJoinChannel("#stockstream")
                                                       .addListener(cacheBeans.sortedInstrumentCache())
                                                       .addListener(cacheBeans.playerCommandCache())
                                                       .addListener(dispatcherBeans.liveCommands())
                                                       .addListener(dispatcherBeans.responder())
                                                       .setAutoReconnect(true)
                                                       .setAutoReconnectDelay(5 * 1000)
                                                       .buildConfiguration();
    }

    // LOCAL CONFIG
    @Bean
    public org.pircbotx.Configuration localChannelConfiguration() {
        return new org.pircbotx.Configuration.Builder().setAutoNickChange(false)
                                                       .setOnJoinWhoEnabled(false)
                                                       .setName("stockstream")
                                                       .addServer("192.168.1.107")
                                                       .addAutoJoinChannel("#stockstream")
                                                       .addListener(cacheBeans.sortedInstrumentCache())
                                                       .addListener(cacheBeans.playerCommandCache())
                                                       .addListener(dispatcherBeans.liveCommands())
                                                       .addListener(dispatcherBeans.responder())
                                                       .setAutoReconnect(true)
                                                       .setAutoReconnectDelay(5 * 1000)
                                                       .buildConfiguration();
    }

    // GAMMA CONFIG
    @Bean
    public org.pircbotx.Configuration gammaChannelConfiguration() {
        return new org.pircbotx.Configuration.Builder().setAutoNickChange(false)
                                                       .setOnJoinWhoEnabled(false)
                                                       .setName("stockstream")
                                                       .addServer("localhost")
                                                       .addAutoJoinChannel("#stockstream")
                                                       .addListener(cacheBeans.sortedInstrumentCache())
                                                       .addListener(cacheBeans.playerCommandCache())
                                                       .addListener(dispatcherBeans.liveCommands())
                                                       .addListener(dispatcherBeans.responder())
                                                       .setAutoReconnect(true)
                                                       .setAutoReconnectDelay(5 * 1000)
                                                       .buildConfiguration();
    }

    // PROD CONFIG
    public org.pircbotx.Configuration prodChannelConfiguration() {
        return new org.pircbotx.Configuration.Builder().setAutoNickChange(false)
                                                       .setOnJoinWhoEnabled(false)
                                                       .setCapEnabled(true)
                                                       .addCapHandler(new EnableCapHandler("twitch.tv/membership"))
                                                       .addCapHandler(new EnableCapHandler("twitch.tv/tags"))
                                                       .addCapHandler(new EnableCapHandler("twitch.tv/commands"))
                                                       .setName("stockstream")
                                                       .setServerPassword("oauth:wee12rhgskga3qf1iq4rbvq0f61i7s")
                                                       .addServer("irc.chat.twitch.tv")
                                                       .addAutoJoinChannel("#stockstream")
                                                       .addListener(cacheBeans.sortedInstrumentCache())
                                                       .addListener(cacheBeans.playerCommandCache())
                                                       .addListener(dispatcherBeans.liveCommands())
                                                       .addListener(dispatcherBeans.responder())
                                                       .setAutoReconnect(true)
                                                       .setAutoReconnectDelay(5 * 1000)
                                                       .buildConfiguration();
    }

    @Bean
    public ImmutableMap<Stage, org.pircbotx.Configuration> configurations() {
        return ImmutableMap.of(Stage.TEST, testChannelConfiguration(),
                               Stage.LOCAL, localChannelConfiguration(),
                               Stage.GAMMA, gammaChannelConfiguration(),
                               Stage.PROD, prodChannelConfiguration());
    }

    @Bean
    public Twitch twitch() {
        return new Twitch();
    }

}
