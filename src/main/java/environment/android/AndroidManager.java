package environment.android;

import data.MarketEvent;
import logic.MarketClock;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;

@Slf4j
public class AndroidManager {

    @Autowired
    private MarketClock marketClock;

    @AllArgsConstructor
    class Coordinate {
        public final int x;
        public final int y;
    }

    private Coordinate robinhoodSearch = new Coordinate(1900, 160);
    private Coordinate robinhoodFirstSearchResult = new Coordinate(1000, 420);
    private Coordinate robinhoodBuyButton = new Coordinate(1500, 1537);
    private Coordinate robinhoodSellButton = new Coordinate(500, 1537);
    private Coordinate robinhoodOrderOkButton = new Coordinate(1875, 1750);
    private Coordinate robinhoodOrderSwipeFrom = new Coordinate(1000, 2190);
    private Coordinate robinhoodOrderSwipeTo = new Coordinate(1000, 420);
    private Coordinate robinhoodDoneButton = new Coordinate(1000, 1525);
    private Coordinate robinhoodBackButton = new Coordinate(100, 165);

    private String robinhoodAppId = "com.robinhood.android";
    private String robinhoodMainActivity = "com.robinhood.android/com.robinhood.android.ui.login.WelcomeActivity";
    private String stockTwitsAppId = "org.stocktwits.android.activity";

    public AndroidManager() {
    }

    @PostConstruct
    public void init() {
        this.marketClock.subscribe(() -> AndroidDevice.restartApp(robinhoodAppId), new MarketEvent(MarketEvent.Status.OPEN, -13, 0));
        this.marketClock.subscribe(AndroidDevice::resetScreen, new MarketEvent(MarketEvent.Status.OPEN, -12, 0));
        this.marketClock.subscribe(AndroidDevice::resetScreen, new MarketEvent(MarketEvent.Status.OPEN, 2, 0));
        this.marketClock.subscribe(AndroidDevice::resetScreen, new MarketEvent(MarketEvent.Status.CLOSE, 2, 0));
    }

    public String restartRobinhoodApp() {
        return AndroidDevice.restartApp(robinhoodAppId);
    }

    public String resetScreen() {
        return AndroidDevice.resetScreen();
    }

    public synchronized Void orderStockWithRobinhoodApp(final String symbol, final String side) {
        AndroidDevice.startApp(robinhoodAppId);
        AndroidDevice.runScript("sleep 2");
        AndroidDevice.tapScreen(robinhoodSearch);
        AndroidDevice.runScript("sleep 1");
        AndroidDevice.sendInput(symbol);
        AndroidDevice.runScript("sleep 1");
        AndroidDevice.tapScreen(robinhoodFirstSearchResult);
        AndroidDevice.runScript("sleep 2");
        if (side.equalsIgnoreCase("buy")) {
            AndroidDevice.tapScreen(robinhoodBuyButton);
        } else {
            AndroidDevice.tapScreen(robinhoodSellButton);
        }
        AndroidDevice.runScript("sleep 1");
        AndroidDevice.sendInput("1");
        AndroidDevice.runScript("sleep 1.2");
        AndroidDevice.tapScreen(robinhoodOrderOkButton);
        AndroidDevice.runScript("sleep 1");
        AndroidDevice.swipeScreen(robinhoodOrderSwipeFrom, robinhoodOrderSwipeTo);
        AndroidDevice.runScript("sleep 1.5");
        AndroidDevice.tapScreen(robinhoodDoneButton);
        AndroidDevice.runScript("sleep 1");
        AndroidDevice.tapScreen(robinhoodBackButton);
        return null;
    }

}
