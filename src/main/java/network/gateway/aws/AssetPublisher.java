package network.gateway.aws;


import application.Config;
import application.Stage;
import cache.BrokerCache;
import cache.InstrumentCache;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import data.Asset;
import data.JobInterval;
import data.Order;
import exception.RobinhoodException;
import logic.Scheduler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import network.gateway.broker.Broker;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import utils.JSONUtil;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class AssetPublisher {

    private static final Set<String> ORDER_STATUS_WHITELIST = ImmutableSet.of("confirmed", "queued");

    @Data
    @AllArgsConstructor
    class AssetNode {
        private String symbol;
        private int shares;
        private float avgCost;

        public AssetNode(final Asset asset) {
            this.symbol = asset.getSymbol();
            this.shares = asset.getShares();
            this.avgCost = (float)asset.getAvgBuyPrice();
        }
    }

    @Data
    @AllArgsConstructor
    class OrderNode {
        private String symbol;
        private String state;
        private String createdAt;
        private float price;
        private int shares;

        public OrderNode(final Order order) {
            this.symbol = instrumentCache.getUrlToInstrument().get(order.getInstrument()).getSymbol();
            this.shares = (int)Float.parseFloat(order.getQuantity());
            this.price = Float.parseFloat(order.getPrice());
            this.createdAt = order.getCreated_at();
            this.state = "Pending";
        }
    }

    @Data
    @AllArgsConstructor
    class Account {
        final float cashBalance;
        final Collection<AssetNode> assets;
        final Collection<OrderNode> orders;
    }

    private static final Map<Stage, String> ARN_MAP =
            ImmutableMap.of(Stage.TEST, "arn:aws:sns:us-west-2:534605677915:StockStreamAssetsBeta",
                            Stage.LOCAL, "arn:aws:sns:us-west-2:534605677915:StockStreamAssetsBeta",
                            Stage.GAMMA, "arn:aws:sns:us-west-2:534605677915:StockStreamAssetsBeta",
                            Stage.PROD, "arn:aws:sns:us-west-2:534605677915:StockStreamAssets");

    private static final Map<Stage, String> buckets =
            ImmutableMap.of(Stage.TEST, "stockstream-data-test",
                            Stage.LOCAL, "stockstream-data-beta",
                            Stage.GAMMA, "stockstream-data-gamma",
                            Stage.PROD, "stockstream-data");

    private static final Map<Stage, String> folders =
            ImmutableMap.of(Stage.TEST, "assets_data-test",
                            Stage.LOCAL, "assets_data-beta",
                            Stage.GAMMA, "assets_data-gamma",
                            Stage.PROD, "assets_data");

    @Autowired
    private Broker broker;

    @Autowired
    private BrokerCache brokerCache;

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private InstrumentCache instrumentCache;

    public AssetPublisher() {
    }

    @PostConstruct
    public void init() {
        scheduler.scheduleJob(this::publishAssets, new JobInterval(1, 5, TimeUnit.MINUTES));
    }

    private void publishAssets() {
        log.info("Publishing assets.");

        Optional<String> accountDataOptional = constructPortfolioJSON();

        if (!accountDataOptional.isPresent()) {
            log.warn("Account message empty, not sending anything.");
            return;
        }

        publishToSNS(accountDataOptional.get());
        publishToS3(accountDataOptional.get());
    }

    private Optional<String> constructPortfolioJSON() {
        Optional<String> assetsMessage = Optional.empty();

        try {

            final float cashBalance = this.brokerCache.getAccountBalance();
            final List<AssetNode> assets = constructAssetNodes();

            if (assets.size() == 0) {
                return Optional.empty();
            }

            final List<OrderNode> pendingOrders = constructPendingOrderNodes();
            final Account account = new Account(cashBalance, assets, pendingOrders);
            assetsMessage = JSONUtil.serializeObject(account, false);

        } catch (final RobinhoodException e) {
            log.warn("Could not get assets from broker!", e);
        }

        return assetsMessage;
    }

    private List<AssetNode> constructAssetNodes() throws RobinhoodException {
        final List<AssetNode> assetNodes = this.broker.getOwnedAssets().stream().map(AssetNode::new).collect(Collectors.toList());
        assetNodes.add(new AssetNode("WFM", 2, 42.88f));
        return assetNodes;
    }

    private List<OrderNode> constructPendingOrderNodes() throws RobinhoodException {
        return this.broker.getRecentOrders().stream().filter(order -> ORDER_STATUS_WHITELIST.contains(order.getState())).map(OrderNode::new).collect(Collectors.toList());
    }

    private void publishToSNS(final String accountData) {
        final AmazonSNSClient snsClient = new AmazonSNSClient(Credentials.PROVIDER);
        snsClient.setRegion(Region.getRegion(Regions.US_WEST_2));
        snsClient.publish(ARN_MAP.get(Config.stage), accountData, "$");
    }

    private void publishToS3(final String accountData) {
        final AmazonS3 s3client = new AmazonS3Client(Credentials.PROVIDER);

        final String path = String.format("%s/assets-%s", folders.get(Config.stage), new DateTime().getMillis());
        try {
            Files.write(Paths.get(path), accountData.getBytes());
        } catch (final IOException e) {
            log.error("Could not update portfolio at path: {}", path, e);
            return;
        }

        s3client.putObject(new PutObjectRequest(buckets.get(Config.stage), "portfolio", new File(path)));
    }

}
