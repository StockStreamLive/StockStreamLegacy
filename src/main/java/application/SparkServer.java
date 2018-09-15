package application;


import cache.BrokerCache;
import cache.InfoMessageCache;
import cache.MusicCache;
import cache.QuotesCache;
import com.codahale.metrics.JmxReporter;
import com.google.common.collect.ImmutableMap;
import computer.CommandComputer;
import logic.game.GameClock;
import network.gateway.localhost.LiveCommands;
import network.gateway.localhost.StreamLabels;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import spark.Filter;
import spark.Spark;
import utils.JSONUtil;

import java.util.HashMap;

public class SparkServer {

    private static final HashMap<String, String> corsHeaders = new HashMap<>();

    static {
        corsHeaders.put("Access-Control-Allow-Methods", "GET,PUT,POST,DELETE,OPTIONS");
        corsHeaders.put("Access-Control-Allow-Origin", "*");
        corsHeaders.put("Access-Control-Allow-Headers", "Content-Type,Authorization,X-Requested-With,Content-Length,Accept,Origin,");
        corsHeaders.put("Access-Control-Allow-Credentials", "true");
    }

    @Autowired
    private LiveCommands liveCommands;

    @Autowired
    private QuotesCache quotesCache;

    @Autowired
    private InfoMessageCache infoMessageCache;

    @Autowired
    private MusicCache musicCache;

    @Autowired
    private JmxReporter jmxReporter;

    @Autowired
    private BrokerCache brokerCache;

    @Autowired
    private GameClock gameClock;

    @Autowired
    private StreamLabels streamLabels;

    @Autowired
    private CommandComputer commandComputer;

    public SparkServer() {
    }

    private final static void apply() {
        Filter filter = (request, response) -> corsHeaders.forEach(response::header);
        Spark.after(filter);
    }

    public void startServer() {
        this.jmxReporter.start();

        Spark.staticFileLocation("/public");
        Spark.webSocket("/liveCommands", this.liveCommands);
        apply();

        Spark.get("/topCommands", (req, res) -> JSONUtil.serializeObject(this.commandComputer.computeRankedCommands()).get());
        Spark.get("/accountStatus", (req, res) -> JSONUtil.serializeObject(ImmutableMap.of("spendableBalance", this.brokerCache.getAccountBalance())).get());
        Spark.get("/assetsList", (req, res) -> JSONUtil.serializeObject(this.brokerCache.getAssets()).get());
        Spark.get("/nextEvent", (req, res) -> JSONUtil.serializeObject(this.gameClock.getNextGameEvent()).get());
        Spark.get("/marketState", (req, res) -> JSONUtil.serializeObject(this.brokerCache.getMarketState(new DateTime())).get());
        Spark.get("/quotes", (req, res) -> JSONUtil.serializeObject(this.quotesCache.getQuotes()).get());
        Spark.get("/infoMessage", (req, res) -> JSONUtil.serializeObject(this.infoMessageCache.getNext()).get());
        Spark.get("/topDonor", (req, res) -> JSONUtil.serializeObject(this.streamLabels.getTopDonor()).get());
        Spark.get("/portfolioHistoricals", (req, res) -> JSONUtil.serializeObject(this.brokerCache.getHistoricalAccountValue()).get());
        Spark.get("/recentOrders", (req, res) -> JSONUtil.serializeObject(this.brokerCache.getRecentOrders()).get());

        Spark.get("/music", (req, res) -> JSONUtil.serializeObject(this.musicCache.getMusicFiles()).get());

        Spark.init();
    }
}
