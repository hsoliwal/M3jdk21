// Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import java.lang.reflect.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.core.*;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/** QA-only adapter; uses existing production owners without changing their storage or API. */
public final class M3SharedImageProcessProbe {
    private static Object image;
    private static Class<?> owner;
    private static MappedByteBuffer mapping;
    private static int[] offsets, lengths;
    private static int payload;
    private static final List<String> WORDS = words();

    private static List<String> words() {
        var words = new TreeSet<String>(List.of("", "A", "A\u030a", "Stra\u00dfe", "\u00c5",
                "\u0905", "\ud800", "\ud800x", "\ud83d\ude00", "\udfff", "\uffff"));
        for (int i = 0; i < 128; i++) words.add("w" + String.format(Locale.ROOT, "%03d", i));
        return List.copyOf(words);
    }

    private static Object call(String name, Class<?>[] types, Object... arguments) throws Exception {
        var method = owner.getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(image, arguments);
    }

    private static Object field(String name) throws Exception {
        var field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(image);
    }

    private static byte[] digest(byte[] bytes) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(bytes);
    }

    @Test void allCodeUnitsSurviveArenaCopies() throws Exception {
        Object arena = Class.forName("jdk.m3.text.LocalM3Arena").getConstructor().newInstance();
        Class<?> arenaType = arena.getClass();
        for (int i = 0; i < WORDS.size(); i++) {
            CharSequence copy = (CharSequence)call("copyRecord", new Class<?>[]{int.class, arenaType}, i, arena);
            assertEquals(WORDS.get(i), copy.toString(), "row " + i);
            for (int j = 0; j < copy.length(); j++) assertEquals(WORDS.get(i).charAt(j), copy.charAt(j));
        }
    }

    @Test void exactLowerBoundAndPrefixMatchReference() throws Exception {
        var queries = new ArrayList<>(WORDS);
        queries.addAll(List.of("missing", "w", "w000x", "\ud800y", "\uffffx"));
        for (String query : queries) {
            int found = Collections.binarySearch(WORDS, query);
            int lower = found < 0 ? -found - 1 : found;
            assertEquals(found < 0 ? -1 : found, call("findRecord", new Class<?>[]{String.class}, query));
            assertEquals(lower, call("lowerBound", new Class<?>[]{String.class}, query));
        }
        for (int i = 0; i < WORDS.size(); i++)
            for (String prefix : List.of("", "w0", "A", "\ud800", "\uffff"))
                assertEquals(WORDS.get(i).startsWith(prefix),
                        call("startsWith", new Class<?>[]{int.class, String.class}, i, prefix));
    }

    @Test void directoryCoordinatesDescribeMappedUtf16() throws Exception {
        int offset = 0;
        assertEquals(WORDS.size(), offsets.length);
        assertEquals(offsets.length, lengths.length);
        for (int i = 0; i < WORDS.size(); i++) {
            assertEquals(offset, offsets[i]);
            assertEquals(WORDS.get(i).length(), lengths[i]);
            for (int j = 0; j < lengths[i]; j++) {
                int at = payload + 2 * (offset + j);
                assertEquals(WORDS.get(i).charAt(j),
                        (char)((mapping.get(at) & 255) | ((mapping.get(at + 1) & 255) << 8)));
            }
            offset += lengths[i];
        }
        assertEquals((long)offset, call("utf16Units", new Class<?>[]{}));
        assertEquals(payload + 2 * offset, mapping.capacity());
    }

    @Test void mappingIsReadOnlyAndLoadHintIsAdvisory() {
        assertTrue(mapping.isReadOnly(), "mapped source must be read-only");
        assertSame(mapping, mapping.load(), "load returns this buffer; residency is best effort");
        // isLoaded is a hint, so the parent separately samples actual Windows pages.
        assertThrows(ReadOnlyBufferException.class, () -> mapping.duplicate().put(0, (byte)0));
    }

    @Test void exportedCopiesKeepLocalArenaOwnership() throws Exception {
        Class<?> arenaType = Class.forName("jdk.m3.text.LocalM3Arena");
        Object a = arenaType.getConstructor().newInstance();
        Object b = arenaType.getConstructor().newInstance();
        Object first = call("copyRecord", new Class<?>[]{int.class, arenaType}, 1, a);
        Object second = call("copyRecord", new Class<?>[]{int.class, arenaType}, 1, b);
        var identity = first.getClass().getMethod("storageIdentity");
        identity.setAccessible(true);
        assertNotEquals(identity.invoke(first), identity.invoke(second));
        assertEquals(first.toString(), second.toString());
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("mode, fixture path, callback URL");
        owner = Class.forName("jdk.m3.text.SharedLexiconImage");
        Path file = Path.of(args[1]);
        if (args[0].equals("create")) {
            owner.getMethod("create", Path.class, List.class).invoke(null, file, WORDS);
            System.out.println("M3_IMAGE_FIXTURE_CREATED records=" + WORDS.size());
            return;
        }
        if (!args[0].equals("read")) throw new IllegalArgumentException("mode");
        image = owner.getMethod("open", Path.class).invoke(null, file);
        owner.getMethod("warm").invoke(image);
        mapping = (MappedByteBuffer)field("mapping");
        offsets = ((int[])field("offsets")).clone();
        lengths = ((int[])field("lengths")).clone();
        payload = (Integer)field("payloadOffset");
        var listener = new SummaryGeneratingListener();
        var launcher = LauncherFactory.create();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(M3SharedImageProcessProbe.class)).build());
        var result = listener.getSummary();
        result.printTo(new java.io.PrintWriter(System.out, true));
        result.printFailuresTo(new java.io.PrintWriter(System.out, true));
        if (result.getTestsFoundCount() != 5 || result.getTestsSucceededCount() != 5
                || result.getTestsFailedCount() != 0 || result.getTestsAbortedCount() != 0)
            throw new AssertionError("process tests did not all execute");
        var address = Buffer.class.getDeclaredField("address");
        address.setAccessible(true);
        long base = address.getLong(mapping);
        ByteBuffer directory = ByteBuffer.allocate(offsets.length * 8).order(ByteOrder.BIG_ENDIAN);
        for (int i = 0; i < offsets.length; i++) directory.putInt(offsets[i]).putInt(lengths[i]);
        String json = "{\"pid\":" + ProcessHandle.current().pid()
                + ",\"address\":" + base + ",\"mappedBytes\":" + mapping.capacity()
                + ",\"records\":" + offsets.length + ",\"tests\":5,\"loadedHint\":" + mapping.isLoaded() + ",\"identity\":\""
                + call("imageIdentity", new Class<?>[]{}) + "\",\"directorySha256\":\""
                + HexFormat.of().formatHex(digest(directory.array())) + "\"}";
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        var connection = (HttpURLConnection)URI.create(args[2]).toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(60000);
        connection.setDoOutput(true);
        connection.setFixedLengthStreamingMode(data.length);
        try (var output = connection.getOutputStream()) { output.write(data); }
        if (connection.getResponseCode() != 204) throw new AssertionError("parent refused observation");
        connection.disconnect();
        java.lang.ref.Reference.reachabilityFence(image);
        System.out.println("M3_IMAGE_PROCESS_PROBE_PASS tests=5 records=" + offsets.length);
    }
}
