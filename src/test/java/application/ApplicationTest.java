package application;


import cache.BrokerCache;
import cache.PlayerCommandCache;
import cache.PlayerScoreCache;
import javazoom.jl.decoder.JavaLayerException;
import org.junit.Before;
import org.junit.Test;
import org.pircbotx.PircBotX;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

public class ApplicationTest {

    private Application application = new Application();

    private org.pircbotx.Configuration testChannelConfiguration =
            new org.pircbotx.Configuration.Builder().setAutoNickChange(false)
                                                    .setOnJoinWhoEnabled(false)
                                                    .setName("chaosmonkey")
                                                    .addServer("localhost")
                                                    .addAutoJoinChannel("#stockstream")
                                                    .addListener(System.out::println)
                                                    .setAutoReconnect(true)
                                                    .setAutoReconnectDelay(5 * 1000)
                                                    .buildConfiguration();

    private AnnotationConfigWebApplicationContext applicationContext;
    private final PircBotX bot = new PircBotX(testChannelConfiguration);

    @Before
    public void setupTest() throws InterruptedException {
        Config.stage = Stage.TEST;

        applicationContext = Application.initApplicationContext();

        new Thread(() -> {
            try {
                bot.startBot();
            } catch (final Exception e) {
                System.out.println(e);
            }
        }).start();

        Thread.sleep(10000);
    }

    @Test
    public void testGame_oneBuyVote_expectPurchasedAndTwoScores() throws IOException, JavaLayerException, InterruptedException {
        final String buyCommand = String.format("PRIVMSG %s :%s\r\n", "#stockstream", "!buy seb");

        bot.sendRaw().rawLine(buyCommand);

        Thread.sleep(50);

        PlayerCommandCache playerCommandCache = applicationContext.getBean(PlayerCommandCache.class);
        assertTrue(playerCommandCache.getPlayerToCommand().size() == 1);
        assertTrue(playerCommandCache.getPlayerToCommand().containsKey("chaosmonkey"));

        Thread.sleep(20000);

        PlayerScoreCache playerScoreCache = applicationContext.getBean(PlayerScoreCache.class);
        assertTrue(playerScoreCache.getScore("chaosmonkey") != 0d);
        assertTrue(playerScoreCache.getScore("StockStream") != 0d);

        BrokerCache brokerCache = applicationContext.getBean(BrokerCache.class);
        assertTrue(brokerCache.getAssets().size() == 1);
        assertTrue(brokerCache.getAssets().iterator().next().getSymbol().equalsIgnoreCase("seb"));
        assertTrue(brokerCache.getAssets().iterator().next().getShares() == 1);
        assertTrue(brokerCache.getAccountBalance() < 50000);
    }


}
