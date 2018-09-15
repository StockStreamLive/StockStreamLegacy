package data.factory;

import application.Config;
import data.MusicFile;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

@Slf4j
public class AffiliateURLFactory {

    private static final String BASE_URL = "https://www.amazon.com/s/ref=nb_sb_noss?url=search-alias%3Ddigital-music&field-keywords=";

    public String constructURL(final MusicFile musicFile) {
        final String comment = musicFile.getComment();
        if (!StringUtils.isEmpty(comment) && comment.contains("amazon.com")) {
            return comment + "/?" + Config.AFFILIATE_CODE;
        }
        return constructSearchURL(musicFile);
    }

    private String constructSearchURL(final MusicFile musicFile) {
        final StringBuilder searchWords = new StringBuilder();

        if (!StringUtils.isEmpty(musicFile.getArtist())) {
            searchWords.append(" ").append(musicFile.getArtist());
        }

        if (!StringUtils.isEmpty(musicFile.getName())) {
            searchWords.append(" ").append(musicFile.getName());
        }

        if (searchWords.length() == 0) {
            searchWords.append(musicFile.getFile().getName());
        }

        if (!StringUtils.isEmpty(musicFile.getAlbum())) {
            searchWords.append(" ").append(musicFile.getAlbum());
        }

        String parameter;
        try {
            parameter = URLEncoder.encode(searchWords.toString(), "UTF-8");
        } catch (final UnsupportedEncodingException e) {
            log.warn(e.getMessage(), e);
            parameter = musicFile.getFile().getName();
        }

        parameter = parameter.replaceAll("(?i)twitch", "");

        return BASE_URL + parameter + "&" + Config.AFFILIATE_CODE;
    }
}
