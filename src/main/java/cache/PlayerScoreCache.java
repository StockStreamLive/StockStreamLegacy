package cache;


import application.Config;
import application.Stage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableMap;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import utils.JSONUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class PlayerScoreCache {

    @Data
    @AllArgsConstructor
    class Score {
        private final String player;
        private final Double score;
    }

    private static final Map<Stage, String> suffixes =
            ImmutableMap.of(Stage.TEST, "_test",
                            Stage.LOCAL, "_local",
                            Stage.GAMMA, "_gamma",
                            Stage.PROD, "");
    public static final String TOP_SCORES_FILE = String.format("topScores%s", suffixes.get(Config.stage));
    private static final String PLAYER_SCORES_FILE = String.format("scores%s.json", suffixes.get(Config.stage));

    private Map<String, Double> playerScores = new ConcurrentHashMap<>();

    public PlayerScoreCache() {
        playerScores = loadScores();
    }

    private Map<String, Double> loadScores() {
        String jsonScores = "";

        final File scoresFile = new File(PLAYER_SCORES_FILE);

        if (!scoresFile.exists()) {
            return new ConcurrentHashMap<>();
        }

        try {
            jsonScores = new Scanner(scoresFile).useDelimiter("\\Z").next();
        } catch (final Exception e) {
            log.warn(e.getMessage(), e);
        }

        Optional<ConcurrentHashMap<String, Double>> scoreMapOptional =
                JSONUtil.deserializeString(jsonScores, new TypeReference<ConcurrentHashMap<String, Double>>() { });

        final Map<String, Double> scoreMap = scoreMapOptional.orElse(new ConcurrentHashMap<>());

        return scoreMap;
    }

    public synchronized void persistScoreUpdates(final Map<String, Double> scoreModifications) {
        scoreModifications.forEach((key, value) -> {
            final double currentScore = playerScores.computeIfAbsent(key, dubbz -> 0d);
            final double scoreMod = value;
            playerScores.put(key, currentScore + scoreMod);
        });

        final List<Score> topScores = computeTopPlayerScores(50, playerScores);

        atomicWriteJSON(PLAYER_SCORES_FILE, playerScores);
        atomicWriteJSON(TOP_SCORES_FILE, topScores);
    }

    private List<Score> computeTopPlayerScores(final int n, final Map<String, Double> scores) {
        final List<Score> allScores =
                scores.entrySet().stream().map(entry -> new Score(entry.getKey(), entry.getValue())).collect(Collectors.toList());
        allScores.sort((o1, o2) -> o2.getScore().compareTo(o1.getScore()));

        final int scoresToReturn = Math.min(n, allScores.size());
        return allScores.subList(0, scoresToReturn);
    }

    private void atomicWriteJSON(final String outputFile, final Object object) {
        final String tmpFile = outputFile + ".tmp";
        JSONUtil.saveObjectToFile(tmpFile, object);
        try {
            Files.move(new File(tmpFile).toPath(), new File(outputFile).toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Exception moving file {} to {}", tmpFile, outputFile, e);
        }
    }

    public synchronized double getScore(final String player) {
        return playerScores.getOrDefault(player, 0d);
    }

}
