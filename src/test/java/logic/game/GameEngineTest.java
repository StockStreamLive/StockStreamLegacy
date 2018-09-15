package logic.game;


import cache.BrokerCache;
import cache.PlayerCommandCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import computer.CommandComputer;
import computer.TimeComputer;
import data.OrderResult;
import data.OrderStatus;
import data.RoundResult;
import data.command.ActionType;
import data.command.Command;
import data.command.RankedCommand;
import data.comparator.RankedCommandComparator;
import exception.RobinhoodException;
import logic.PubSub;
import logic.score.ScoreEngine;
import network.gateway.broker.Broker;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.SortedSet;
import java.util.TreeSet;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class GameEngineTest {

    @Mock
    private PubSub pubSub;

    @Mock
    private Broker broker;

    @Mock
    private BrokerCache brokerCache;

    @Mock
    private ScoreEngine scoreEngine;

    @Mock
    private CommandComputer commandComputer;

    @Mock
    private TimeComputer timeComputer;

    @Mock
    private PlayerCommandCache playerCommandCache;

    @InjectMocks
    private GameEngine gameEngine;

    @Before
    public void setupTest() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testTick_noCommand_expectNoVotesResult() {
        Mockito.doAnswer(invocation -> {
            final Object[] args = invocation.getArguments();

            assertTrue(args.length == 2);
            assertTrue(args[0].equals(OrderResult.class));
            assertTrue(args[1] instanceof OrderResult);

            final OrderResult orderResult = (OrderResult) args[1];
            assertTrue(orderResult.getOrderStatus().equals(OrderStatus.NOT_ENOUGH_VOTES));

            return null;
        }).when(pubSub).publish(any(), any());

        gameEngine.executeBestCommand(new RoundResult(ImmutableMap.of(), ImmutableSortedSet.of()));

        verify(pubSub, times(1)).publish(any(), any());
    }

    @Test
    public void testTick_someCommandMarketClosed_expectNoAction() {
        final RankedCommand bestCommand = new RankedCommand(new Command(ActionType.BUY, "AAPL"), 1);

        when(timeComputer.isMarketOpenNow()).thenReturn(false);

        Mockito.doAnswer(invocation -> {
            final Object[] args = invocation.getArguments();

            assertTrue(args.length == 2);
            assertTrue(args[0].equals(OrderResult.class));
            assertTrue(args[1] instanceof OrderResult);

            final OrderResult orderResult = (OrderResult) args[1];
            assertTrue(orderResult.getOrderStatus().equals(OrderStatus.MARKET_CLOSED));

            return null;
        }).when(pubSub).publish(any(), any());

        final SortedSet<RankedCommand> rankedCommands = new TreeSet<>(new RankedCommandComparator());
        rankedCommands.add(bestCommand);

        gameEngine.executeBestCommand(new RoundResult(ImmutableMap.of(), rankedCommands));

        verify(pubSub, times(1)).publish(any(), any());
        verify(timeComputer, times(1)).isMarketOpenNow();
    }

    @Test
    public void testTick_someCommandNetWorthTooLow_expectNoAction() {
        final RankedCommand bestCommand = new RankedCommand(new Command(ActionType.BUY, "AAPL"), 1);

        when(timeComputer.isMarketOpenNow()).thenReturn(true);
        when(brokerCache.getAccountNetWorth()).thenReturn(100d);

        Mockito.doAnswer(invocation -> {
            final Object[] args = invocation.getArguments();

            assertTrue(args.length == 2);
            assertTrue(args[0].equals(OrderResult.class));
            assertTrue(args[1] instanceof OrderResult);

            final OrderResult orderResult = (OrderResult) args[1];
            assertTrue(orderResult.getOrderStatus().equals(OrderStatus.NET_WORTH_TOO_LOW));

            return null;
        }).when(pubSub).publish(any(), any());

        final SortedSet<RankedCommand> rankedCommands = new TreeSet<>(new RankedCommandComparator());
        rankedCommands.add(bestCommand);

        gameEngine.executeBestCommand(new RoundResult(ImmutableMap.of(), rankedCommands));

        verify(pubSub, times(1)).publish(any(), any());
        verify(timeComputer, times(1)).isMarketOpenNow();
        verify(brokerCache, times(1)).getAccountNetWorth();
    }

    @Test
    public void testTick_someBuyCommandHappyCase_expectBuyCommand() throws RobinhoodException {
        final RankedCommand bestCommand = new RankedCommand(new Command(ActionType.BUY, "AAPL"), 1);

        when(timeComputer.isMarketOpenNow()).thenReturn(true);
        when(brokerCache.getAccountNetWorth()).thenReturn(50000d);
        when(broker.buyShares("AAPL", 1)).thenReturn(OrderStatus.OK);

        Mockito.doAnswer(invocation -> {
            final Object[] args = invocation.getArguments();

            assertTrue(args.length == 2);
            assertTrue(args[0].equals(OrderResult.class));
            assertTrue(args[1] instanceof OrderResult);

            final OrderResult orderResult = (OrderResult) args[1];
            assertTrue(orderResult.getOrderStatus().equals(OrderStatus.OK));

            return null;
        }).when(pubSub).publish(any(), any());

        final SortedSet<RankedCommand> rankedCommands = new TreeSet<>(new RankedCommandComparator());
        rankedCommands.add(bestCommand);

        gameEngine.executeBestCommand(new RoundResult(ImmutableMap.of(), rankedCommands));

        verify(pubSub, times(1)).publish(any(), any());
        verify(timeComputer, times(1)).isMarketOpenNow();
        verify(brokerCache, times(1)).getAccountNetWorth();
    }

}
