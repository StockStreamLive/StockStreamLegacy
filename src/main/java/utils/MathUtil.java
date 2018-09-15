package utils;


public class MathUtil {

    public static float roundToTick(double value, float tick) {
        if (tick <= 0) {
            return (float) value;
        }

        float tickMod = tick * 100;
        float round = Math.round( ( value * 100 )/ (tickMod)) * (tickMod);
        return round / 100;
    }

    public static double computePercentChange(final double valueThen, final double valueNow) {
        final double change = valueNow - valueThen;

        return change / valueThen * 100;
    }

}
