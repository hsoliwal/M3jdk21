<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Borrowed progress scope lint prerequisite

The existing progress overloads use try-with-resources to restore lexical monitor bindings. Their scope variables intentionally exist for resource closure. This recipe adds exactly five method-local `@SuppressWarnings("try")` annotations after the twelve-owner documentation prerequisite: two in `ProgressMonitors`, and one each in `Consumer`, `Function` and `Supplier`.

The canonical class is `com.synexia.rewrite.M3ProgressScopeLintRecipe`, backed by the existing `M3HashPinnedJavaSnapshotRecipe`. The Maven coordinate is `com.synexia.verification:m3-progress-scope-lint-recipe-proof:1`. Run `mvn -f recipes/m3-progress-scope-lint-20261018/pom.xml test` with Java 21 from the canonical recipe module.

## Fresh recovery qualification

Both JUnit groups passed on October 19. The SDK checks all sixteen partial application states, fixed point, missing and duplicate owners, source drift, late changes and corrupt resources. Each output equals its input after removing the exact admitted annotation lines; there are five annotations across four files.

The compiler proof seals twelve documentation-qualified context sources and the actual public baseline `SuppressWarnings` declaration, whose retention is `SOURCE`. The baseline witness compiles with strict lint and doclint but omits `-Werror` solely to obtain original class files; it requires exactly five existing try-resource warnings. The candidate adds `-Werror` and produces zero warnings. All fifteen class files are byte-for-byte identical under `-g:none`; all fifteen public/protected API listings are identical. Production build configuration is unchanged.

Resources and the context manifest are hash pinned. SDK Results, rather than handwritten target copies, supply the candidate compilation. The original transient logs were lost during the environment reset; the packet contains fresh qualification receipts. Full-image outcome is recorded separately in the receiving PR.
