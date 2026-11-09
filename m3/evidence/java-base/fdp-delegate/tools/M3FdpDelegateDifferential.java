// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
// Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
// Gate tool (not shipped): FDP_DELEGATES_TO_JDK v1 differential. The upstream FastDoubleParser 1.0.90 facades and
// the M3 overlay facades are loaded side by side (one class loader each) and fed one deterministic corpus through
// every delegated method: CharSequence (String and StringBuilder), byte[] and char[], whole and offset/length
// (the literal embedded in random padding). Accepted input: identical raw bits. Rejected input: the same exception
// class; messages are tallied, they differ by design (the overlay reports the JDK's). Illegal bounds and null are
// probed too. Usage: java M3FdpDelegateDifferential <upstream.jar> <overlay.jar> <count> <report.tsv> [<bits.tsv>]
// The optional <bits.tsv> lists every raw-bits difference (method, literal, upstream, overlay) for re-verification.
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.TreeMap;

public final class M3FdpDelegateDifferential {
    private static final String PACKAGE = "ch.randelshofer.fastdoubleparser.";
    private static final String[] FACADES = {"JavaDoubleParser", "JavaFloatParser"};
    private static final String[] METHODS = {"parseDouble", "parseFloat"};
    private static final Class<?>[] RESULTS = {double.class, float.class};
    private static final Class<?>[] INPUTS = {CharSequence.class, byte[].class, char[].class};
    private static final String[] INPUT_NAMES = {"CharSequence", "byte[]", "char[]"};
    private static final String PADDING = "0123456789 .eE-+x";
    private static final String TAB = "\t";
    private static final String NEWLINE = "\n";
    private static final String ECHO = "For input string: ";
    private static final String ECHO_NORMALIZED = ECHO + "<input>";
    private static final String NULL_TEXT = "<null>";
    private static final String STATUS_PASS = "PASS";
    private static final String STATUS_FAIL = "FAIL";
    private static final int MAX_PAD = 3;
    private static final int EXAMPLES = 12;
    private static final int BOUNDS_PROBES = 4096;
    private static final int LATIN1_LIMIT = 0x100;
    private static final int ARGUMENTS = 4;
    private static final int ARGUMENTS_WITH_DIFFERENCES = 5;
    private static final int EXIT_USAGE = 2;
    private static final int EXIT_MISMATCH = 1;
    private static final long PAD_SEED = M3FdpParseCorpus.SEED + 1;
    private static final long FLOAT_BITS = 0xffff_ffffL;
    private static final String PROBE_LITERAL = "1.5";
    private static final int[] BOUND_VALUES = {-1, -2, Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 1, 2, 3, 4, 5};
    private static final List<String> DIFFERENCES = new ArrayList<>();

    private M3FdpDelegateDifferential() {
    }

    /** One delegated method in both builds, adapted to (Object input, int offset, int length) -> raw bits. */
    private record Method(String name, boolean ranged, int input, MethodHandle upstream, MethodHandle overlay,
                          Tally tally) {
    }

    private static final class Tally {
        long inputs, accepted, rejected, bitsEqual, bitsDiffer, acceptDiffer, classDiffer, indexFamily, messageDiffer;
        final Map<String, Long> messages = new TreeMap<>();
        final List<String> examples = new ArrayList<>();
    }

    public static void main(String[] args) throws Throwable {
        if (args.length != ARGUMENTS && args.length != ARGUMENTS_WITH_DIFFERENCES) {
            System.err.println("usage: M3FdpDelegateDifferential <upstream.jar> <overlay.jar> <count> <report.tsv> [<bits.tsv>]");
            System.exit(EXIT_USAGE);
        }
        List<Method> methods = methods(loader(args[0]), loader(args[1]));
        List<String> corpus = M3FdpParseCorpus.literals(Integer.parseInt(args[2]));
        SplittableRandom padding = new SplittableRandom(PAD_SEED);
        for (int i = 0; i < corpus.size(); i++) {
            String pre = pad(padding);
            String post = pad(padding);
            for (Method method : methods) {
                feed(method, corpus.get(i), pre, post, i % 2 == 0);
            }
        }
        for (Method method : methods) {
            probe(method);
        }
        boolean pass = methods.stream().allMatch(m -> m.tally.bitsDiffer + m.tally.acceptDiffer + m.tally.classDiffer == 0);
        String report = report(methods, corpus, pass);
        Files.writeString(Path.of(args[3]), report, StandardCharsets.UTF_8);
        if (args.length == ARGUMENTS_WITH_DIFFERENCES) {
            Files.writeString(Path.of(args[ARGUMENTS]), DIFFERENCES.stream().map(d -> d + NEWLINE)
                    .collect(java.util.stream.Collectors.joining()), StandardCharsets.UTF_8);
        }
        System.out.print(report.substring(0, report.indexOf("#messages")));
        System.exit(pass ? 0 : EXIT_MISMATCH);
    }

    private static ClassLoader loader(String jar) throws Exception {
        return new URLClassLoader(new URL[] {Path.of(jar).toUri().toURL()}, ClassLoader.getPlatformClassLoader());
    }

    private static List<Method> methods(ClassLoader upstream, ClassLoader overlay) throws Exception {
        List<Method> methods = new ArrayList<>();
        for (int facade = 0; facade < FACADES.length; facade++) {
            for (int input = 0; input < INPUTS.length; input++) {
                for (boolean ranged : new boolean[] {false, true}) {
                    String name = FACADES[facade] + "." + METHODS[facade] + "(" + INPUT_NAMES[input]
                            + (ranged ? ",int,int)" : ")");
                    methods.add(new Method(name, ranged, input, handle(upstream, facade, input, ranged),
                            handle(overlay, facade, input, ranged), new Tally()));
                }
            }
        }
        return methods;
    }

    private static MethodHandle handle(ClassLoader loader, int facade, int input, boolean ranged) throws Exception {
        Class<?> owner = Class.forName(PACKAGE + FACADES[facade], true, loader);
        MethodType type = ranged ? MethodType.methodType(RESULTS[facade], INPUTS[input], int.class, int.class)
                : MethodType.methodType(RESULTS[facade], INPUTS[input]);
        MethodHandle target = MethodHandles.publicLookup().findStatic(owner, METHODS[facade], type);
        if (!ranged) {
            target = MethodHandles.dropArguments(target, 1, int.class, int.class);
        }
        MethodHandles.Lookup self = MethodHandles.lookup();
        MethodHandle bits = facade == 0
                ? self.findStatic(Double.class, "doubleToRawLongBits", MethodType.methodType(long.class, double.class))
                : self.findStatic(M3FdpDelegateDifferential.class, "floatBits", MethodType.methodType(long.class, float.class));
        target = MethodHandles.filterReturnValue(target, bits);
        return target.asType(MethodType.methodType(long.class, Object.class, int.class, int.class));
    }

    private static long floatBits(float value) {
        return Float.floatToRawIntBits(value) & FLOAT_BITS;
    }

    private static String pad(SplittableRandom random) {
        int length = random.nextInt(MAX_PAD + 1);
        StringBuilder text = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            text.append(PADDING.charAt(random.nextInt(PADDING.length())));
        }
        return text.toString();
    }

    private static void feed(Method method, String literal, String pre, String post, boolean asString) {
        String whole = method.ranged ? pre + literal + post : literal;
        int offset = method.ranged ? pre.length() : 0;
        Object input;
        int length;
        if (method.input == 1) {
            byte[] prefix = bytes(method.ranged ? pre : "");
            byte[] core = bytes(literal);
            byte[] suffix = bytes(method.ranged ? post : "");
            byte[] all = new byte[prefix.length + core.length + suffix.length];
            System.arraycopy(prefix, 0, all, 0, prefix.length);
            System.arraycopy(core, 0, all, prefix.length, core.length);
            System.arraycopy(suffix, 0, all, prefix.length + core.length, suffix.length);
            input = all;
            offset = prefix.length;
            length = core.length;
        } else {
            input = method.input == 2 ? whole.toCharArray() : asString ? whole : new StringBuilder(whole);
            length = literal.length();
        }
        compare(method, literal, input, offset, length);
    }

    /** ISO-8859-1 when every char fits a byte, else UTF-8 (the byte facades document both). */
    private static byte[] bytes(String text) {
        boolean latin1 = text.chars().allMatch(c -> c < LATIN1_LIMIT);
        return text.getBytes(latin1 ? StandardCharsets.ISO_8859_1 : StandardCharsets.UTF_8);
    }

    private static void probe(Method method) {
        if (!method.ranged) {
            compare(method, NULL_TEXT, null, 0, 0);
            return;
        }
        SplittableRandom random = new SplittableRandom(PAD_SEED + method.name.hashCode());
        Object input = method.input == 1 ? bytes(PROBE_LITERAL)
                : method.input == 2 ? PROBE_LITERAL.toCharArray() : PROBE_LITERAL;
        for (int i = 0; i < BOUNDS_PROBES; i++) {
            int offset = random.nextBoolean() ? BOUND_VALUES[random.nextInt(BOUND_VALUES.length)] : random.nextInt();
            int length = random.nextBoolean() ? BOUND_VALUES[random.nextInt(BOUND_VALUES.length)] : random.nextInt();
            compare(method, "bounds(" + offset + "," + length + ")", input, offset, length);
        }
        compare(method, NULL_TEXT, null, 0, 0);
    }

    private static void compare(Method method, String label, Object input, int offset, int length) {
        Object upstream = outcome(method.upstream, input, offset, length);
        Object overlay = outcome(method.overlay, input, offset, length);
        Tally tally = method.tally;
        tally.inputs++;
        if (upstream instanceof Long a && overlay instanceof Long b) {
            tally.accepted++;
            if (a.longValue() == b.longValue()) {
                tally.bitsEqual++;
            } else {
                tally.bitsDiffer++;
                DIFFERENCES.add(method.name + TAB + escape(label) + TAB + render(upstream) + TAB + render(overlay));
                example(tally, "bits", label, upstream, overlay);
            }
        } else if (upstream instanceof Throwable a && overlay instanceof Throwable b) {
            tally.rejected++;
            classify(tally, label, a, b);
        } else {
            tally.acceptDiffer++;
            example(tally, "accept", label, upstream, overlay);
        }
    }

    private static void classify(Tally tally, String label, Throwable upstream, Throwable overlay) {
        if (upstream.getClass() != overlay.getClass()) {
            if (upstream instanceof IndexOutOfBoundsException && overlay instanceof IndexOutOfBoundsException) {
                tally.indexFamily++;
            } else {
                tally.classDiffer++;
                example(tally, "exception-class", label, upstream, overlay);
            }
        }
        String a = normalized(upstream);
        String b = normalized(overlay);
        if (!a.equals(b)) {
            tally.messageDiffer++;
        }
        tally.messages.merge(a + TAB + b, 1L, Long::sum);
    }

    private static String normalized(Throwable failure) {
        String message = failure.getMessage();
        String text = message == null ? NULL_TEXT : message.startsWith(ECHO) ? ECHO_NORMALIZED : message;
        return failure.getClass().getSimpleName() + ": " + text;
    }

    private static Object outcome(MethodHandle handle, Object input, int offset, int length) {
        try {
            return (long) handle.invokeExact(input, offset, length);
        } catch (Throwable failure) {
            return failure;
        }
    }

    private static void example(Tally tally, String kind, String label, Object upstream, Object overlay) {
        if (tally.examples.size() < EXAMPLES) {
            tally.examples.add(kind + TAB + escape(label) + TAB + render(upstream) + TAB + render(overlay));
        }
    }

    private static String render(Object outcome) {
        return outcome instanceof Long bits ? "0x" + Long.toHexString(bits) : escape(String.valueOf(outcome));
    }

    private static String escape(String text) {
        StringBuilder out = new StringBuilder();
        text.chars().forEach(c -> out.append(c >= ' ' && c < 0x7f && c != '\\' ? String.valueOf((char) c)
                : String.format("\\u%04x", c)));
        return out.toString();
    }

    private static String report(List<Method> methods, List<String> corpus, boolean pass) {
        StringBuilder out = new StringBuilder();
        out.append("#M3FdpDelegateDifferential").append(TAB).append("corpus=literals").append(TAB)
                .append("count=").append(corpus.size()).append(TAB).append("sha256=")
                .append(M3FdpParseCorpus.sha256(corpus)).append(TAB).append("seed=").append(M3FdpParseCorpus.SEED)
                .append(TAB).append("java=").append(Runtime.version()).append(NEWLINE);
        out.append("method\tinputs\taccepted\trejected\tbits_equal\tbits_differ\taccept_differ\t"
                + "exception_class_differ\tindex_family\tmessage_differ\n");
        long total = 0;
        for (Method m : methods) {
            Tally t = m.tally;
            total += t.inputs;
            out.append(String.join(TAB, m.name, Long.toString(t.inputs), Long.toString(t.accepted),
                    Long.toString(t.rejected), Long.toString(t.bitsEqual), Long.toString(t.bitsDiffer),
                    Long.toString(t.acceptDiffer), Long.toString(t.classDiffer), Long.toString(t.indexFamily),
                    Long.toString(t.messageDiffer))).append(NEWLINE);
        }
        out.append("RESULT").append(TAB).append(pass ? STATUS_PASS : STATUS_FAIL).append(TAB).append("calls_per_build=")
                .append(total).append(NEWLINE);
        out.append("#messages\tmethod\tcount\tupstream\toverlay\n");
        methods.forEach(m -> m.tally.messages.forEach((pair, count) ->
                out.append("message").append(TAB).append(m.name).append(TAB).append(count).append(TAB)
                        .append(escape(pair).replace("\\u0009", TAB)).append(NEWLINE)));
        out.append("#examples\tmethod\tkind\tinput\tupstream\toverlay\n");
        methods.forEach(m -> m.tally.examples.forEach(e ->
                out.append("example").append(TAB).append(m.name).append(TAB).append(e).append(NEWLINE)));
        return out.toString();
    }
}
