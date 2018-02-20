package daris.plugin.services;

public class ObjectUtils {

    public static <T> boolean equals(T a, T b) {
        return (a == null && b == null) || (a != null && a.equals(b));
    }

}
