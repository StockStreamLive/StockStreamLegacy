package network.http;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class HTTPResult {
    private final int statusCode;
    private final Map<String, String> headers;
    private final String body;

    public boolean isOk() {
        return statusCode == 200;
    }
}
