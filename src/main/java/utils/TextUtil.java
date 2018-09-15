package utils;


import java.text.Normalizer;
import java.util.regex.Pattern;

public class TextUtil {

    private static final Pattern UNDESIRABLES = Pattern.compile("[(){},;!?<>%]");

    public static String stripNormalPunctuation(final String fromStr) {
        return UNDESIRABLES.matcher(fromStr).replaceAll("");
    }

    public static String replaceNonPrintableText(final String inputText) {
        StringBuilder sb = new StringBuilder(inputText.length());

        String newText = inputText.replace((char) 160, ' ');
        newText = Normalizer.normalize(newText, Normalizer.Form.NFKD);
        for (char c : newText.toCharArray()) {
            if (c <= '\u007F') sb.append(c);
        }
        return sb.toString();
    }

    public static String stripURLS(final String fromStr) {
        final String[] tokens = fromStr.split("\\s+");
        final StringBuilder newString = new StringBuilder();
        for (final String token : tokens) {
            if (token.startsWith("http")) {
                continue;
            }
            newString.append(token).append(" ");
        }
        return newString.toString().trim();
    }

    public static boolean containsURL(final String str) {
        return str.contains("http");
    }
}
