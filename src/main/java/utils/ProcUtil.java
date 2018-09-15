package utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Optional;

@Slf4j
public class ProcUtil {

    public static void runCommandAndContinue(final List<String> commandAndArgs) {
        final ProcessBuilder ps = new ProcessBuilder(commandAndArgs);

        try {
            ps.start();
        } catch (Exception ex) {
            log.warn(ex.getMessage(), ex);
        }
    }

    public static Optional<String> runCommandAndWait(final List<String> commandAndArgs) {
        log.info("EXEC: " + commandAndArgs);

        try {
            final ProcessBuilder ps = new ProcessBuilder(commandAndArgs);
            ps.redirectErrorStream(true);

            Process pr = ps.start();

            BufferedReader in = new BufferedReader(new InputStreamReader(pr.getInputStream()));

            StringBuilder commandOutput = new StringBuilder();

            String line;
            while ((line = in.readLine()) != null) {
                commandOutput.append(line);
            }
            pr.waitFor();
            in.close();

            final String outputString = commandOutput.toString();

            log.info(outputString);

            if (StringUtils.isEmpty(outputString)) {
                return Optional.empty();
            }

            return Optional.of(outputString);

        } catch (final Exception ex) {
            log.warn(ex.getMessage(), ex);
        }

        return Optional.empty();
    }
}
