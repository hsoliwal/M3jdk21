// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite.memory;

import com.synexia.rewrite.sealed.SealHash;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Independent golden byte and negative-schema fixtures for this crate-local record profile. */
public final class M3RecoveryMemoryTest {
    private static int checks;
    private M3RecoveryMemoryTest() { }
    @FunctionalInterface private interface Operation { void run() throws Exception; }

    public static void main(String[] args) throws Exception {
        for (int unit = 0; unit < 32; unit++) {
            String value = Character.toString(unit);
            String expected = String.format("\"\\u%04x\"", unit);
            check(encoded(value).equals(expected), "control escape " + unit);
            check(M3RecoveryMemory.parse(expected.getBytes(StandardCharsets.UTF_8)).equals(value), "control roundtrip " + unit);
        }
        check(encoded("\"\\/\u00e9\u03a9\ud83d\ude80\u2028").equals("\"\\\"\\\\/\u00e9\u03a9\ud83d\ude80\u2028\""), "quote backslash solidus and scalars");
        check(!Arrays.equals(M3RecoveryMemory.canonicalBytes("\u00e9"), M3RecoveryMemory.canonicalBytes("e\u0301")), "no Unicode normalization");
        reject(() -> M3RecoveryMemory.canonicalBytes("\ud800"), "unpaired high surrogate");
        reject(() -> M3RecoveryMemory.canonicalBytes("\udc00"), "unpaired low surrogate");
        reject(() -> M3RecoveryMemory.canonicalBytes("\ud800A"), "broken surrogate pair");
        reject(() -> M3RecoveryMemory.canonicalBytes(M3RecoveryMemory.parse("\"\\ud800\"".getBytes(StandardCharsets.UTF_8))), "escaped unpaired surrogate");
        reject(() -> M3RecoveryMemory.parse(new byte[] {(byte) 0xc3, (byte) 0x28}), "invalid UTF-8");
        for (String value : List.of("-9223372036854775808", "9223372036854775807", "0", "-1"))
            check(encoded(M3RecoveryMemory.parse(value.getBytes(StandardCharsets.UTF_8))).equals(value), "integer limit " + value);
        for (String value : List.of("9223372036854775808", "-9223372036854775809", "1.0", "1e0", "NaN", "Infinity", "+1", "01", "-0"))
            reject(() -> M3RecoveryMemory.parse(value.getBytes(StandardCharsets.UTF_8)), "invalid number " + value);
        reject(() -> M3RecoveryMemory.canonicalBytes(Double.valueOf(1.0)), "floating Java value");
        check(encoded(Map.of("z", Map.of("b", 2L, "a", 1L), "a", List.of(2L, 1L)))
                .equals("{\"a\":[2,1],\"z\":{\"a\":1,\"b\":2}}"), "recursive lexical keys and ordered arrays");
        reject(() -> M3RecoveryMemory.canonicalBytes(Map.of("\u00e9", 1L)), "non-ASCII key");
        reject(() -> M3RecoveryMemory.parse("{\"x\":1,\"x\":2}".getBytes(StandardCharsets.UTF_8)), "duplicate root key");
        reject(() -> M3RecoveryMemory.parse("{\"x\":{\"y\":1,\"y\":2}}".getBytes(StandardCharsets.UTF_8)), "duplicate nested key");
        reject(() -> M3RecoveryMemory.parse("{} {}".getBytes(StandardCharsets.UTF_8)), "trailing value");
        reject(() -> M3RecoveryMemory.parse("{\"x\":[1".getBytes(StandardCharsets.UTF_8)), "truncated value");
        reject(() -> M3RecoveryMemory.parse(new byte[] {(byte) 0xef, (byte) 0xbb, (byte) 0xbf, '{', '}'}), "BOM");

        Path root = Files.createTempDirectory("mop-memory-proof-");
        Files.writeString(root.resolve("artifact.txt"), "actual bounded fixture\n");
        Map<String, Object> event = fixture(root);
        M3RecoveryMemory.validate(event); checks++;
        Map<String, Object> changed = new TreeMap<>(event); changed.put("unknown", "x");
        reject(() -> M3RecoveryMemory.validate(changed), "unknown root field");
        rejectField(event, "schema", "MOP-RCM/2.0");
        rejectField(event, "profile", "unknown");
        rejectField(event, "admission", "ADMITTED");
        rejectField(event, "event_type", "VALIDATED");
        rejectField(event, "observation_epoch_seconds", Double.valueOf(1.0));
        rejectField(event, "source", Map.of("repository", "example", "revision", "0".repeat(40), "unknown", "x"));
        rejectField(event, "parents", List.of("1".repeat(64), "0".repeat(64)));
        rejectField(event, "parents", List.of("0".repeat(64), "0".repeat(64)));
        rejectField(event, "parents", List.of("A".repeat(64)));
        rejectField(event, "supersedes", List.of("bad"));
        rejectField(event, "recipe_after", Map.of("path", "../escape", "sha256", "0".repeat(64)));
        rejectField(event, "recipe_after", Map.of("path", "/absolute", "sha256", "0".repeat(64)));
        rejectField(event, "recipe_after", Map.of("path", "artifact.txt", "sha256", "0".repeat(64), "unknown", "x"));
        rejectField(event, "unknowns", List.of());
        rejectField(event, "verification", List.of(Map.of("gate", "invented", "status", "PASS", "command", List.of("java"), "evidence", List.of())));
        rejectField(event, "regression_fixtures", List.of());
        rejectField(event, "atoms", List.of());
        rejectField(event, "atoms", List.of(Map.of("id", "x", "unknown", "x")));
        Map<String, Object> sortedSets = new TreeMap<>(event);
        sortedSets.put("parents", List.of("0".repeat(64), "1".repeat(64)));
        sortedSets.put("supersedes", List.of("2".repeat(64), "3".repeat(64)));
        M3RecoveryMemory.validate(sortedSets); checks++;
        Map<String, Object> scalar = new TreeMap<>(event); scalar.put("observation_epoch_seconds", Long.MIN_VALUE);
        M3RecoveryMemory.validate(scalar); checks++;
        scalar.put("observation_epoch_seconds", Long.MAX_VALUE); M3RecoveryMemory.validate(scalar); checks++;

        Path events = root.resolve("memory/events");
        Path first = M3RecoveryMemory.publish(event, root, events);
        byte[] canonical = Files.readAllBytes(first);
        check(canonical[canonical.length - 1] == '}', "no trailing newline or BOM");
        check(first.getFileName().toString().equals(SealHash.bytes(canonical) + ".json"), "filename digest");
        check(M3RecoveryMemory.publish(event, root, events).equals(first), "identical publication is idempotent");
        check(Arrays.equals(Files.readAllBytes(first), canonical), "idempotence does not alter bytes");
        M3RecoveryMemory.main(new String[] {"check", first.toString(), root.toString(), events.toString()}); checks++;
        byte[] withNewline = Arrays.copyOf(canonical, canonical.length + 1); withNewline[canonical.length] = '\n';
        Files.write(first, withNewline);
        reject(() -> M3RecoveryMemory.main(new String[] {"check", first.toString(), root.toString(), events.toString()}), "trailing newline in stored event");
        Files.write(first, canonical);
        Map<String, Object> child = new TreeMap<>(event);
        child.put("parents", List.of(SealHash.bytes(canonical))); child.put("lesson", "Child retains verified predecessor.");
        Path second = M3RecoveryMemory.publish(child, root, events);
        check(!second.equals(first), "linked correction produces distinct immutable event");
        M3RecoveryMemory.verify(child, root, events); checks++;
        Map<String, Object> missing = new TreeMap<>(event); missing.put("parents", List.of("0".repeat(64)));
        reject(() -> M3RecoveryMemory.publish(missing, root, events), "missing parent refuses publication");
        Map<String, Object> otherRecipe = new TreeMap<>(child); otherRecipe.put("recipe_id", "different-owner");
        reject(() -> M3RecoveryMemory.verify(otherRecipe, root, events), "parent recipe mismatch");
        Files.writeString(first, "corrupt");
        reject(() -> M3RecoveryMemory.verify(child, root, events), "corrupt parent refused");
        reject(() -> M3RecoveryMemory.publish(event, root, events), "existing differing event never overwritten");
        check(Files.readString(first).equals("corrupt"), "overwrite refusal preserves existing bytes");
        Files.write(first, canonical);
        Files.writeString(root.resolve("artifact.txt"), "changed");
        reject(() -> M3RecoveryMemory.verify(event, root, events), "artifact drift refused");
        System.out.println("MOP_RCM_CONFORMANCE PASS checks=" + checks + " controlCases=32 profile=" + M3RecoveryMemory.PROFILE);
    }

    static Map<String, Object> fixture(Path root) throws Exception {
        Map<String, Object> artifact = Map.of("path", "artifact.txt", "sha256", SealHash.bytes(Files.readAllBytes(root.resolve("artifact.txt"))));
        Map<String, Object> event = new TreeMap<>();
        event.put("schema", "MOP-RCM/1.0"); event.put("profile", M3RecoveryMemory.PROFILE);
        event.put("recipe_id", "synthetic-conformance-fixture"); event.put("event_type", "RECORDED"); event.put("admission", "PENDING");
        event.put("source", Map.of("repository", "fixture-only", "revision", "0".repeat(40)));
        event.put("parents", new ArrayList<>()); event.put("supersedes", new ArrayList<>());
        event.put("recipe_before", null); event.put("recipe_after", artifact);
        event.put("source_manifest_before", artifact); event.put("source_manifest_after", artifact);
        event.put("observation_epoch_seconds", 0L); event.put("author", "Synexia conformance fixture"); event.put("license", "Apache-2.0");
        event.put("trigger", "Synthetic fixture only."); event.put("root_cause", Map.of("status", "hypothesized", "explanation", "Synthetic fixture only."));
        event.put("change", "Exercise validation."); event.put("lesson", "No production outcome is asserted by this fixture.");
        event.put("applicability", List.of("This test only")); event.put("exclusions", List.of("Production admission"));
        event.put("known_failures", List.of()); event.put("pending_gates", List.of("Production qualification"));
        event.put("unknowns", List.of("No preceding production recipe is represented by this synthetic fixture."));
        event.put("atoms", List.of(Map.of("id", "fixture", "owner", "conformance test", "contract", "Synthetic bytes only",
                "pattern", "Canonical correction event", "applicability", List.of("Fixture"), "exclusions", List.of("Production"))));
        event.put("regression_fixtures", List.of(artifact));
        event.put("verification", List.of(Map.of("gate", "production", "status", "NOT_RUN", "command", List.of(), "evidence", List.of())));
        event.put("replay", Map.of("status", "NOT_RUN", "command", List.of(), "evidence", List.of()));
        event.put("environment", "Synthetic fixture, no production measurements.");
        return event;
    }
    private static String encoded(Object value) { return SealHash.decode(M3RecoveryMemory.canonicalBytes(value)); }
    private static void rejectField(Map<String, Object> event, String field, Object value) throws Exception {
        Map<String, Object> changed = new TreeMap<>(event); changed.put(field, value);
        reject(() -> M3RecoveryMemory.validate(changed), "invalid field " + field);
    }
    private static void reject(Operation operation, String message) throws Exception {
        try { operation.run(); }
        catch (IllegalArgumentException | java.io.IOException expected) { checks++; return; }
        throw new AssertionError("accepted " + message);
    }
    private static void check(boolean condition, String message) {
        checks++; if (!condition) throw new AssertionError(message);
    }
}
