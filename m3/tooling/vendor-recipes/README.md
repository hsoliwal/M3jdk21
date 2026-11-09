<!-- SPDX-License-Identifier: Apache-2.0 -->
# m3/tooling/vendor-recipes — consumer pins to the vendored sources, as an OpenRewrite recipe

`M3VendoredPinRecipe` is an XML recipe on the SDK's own vendored OpenRewrite (rewrite-core and rewrite-xml
8.17.1-m3-source-1). It replaces the regex script `switch_consumers.py` that switched consumer poms to the source-built
coordinates (`<version>-m3-source-1` / `-synexia-source-1`). It reproduces that script exactly, then becomes the
maintained path ("increasingly better OpenRewrite recipes").

**Options:**
- `modules`: the `THIRD_PARTY_MODULES.tsv` table;
- `modulesSha256`: its pin, which is required;
- `suffix`;
- `nativeInBinary`;
- `defaultProps`.

**Per `<dependency>`, in document order:**
1. Under a `<plugin>`: untouched, row `PLUGIN_DEPENDENCY_UNTOUCHED`.
2. Version already ends with the suffix: untouched, `ALREADY_SOURCE`.
3. `group:artifact` is not a vendored MODULE: untouched, no row.
4. The effective version (a literal, or a whole `${p}` resolved from the defaults overlaid by the pom's `<*.version>`
   properties) is not inlined: untouched, `VERSION_NOT_INLINED (inlined: …)`.
5. Native libraries kept in the binary: untouched, `KEPT_BINARY_NATIVE_LIBRARIES`.
6. Otherwise the version becomes `strip(declared) + suffix` (so `${p}` becomes `${p}-suffix`), `SWITCHED`.

**`M3VendoredPinApply`:**
- It reads each target from `git show <base>:<pom>` and never writes to the repository.
- Output poms go to `--out-dir`, plus a rows TSV in the script's schema and a receipt (base commit, input pins, per-pom
  before/after sha256, `FIXED_POINT`).
- Exit codes: 0 done; 3 drift refusal (nothing written); 4 not a fixed point; 1 refused input (modules table off its pin,
  invalid XML, non-canonical path).

**Proof (2026-10-13):**
- `mvn -o verify`: 10 tests, including fixed point, drift refusal and wrong pin. RewriteTest also checks 2 cycles and
  the recipe's serialization.
- `tools/differential.sh` compares the recipe with the oracle copy `oracle/switch_consumers_outdir.py`, which is the
  original script plus `--out-dir`:

| Lane | Base | Targets | Identical poms | Rows equal (bytes) |
|---|---|---|---|---|
| m3 | origin/master `76c62f01` | 51 | 51 | 176/176 yes |
| synexia | origin/develop `0894a5c1` | 10 | 10 | 181/181 yes |
| m3-preswitch | `cc785460` (49 switches) | 51 | 51 | 176/176 yes |
| synexia-preswitch | `5bb1089c` (59 switches) | 10 | 10 | 178/178 yes |

Two oracle behaviours are reproduced on purpose:
- A `${prop}` whose property already carries the suffix is reported `VERSION_NOT_INLINED`, because the script tests the
  declared text, not the resolved value.
- The Python CLI needs `--suffix=-m3-source-1`, with the `=`.

rewrite-maven is not used: it is vendored only as a source image, not as a built module, and it needs dependency
resolution (see `FOSS_REUSE_DECISION.tsv`).
