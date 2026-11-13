<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# absorb-common-b28-20261109

This batch converges synexia-common packages outside `language`, using RELOCATE_M3_OWNER v6.3. The donor is
com.synexia develop `8ab80ea57d9f`, staged with `stage_blobs.py` because `git archive` stalled on the large clone.

## Selection

`select_multi` ran over these packages: canonical, utils, semantic, convergence, io, extracted, validation,
relationbits, progress, intern, utils.varhandle, tools and lifecycle (SEL_b28.tsv).

- **`language` is excluded.** It is mostly IndexString, which is lane d7's.
- **Result:** 43 ELIGIBLE, 102 HOLD (third-party imports, lombok, or missing dependencies), 9 HUB, 3 LANDED.
- **Landed here:** 42 classes and 7 tests, placed by v6_place:

| Package | Classes |
|---|---|
| `jdk.internal.m3.collections` | 34 |
| `jdk.internal.m3.collections.packed` | 7 |
| `jdk.internal.m3.collections.bits` | 1 |

- **Deferred:** `LanguageSpec` (no dependents). jdk_naming's PKG_PATH rule would rewrite a GitHub URL in its
  javadoc into `github.jdk/internal/m3/...`, which is a defect in the naming tool. The class waits for that fix.

## Declared deviations

| Code | Where | Change |
|---|---|---|
| DONOR_BUG_FIX | `CoreOps.isFalseValue` | The donor tests `"of"`, so `parseBoolean("off", true)` returns true and the donor's own `CoreOpsTest.preservesStringAndParsingContract` fails. M3 tests `"off"`. In BYTECODE_RESIDUAL.txt, with constant-pool indices stripped, this is the only bytecode difference. The same fix goes to com.synexia. |
| LINT_SUPPRESS | `CoreOps.coalesce` | `@SuppressWarnings("varargs")` next to the existing `@SafeVarargs`, because the module builds with `-Xlint:all -Werror`. It is source-retention, so the bytecode is unchanged. |
| NAMING_CONST | `TokenIdentityScope.INVARIANT_ID` | JDK_NAMING_RENAME (user decision 2026-10-31) renames it from `SYNEXIA-FILE-ATOM-CANONICALIZATION-1` to `M3-FILE-ATOM-CANONICALIZATION-1`. Its only use is an exception message in `TokenInterner`. |
| NAMING | `CoreOps` javadoc | `synexia-common` becomes `m3-common`. |

## Proof (PROOF.tsv)

- **Bytecode:** across 81 class files, `javap -c` and `-v` are identical modulo names, except for the CoreOps fix
  above.
- **Mutant:** caught.
- **JUnit, relocated:** 46 of 46 pass.
- **JUnit, donor:** 45 of 46 pass. The one failure is the donor bug above.

## Gates

- **Sprawl gate:** new=43, refused=0.
- **jdk_naming:** fixed point once LanguageSpec is deferred.
- **Custody:** resealed; `--check` reports changed=0.
- **Default suite:** 2836 tests, 0 failures, 0 errors, 151 skipped.
