<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# absorb-common-b29-20261109

This is the first batch on the PlaceholderLogger facade (#2915; user decision 2026-11-09, "Internal {} facade").
It contains 45 synexia-common owners that were held only on lombok `@Slf4j` or slf4j. The donor is com.synexia
develop `8ab80ea57d9f`.

## Selection

`select_multi` v1.4 ran with `SLF4J_FACADE=1`, which treats `lombok.extern.slf4j.Slf4j`, `org.slf4j.Logger` and
`org.slf4j.LoggerFactory` as satisfiable. It found 48 ELIGIBLE; 45 land, placed in `jdk.internal.m3.collections`
(44) and `.graph` (1). Three are deferred:

| Class | Why it waits |
|---|---|
| LanguageSpec | jdk_naming PKG_PATH would rewrite a github.com/synexia URL in its javadoc |
| LexerSpecOps | the same rewrite would hit a URL in a runtime string constant, DEFAULT_REGISTRY |
| TextOps | the sprawl gate refuses it: PKG_IN_NAME (`text` is a package and `Ops` an existing type) |

None of them has dependents in this batch.

## Recipes applied (tools/)

| Recipe | Change | Bytecode |
|---|---|---|
| SLF4J_TO_PLACEHOLDER_LOGGER v1 | `@Slf4j` becomes `private static final PlaceholderLogger log = PlaceholderLogger.of(T.class);` as the first member, where lombok places it. An explicit slf4j field is retyped. Messages and call shapes are unchanged. | changes: logger owner type |
| LINT_SUPPRESS_VARARGS v1.1 | `@SuppressWarnings("varargs")` next to `@SafeVarargs` for the -Werror build. Methods declared on one line get it inline. | unchanged (source retention) |
| DOCLINT_REPAIR v1 | The 11 doclint errors in donor javadoc are fixed: a bare `<` becomes `&lt;`, member `<h2>` becomes `<h4>`, class `<h3>` becomes `<h2>`, and a dangling `@see TimingOps` is dropped. | unchanged (comment only) |
| DOC_REF_DROP | `PaddedAtomicLong`'s `@see com.synexia.common.utils.CacheLineOps` is removed, because that class is not in M3. | unchanged (comment only) |
| Banner | Line 1 `/*****` becomes `/* ****`, the release lane's doclint rule. | unchanged (comment only) |

## Proof (PROOF.tsv)

The donor side compiles with lombok 1.18.46 and slf4j-api 2.0.17 (prove_mat `SYN_EXTRA_CP`). `norm_b29.full.sed`
declares these mappings:
- `org/slf4j/Logger` to `PlaceholderLogger`, and `LoggerFactory.getLogger` to `PlaceholderLogger.of`;
- the `of` call's `InterfaceMethod` kind to `Method`;
- the helpers `PaddedAtomicLong*`;
- constant-pool indices are stripped, because the facade folds two slf4j class entries into one; javap keeps the
  resolved names.

Results:
- **javap -c:** 0 of 88 class files differ.
- **Mutant:** caught.
- **javap -v:** the only residual is the constant-pool shape. `PlaceholderLogger.of` is an `InterfaceMethodref` where
  `LoggerFactory.getLogger` was a `Methodref`, and the `LoggerFactory` class entry and its name are gone (6 lines per
  class).

## Gates

- **jdk_naming:** fixed point.
- **Sprawl gate:** new=45, refused=0.
- **Compile:** the module builds under -Xlint:all -Werror.
- **Doclint:** -Xdoclint:all,-missing on the 45 files reports 0 errors.
- **Custody:** resealed; --check reports changed=0.
- **Default suite:** 2841 tests, 0 failures, 0 errors, 151 skipped.
