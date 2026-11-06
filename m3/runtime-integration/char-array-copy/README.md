# Segmented char-array projection recipe

Input: `hsoliwal/M3jdk21` master `e28f41df0d18cc1e651eebcd71cc6cf951b0759b`.

Owned production source: `src/java.base/share/classes/java/lang/MIndexString.java`.
Owned regression source: `m3/runtime-integration/tests/MIndexStringInvariant.java`.
Superseding-owner custody is pinned to the exact merged source observation `9237d21cccc0d700555ccc4eab3b87edb2833af3` (M3JDK21 PR #1371, landed through PR #2070): `M3String.java` has Git blob `8ddd767e9198f5716ebde4aada80b91591125d9d`. The earlier merged-result blob `8a57ca4f085e4e8f1f1d9f2ec0fdb2fc5888597e` remains in `manifest.json` supersession history. `M3StringInvariant.java` remains pinned at `a39f876f0d3065d0754f8381e57aa097b6051e35`. This is a metadata-only custody correction; no production Java/JNI source or test body is changed. Full foundation compilation, recipe refusal tests, and runtime proofs remain required before acceptance.

Owned gate: `.github/workflows/m3-foundation.yml`. The older runtime recipe remains
source-sealed at its original postimage. CI reverses this overlay in its isolated
checkout, runs the complete older recipe check and test suite, then reapplies this
overlay and verifies its postimage.

Contract: Ordinary `String` retains its existing descriptor, coder, immutable input snapshot,
and public `getChars`/`toCharArray` behavior. Copy joined descriptors by walking the
canonical segments once, direct local scalars via Compact-String range routines, and
mapped scalars from their retained mapped owner. No flattened cache is allocated.
Zero-length copies preserve the destination. The recipe must reject source drift,
be idempotent, and replay/reverse against exact source images.

Independent mutable `char[]` values become immutable atoms with `new String(chars)`.
The existing `String.join("", atomStrings)` path makes a canonical joined descriptor
for at most 127 input Strings; `concat` can extend it beyond that fast-path bound.
Those operations still allocate wrappers and tuple metadata. They do not create a
single Java array reference over distinct heap arrays. `toCharArray()` intentionally
allocates its required flat result, now copying each joined range without a segment
lookup per character. The JOINED storage kind is the combined MIndex representation;
there is no competing combined-String owner.

Acceptance: JDK source compilation, layered recipe replay and drift tests, and the runtime
`MIndexStringInvariant` on a matched M3 JDK image when available. Runtime flag-off,
interpreter, lexicon, shared mapped owner, and non-compact modes remain gates.

```sh
python3 m3/runtime-integration/char-array-copy/recipe/apply.py --check
python3 m3/runtime-integration/char-array-copy/recipe/test_recipe.py
M3_TEST_JDK=/path/to/matched/image M3_BOOT_JDK=/path/to/java21 \
  M3_OWNER_SRC=/path/to/owner bash m3/runtime-integration/run-focused.sh
```
