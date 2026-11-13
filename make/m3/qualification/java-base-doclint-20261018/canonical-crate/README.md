<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# java.base public documentation prerequisite

The pinned M3JDK21 baseline `2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49` reports 121 public Javadoc diagnostics under `-Xlint:all -Werror -Xdoclint:all/protected`. This class-backed recipe repairs only comments in twelve existing source owners. It completes missing parameter, return and checked-exception documentation, records existing null and range behavior, and corrects the reversed whitespace description in `Strings.isNullOrBlank` to match its unchanged code-point loop.

The canonical class is `com.synexia.rewrite.M3JavaBaseDoclintRecipe`. It delegates exact source admission and candidate generation to the existing `M3HashPinnedJavaSnapshotRecipe`. Its Maven coordinate is `com.synexia.verification:m3-java-base-doclint-recipe-proof:1`. Run `mvn -f recipes/m3-java-base-doclint-20261018/pom.xml test` from this recipe module with Java 21. The proof compiles only the exact declared engine dependencies and crate owner, then uses actual OpenRewrite Results as compiler inputs.

## Fresh recovery qualification

The October 19 rerun passed all three JUnit groups. Tests prove comment-stripped Java syntax identity, exact generated text, fixed point, twelve successive partial states, missing-owner, duplicate-owner, source-drift, late-source and corrupt-template refusals. The compiler witness starts at exactly 121 diagnostics and applies one source file at each of twelve passes. Remaining doc warnings are 119, 47, 44, 41, 34, 31, 30, 17, 7, 5, 2 and 0. No compiler-policy flag changes.

After the last documentation pass, javac reaches two existing lexical-scope warnings in `ProgressMonitors`. The separate `M3ProgressScopeLintRecipe` qualifies those and the other three corresponding functional-interface scope sites using source-retained annotations. Its strict final closure has zero warnings and fifteen identical class files and public/protected API listings. The documentation-only compiler witness deliberately records its remaining two warnings and nonzero `-Werror` exit.

The earlier transient workspace and logs were lost. These sources were reconstructed from the exact public baseline and their prose was freshly reviewed; the retained receipts describe the new run. The receiver preserves this proof packet and links to the canonical Synexia source commit. Full-image qualification is recorded separately by the receiving PR.
