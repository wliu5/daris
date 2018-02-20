package daris.util;

import java.util.Collection;

public class StringUtils {

    public static String join(Collection<String> strs, String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (String str : strs) {
            append(sb, delimiter, str);
        }
        return sb.toString();
    }

    public static void append(StringBuilder sb, String delimiter, String str) {
        if (sb.length() > 0) {
            sb.append(delimiter);
        }
        sb.append(str);
    }

}
