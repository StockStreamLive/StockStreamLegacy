package logic.score;

import data.command.Command;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
class TradeChoice {
    private String player;
    private Command command;
    private float lastTradePrice;
    private boolean isPossible;
}
