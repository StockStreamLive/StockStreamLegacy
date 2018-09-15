package utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class TextUtilTest {

    @Test
    public void testStripURLS_noURL_expectNoChange() {
        final String testData = "This is a test!";
        assertEquals(testData, TextUtil.stripURLS(testData));
    }

    @Test
    public void testStripURLS_someURL_expectURLGone() {
        final String testData = "This is a test! http://stockstream.live";
        final String modifiedData = TextUtil.stripURLS(testData);
        assertFalse(modifiedData.contains("http"));
    }
}
