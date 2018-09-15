package data;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TwitchMessage {
    private String text;
    private String sender;
    private String color;
}
