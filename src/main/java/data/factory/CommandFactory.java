package data.factory;


import data.command.ActionType;
import data.command.Command;
import computer.StockComputer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

public class CommandFactory {

    private static final int REJECT_LENGTH = 15;

    @Autowired
    private StockComputer stockComputer;

    public CommandFactory() {
    }

    public Optional<Command> constructCommand(final String input) {
        if (input.length() > REJECT_LENGTH) {
            return Optional.empty();
        }

        final String message = input.trim().toUpperCase();

        if ("!hold".equalsIgnoreCase(message) || "!hodl".equalsIgnoreCase(message)) {
            return Optional.of(new Command(ActionType.HOLD, StringUtils.EMPTY));
        }

        final String[] tokens = message.split("\\s+");

        if (tokens.length != 2) {
            return Optional.empty();
        }

        if (!this.stockComputer.isSymbol(tokens[1])) {
            return Optional.empty();
        }

        final String commandString = tokens[0].substring(1);

        ActionType action = null;
        if ("buy".equalsIgnoreCase(commandString)) {
            action = ActionType.BUY;
        } else if ("sell".equalsIgnoreCase(commandString)) {
            action = ActionType.SELL;
        }

        if (null == action) {
            return Optional.empty();
        }

        final String symbol = tokens[1];

        final Command newCommand = new Command(action, symbol);
        return Optional.of(newCommand);
    }

}
