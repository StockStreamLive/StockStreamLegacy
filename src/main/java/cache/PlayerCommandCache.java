package cache;


import computer.TimeComputer;
import data.RoundResult;
import data.command.Command;
import data.factory.CommandFactory;
import logic.PubSub;
import lombok.extern.slf4j.Slf4j;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class PlayerCommandCache extends ListenerAdapter {
    @Autowired
    private PubSub pubSub;

    @Autowired
    private CommandFactory commandFactory;

    @Autowired
    private TimeComputer timeComputer;

    private final Map<String, Command> playerToCommand = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        pubSub.subscribeRunnable(this::reset, RoundResult.class);
    }

    public synchronized Map<String, Command> getPlayerToCommand() {
        return Collections.unmodifiableMap(playerToCommand);
    }

    @Override
    public synchronized void onMessage(final MessageEvent event) {
        if (!this.timeComputer.isMarketOpenNow()) {
            return;
        }

        final Optional<Command> commandOptional = this.commandFactory.constructCommand(event.getMessage());

        if (!commandOptional.isPresent()) {
            return;
        }

        if (event.getUser() == null) {
            return;
        }

        final String player = event.getUser().getNick();
        final Command command = commandOptional.get();

        playerToCommand.put(player, command);
    }

    private synchronized void reset() {
        playerToCommand.clear();
    }

}
