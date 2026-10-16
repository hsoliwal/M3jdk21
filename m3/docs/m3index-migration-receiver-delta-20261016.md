<!-- SPDX-License-Identifier: Apache-2.0 -->
# Synexia MIndex / MatIndex to M3JDK21 receiving delta

Status: **SOURCE-PINNED, NO TARGET SOURCE MUTATION, NO API ADMISSION**.

This is a small delta, not a second master catalogue. Canonical source recipe
ownership stays in hsoliwal/com.synexia, while M3JDK21 is the Java 21 product
owner. Preserve the existing target authorities:

- m3/docs/m3jdk21-porting-invariant.md and m3/docs/name-mapping.json
- m3/docs/synexia-full-family-catalogue.json (4,770 scoped historical
  named-path candidates, not whole-portfolio completion)
- m3/synexia-import/current-full-borrow/ (32 intake-family categories with
  independent statuses and source revision)
- m3/compatibility/m3-family-naming-target-disposition.tsv
- Public JDK Java APIs, HotSpot/JNI/GC compatibility and license/NOTICE rules.

## Exact source custody

The mindex-rename-synexia-source-delta-20261016.tsv records seven precise
upstream Git blob objects: the canonical M3 naming and additive copy mechanics,
plus read-only portfolio inventory and typed-caller candidates in Synexia draft
PRs #11918 and #11921. Every row is an external authoring/proof tool, not a
runtime java.base dependency or an already M3JDK-admitted implementation.
The pins are immutable reproducibility evidence; newer work requires a reviewed
new pin. The review must fetch source blobs from GitHub. The local validator
does not itself establish remote availability or correct compilation.

## Migration order

1. Inventory all hsoliwal repositories, branches and PRs. Reconcile the
   existing Synexia snapshots and target source lineage, not just MIndex names.
2. Classify MIndex/MatIndex/IndexString source families, callers, JNI/native
   ABI, reflection, resource paths and serialized formats by contract.
3. Map owners by exact M3JDK capability, never by prefix. MIndexString maps to
   java.lang.M3String and MIndexAST to jdk.internal.mindex.M3AST. The
   MIndexStringCanonicalFacts responsibilities split across multiple owners.
   Unknown or nested names remain REVIEW_REQUIRED.
4. Prove source-bounded additive copy, then migrate callers/imports one Java
   file at a time with typed source-hash-sealed OpenRewrite ChangeType. Fix and
   replay the reusable recipe; never manually patch generated output.
5. Keep MIndex ABI aliases/facades until caller, reflection, native JNI,
   resource/persistence and behavior gates qualify retirement. Java import
   recipes do not rename C/C++ symbols, module/service XML or serialized IDs.
6. Converge strongest correct Java/JNI work in canonical Synexia behavioral
   atoms with exact hashes, license/NOTICE, counterexamples and provenance;
   deliver only fully qualified superset packets to the target.
7. Receive in JDK dependency order: String, arrays, collections, AST/compiler
   then remaining families. Verify Java21/JUnit, target runtime/jtreg,
   JNI/GC and benchmarks as applicable. Reusable recipe ownership remains
   in Synexia.

## Local source-only verifier

    python3 m3/compatibility/check_mindex_rename_synexia_delta.py --self-test
    python3 m3/compatibility/check_synexia_full_borrow_receiver.py

The new stdlib verifier refuses modified/missing/duplicate blobs, unexpected
paths, target promotion or runtime dependencies. Source PR #11918 locally
passed an independent Java21 smoke against a compile-compatible naming policy
stand-in; canonical Maven and hosted jobs have not reported executable passes.
Source PR #11921's Maven/JUnit gate has not shown executable proof.
Both are draft candidates only, and must not be marked as admitted.

## Copyright, licensing and ABI membrane

Only eligible first-party Apache-2.0 recipes cross as external authoring tools
with attribution. Third-party/native and OpenJDK-derived bodies preserve their
rights and target module rules. This receiver changes no java.lang.String,
java.util, jdk.internal runtime API, native exports or java.base dependency.
