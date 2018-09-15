package environment.jukebox;


import data.MusicFile;
import javazoom.jl.player.advanced.AdvancedPlayer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;

@Slf4j
public class AudioPlayer {

    private class PlayerWrapper implements Runnable {
        private AdvancedPlayer player;

        @Getter private boolean isPlaying = false;
        @Getter private MusicFile currentTrack;

        public PlayerWrapper(final MusicFile currentTrack) {
            this.currentTrack = currentTrack;
        }

        @Override
        public void run() {
            log.info("Playing track {}", currentTrack);
            try {
                player = new AdvancedPlayer(new URL(currentTrack.getURI()).openStream());
                isPlaying = true;
                player.play();
            } catch (final Exception e) {
                log.warn(e.getMessage(), e);
            }

            isPlaying = false;
        }
    }

    private Thread playerThread;
    private PlayerWrapper playerWrapper;

    public void playTrack(final MusicFile musicFile) {
        playerWrapper = new PlayerWrapper(musicFile);
        playerThread = new Thread(playerWrapper);
        playerThread.start();
    }

    public boolean isPlaying() {
        if (playerThread == null) {
            return false;
        }
        return playerThread.isAlive() && playerWrapper.isPlaying();
    }

    public MusicFile getCurrentTrack() {
        return playerWrapper.getCurrentTrack();
    }

}
