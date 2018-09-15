package data;


import data.command.ActionType;
import data.command.Command;
import org.junit.Test;

import static org.junit.Assert.*;

public class CommandTest {

    @Test
    public void testEquals_sameCommand_expectEqual() {
        final Command command1 = new Command(ActionType.BUY, "AAPL");
        final Command command2 = new Command(ActionType.BUY, "AAPL");

        assertTrue(command1.equals(command2));
        assertEquals(command1.hashCode(), command2.hashCode());
    }

    @Test
    public void testEquals_sameActionDiffParam_expectNotEqual() {
        final Command command1 = new Command(ActionType.BUY, "AAPL");
        final Command command2 = new Command(ActionType.BUY, "GOOGL");

        assertFalse(command1.equals(command2));
        assertNotEquals(command1.hashCode(), command2.hashCode());
    }

    @Test
    public void testEquals_sameParamDiffAction_expectNotEqual() {
        final Command command1 = new Command(ActionType.BUY, "AAPL");
        final Command command2 = new Command(ActionType.SELL, "AAPL");

        assertFalse(command1.equals(command2));
        assertNotEquals(command1.hashCode(), command2.hashCode());
    }

}
