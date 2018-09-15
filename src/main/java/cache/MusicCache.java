package cache;

import data.MusicFile;
import logic.Scheduler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MusicCache {
    @Autowired
    private Scheduler scheduler;

    private static final String MUSIC_DIR = "music/";

    // uri -> file
    private Map<String, MusicFile> musicFiles = new ConcurrentHashMap<>();

    public MusicCache() {
    }

    @PostConstruct
    public void init() {
        this.scheduler.scheduleJob(this::reloadMusicFiles, 5, 500, TimeUnit.SECONDS);
    }

    private synchronized void reloadMusicFiles() {
        File curDir = new File(MUSIC_DIR);
        Collection<File> files = FileUtils.listFiles(curDir, null, true);

        files.forEach(file -> {
            final String filePath = file.toURI().toASCIIString();
            if (!filePath.endsWith("mp3")) {
                return;
            }
            if (!musicFiles.containsKey(filePath)) {
                try {
                    musicFiles.put(filePath, new MusicFile(file));
                } catch (Exception e) {
                    log.warn(e.getMessage(), e);
                }
            }
        });
    }

    public synchronized Map<String, MusicFile> getMusicFiles() {
        return Collections.unmodifiableMap(musicFiles);
    }

}
