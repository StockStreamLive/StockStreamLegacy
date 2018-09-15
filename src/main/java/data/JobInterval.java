package data;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.concurrent.TimeUnit;

@Data
@AllArgsConstructor
public class JobInterval {
    long initialDelay = 0;
    long intervalDelay = 0;
    TimeUnit timeUnit = TimeUnit.SECONDS;
}
