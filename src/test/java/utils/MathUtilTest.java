package utils;


import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MathUtilTest {

    @Test
    public void testComputePercentChange_noChange_expectZero() {
        double percentChange = MathUtil.computePercentChange(123d, 123d);
        assertEquals(0, percentChange, .01);
    }

    @Test
    public void testComputePercentChange_100PercentDrop_expectNegative100Percent() {
        double percentChange = MathUtil.computePercentChange(123d, 0);
        assertEquals(-100d, percentChange, .01);
    }

    @Test
    public void testComputePercentChange_someChange_expectCorrectChange() {
        double percentChange = MathUtil.computePercentChange(50d, 75d);
        assertEquals(50d, percentChange, .01);
    }

}
