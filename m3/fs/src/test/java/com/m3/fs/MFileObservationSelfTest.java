/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.fs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Executable consumer checks for observation evidence, without native code or a provider network. */
public final class MFileObservationSelfTest {
    private static final String SOURCE = "file:///facts/example.java";
    private static final int FILE_FLAGS = MFileObservation.EXISTS_KNOWN | MFileObservation.EXISTS
            | MFileObservation.TYPE_KNOWN | MFileObservation.FILE;
    private static int checks;

    private MFileObservationSelfTest() { }

    public static void main(String[] args) throws Exception {
        runAll();
        System.out.println("MFILE_OBSERVATION_CHECKS=" + checks);
    }

    public static void runAll() throws Exception {
        checks = 0;
        equalMetadataDoesNotProveContentEquality();
        unknownPresenceIsNotAbsence();
        zeroAndNegativeTimestampsAreKnownValues();
        sourceConflictRefusesCallback();
        explicitPresenceTransitionsHaveReasons();
        defaultProviderStampIsUnavailable();
        equalAuthoritativeTokensHaveQualifiedEvidence();
        changedRevisionDoesNotProveChangedBytes();
        weakStampsNeverProveContentEquality();
        stampDomainsMustMatch();
        contentIdsOverrideMetadataChanges();
        changedContentOverridesEqualMetadata();
        contradictoryAuthoritativeTokensAreReported();
        conflictingLengthsDoNotClaimUnchangedContent();
        contentAttachmentValidatesCompleteRawReferences();
        hybridProviderRetainsContentAndChildrenCapabilities();
        sameStructureCanSurviveChangedRawContent();
        changedStructureIsComparedWithinOneMethod();
        structureMethodAndContextChangesAreUnknown();
        structuresAreBoundToTheirObservedInput();
        callbackRunsOnceWithTheReturnedDelta();
        snapshotsShareImmutableManifestReferences();
    }

    private static void equalMetadataDoesNotProveContentEquality() {
        MFileDelta delta = MFileDelta.between(metadata(3, 12), metadata(3, 12));
        eq(MFileDelta.State.SAME, delta.metadata(), "complete metadata equality");
        eq(MFileDelta.State.UNKNOWN, delta.content(), "metadata cannot establish byte equality");
        eq(MFileDelta.ContentEvidence.NONE, delta.contentEvidence(), "no content evidence");
        has(delta, MFileDelta.CONTENT_UNKNOWN);
    }

    private static void unknownPresenceIsNotAbsence() {
        MFileObservation unknown = MFileObservation.unknown(SOURCE);
        eq(MFileObservation.Presence.UNKNOWN, unknown.presence(), "unknown presence");
        check(unknown.size().isEmpty() && unknown.modifiedTimeMillis().isEmpty(), "unknown metadata");
        MFileDelta delta = MFileDelta.between(unknown, MFileObservation.unknown(SOURCE));
        eq(MFileDelta.State.UNKNOWN, delta.presence(), "matching unknown presence is unknown");
        eq(MFileDelta.State.UNKNOWN, delta.metadata(), "matching unknown metadata is unknown");
        eq(MFileDelta.State.UNKNOWN, delta.content(), "matching unknown content is unknown");
        check(!delta.hasReason(MFileDelta.ADDED | MFileDelta.REMOVED), "unknown is not a presence transition");
    }

    private static void zeroAndNegativeTimestampsAreKnownValues() {
        eq(0L, metadata(3, 0).modifiedTimeMillis().orElseThrow(), "epoch timestamp is available");
        eq(-1L, metadata(3, -1).modifiedTimeMillis().orElseThrow(), "pre-epoch timestamp is available");
        expect(IllegalArgumentException.class, () -> MFileObservation.metadata(SOURCE, 0, 0, 1,
                MFileObservation.ChangeStamp.unavailable()));
    }

    private static void sourceConflictRefusesCallback() {
        AtomicInteger calls = new AtomicInteger();
        expect(IllegalArgumentException.class, () -> MFileDelta.between(MFileObservation.unknown(SOURCE),
                MFileObservation.unknown("file:///facts/other.java"), ignored -> calls.incrementAndGet()));
        eq(0, calls.get(), "cross-source callback refused");
    }

    private static void explicitPresenceTransitionsHaveReasons() {
        MFileObservation absent = MFileObservation.absent(SOURCE);
        eq(MFileObservation.Presence.ABSENT, absent.presence(), "explicit absence");
        MFileDelta added = MFileDelta.between(absent, metadata(3, 12));
        has(added, MFileDelta.ADDED);
        eq(MFileDelta.State.DIFFERENT, added.presence(), "addition presence");
        MFileDelta removed = MFileDelta.between(metadata(3, 12), absent);
        has(removed, MFileDelta.REMOVED);
        eq(MFileDelta.State.UNKNOWN, removed.content(), "absence does not supply bytes");
        check(!MFileDelta.between(metadata(3, 12), MFileObservation.unknown(SOURCE))
                .hasReason(MFileDelta.REMOVED), "missing evidence is not removal");
        eq(MFileDelta.State.SAME, MFileDelta.between(absent, absent).presence(), "stable absence");
    }

    private static void defaultProviderStampIsUnavailable() throws IOException {
        MFileChangeSource provider = new MFileChangeSource() { };
        check(!provider.changeStamp().available(), "provider opt-in is unavailable by default");
        check(!provider.changeStamp().authoritative(), "unavailable stamp has no authority");
    }

    private static void equalAuthoritativeTokensHaveQualifiedEvidence() {
        MFileObservation left = stamped(strong("r1"));
        MFileDelta delta = MFileDelta.between(left, stamped(strong("r1")));
        eq(MFileDelta.State.SAME, delta.providerRevision(), "authoritative revision equality");
        eq(MFileDelta.State.SAME, delta.content(), "provider-qualified content equality");
        eq(MFileDelta.ContentEvidence.AUTHORITATIVE_PROVIDER_TOKEN, delta.contentEvidence(), "qualified equality");
        has(delta, MFileDelta.PROVIDER_CONTENT_MATCH);
        check(left.content().isEmpty(), "provider evidence is not a fabricated content ID");
    }

    private static void changedRevisionDoesNotProveChangedBytes() {
        MFileDelta revision = MFileDelta.between(stamped(strong("r1")), stamped(strong("r2")));
        eq(MFileDelta.State.DIFFERENT, revision.providerRevision(), "revision changed");
        eq(MFileDelta.State.UNKNOWN, revision.content(), "revision change is not byte inequality");
        M3ObjectRef raw = raw("abc");
        MFileDelta sameBytes = MFileDelta.between(stamped(strong("r1")).withContent(raw),
                stamped(strong("r2")).withContent(raw));
        eq(MFileDelta.State.SAME, sameBytes.content(), "rewritten identical bytes");
        check(!sameBytes.hasReason(MFileDelta.CONFLICTING_EVIDENCE), "changed revision can retain bytes");
    }

    private static void weakStampsNeverProveContentEquality() {
        MFileDelta equal = MFileDelta.between(stamped(weak("mtime=12,size=3")), stamped(weak("mtime=12,size=3")));
        eq(MFileDelta.State.UNKNOWN, equal.providerRevision(), "weak revision equality is unknown");
        eq(MFileDelta.State.UNKNOWN, equal.content(), "weak stamp is not equality proof");
        has(equal, MFileDelta.HEURISTIC_STAMP_MATCH);
        MFileDelta changed = MFileDelta.between(stamped(weak("v1")), stamped(weak("v2")));
        eq(MFileDelta.State.UNKNOWN, changed.content(), "changed heuristic is not byte proof");
        has(changed, MFileDelta.HEURISTIC_STAMP_CHANGED);
    }

    private static void stampDomainsMustMatch() {
        String[][] domains = {
                {"other-provider", "decoded-entry", "version-id", "epoch-1"},
                {"provider/account", "raw-container", "version-id", "epoch-1"},
                {"provider/account", "decoded-entry", "etag", "epoch-1"},
                {"provider/account", "decoded-entry", "version-id", "epoch-2"}
        };
        for (String[] domain : domains) {
            MFileObservation.ChangeStamp right = MFileObservation.ChangeStamp.authoritative(SOURCE,
                    domain[0], domain[1], domain[2], domain[3], "r1");
            MFileDelta delta = MFileDelta.between(stamped(strong("r1")), stamped(right));
            eq(MFileDelta.State.UNKNOWN, delta.content(), "different authority domain");
            has(delta, MFileDelta.STAMP_DOMAIN_CHANGED);
        }
        MFileObservation.ChangeStamp otherSource = MFileObservation.ChangeStamp.authoritative(
                "file:///facts/other.java", "provider/account", "decoded-entry", "version-id", "epoch-1", "r1");
        expect(IllegalArgumentException.class, () -> stamped(otherSource));
        expect(IllegalArgumentException.class, () -> MFileObservation.ChangeStamp.authoritative(
                SOURCE, "provider/account", "decoded-entry", "version-id", "", "r1"));
    }

    private static void contentIdsOverrideMetadataChanges() {
        M3ObjectRef raw = raw("abc");
        MFileDelta delta = MFileDelta.between(metadata(3, 12).withContent(raw), metadata(3, 13).withContent(raw));
        eq(MFileDelta.State.SAME, delta.content(), "equal complete content IDs");
        eq(MFileDelta.ContentEvidence.FULL_CONTENT_ID, delta.contentEvidence(), "complete byte evidence");
        eq(MFileDelta.State.DIFFERENT, delta.metadata(), "metadata change retained independently");
        has(delta, MFileDelta.MODIFIED_CHANGED);
    }

    private static void changedContentOverridesEqualMetadata() {
        MFileDelta delta = MFileDelta.between(metadata(3, 12).withContent(raw("abc")),
                metadata(3, 12).withContent(raw("xyz")));
        eq(MFileDelta.State.SAME, delta.metadata(), "equal sampled metadata");
        eq(MFileDelta.State.DIFFERENT, delta.content(), "different complete content");
        has(delta, MFileDelta.CONTENT_ID_CHANGED);
    }

    private static void contradictoryAuthoritativeTokensAreReported() {
        MFileDelta delta = MFileDelta.between(stamped(strong("r1")).withContent(raw("abc")),
                stamped(strong("r1")).withContent(raw("xyz")));
        eq(MFileDelta.State.DIFFERENT, delta.content(), "observed bytes defeat contradicted token equality");
        eq(MFileDelta.ContentEvidence.FULL_CONTENT_ID, delta.contentEvidence(), "full content retained");
        has(delta, MFileDelta.CONFLICTING_EVIDENCE);
    }

    private static void conflictingLengthsDoNotClaimUnchangedContent() {
        MFileObservation differentSize = MFileObservation.metadata(SOURCE,
                FILE_FLAGS | MFileObservation.SIZE_KNOWN | MFileObservation.MODIFIED_KNOWN, 4, 12, strong("r1"));
        MFileDelta token = MFileDelta.between(stamped(strong("r1")), differentSize);
        eq(MFileDelta.State.UNKNOWN, token.content(), "contradictory sampled size prevents token-only unchanged claim");
        has(token, MFileDelta.CONFLICTING_EVIDENCE);
        M3ObjectRef raw = raw("abc");
        M3ObjectRef inconsistentLength = new M3ObjectRef(raw.id(), 4, M3ObjectKind.RAW_BYTES);
        MFileDelta length = MFileDelta.between(MFileObservation.unknown(SOURCE).withContent(raw),
                MFileObservation.unknown(SOURCE).withContent(inconsistentLength));
        eq(MFileDelta.State.DIFFERENT, length.content(), "different attested lengths cannot be equal byte sequences");
        has(length, MFileDelta.CONFLICTING_EVIDENCE);
        has(length, MFileDelta.CONTENT_SIZE_CHANGED);
        check(!length.hasReason(MFileDelta.CONTENT_ID_CHANGED), "unchanged digest is not mislabeled changed");
    }

    private static void contentAttachmentValidatesCompleteRawReferences() {
        M3ObjectRef raw = raw("abc");
        MFileObservation unknown = MFileObservation.unknown(SOURCE);
        eq(raw, unknown.withContent(raw).content().orElseThrow(), "unknown size may acquire complete content");
        check(unknown.content().isEmpty(), "content attachment leaves original immutable");
        expect(IllegalArgumentException.class, () -> metadata(4, 12).withContent(raw));
        expect(IllegalArgumentException.class, () -> unknown.withContent(new M3ObjectRef(
                raw.id(), raw.size(), M3ObjectKind.PRECOMPUTE_OUTPUT)));
        expect(IllegalArgumentException.class, () -> new M3ObjectRef(raw.id(), -1, M3ObjectKind.RAW_BYTES));
        expect(IllegalArgumentException.class, () -> MFileObservation.absent(SOURCE).withContent(raw));
        MFileObservation directory = MFileObservation.metadata(SOURCE,
                MFileObservation.EXISTS_KNOWN | MFileObservation.EXISTS
                | MFileObservation.TYPE_KNOWN | MFileObservation.DIRECTORY, 0, 0,
                MFileObservation.ChangeStamp.unavailable());
        expect(IllegalArgumentException.class, () -> directory.withContent(raw));
        expect(IllegalArgumentException.class, () -> unknown.withContent(raw).withContent(raw("xyz")));
        expect(IllegalArgumentException.class, () -> MFileObservation.metadata(SOURCE,
                MFileObservation.EXISTS, 0, 0, MFileObservation.ChangeStamp.unavailable()));
    }

    private static void hybridProviderRetainsContentAndChildrenCapabilities() {
        M3ObjectRef raw = raw("abc");
        MFileObservation hybrid = MFileObservation.metadata(SOURCE,
                FILE_FLAGS | MFileObservation.DIRECTORY | MFileObservation.SIZE_KNOWN, 3, 0,
                MFileObservation.ChangeStamp.unavailable()).withContent(raw);
        check(hybrid.isFile() && hybrid.isDirectory(), "hybrid provider has content and children");
        eq(raw, hybrid.content().orElseThrow(), "hybrid content can be fully observed");
    }

    private static void sameStructureCanSurviveChangedRawContent() {
        M3ObjectRef output = structure("normalized-ast");
        MFileObservation before = structured(raw("int x;"), output, "ast", "parser", 1, Map.of("trivia", "ignore"), List.of());
        MFileObservation after = structured(raw("int  x;"), output, "ast", "parser", 1, Map.of("trivia", "ignore"), List.of());
        MFileDelta delta = MFileDelta.between(before, after);
        eq(MFileDelta.State.DIFFERENT, delta.content(), "raw spacing changed");
        eq(MFileDelta.State.SAME, delta.structure(), "same keyed structural output");
        has(delta, MFileDelta.STRUCTURE_ID_MATCH);
        check(!before.structureKey().orElseThrow().id().equals(after.structureKey().orElseThrow().id()),
                "full precompute keys remain bound to their different raw inputs");
    }

    private static void changedStructureIsComparedWithinOneMethod() {
        MFileObservation before = structured(raw("int x;"), structure("ast-x"), "ast", "parser", 1, Map.of(), List.of());
        MFileObservation after = structured(raw("int y;"), structure("ast-y"), "ast", "parser", 1, Map.of(), List.of());
        MFileDelta delta = MFileDelta.between(before, after);
        eq(MFileDelta.State.DIFFERENT, delta.structure(), "changed structural fingerprint");
        has(delta, MFileDelta.STRUCTURE_ID_CHANGED);
    }

    private static void structureMethodAndContextChangesAreUnknown() {
        M3ObjectRef input = raw("int x;"), output = structure("same-output");
        MFileObservation base = structured(input, output, "ast", "parser", 1, Map.of("trivia", "ignore"), List.of(raw("classpath-a").id()));
        MFileObservation[] different = {
                structured(input, output, "ast", "other-parser", 1, Map.of("trivia", "ignore"), List.of(raw("classpath-a").id())),
                structured(input, output, "ast", "parser", 2, Map.of("trivia", "ignore"), List.of(raw("classpath-a").id())),
                structured(input, output, "ast", "parser", 1, Map.of("trivia", "retain"), List.of(raw("classpath-a").id())),
                structured(input, output, "symbols", "parser", 1, Map.of("trivia", "ignore"), List.of(raw("classpath-a").id())),
                structured(input, output, "ast", "parser", 1, Map.of("trivia", "ignore"), List.of(raw("classpath-b").id()))
        };
        for (MFileObservation after : different) {
            MFileDelta delta = MFileDelta.between(base, after);
            eq(MFileDelta.State.UNKNOWN, delta.structure(), "incompatible structural derivation");
            has(delta, MFileDelta.STRUCTURE_DOMAIN_CHANGED);
        }
    }

    private static void structuresAreBoundToTheirObservedInput() {
        M3ObjectRef input = raw("abc"), other = raw("xyz"), output = structure("ast");
        MFileObservation observed = MFileObservation.unknown(SOURCE).withContent(input);
        M3FileManifest correct = manifest(input, "ast", output);
        M3PrecomputeKey correctKey = key(input, "ast", "parser", 1, Map.of(), List.of());
        expect(IllegalArgumentException.class, () -> observed.withStructure(manifest(other, "ast", output), "ast", correctKey));
        expect(IllegalArgumentException.class, () -> observed.withStructure(correct, "ast",
                key(other, "ast", "parser", 1, Map.of(), List.of())));
        expect(IllegalArgumentException.class, () -> observed.withStructure(correct, "missing", correctKey));
        expect(IllegalArgumentException.class, () -> MFileObservation.unknown(SOURCE).withStructure(correct, "ast", correctKey));
        expect(IllegalArgumentException.class, () -> observed.withStructure(correct, "ast",
                new M3PrecomputeKey("ast", "parser", 1, List.of(), Map.of())));
    }

    private static void callbackRunsOnceWithTheReturnedDelta() {
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<MFileDelta> delivered = new AtomicReference<>();
        MFileObservation before = metadata(3, 12), after = metadata(3, 13);
        MFileDelta delta = MFileDelta.between(before, after, value -> {
            calls.incrementAndGet();
            delivered.set(value);
        });
        eq(1, calls.get(), "callback count");
        check(delivered.get() == delta && delta.before() == before && delta.after() == after, "callback and delta retain exact refs");
        AtomicInteger throwingCalls = new AtomicInteger();
        expect(IllegalStateException.class, () -> MFileDelta.between(before, after, value -> {
            throwingCalls.incrementAndGet();
            throw new IllegalStateException("consumer failure");
        }));
        eq(1, throwingCalls.get(), "throwing callback is not retried");
        expect(NullPointerException.class, () -> MFileDelta.between(before, after, null));
    }

    private static void snapshotsShareImmutableManifestReferences() {
        M3ObjectRef input = raw("abc"), output = structure("ast");
        M3FileManifest manifest = manifest(input, "ast", output);
        M3PrecomputeKey key = key(input, "ast", "parser", 1, Map.of(), List.of());
        MFileObservation metadata = metadata(3, 12);
        MFileObservation bytes = metadata.withContent(input);
        MFileObservation structured = bytes.withStructure(manifest, "ast", key);
        check(metadata.content().isEmpty() && bytes.manifest().isEmpty(), "prior snapshots unchanged");
        check(structured.manifest().orElseThrow() == manifest && structured.structureKey().orElseThrow() == key,
                "existing immutable manifest and key are shared");
        eq("ast", structured.structureName().orElseThrow(), "named structure projection");
        eq(output, structured.structureReference().orElseThrow(), "existing structure reference projection");
        eq(SOURCE, structured.sourceKey(), "source identity retained");
    }

    private static MFileObservation metadata(long size, long modified) {
        return MFileObservation.metadata(SOURCE, FILE_FLAGS | MFileObservation.SIZE_KNOWN
                | MFileObservation.MODIFIED_KNOWN, size, modified, MFileObservation.ChangeStamp.unavailable());
    }

    private static MFileObservation stamped(MFileObservation.ChangeStamp stamp) {
        return MFileObservation.metadata(SOURCE, FILE_FLAGS | MFileObservation.SIZE_KNOWN
                | MFileObservation.MODIFIED_KNOWN, 3, 12, stamp);
    }

    private static MFileObservation.ChangeStamp strong(String token) {
        return MFileObservation.ChangeStamp.authoritative(SOURCE, "provider/account", "decoded-entry", "version-id", "epoch-1", token);
    }

    private static MFileObservation.ChangeStamp weak(String token) {
        return MFileObservation.ChangeStamp.heuristic(SOURCE, "provider/account", "decoded-entry", "metadata", "epoch-1", token);
    }

    private static M3ObjectRef raw(String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        return new M3ObjectRef(M3ContentId.digest(bytes), bytes.length, M3ObjectKind.RAW_BYTES);
    }

    private static M3ObjectRef structure(String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        return new M3ObjectRef(M3ContentId.digest(bytes), bytes.length, M3ObjectKind.PRECOMPUTE_OUTPUT);
    }

    private static M3FileManifest manifest(M3ObjectRef raw, String name, M3ObjectRef output) {
        return new M3FileManifest(raw, Map.of(name, output), Map.of(), Map.of());
    }

    private static M3PrecomputeKey key(M3ObjectRef input, String type, String algorithm, int version,
            Map<String, String> config, List<M3ContentId> extraInputs) {
        java.util.ArrayList<M3ContentId> inputs = new java.util.ArrayList<>();
        inputs.add(input.id());
        inputs.addAll(extraInputs);
        return new M3PrecomputeKey(type, algorithm, version, inputs, config);
    }

    private static MFileObservation structured(M3ObjectRef input, M3ObjectRef output, String type,
            String algorithm, int version, Map<String, String> config, List<M3ContentId> extraInputs) {
        return MFileObservation.unknown(SOURCE).withContent(input)
                .withStructure(manifest(input, type, output), type, key(input, type, algorithm, version, config, extraInputs));
    }

    private static void has(MFileDelta delta, long reason) {
        check(delta.hasReason(reason), "missing reason " + reason + " in " + delta.reasons());
    }

    private static void eq(Object expected, Object actual, String message) {
        check(java.util.Objects.equals(expected, actual), message + ": expected=" + expected + ", actual=" + actual);
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void expect(Class<? extends Throwable> expected, Checked action) {
        checks++;
        try {
            action.run();
        } catch (Throwable failure) {
            if (expected.isInstance(failure)) return;
            throw new AssertionError("expected " + expected.getName() + ", got " + failure, failure);
        }
        throw new AssertionError("expected " + expected.getName());
    }

    @FunctionalInterface
    private interface Checked { void run() throws Exception; }
}
