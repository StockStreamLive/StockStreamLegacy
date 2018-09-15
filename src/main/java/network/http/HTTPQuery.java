package network.http;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Map;
import java.util.Objects;

@Data
@AllArgsConstructor
public class HTTPQuery {
    private final String url;
    private final Map<String, String> parameters;
    private final Map<String, String> headers;

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(url).append(parameters).append(headers).toHashCode();
    }

    @Override
    public boolean equals(final Object otherObject) {
        if (!(otherObject instanceof HTTPQuery)) {
            return false;
        }
        final HTTPQuery otherQuery = (HTTPQuery)  otherObject;
        return (Objects.equals(url, otherQuery.getUrl()) &&
                Objects.equals(parameters, otherQuery.getParameters()) &&
                Objects.equals(headers, otherQuery.getHeaders()));
    }
}
