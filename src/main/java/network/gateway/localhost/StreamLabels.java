package network.gateway.localhost;


import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
public class StreamLabels {

    private static final String STREAMLABELS_DIR = "streamlabels/";

    public String getTopDonor() {
        String topDonor = "nobody: $0";

        try {
            topDonor = new String(Files.readAllBytes(Paths.get(STREAMLABELS_DIR + "all_time_top_donator.txt")));
        } catch (IOException e) {
            log.warn(e.getMessage(), e);
        }

        return topDonor;
    }

}
