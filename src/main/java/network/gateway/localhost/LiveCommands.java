package network.gateway.localhost;

import com.google.common.collect.ImmutableSet;
import data.TwitchMessage;
import data.command.Command;
import data.factory.CommandFactory;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;
import org.springframework.beans.factory.annotation.Autowired;
import utils.JSONUtil;
import utils.RandomUtil;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@WebSocket
public class LiveCommands extends ListenerAdapter {

    @Autowired
    private CommandFactory commandFactory;

    private static final Set<String> COLORS = ImmutableSet.of("#fa5311", "#00d097");

    private final static Map<Session, String> usernameMap = new ConcurrentHashMap<>();
    private int nextUserNumber = 1;

    public LiveCommands() {
    }

    @Override
    public void onMessage(final MessageEvent event) {
        final String eventMessage = event.getMessage();
        log.debug("processing message {}", eventMessage);

        Optional<Command> commandOptional = this.commandFactory.constructCommand(eventMessage);

        if (!commandOptional.isPresent()) {
            return;
        }

        if (event.getUser() == null) {
            return;
        }

        String color = RandomUtil.randomChoice(COLORS).get();
        color = event.getV3Tags().getOrDefault("color", color);

        final TwitchMessage twitchMessage = new TwitchMessage(eventMessage, event.getUser().getNick(), color);

        final Optional<String> broadcast = JSONUtil.serializeObject(twitchMessage);

        if (!broadcast.isPresent()) {
            return;
        }

        broadcastString(broadcast.get());
    }

    private void broadcastString(final String string) {
        usernameMap.keySet().stream().filter(Session::isOpen).forEach(session -> {
            try {
                session.getRemote().sendString(string, new WriteCallback() {
                    @Override
                    public void writeFailed(final Throwable x) {
                        log.warn("Can't send message to command list {}", x.toString());
                    }

                    @Override
                    public void writeSuccess() {
                    }
                });
            } catch (final Exception e) {
                log.warn(e.getMessage(), e);
            }
        });
    }

    @OnWebSocketConnect
    public void onConnect(final Session user) throws Exception {
        String username = "User" + nextUserNumber++;
        usernameMap.put(user, username);
    }

    @OnWebSocketClose
    public void onClose(final Session user, final int statusCode, final String reason) {
        usernameMap.remove(user);
    }

    @OnWebSocketMessage
    public void onMessage(final Session user, final String message) {
    }

}
