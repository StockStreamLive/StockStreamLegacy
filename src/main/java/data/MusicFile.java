package data;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;

@Data
@Slf4j
public class MusicFile {

    private File file;
    private Mp3File mp3File;

    private String comment = StringUtils.EMPTY;
    private String artist = StringUtils.EMPTY;
    private String name = StringUtils.EMPTY;
    private String album = StringUtils.EMPTY;

    public MusicFile(final File file) throws Exception {
        this.file = file;
        try {
            this.mp3File = new Mp3File(file.getAbsoluteFile());
            if (this.mp3File.hasId3v2Tag()) {
                final ID3v2 id3v2Tag = mp3File.getId3v2Tag();
                comment = id3v2Tag.getComment();

                if (!StringUtils.isEmpty(id3v2Tag.getArtist())) {
                    artist = id3v2Tag.getArtist();
                }
                if (!StringUtils.isEmpty(id3v2Tag.getAlbumArtist())) {
                    artist += " " + id3v2Tag.getAlbumArtist();
                }

                if (!StringUtils.isEmpty(id3v2Tag.getTitle())) {
                    name = id3v2Tag.getTitle();
                }

                if (!StringUtils.isEmpty(id3v2Tag.getTitle())) {
                    album = id3v2Tag.getAlbum();
                }
            }
        } catch (final IOException ex) {
            log.warn(ex.getMessage());
        }
    }

    public String getURI() {
        return file.toURI().toASCIIString();
    }

    @Override
    public String toString() {
        return String.format("%s", file.getAbsoluteFile());
    }
}
