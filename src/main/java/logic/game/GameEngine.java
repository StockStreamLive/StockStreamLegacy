package logic.game;

import application.Config;
import cache.BrokerCache;
import com.google.common.annotations.VisibleForTesting;
import computer.TimeComputer;
import data.Asset;
import data.OrderResult;
import data.OrderStatus;
import data.RoundResult;
import data.command.Command;
import data.command.RankedCommand;
import exception.RobinhoodException;
import logic.PubSub;
import logic.score.ScoreEngine;
import lombok.extern.slf4j.Slf4j;
import network.gateway.broker.Broker;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.springframework.beans.factory.annotation.Autowired;
import spark.utils.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.SortedSet;

@Slf4j
public class GameEngine {

    @Autowired
    private PubSub pubSub;

    @Autowired
    private Broker broker;

    @Autowired
    private BrokerCache brokerCache;

    @Autowired
    private ScoreEngine scoreEngine;

    @Autowired
    private TimeComputer timeComputer;

    @PostConstruct
    public void init() {
        pubSub.subscribe(this::executeBestCommand, RoundResult.class);
    }

    @VisibleForTesting
    protected Void executeBestCommand(final RoundResult roundResult) {
        final SortedSet<RankedCommand> rankedCommands = roundResult.getRankedCommands();

        if (CollectionUtils.isEmpty(rankedCommands)) {
            OrderResult tickResult = new OrderResult(null, "", 0, OrderStatus.NOT_ENOUGH_VOTES);
            this.pubSub.publish(OrderResult.class, tickResult);
            return null;
        }

        final RankedCommand bestRankedCommand = rankedCommands.first();
        final Command bestCommand = bestRankedCommand.getCommand();
        final int votes = bestRankedCommand.getVotes();
        final String symbol = bestCommand.getParameter();

        if (!this.timeComputer.isMarketOpenNow()) {
            final OrderResult orderResult = new OrderResult(bestCommand.getAction(), symbol, votes, OrderStatus.MARKET_CLOSED);
            this.pubSub.publish(OrderResult.class, orderResult);
            return null;
        }

        log.info("Best command: {} votes {}", bestRankedCommand, votes);

        OrderStatus orderStatus = OrderStatus.UNKNOWN;

        final double netWorth = this.brokerCache.getAccountNetWorth();
        log.info("Got an account net worth of {}.", netWorth);
        if (netWorth <= Config.MIN_NET_WORTH) {
            log.warn("Got an account net worth of {} so not executing command {}.", netWorth, bestRankedCommand);
            OrderResult tickResult = new OrderResult(null, "", 0, OrderStatus.NET_WORTH_TOO_LOW);
            this.pubSub.publish(OrderResult.class, tickResult);
            return null;
        }

        try {
            switch (bestCommand.getAction()) {
                case BUY: {
                    orderStatus = processBuy(bestCommand);
                    break;
                } case SELL: {
                    orderStatus = processSell(bestCommand);
                    break;
                } case HOLD: {
                    orderStatus = OrderStatus.OK;
                    break;
                } default: {
                    log.warn("Invalid action type: {}", bestCommand);
                    break;
                }
            }
        } catch (final RobinhoodException e) {
            log.warn(e.getMessage(), e);
            orderStatus = OrderStatus.BROKER_EXCEPTION;
        }

        log.info("Got status {} after executing command {}", orderStatus, bestCommand);

        final OrderResult tickResult = new OrderResult(bestCommand.getAction(), symbol, votes, orderStatus);
        this.pubSub.publish(OrderResult.class, tickResult);

        return null;
    }

    private OrderStatus processSell(final Command command) throws RobinhoodException {
        log.info("Executing SELL command {} ", command);

        final String symbolToSell = command.getParameter();

        final MutableBoolean doOwnSymbol = new MutableBoolean(false);

        Collection<Asset> ownedAssets = this.broker.getOwnedAssets();

        ownedAssets.forEach(asset -> {
            if (asset.getSymbol().equals(symbolToSell)) {
                doOwnSymbol.setTrue();
            }
        });

        if (!doOwnSymbol.getValue()) {
            log.warn("Don't own any shares of {} to sell, so not executing command {}", symbolToSell, command);
            return OrderStatus.NO_SHARES;
        }

        return this.broker.sellShares(symbolToSell, 1);
    }

    private OrderStatus processBuy(final Command command) throws RobinhoodException {
        log.info("Executing BUY command {} ", command);

        return this.broker.buyShares(command.getParameter(), 1);
    }

}
