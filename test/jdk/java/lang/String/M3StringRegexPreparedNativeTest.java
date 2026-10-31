/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @requires (os.family == "linux") & vm.jvmti
 * @summary Repeated canonical UTF-16 matching reaches the installed libjava effect kernels
 * @build M3RegexNativeRouteProbe
 * @run main/native/othervm -Xint -Xshare:off -Xcheck:jni --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -agentpath:${test.nativepath}/libM3RegexNativeRouteProbe.so -Dm3.regex.route.probe=${test.nativepath}/libM3RegexNativeRouteProbe.so M3StringRegexPreparedNativeTest
 * @run main/native/othervm -Xint -Xshare:off -Xcheck:jni --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings -agentpath:${test.nativepath}/libM3RegexNativeRouteProbe.so -Dm3.regex.route.probe=${test.nativepath}/libM3RegexNativeRouteProbe.so M3StringRegexPreparedNativeTest
 */
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.regex.Pattern;

/** A disabled prepared caller or disabled JNI power selector must fail this test. */
public final class M3StringRegexPreparedNativeTest {
    private static int checks;
    private static Field storage;
    private static Method decision;

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    private static String repeated(String base) {
        String value = new String(base.toCharArray());
        for (int i = 0; i < 8; i++) value = value.concat(value);
        return value;
    }

    private static void initialize() throws ReflectiveOperationException {
        storage = String.class.getDeclaredField("m3");
        storage.setAccessible(true);
        Class<?> type = Class.forName("java.lang.M3String");
        decision = Class.forName("java.lang.M3StringRegexDp").getDeclaredMethod("tryMatch", type, String.class);
        decision.setAccessible(true);
    }

    private static void observed(String text, boolean publicCall, boolean boundaryPower)
            throws ReflectiveOperationException {
        String regex = ".*".repeat(16);
        check(Pattern.compile(regex).matcher(new StringBuilder(text)).matches(), "independent positive oracle");
        Object source = storage.get(text);
        check(source != null, "actual canonical M3 String storage required");
        long[] counts;
        M3RegexNativeRouteProbe.arm();
        try {
            if (publicCall) check(text.matches(regex), "public prepared UTF16 match");
            else check((int) decision.invoke(null, source, regex) == 1, "definite direct prepared UTF16 match");
        } finally {
            M3RegexNativeRouteProbe.disarm();
            counts = M3RegexNativeRouteProbe.counts();
        }
        check(counts.length == 3, "native counter shape");
        if (boundaryPower) {
            check(counts[M3RegexNativeRouteProbe.REPEAT] == 0, "boundary metadata must not use repeat0");
            check(counts[M3RegexNativeRouteProbe.COMPOSE]
                    + counts[M3RegexNativeRouteProbe.STRUCTURED_COMPOSE] > 0,
                    "boundary-aware power must reach existing ordered composition JNI");
        } else {
            check(counts[M3RegexNativeRouteProbe.REPEAT] > 0, "required installed repeat0 route");
        }
        check(M3RegexNativeRouteProbe.bindingErrors() == 0, "native forwarding preserves bindings");
        System.out.println("M3_PREPARED_NATIVE public=" + publicCall + " boundary=" + boundaryPower
                + " entries=" + Arrays.toString(counts));
    }

    public static void main(String[] ignored) throws ReflectiveOperationException {
        initialize();
        // Paired surrogate is internal to the base: its completed effect has no open boundary.
        String closed = repeated("a\uD83D\uDE00b");
        observed(closed, false, false);
        observed(closed, true, false);
        // The low/high boundary requires pair repair during ordered composition, not repeat0.
        String open = repeated("\uDC00\uD800");
        observed(open, false, true);
        observed(open, true, true);
        check((M3RegexNativeRouteProbe.boundMask() & 1) != 0, "repeat0 was bound");
        System.out.println("M3_PREPARED_NATIVE checks=" + checks);
    }
}
