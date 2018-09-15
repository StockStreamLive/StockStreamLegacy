package data;

import data.command.ActionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResult {
    private ActionType action;
    private String symbol = "";
    private int votesReceived = 0;
    private OrderStatus orderStatus;
}
