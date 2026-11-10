# Parser-incompatible exact Java snapshot custody

Status: bounded repair for retained recipe tests in PR #117.

## Problem

Two existing source-sealed recipe families currently fail before their own transformation logic runs:

- `mindex-jni-newstring-admission` — the pinned OpenRewrite 8.17.1 Java 21 Javadoc visitor
  desynchronizes while parsing the full OpenJDK `java.lang.String` owner and reports that it cannot
  find an `@exception` token.
- `mindex-string-bulk-char-boundary` — the full internal `java.lang.MIndexString` owner reaches
  the recipe test with missing/invalid JDK-internal type attribution.

Both transformations are exact whole-file snapshots with hash-pinned pre/postimages. Their semantic
product changes remain subject to OpenJDK compile/jtreg/runtime proof.

## Decision

Reuse the existing `M3HashPinnedJavaSnapshotRecipe` and add one explicit mode bit:

- `lst=true` (default): current behavior; target/template must be Java compilation units and the
  OpenRewrite Java parser remains part of the admission proof.
- `lst=false`: exact Java-source custody as OpenRewrite `PlainText`; source identity, exact
  pre/post hashes, path, metadata and fixed point remain enforced, but Java semantic/type proof is
  deliberately **not** claimed by this recipe mode.

Only the two parser-incompatible crates opt out. All existing callers remain in LST mode.

## Safety boundary

PlainText mode is not permission to perform semantic Java rewrites. It is allowed only for an exact
reviewed snapshot where:

1. the manifest seals complete before/after Java source;
2. the delta is independently reviewed and documented;
3. product compilation/jtreg/runtime remain mandatory;
4. no compatibility, semantic-equivalence or promotion authority is inferred from snapshot replay.

For the two retained owners the reviewed deltas are narrow:

- `String.java`: additive VM-private `m3AdmitNative(String)` ingress hook.
- `MIndexString.java`: one `getChars` boundary implementation and documentation update.

## Verification target

The existing failing JUnit owners must pass using PlainText input/output and must prove second-pass
fixed point. Recipe serialization must preserve the explicit mode. The existing `gate-java`
meta-crate must seal the revised test source exactly.

This repair does not close the String/JNI product work, type attribution work, native image build,
or whole-JDK admission.
