package logic.score;

import cache.PlayerScoreCache;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import computer.StockComputer;
import data.Quote;
import data.command.ActionType;
import data.command.Command;
import exception.RobinhoodException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import utils.MathUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class ScoreComputer {
    @Autowired
    private PlayerScoreCache playerScoreCache;

    @Autowired
    private MetricRegistry metricsRegistry;

    @Autowired
    private StockComputer stockComputer;

    public void computeScores(final Set<TradeChoice> tradeChoices) {
        final Timer latencyTimer = this.metricsRegistry.timer("ScoreUpdateRunnable.runtime");
        Timer.Context timerContext = latencyTimer.time();

        final Set<String> symbols = tradeChoices.stream()
                                                .map(TradeChoice::getCommand)
                                                .map(Command::getParameter)
                                                .filter(str -> !StringUtils.isEmpty(str))
                                                .collect(Collectors.toSet());

        final Map<String, Quote> symbolToQuote = new HashMap<>();

        try {
            symbolToQuote.putAll(this.stockComputer.loadSymbolToQuote(symbols));
        } catch (final RobinhoodException e) {
            log.error("Could not load quotes for symbols {}, skipping score updates!", symbols, e);
            return;
        }

        final Map<String, Double> scoreModifications = new HashMap<>();

        tradeChoices.forEach(tradeChoice -> {
            final String player = tradeChoice.getPlayer();
            final Command command = tradeChoice.getCommand();

            if (!symbolToQuote.containsKey(command.getParameter())) {
                return;
            }

            scoreModifications.put(player, computeScore(tradeChoice, symbolToQuote.get(command.getParameter())));
        });

        this.playerScoreCache.persistScoreUpdates(scoreModifications);

        timerContext.stop();
    }

    private double computeScore(final TradeChoice tradeChoice, final Quote recentQuote) {
        if (!tradeChoice.isPossible()) {
            return 0;
        }

        double percentChange = MathUtil.computePercentChange(tradeChoice.getLastTradePrice(), recentQuote.getMostRecentPrice());
        if (ActionType.SELL.equals(tradeChoice.getCommand().getAction())) {
            percentChange *= -1;
        }

        log.info("TradeChoice {} resulted in score {} with recent quote of {}",
                 tradeChoice, percentChange, recentQuote);

        return percentChange;
    }
}