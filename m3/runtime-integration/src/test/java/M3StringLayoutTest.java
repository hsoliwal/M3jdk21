// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Bounded source-layout regression corpus; not runtime or ABI qualification. */
final class M3StringLayoutTest {
    private static final String FIELDS = "private final M3StringOwner owner; private final long value;";
    private static final String GOOD = "package java.lang; final class M3String { " + FIELDS
            + " M3String(M3StringOwner o, long v) { owner=o; value=v; } }";
    private static int accepted;
    private static int refused;

    private M3StringLayoutTest() { }

    private static void accept(String source) throws Exception {
        M3StringLayout.check(source);
        accepted++;
    }

    private static void refuse(String source) throws Exception {
        try {
            M3StringLayout.check(source);
        } catch (IllegalStateException expected) {
            refused++;
            return;
        }
        throw new AssertionError("accepted forbidden source: " + source);
    }

    public static void main(String[] args) throws Exception {
        accept(GOOD);
        // Exercise placement and combinations; braces and code in payloads remain opaque.
        var payloads = List.of(
                "/* } private final byte[] cache; class M3String { */",
                "static final String TEXT = \"} private final byte[] cache; {\";",
                "static final String REGEX = \"[{}]\\\\w+\";",
                "static final String BLOCK = \"\"\"\n} private final long impostor; {\n\"\"\";",
                "// } private final long impostor;\n");
        var nested = List.of(
                "static class Cursor { private final M3String source=null; private final int fence=0; }",
                "class Cursor { private final M3String source=null; private final int fence=0; }",
                "void visit() { class Cursor { private final int fence=0; } }",
                "void visit() { Object cursor = new Object() { private final int fence=0; }; }",
                "static final long STATIC_ONLY = 0;");
        for (String payload : payloads) {
            for (String child : nested) {
                accept(GOOD.replace(FIELDS, payload + FIELDS + child));
                accept(GOOD.replace(FIELDS, child + FIELDS + payload));
            }
        }
        accept(GOOD.replace("private final long value", "final private long value"));
        accept(GOOD.replace("value;", "v" + "\\u0061" + "lue;"));
        accept(GOOD.replace("private final long value;", "private /* braces } { */ final long\n value;"));
        for (String extra : List.of(
                "private final long extra;", "long extra;", "private long extra;",
                "protected final long extra;", "public final long extra;",
                "private volatile long extra;", "private transient long extra;",
                "private final long extra=1;", "private final byte[] extra;",
                "private final byte extra[];", "private final long[][] extra;",
                "private final java.util.List<String> extra;", "private final Object extra = new Object();",
                "private final M3StringOwner extra;", "@Deprecated private final long extra;",
                "private final long e" + "\\u0078" + "tra;")) {
            refuse(GOOD.replace(FIELDS, FIELDS + extra));
        }
        for (String changed : List.of(
                "private final M3StringOwner owner; private final int value;",
                "private final M3StringOwner owner; private final long value, extra;",
                "private final M3StringOwner owner; private final long value[];",
                "private final M3StringOwner owner; private final long value = 0;",
                "private final M3StringOwner owner; private static final long value = 0;",
                "private final M3StringOwner owner; private long value;",
                "private final long value;", "", "/* " + FIELDS + " */",
                "static class Decoy { " + FIELDS + " }")) {
            refuse(GOOD.replace(FIELDS, changed));
        }
        refuse(GOOD.replace("package java.lang;", "package other;"));
        refuse(GOOD.replace("package java.lang;", ""));
        refuse(GOOD.replace("final class M3String", "class M3String"));
        refuse(GOOD.replace("final class M3String", "public final class M3String"));
        refuse(GOOD.replace("class M3String", "class M3String<T>"));
        refuse(GOOD.replace("class M3String", "class M3String extends Parent"));
        refuse(GOOD.replace("class M3String", "class WrongName"));
        refuse(GOOD + " final class M3String {}");
        refuse("package java.lang; record M3String(M3StringOwner owner, long value) {}");
        refuse(GOOD.substring(0, GOOD.length() - 1));
        refuse(GOOD.replace(FIELDS, FIELDS + "void broken( {"));
        refuse("package java.lang; /* unterminated");
        if (args.length != 1) throw new AssertionError("expected exact current M3String fixture");
        accept(Files.readString(Path.of(args[0])));
        System.out.println("M3_STRING_LAYOUT_TEST_OK accepted=" + accepted + " refused=" + refused);
    }
}
