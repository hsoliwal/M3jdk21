// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import java.security.Permission;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PropertyPermission;
import jdk.m3.text.Limits;
import jdk.m3.text.TextFrame;
import jdk.m3.text.TextLine;
import jdk.m3.text.TextTools;

/** Optional JNI remains optional when a Java 21 security manager denies its configuration/load. */
@SuppressWarnings("removal") // Exercise the supported Java 21 permission boundary in isolated JVMs.
public final class M3OptionalNativeFallbackRegression {
    private M3OptionalNativeFallbackRegression() { }

    private static void same(Object expected, Object actual) {
        if (!expected.equals(actual))
            throw new AssertionError("expected " + expected + ", got " + actual);
    }

    private static void lineFallback() {
        same(false, TextTools.nativeTextAvailable());
        List<TextLine> values = List.of(new TextLine("z", "\n"), new TextLine("a", "\r\n"),
                new TextLine("a", "\n"), new TextLine("\u0000", "\r"),
                new TextLine("\ud83d\ude00", "\n"), new TextLine("12345678a", "\r\n"));
        for (int size : new int[] {0, 1, 2, 63, 64, 65, 127, 128, 129}) {
            List<TextLine> lines = new ArrayList<>();
            for (int i = 0; i < size; i++) lines.add(values.get((i / 2) % values.size()));
            TextFrame frame = new TextFrame(lines);
            for (boolean reverse : new boolean[] {false, true}) {
                List<TextLine> expected = new ArrayList<>(lines);
                Comparator<TextLine> order = Comparator.comparing(TextLine::content);
                expected.sort(reverse ? order.reversed() : order);
                same(expected, TextTools.sort(frame, reverse, Limits.standard()).lines());
            }
            List<TextLine> expected = new ArrayList<>();
            String previous = null;
            for (TextLine line : lines) {
                if (!line.content().equals(previous)) expected.add(line);
                previous = line.content();
            }
            same(expected, TextTools.uniq(frame, Limits.standard()).lines());
        }
        same(false, TextTools.nativeTextAvailable());
    }

    private static void inputDenialPropagates() {
        SecurityException marker = new SecurityException("input access is not optional native loading");
        CharSequence denied = new CharSequence() {
            @Override public int length() { return 2; }
            @Override public char charAt(int index) { throw marker; }
            @Override public CharSequence subSequence(int from, int to) { throw new AssertionError("copy"); }
        };
        for (boolean pattern : new boolean[] {false, true}) {
            try {
                if (pattern) TextTools.containsLiteral("aa", denied);
                else TextTools.containsLiteral(denied, "a");
                throw new AssertionError("input SecurityException was swallowed");
            } catch (SecurityException observed) {
                if (observed != marker) throw new AssertionError("input exception identity", observed);
            }
        }
    }

    private static void literalFallback() {
        same(true, TextTools.containsLiteral("x\u0000\ud800", "\u0000\ud800"));
        same(false, TextTools.containsLiteral("x\u0000\ud800", "\u0000\udc00"));
        TextFrame frame = new TextFrame(List.of(new TextLine("a\u0000x", "\n"),
                new TextLine("a", "\r\n"), new TextLine("\u0000", "\r")));
        same(List.of(frame.lines().get(0), frame.lines().get(2)),
                TextTools.grepLiteral(frame, "\u0000", Limits.standard()).lines());
        // Reuse the existing UTF-16/generic-observation oracle unchanged.
        M3TextKernelRegression.main(new String[] {"java"});
        inputDenialPropagates();
    }

    public static void main(String[] arguments) {
        if (arguments.length != 1 || !(arguments[0].equals("line-load-denied")
                || arguments[0].equals("literal-property-denied")
                || arguments[0].equals("literal-load-denied")))
            throw new IllegalArgumentException("one optional native denial mode required");
        String mode = arguments[0];
        Denial permission = new Denial(mode);
        SecurityManager previous = System.getSecurityManager();
        System.setSecurityManager(permission);
        try {
            if (mode.equals("line-load-denied")) {
                lineFallback();
                same(1, permission.deniedLoads);
                same(0, permission.deniedProperties);
            } else {
                literalFallback();
                same(mode.equals("literal-property-denied") ? 1 : 0, permission.deniedProperties);
                same(mode.equals("literal-load-denied") ? 1 : 0, permission.deniedLoads);
                if (mode.equals("literal-property-denied")) same(0, permission.literalLoadAttempts);
            }
            System.out.println("M3_OPTIONAL_NATIVE_FALLBACK_PASS mode=" + mode);
        } finally {
            System.setSecurityManager(previous);
        }
    }

    private static final class Denial extends SecurityManager {
        private final String mode;
        private int deniedProperties;
        private int deniedLoads;
        private int literalLoadAttempts;

        Denial(String mode) { this.mode = mode; }

        @Override public void checkPermission(Permission permission) {
            if (mode.equals("literal-property-denied") && permission instanceof PropertyPermission
                    && permission.getName().equals("m3.text.native.disable")
                    && permission.getActions().contains("read")) {
                deniedProperties++;
                throw new SecurityException("denied optional literal configuration");
            }
        }

        @Override public void checkLink(String library) {
            if (library.contains("m3textio")) literalLoadAttempts++;
            if ((mode.equals("line-load-denied") && library.contains("m3textops"))
                    || (mode.equals("literal-load-denied") && library.contains("m3textio"))) {
                deniedLoads++;
                throw new SecurityException("denied optional native library " + library);
            }
        }
    }
}
