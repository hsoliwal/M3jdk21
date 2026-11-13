/* Copyright 2026 Synexia. SPDX-License-Identifier: Apache-2.0 */
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;

/** Only empty constructors and three public observers; no insertions or views. */
public final class SpringEmptyLargeConstructorProbe {
    private static final int[] SIZES = {Integer.MAX_VALUE, Integer.MAX_VALUE - 1,
        Integer.MAX_VALUE - 16, 1073741824, 0, -1, Integer.MIN_VALUE};
    private static final Locale TURKISH = Locale.forLanguageTag("tr");
    private static int invocations;
    private static int returnedEmpty;
    private static int illegalArguments;
    private static int outOfMemory;
    private static int otherFailures;
    private static final StringBuilder trace = new StringBuilder();
    private SpringEmptyLargeConstructorProbe() { }

    private static String quote(String text) {
        if (text == null) return "null";
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"";
    }
    private static void probe(String id, Constructor<?> constructor, Method locale, Object... arguments) {
        invocations++;
        String result;
        try {
            Object made = constructor.newInstance(arguments);
            if (!(made instanceof Map<?, ?> map)) throw new AssertionError("not a Map");
            int size = map.size(); boolean empty = map.isEmpty();
            if (size != 0 || !empty) throw new AssertionError("empty construction result");
            returnedEmpty++;
            result = "{\"status\":\"RETURNED_EMPTY\",\"size\":" + size + ",\"empty\":" + empty
                + ",\"locale\":" + quote(((Locale) locale.invoke(made)).toLanguageTag()) + "}";
        } catch (ReflectiveOperationException caught) {
            Throwable failure = caught instanceof InvocationTargetException wrapped ? wrapped.getCause() : caught;
            if (failure instanceof OutOfMemoryError) outOfMemory++;
            else if (failure instanceof IllegalArgumentException) illegalArguments++;
            else otherFailures++;
            result = "{\"status\":\"THREW\",\"exception\":" + quote(failure.getClass().getSimpleName())
                + ",\"message\":" + quote(failure.getMessage()) + "}";
        }
        String line = "{\"case\":" + quote(id) + ",\"result\":" + result + "}";
        trace.append(line).append('\n'); System.out.println(line);
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("exact provider class required");
        Class<?> owner = Class.forName(args[0]);
        Constructor<?> empty = owner.getConstructor();
        Constructor<?> localeOnly = owner.getConstructor(Locale.class);
        Constructor<?> intOnly = owner.getConstructor(int.class);
        Constructor<?> intLocale = owner.getConstructor(int.class, Locale.class);
        Method getLocale = owner.getMethod("getLocale");
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(TURKISH);
            probe("default", empty, getLocale);
            probe("Locale-ROOT", localeOnly, getLocale, Locale.ROOT);
            probe("Locale-tr", localeOnly, getLocale, TURKISH);
            probe("Locale-null", localeOnly, getLocale, (Object) null);
            for (int size : SIZES) {
                probe("int-only/" + size, intOnly, getLocale, size);
                probe("int-ROOT/" + size, intLocale, getLocale, size, Locale.ROOT);
                probe("int-tr/" + size, intLocale, getLocale, size, TURKISH);
                probe("int-null/" + size, intLocale, getLocale, size, null);
            }
        } finally { Locale.setDefault(previous); }
        if (invocations != 32) throw new AssertionError("frozen invocation count");
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
            .digest(trace.toString().getBytes(StandardCharsets.UTF_8)));
        System.out.println("{\"status\":\"PROVIDER_EMPTY_CONSTRUCTORS_RECORDED\",\"invocations\":" + invocations
            + ",\"returnedEmpty\":" + returnedEmpty + ",\"illegalArguments\":" + illegalArguments
            + ",\"actualOutOfMemoryErrors\":" + outOfMemory + ",\"otherFailures\":" + otherFailures
            + ",\"traceSHA256\":" + quote(digest) + ",\"javaRuntime\":" + quote(System.getProperty("java.runtime.version"))
            + ",\"maxHeapBytes\":" + Runtime.getRuntime().maxMemory()
            + ",\"activeProcessors\":" + Runtime.getRuntime().availableProcessors() + "}");
    }
}
