<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# M3Lexicons: owner-scoped phrase transfers

This continues the existing `M3RegexWordTransferRows` receiver. M3JDK21 owns the runtime;
Synexia owns the reusable recipe. It is not another dictionary, model service, regex
engine or M3 String overlay. Existing M3RXW001 v1 image bytes remain unchanged for
valid inputs. No corpus, vocabulary payload, external implementation or model is imported.

## Additive operations

```java
var rows = M3RegexWordTransferRows.read(image, expectedGeneration64, expectedPatternKey);
var phrase = rows.summarize(wordIds, from, count);
int exit = phrase.step(entryState);
boolean acceptingVisited = phrase.acceptsInside(entryState);
var whole = leftPhrase.then(rightPhrase); // both must originate from the same rows object
```

`wordIds` contains the existing dense CWIDs 1..V in the caller's vocabulary order.
CWID 0/OOV, SYNDICT1 IDs and MIWROWS1 IDs are not interchangeable: resolve/remap them
through their actual owners first. Separator atoms must also be represented explicitly;
these methods neither insert spaces nor infer missing tokens. `run(ids,from,count,state)`
is a non-copying single-entry-state range overload. `stateCount()` exposes Q.

A summary stores private `int[Q]` exits, `long[ceil(Q/64)]` accepting-visit bits and a
shared opaque owner token. It does not retain the spelling list, original ID lane or
vocabulary table. The arrays never escape. Lookup takes one exit read or bit test.

For left followed by right:

```
exit_AB[q] = exit_B[exit_A[q]]
seen_AB[q] = seen_A[q] OR seen_B[exit_A[q]]
```

Composition is associative but not commutative. Empty ranges are identity. Acceptance
means a state visited after consuming at least one UTF-16 unit, preserving the existing
word-row contract; it does not establish zero-width matches, captures, match starts/ends,
backreferences or complete `Pattern`/`Matcher` compatibility. Split-surrogate spellings
remain exact UTF-16 input, not silently normalized Unicode strings.

## Refusal and identity

The existing scalar methods now check state and word coordinates separately before
forming a row-major index. Invalid columns cannot alias another row, and negative IDs
cannot wrap into a valid cell. Builders reject nonpositive state counts and out-of-range
DFA exits before invoking `accepting` with that exit.

The reader validates canonical v1 flags, the 4096 page-size marker, reserved fields,
state values and zero unused acceptance bits. Geometry is bounded by the actual buffer
before multiplication/narrowing. These are declared malformed-input repairs, not
preservation of defective acceptance.

The additive bound-read overload checks all 64 stored generation bits and all 32
pattern-key bytes. The old `read(image)` remains integrity-only. Buffer index zero is
the image origin; position/order/mark are unchanged. Keep input bytes stable while
reading; copied table lanes are independent of the buffer after return.

The generation is still a 64-bit field, not a full lexicon SHA-256. Pattern keys are
caller-supplied. Checksums and identifier comparison do not prove a producer's DFA or
lexicon semantics: use trusted source/image manifests externally. No state remapping,
namespace equivalence or permission is inferred. Summaries from separate table objects
refuse composition, even when their images happen to be identical. This deliberately
avoids accepting equal Q as proof of shared automaton/lexicon ownership.

## Cost and lifecycle

Table construction remains O(VQ) cells plus spelling traversal. Phrase precomputation
is O(NQ), composition O(Q), application O(1); storage per summary is approximately
4Q + 8*ceil(Q/64) payload bytes plus object/array overhead. No hidden global cache,
per-word object, native copy or character rescan is added to phrase composition.
Caller policy must admit state/vocabulary sizes and work budgets; this change does not
implement paging, automatic memory-pressure management or universal safe allocations.
Input CharSequences, DFA behavior and ID lanes must be stable during their calls.

## Verification

```
python m3/lexicon/receiver/verify_transfer.py --out /absolute/fresh/proof
python m3/lexicon/receiver/verify_transfer.py --out /absolute/fresh/native-proof --jni --ubsan
mvn -f m3/lexicon/receiver/pom.xml verify
```

The Python launcher also accepts absolute Windows paths and requires no WSL. Native
qualification requires a GCC/Clang-compatible compiler and matching JNI headers. Only
Linux was executed in this delivery; Windows/macOS support is unqualified.

The first two commands exercise the actual production owner and unchanged dependency
closure with javac warnings-as-errors. The JNI C driver invokes the actual Java summary
methods through generated JNI headers and checks exceptions. It is test-only: no new
production native ABI or native-acceleration implementation is claimed. Both normal
and interpreted `-Xcheck:jni` modes are exercised; UBSan is optional.

The existing Maven proof remains, with two additional executions for regression and
composition. Maven, the full existing receiver proof's Commons oracles, OpenRewrite SDK,
installed M3JDK/java.base binding, jtreg/JCK and production performance were not executed
in this environment. The focused standalone source closure is not a whole-module pass.

Canonical recipe:
`synexia-openrewrite-recipes/recipe-crates/m3-lexicons-transfer-20261021/` in com.synexia.
It uses the unchanged source-seal manifest host and an OpenRewrite adapter, with exact
pre/postimages, drift refusal and fixed-point replay. Do not overwrite newer source:
reconcile any manifest mismatch as a new recipe generation.
