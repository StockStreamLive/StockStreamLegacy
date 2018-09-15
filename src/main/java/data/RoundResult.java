package data;

import data.command.Command;
import data.command.RankedCommand;
import lombok.AllArgsConstructor;

import java.util.Collections;
import java.util.Map;
import java.util.SortedSet;

@AllArgsConstructor
public class RoundResult {
    private Map<String, Command> playerToCommand;
    private SortedSet<RankedCommand> rankedCommands;

    public SortedSet<RankedCommand> getRankedCommands() {
        return Collections.unmodifiableSortedSet(rankedCommands);
    }

    public Map<String, Command> getPlayerToCommand() {
        return Collections.unmodifiableMap(playerToCommand);
    }
}
