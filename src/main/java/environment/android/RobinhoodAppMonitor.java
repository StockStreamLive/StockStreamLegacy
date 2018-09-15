package environment.android;


import application.Config;
import cache.BrokerCache;
import com.google.common.collect.ImmutableList;
import computer.TimeComputer;
import logic.Scheduler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import utils.ProcUtil;
import utils.TimeUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RobinhoodAppMonitor {
    private final BrokerCache brokerCache;
    private final TimeComputer timeComputer;

    private static final String FILENAME = "androidScreenshot.png";
    private static final String FILENAME_CROPPED = "androidScreenshotCropped.png";

    private String lastAppBalance = StringUtils.EMPTY;

    public RobinhoodAppMonitor(final Scheduler scheduler,
                               final BrokerCache brokerCache,
                               final TimeComputer timeComputer) {
        this.brokerCache = brokerCache;
        this.timeComputer = timeComputer;
        scheduler.scheduleJob(this::checkUpdateApp, 1, 1, TimeUnit.MINUTES);
    }

    private void checkUpdateApp() {
        if (!timeComputer.isMarketOpenNow()) {
            return;
        }
        if (this.brokerCache.getAssets().size() <= 0) {
            log.warn("No owned assets, so not doing robinhood app check!");
            return;
        }

        AndroidDevice.takeScreenshot(FILENAME);

        try {
            cutImage();
        } catch (final IOException e) {
            log.warn("Exception cutting image so not doing robinhood app check!");
            return;
        }

        final Optional<String> balanceOptional = ProcUtil.runCommandAndWait(ImmutableList.of(Config.GOCR_LOCATION, FILENAME_CROPPED));

        if (!balanceOptional.isPresent()) {
            log.warn("No output from OCR, not doing robinhood app check!");
            return;
        }

        final String newAppBalance = balanceOptional.get().replaceAll("\\s+","");

        if (StringUtils.isEmpty(lastAppBalance)) {
            log.info("No last balance, setting balance to {} and skipping app check.", newAppBalance);
            lastAppBalance = newAppBalance;
            return;
        }

        if (newAppBalance.equals(lastAppBalance)) {
            log.warn("New app balance of {} same as last app balance {}, so resetting screen.", newAppBalance, lastAppBalance);
            AndroidDevice.resetScreen();
        }

        lastAppBalance = newAppBalance;
    }

    private void cutImage() throws IOException {
        final File inputFile = new File(FILENAME);
        final BufferedImage inputImage = ImageIO.read(inputFile);
        BufferedImage dest = inputImage.getSubimage(675, 260, 625, 140);

        ImageIO.write(dest, "PNG", new FileOutputStream(FILENAME_CROPPED));
    }
}
