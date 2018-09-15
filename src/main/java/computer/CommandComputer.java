package computer;

import cache.PlayerCommandCache;
import data.command.Command;
import data.command.RankedCommand;
import data.comparator.RankedCommandComparator;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

public class CommandComputer {

    @Autowired
    private PlayerCommandCache playerCommandCache;

    public SortedSet<RankedCommand> computeRankedCommands() {
        final Map<String, Command> playerToCommand = playerCommandCache.getPlayerToCommand();

        final Map<Command, RankedCommand> commandToRankedCommand = new ConcurrentHashMap<>();

        playerToCommand.forEach((key, command) -> {
            final RankedCommand rankedCommand = commandToRankedCommand.computeIfAbsent(command, comm -> new RankedCommand(command, 0));
            rankedCommand.setVotes(rankedCommand.getVotes() + 1);
        });

        final SortedSet<RankedCommand> rankedCommands = new TreeSet<>(new RankedCommandComparator());
        rankedCommands.addAll(commandToRankedCommand.values());

        return rankedCommands;
    }

}
