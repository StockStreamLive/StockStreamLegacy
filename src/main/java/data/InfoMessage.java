package data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InfoMessage {
    private String sender;
    private String text;
    private long timestamp;
    private String platform;
    private String url;

    public String getTimeAgo() {
        final long now = new Date().getTime();
        final long msAgo = now - timestamp;


        long unitCount = 0;
        String unitString = "";

        if (msAgo > 86400000) {
            unitCount = TimeUnit.MILLISECONDS.toDays(msAgo);
            unitString = "day";
        } else if (msAgo > 3600000) {
            unitCount = TimeUnit.MILLISECONDS.toHours(msAgo);
            unitString = "hour";
        } else if (msAgo > 60000) {
            unitCount = TimeUnit.MILLISECONDS.toMinutes(msAgo);
            unitString = "minute";
        } else {
            unitCount = TimeUnit.MILLISECONDS.toSeconds(msAgo);
            unitString = "second";
        }

        if (unitCount > 1) {
            unitString += "s";
        }


        return String.format("%s %s ago", unitCount, unitString);
    }
}
