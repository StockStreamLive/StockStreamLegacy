package logic.game;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GameEvent {

    public enum Type {
        GAME_TICK,
        MARKET_OPEN
    }

    private long nextEvent;
    private Type eventType;

    @Override
    public String toString() {
        return String.format("[%s,%s]", eventType, nextEvent);
    }
}
