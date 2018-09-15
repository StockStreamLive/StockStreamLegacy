package application;

import com.google.common.collect.ImmutableSet;
import data.JobInterval;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static application.Stage.PROD;

public class Config {

    public static Stage stage = PROD;

    public static final int GOLD_POWER = 2000;

    public static final String AWS_ACCESS_KEY = "AWS_ACCESS_KEY";
    public static final String AWS_SECRET_KEY = "AWS_SECRET_KEY";

    public static final int CPU_CORE_COUNT = Runtime.getRuntime().availableProcessors();

    public static final float MIN_NET_WORTH = 25000;

    public static final Set<String> ADMINS_TWITCH = ImmutableSet.of("stockstream", "michrob", "aveao");

    public static final String RH_UN = "<robinhood_username>";
    public static final String RH_PW = "<robinhood_password>";

    public static final String AFFILIATE_CODE = "tag=stockstream0e-20";

    public static final String STREAMLABS_ACCESS_TOKEN = "0D4F14C05B2491FBF93D";
    public static final String STREAMLABS_CLIENT_ID = "JgGEnr0fDpVSxy16RPNEKwTRrkneqymPsWtySAc0";
    public static final String STREAMLABS_CLIENT_SECRET = "Ye2Zgp0ggeQgknhDMiqoYGloPBEYmmkaaJFB9dlM";

    public static final String GOCR_LOCATION = "/usr/bin/gocr";
    public static final String ADB_LOCATION = "/home/michrob/Android/Sdk/platform-tools/adb";

    public static final String PROMO_URL = "twitch.tv/stockstream";
    public static final String DISCORD_URL = "https://discord.gg/xnrKgEj";

    public static final JobInterval MEDIA_REFRESH = new JobInterval(15, 180, TimeUnit.SECONDS);

}
