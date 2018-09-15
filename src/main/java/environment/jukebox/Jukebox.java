package environment.jukebox;


import cache.MusicCache;
import computer.TimeComputer;
import data.MusicFile;
import data.factory.AffiliateURLFactory;
import logic.Scheduler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import utils.RandomUtil;

import javax.annotation.PostConstruct;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Jukebox {
    @Autowired
    private Scheduler scheduler;

    @Autowired
    private AffiliateURLFactory affiliateURLFactory;

    @Autowired
    private MusicCache musicCache;

    @Autowired
    private TimeComputer timeComputer;

    private AudioPlayer audioPlayer = new AudioPlayer();

    public Jukebox() {
    }

    @PostConstruct
    public void init() {
        this.scheduler.scheduleJob(this::jukeboxLoop, 10, 1, TimeUnit.SECONDS);
    }

    public String getCurrentTrackURL() {
        return this.affiliateURLFactory.constructURL(audioPlayer.getCurrentTrack());
    }

    private synchronized void jukeboxLoop() {
        final boolean isMarketOpen = this.timeComputer.isMarketOpenNow();

        if (!audioPlayer.isPlaying() && isMarketOpen) {
            playRandomFile();
        }
    }

    private void playRandomFile() {
        final boolean isMarketOpen = this.timeComputer.isMarketOpenNow();
        final Optional<MusicFile> randomFileOptional = RandomUtil.randomChoice(this.musicCache.getMusicFiles().values());
        if (!randomFileOptional.isPresent()) {
            log.warn("Could not play any files, none found market state: {}", isMarketOpen);
            return;
        }

        final MusicFile randomFile = randomFileOptional.get();

        try {
            audioPlayer.playTrack(randomFile);
        } catch (final Exception e) {
            log.warn("Could not play file {} because {}", randomFile, e.getMessage(), e);
        }
    }

}