package network.gateway.aws;


import application.Config;
import application.Stage;
import cache.PlayerScoreCache;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.google.common.collect.ImmutableMap;
import computer.TimeComputer;
import data.JobInterval;
import logic.Scheduler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ScoreUploader {

    private static final Map<Stage, String> buckets =
            ImmutableMap.of(Stage.TEST, "stockstream-data-test",
                            Stage.LOCAL, "stockstream-data-beta",
                            Stage.GAMMA, "stockstream-data-gamma",
                            Stage.PROD, "stockstream-data");

    @Autowired
    private TimeComputer timeComputer;

    @Autowired
    private Scheduler scheduler;

    public ScoreUploader() {
    }

    @PostConstruct
    public void init() {
        scheduler.scheduleJob(this::uploadScores, new JobInterval(1, 5, TimeUnit.MINUTES));
    }

    private void uploadScores() {
        log.info("Uploading scores.");

        final AmazonS3 s3client = new AmazonS3Client(Credentials.PROVIDER);
        s3client.putObject(new PutObjectRequest(buckets.get(Config.stage), "topScores", new File(PlayerScoreCache.TOP_SCORES_FILE)));
    }

}
