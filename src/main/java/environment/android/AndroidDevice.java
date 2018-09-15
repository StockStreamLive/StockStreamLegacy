package environment.android;

import application.Config;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import utils.ProcUtil;

import java.util.List;

public class AndroidDevice {

    public synchronized static String swipeScreen(final AndroidManager.Coordinate from,
                                                  final AndroidManager.Coordinate to) {
        return runScript("input swipe " + String.valueOf(from.x) + " " + String.valueOf(from.y) + " "
                + String.valueOf(to.x) + " " + String.valueOf(to.y));
    }

    public synchronized static String tapScreen(final AndroidManager.Coordinate coordinate) {
        return tapScreen(coordinate.x, coordinate.y);
    }

    public synchronized static String tapScreen(final int x, final int y) {
        return runScript("input tap " + String.valueOf(x) + " " + String.valueOf(y));
    }

    public synchronized static String sendInput(final String inputText) {
        final String formatted = "\"" + inputText.replace(" ", "%s") + "\"";
        return runScript("input text " + formatted);
    }

    public synchronized static String resetScreen() {
        return runScript("wm size 1000x1200 && wm size reset");
    }

    public synchronized static String runActivity(final String activityPath) {
        return runScript("su root am start -n " + activityPath);
    }

    public synchronized static boolean isAppRunning(final String appId) {
        String output = runScript("ps | grep " + appId + " | wc -l");
        return !StringUtils.isEmpty(output);
    }

    public synchronized static String takeScreenshot(final String outputFilename) {
        final String deviceFilePath = "/sdcard/" + outputFilename;
        final String screenshotOutput = runScript("screencap " + deviceFilePath);
        final String pullOutput = adbPull(deviceFilePath);
        return screenshotOutput + " " + pullOutput;
    }

    public synchronized static String restartApp(final String appId) {
        return killApp(appId) + "\n" + startApp(appId);
    }

    public synchronized static String startApp(final String appId) {
        return runScript("monkey -p " + appId + " -c android.intent.category.LAUNCHER 1");
    }

    public synchronized static String killApp(final String appId) {
        return runScript("am force-stop " + appId);
    }

    public synchronized static String runScript(final String script) {
        final List<String> commandAndArgs = ImmutableList.of(Config.ADB_LOCATION, "shell", script);
        return ProcUtil.runCommandAndWait(commandAndArgs).orElse(StringUtils.EMPTY);
    }

    public synchronized static String adbPull(final String filename) {
        final List<String> commandAndArgs = ImmutableList.of(Config.ADB_LOCATION, "pull", filename);
        return ProcUtil.runCommandAndWait(commandAndArgs).orElse(StringUtils.EMPTY);
    }

}
