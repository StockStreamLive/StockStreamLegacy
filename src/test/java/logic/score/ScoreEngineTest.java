package logic.score;


import cache.BrokerCache;
import cache.PlayerCommandCache;
import cache.PlayerScoreCache;
import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import computer.StockComputer;
import data.Quote;
import data.command.ActionType;
import data.command.Command;
import logic.Scheduler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class ScoreEngineTest {

    @Mock
    private BrokerCache brokerCache;

    @Mock
    private PlayerCommandCache playerCommandCache;

    @Mock
    private PlayerScoreCache playerScoreCache;

    @Mock
    private MetricRegistry metricRegistry;

    @Mock
    private Scheduler scheduler;

    @Mock
    private StockComputer stockComputer;

    @InjectMocks
    private ScoreEngine scoreEngine;

    @Before
    public void setupTest() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testIsPossible_stockTooExpensive_expectFalse() {
        when(brokerCache.getAccountBalance()).thenReturn(125.20f);

        final Command command = new Command(ActionType.BUY, "AAPL");
        final Quote quote = new Quote(ImmutableMap.of("last_trade_price", "150.25"));

        boolean possible = scoreEngine.isPossible(command, quote);

        assertFalse(possible);
    }

    @Test
    public void testIsPossible_stockNotOwned_expectFalse() {
        when(brokerCache.getOwnedSymbols()).thenReturn(ImmutableSet.of("MSFT"));

        final Command command = new Command(ActionType.SELL, "AAPL");
        final Quote quote = new Quote(ImmutableMap.of("last_trade_price", "150.25"));

        boolean possible = scoreEngine.isPossible(command, quote);

        assertFalse(possible);
    }

    @Test
    public void testIsPossible_sellCommandOk_expectTrue() {
        when(brokerCache.getOwnedSymbols()).thenReturn(ImmutableSet.of("AAPL"));

        final Command command = new Command(ActionType.SELL, "AAPL");
        final Quote quote = new Quote(ImmutableMap.of("last_trade_price", "150.25"));

        boolean possible = scoreEngine.isPossible(command, quote);

        assertTrue(possible);
    }

    @Test
    public void testIsPossible_buyCommandOk_expectTrue() {
        when(brokerCache.getAccountBalance()).thenReturn(160.20f);

        final Command command = new Command(ActionType.BUY, "AAPL");
        final Quote quote = new Quote(ImmutableMap.of("last_trade_price", "150.25"));

        boolean possible = scoreEngine.isPossible(command, quote);

        assertTrue(possible);
    }

}
