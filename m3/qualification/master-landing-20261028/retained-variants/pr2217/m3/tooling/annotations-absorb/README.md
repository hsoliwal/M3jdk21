<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# Annotations convergence: `jdk.m3.annotations`

The user decided on 2026-10-31 to absorb JSpecify and the other annotation donors into M3JDK by converging them. One
module, `jdk.m3.annotations`, carries the canonical annotations with JSpecify semantics. Every other donor annotation
either maps onto a canonical owner or is absorbed later under a `jdk.m3.annotations.<area>` owner. Donor copies in
`m3/vendor/third_party` are dropped only after the consumers move and the gates pass (absorb first).

## Leaf 0 (this change)

- **Module** `src/jdk.m3.annotations` (in `PLATFORM_MODULES`). It holds `NullMarked`, `NullUnmarked`, `Nullable`
  and `NonNull` in package `jdk.m3.annotations`, absorbed from JSpecify 1.0.1 by `tools/absorb_rename.py`. The tool
  renames the package in code and Javadoc and adds the Apache-2.0 modification notice; nothing else changes.
  `ABSORB_MANIFEST.tsv` binds each file to its donor by SHA-256, and `legal/jspecify.md` carries the license.
  - The simple names are unchanged, so tools that recognise nullness annotations by simple name (NullAway, Error
    Prone) still see them.
  - `requires static java.compiler` exists only for the Javadoc links the donor text makes to `javax.lang.model`.
- **Ledger** `ANNOTATIONS.tsv`, made by `tools/AnnotationLedger.java`. It has one row per annotation type of six
  donors: JSpecify 1.0.1, checker-qual 3.55.1, JetBrains annotations 24.1.0, Error Prone annotations 2.21.1, JSR-305
  3.0.2 and j2objc-annotations 2.8; that is 501 types (`DONORS.tsv`).
  - Each donor is compiled by javac and read back by reflection, so the targets, retention, elements and
    meta-annotations are the compiler's.
  - `concept_donors` counts the donors that declare the same simple name, for example `GuardedBy` (3) and
    `CheckReturnValue` (3). Those are the next convergence candidates.
- **Semantic map** `MAP_RULES.tsv`, hand-written data:
  - 4 CANONICAL rows (JSpecify);
  - 8 MAP rows: Checker Framework `Nullable`/`NonNull` (EQUIVALENT); JetBrains `Nullable`/`NotNull` and JSR-305
    `Nullable`, `CheckForNull`, `Nonnull` and `ParametersAreNonnullByDefault` (APPROX: placement or strength
    differs).
  - The other 489 donor annotations are ABSORB_NEXT, each with a proposed owner.

## Gate

`gate.sh` (repository root, `JAVA_HOME` = JDK 21) runs three checks:

1. **Ledger:** every MAP_RULES row names a donor annotation.
2. **Compile:** the module compiles with `-Xlint:all` (except the module-name warning, which `make/modules` also
   disables), `-Xdoclint:all/protected` and `-Werror`. With `M3JDK_STANDARD_PLUGIN=<classes>` it also runs the
   M3JdkStandard plugin; it reports 0 findings.
3. **API:** each canonical owner has the donor's targets, retention, elements and meta-annotations. This is read back
   by the same ledger code.

Result on 2026-10-31: ledger PASS, compile PASS, API PASS (4/4, 0 mismatches). Negative control: changing the
retention of `Nullable` to CLASS makes the API check fail with exit 1 and names the mismatch.

## Next leaves

1. **Consumer recipe:** an OpenRewrite ChangeType for the EQUIVALENT rows. For the APPROX rows, a site-by-site recipe
   gated by compilation, because a declaration annotation on a qualified or array type needs type-use placement
   (`Map.@Nullable Entry`, `String @Nullable []`).
2. **Duplicate concepts** across donors (GuardedBy, CheckReturnValue, Immutable, ThreadSafe, ...): one canonical owner
   each.
3. **ABSORB_NEXT** owners for the annotations M3JDK sources use. Then drop the donor copies once nothing references
   them.

Rerun the absorption: `python -I m3/tooling/annotations-absorb/tools/absorb_rename.py
m3/vendor/third_party/jspecify-1.0.1/src/main/java org.jspecify.annotations jdk.m3.annotations
src/jdk.m3.annotations/share/classes "<notice>"`. Rerun the ledger and checks: `bash m3/tooling/annotations-absorb/gate.sh`.
