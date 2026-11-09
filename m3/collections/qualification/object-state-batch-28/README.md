# M3 object state batch masks — source-first Java21

This crate is a bounded, additive first-party Apache-2.0 Synexia source convergence handoff for hsoliwal/M3jdk21. Existing generation-safe M3ObjectStateSlots28 owns state and allocation. The new utility has **no fields, payload copies, per-row objects, wrapper hierarchy, JNI symbols or retained mask/index arrays**.

## Operations
- update: two-phase **validate-all, then update** over a checked long[] subrange.
- updateMatching: conditional in-encounter-order transitions, required and excluded unsigned byte masks.
- countMatching: count occurrences (not distinct handles), including repeated handles.
- firstMatchingIndex: return first *array offset*, not physical slot; short-circuit may avoid inspecting a later invalid handle.

Invalid masks, offsets, or stale generation-bearing handles cause mutation operations to refuse **before any write**. Thread races, allocation failures and arbitrary VM errors are outside transactional guarantees. State bit PIN_REQUEST is app metadata, not JVM pinning.

## Recipe
Named com.synexia.rewrite.M3ObjectStateBatch28 composes the **unchanged existing** M3HashPinnedJavaSnapshotRecipe, explicit exact PlainText SourceFile mode. Two ABSENT -> SHA-256 new Java templates. This mode proves hash/identity, not attributed Java rewriting; strict Java21 compilation and differential/runtime tests are separate mandatory gates.

JDK-compatible filtered/sorted collection wrappers remain in their original M3 owners. This utility composes their indexed state underneath rather than duplicating wrappers. Native acceleration reuses existing M3IndexMaskCompactor over caller-exported ordinal mask bytes; no new C or JNI ABI is introduced.

## Proof
Locally executed Linux Java 21.0.12.1 / Maven3.9.9 / OpenRewrite8.17.1 / Jupiter5.10.2: **4 SDK JUnit methods PASS**, partial/ordering/fixed-point/duplicate/drift/late-change refusal, five exact first-party owner SHA-256/Git blobs. Real Maven SDK output compiled with javac --release 21 -proc:none -Xlint:all -Werror and ran **1,720,349 checks / 49,152 seeded array-mask operations**. 4095/4096 page boundaries, null handles outside bounds, duplicate semantics, stale generation preflight and no mutation on invalid masks tested. No source-built JDK, all-platform proof or benchmark speed claim. Product phase stays STRING until predecessor gates admit collections.

### Reproduction
Provide public receiver M3JDK21 source checkout matching M3_OWNER_SHA256.tsv and a local Maven cache holding OpenRewrite8.17.1 and JUnit5.10.2:
mvn -B -ntp -f pom.xml -Dm3.engine.sources=/path/M3jdk21/m3/tooling/migration-recipes/src/main/java -Dm3.receiver.source=/path/M3jdk21/m3/collections/src/main/java/com/m3/collections test

The source crate is canonical in Synexia; generated Java copies and mirrored recipe are target-owned. Do not edit generated files directly. No automatic merge, rebase develop or promotion.
