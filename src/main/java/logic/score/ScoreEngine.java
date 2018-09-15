package logic.score;


import application.Config;
import cache.BrokerCache;
import cache.PlayerCommandCache;
import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import computer.StockComputer;
import data.Quote;
import data.RoundResult;
import data.command.ActionType;
import data.command.Command;
import data.command.RankedCommand;
import exception.RobinhoodException;
import logic.PubSub;
import logic.Scheduler;
import logic.game.GameClock;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import spark.utils.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
public class ScoreEngine {

    @Autowired
    private BrokerCache brokerCache;

    @Autowired
    private PlayerCommandCache playerCommandCache;

    @Autowired
    private MetricRegistry metricRegistry;

    @Autowired
    private Scheduler scheduler;

    @Autowired
    public PubSub pubSub;

    @Autowired
    private StockComputer stockComputer;

    @Autowired
    private ScoreComputer scoreComputer;

    @PostConstruct
    public void init() {
        pubSub.subscribe(this::updateScores, RoundResult.class);
    }

    @VisibleForTesting
    protected Void updateScores(final RoundResult roundResult) {
        final SortedSet<RankedCommand> rankedCommands = roundResult.getRankedCommands();

        if (CollectionUtils.isEmpty(rankedCommands)) {
            return null;
        }

        final Command bestCommand = rankedCommands.first().getCommand();

        final Map<String, Command> topPlayerCommands = roundResult.getPlayerToCommand();

        final Set<String> symbols = topPlayerCommands.values()
                                                     .stream()
                                                     .map(Command::getParameter)
                                                     .filter(str -> !StringUtils.isEmpty(str))
                                                     .collect(Collectors.toSet());

        if (!StringUtils.isEmpty(bestCommand.getParameter())) {
            symbols.add(bestCommand.getParameter());
        }

        final Map<String, Quote> symbolToQuote = new HashMap<>();

        try {
            symbolToQuote.putAll(this.stockComputer.loadSymbolToQuote(symbols));
        } catch (final RobinhoodException e) {
            log.error("Could not load quotes for symbols {}, skipping score updates!", symbols, e);
            return null;
        }

        final Set<TradeChoice> tradeChoices = new HashSet<>();

        if (!ActionType.HOLD.equals(bestCommand.getAction())) {
            final Quote bestCommandQuote = symbolToQuote.get(bestCommand.getParameter());
            final float bestLastTradePrice = bestCommandQuote.getMostRecentPrice();

            final TradeChoice winningTrade = new TradeChoice("StockStream", bestCommand, bestLastTradePrice, isPossible(bestCommand, bestCommandQuote));
            tradeChoices.add(winningTrade);
        }


        topPlayerCommands.forEach((player, command) -> {
            if (ActionType.HOLD.equals(command.getAction())) {
                return;
            }
            if (!symbolToQuote.containsKey(command.getParameter())) {
                return;
            }

            final Quote quote = symbolToQuote.get(command.getParameter());
            final float lastTradePrice = quote.getMostRecentPrice();
            tradeChoices.add(new TradeChoice(player, command, lastTradePrice, isPossible(command, quote)));
        });

        this.scheduler.scheduleJob(() -> scoreComputer.computeScores(tradeChoices),
                                   new DateTime().plusSeconds(GameClock.INTERVALS.get(Config.stage).intValue()));

        return null;
    }

    @VisibleForTesting
    protected boolean isPossible(final Command command, final Quote quote) {
        final float accountBalance = this.brokerCache.getAccountBalance();

        if (ActionType.BUY.equals(command.getAction())) {
            return accountBalance > quote.getMostRecentPrice();
        } else if (ActionType.SELL.equals(command.getAction())) {
            return this.brokerCache.getOwnedSymbols().contains(command.getParameter());
        } else if (ActionType.HOLD.equals(command.getAction())) {
            return true;
        }

        return false;
    }


}
