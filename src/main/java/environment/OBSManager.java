package environment;


import com.google.common.collect.ImmutableList;
import data.MarketEvent;
import logic.MarketClock;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import utils.ProcUtil;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class OBSManager {

    @Autowired
    private MarketClock marketClock;

    private static final List<String> GET_OBS_WINDOWID =
            ImmutableList.of("xdotool", "search", "--name", "TwitchInvests");

    private static final List<String> CHECK_OBS_STREAMING =
            ImmutableList.of("/bin/bash", "-c", "ss -p | grep macromedia | grep EST");

    private static final List<String> START_OBS_STREAM =
            ImmutableList.of("xdotool", "windowactivate", "--sync", "<replace>", "key", "Ctrl+A");

    private static final List<String> STOP_OBS_STREAM =
            ImmutableList.of("xdotool", "windowactivate", "--sync", "<replace>", "key", "Ctrl+Z");

    public OBSManager() {
    }

    @PostConstruct
    public void init() {
        this.marketClock.subscribe(this::startOBSStream, new MarketEvent(MarketEvent.Status.OPEN, -10, 0));
        this.marketClock.subscribe(this::stopOBSStream, new MarketEvent(MarketEvent.Status.CLOSE, 10, 0));
    }

    public synchronized String startOBSStream() {
        final Optional<String> obsWindowId = ProcUtil.runCommandAndWait(GET_OBS_WINDOWID);
        if (!obsWindowId.isPresent()) {
            final String error = "Could not get OBS X Window ID!";
            log.error(error);
            return error;
        }
        final ArrayList<String> startCommand = new ArrayList<>(START_OBS_STREAM);
        startCommand.set(3, obsWindowId.get());
        log.info("Starting obs broadcast.");
        return ProcUtil.runCommandAndWait(startCommand).orElse(String.format("Ran %s got no output.", StringUtils.join(startCommand, " ")));
    }

    public synchronized String stopOBSStream() {
        final Optional<String> obsWindowId = ProcUtil.runCommandAndWait(GET_OBS_WINDOWID);
        if (!obsWindowId.isPresent()) {
            final String error = "Could not get OBS X Window ID!";
            log.error(error);
            return error;
        }
        final ArrayList<String> stopCommand = new ArrayList<>(STOP_OBS_STREAM);
        stopCommand.set(3, obsWindowId.get());
        log.info("Stopping obs broadcast.");
        return ProcUtil.runCommandAndWait(stopCommand).orElse(String.format("Ran %s got no output.", StringUtils.join(stopCommand, " ")));
    }

}
