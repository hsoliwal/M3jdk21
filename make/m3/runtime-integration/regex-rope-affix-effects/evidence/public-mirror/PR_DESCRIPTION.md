## Why

M3 rope full matches can reject an incorrect mandatory literal boundary before evaluating the rest of the text. Repeated composite matches can also reuse the same fixed prefix and suffix computation across many rows. This change implements those refinements within the existing String regex and numeric Effect owners.

## Changes

- Check complete mandatory literal prefixes and suffixes through the existing rope `CodeUnitCursor`. Preserve exact cached root effects before boundary reads, existing mandatory facts, and the established UNKNOWN/Pattern fallback contract.
- Add an opt-in internal prepared batch API to `M3RegexDpPlan`: immutable shared Effects, copied CSR row coordinates, a fixed prefix seed and two suffix acceptance preimages. Java and JNI evaluate each row's variable fragments while preserving UTF-16 surrogate seams across fragments and fixed context.
- Bound preparation and execution work, validate native numeric images, retain explicit backend receipts, and cover close, cancellation and native allocation failure behavior.
- Include the source-pinned transformation recipe, its four bundled SDK owners, source manifests, catalogue and qualification evidence. The public adaptation has no Synexia runtime dependency. It receives from #1029 (`3bbb4712fb20adba1405741db8457164b9f436f5`).

Public String/Pattern signatures and the admitted String shortcut grammar remain unchanged. The new internal prepared batch API is an explicit consumer interface; this change does not automatically route general String matching to a GPU.

## Validation

- Five actual OpenRewrite SDK tests pass, including named YAML activation, replay, fixed point and refusal cases. The frozen public mirror separately passes a fresh offline run using only its bundled SDK owners. Receiving checks and applies all 15 already-converged targets with zero changes; bytes, modes and modification times remain unchanged.
- The final source/JNI contract completes all 16 required commands with the existing full-String baseline hold preserved.
- A complete Linux x86_64 fastdebug JDK image builds from the exact production Java/C postimages. The final SDK-emitted whole DP test passes in four modes: normal/interpreted execution with CompactStrings enabled/disabled, with 56,763–56,764 checks per invocation. Both declared prepared batch runs pass with 231,612 checks and 46,278 strict JNI rows each, using the image's built `libjava`. All six critical invocations use `-Xcheck:jni`.
- The additional unchanged focused suite records seven passing tests and four failing fixtures. The failures remain explicit: historical `caches` and `RELATION_ACCESS` reflection, an empty-string attached-owner assumption, and a nested substring expected literal that incorrectly retains an excluded low surrogate. Their original failures and bounded diagnoses are retained.

## Remaining gates

The complete String stage remains on HOLD: the original BMH/KMP invariant checker reports the same failure on the sealed baseline and candidate. No family completion or ordered promotion gate is advanced. The full regex suite was not run.

The local source-fixture timings are mixed, so this PR does not claim a universal speedup or product performance acceptance. CUDA compilation, physical GPU execution, transfer/residency measurements and GPU performance remain unqualified; the optional provider stays in private convergence.

Detailed scope, source identities, original failed attempts and executed receipts are retained in `m3/runtime-integration/regex-rope-affix-effects/recipe/QUALIFICATION.md` and its `evidence/` directory.
