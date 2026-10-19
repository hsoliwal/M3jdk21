# Existing collection JNI convergence and sorting parity

This scope combines two previously published native atoms inside their existing collection owners. The `java.util.Sorts` implementation remains a Java implementation; this scope does not add an implicit native backend to it. The existing `M3SignalOrderNative` LSD kernel is retained and checked against the new sorting API's key and permutation semantics.

## Native source selection

| Runtime owner | Selected source | Existing behavior retained | Concrete contribution |
| --- | --- | --- | --- |
| `m3_index_path_masks.c` / `M3IndexPathMasks` | [M3JDK21 #1294](https://github.com/hsoliwal/M3jdk21/pull/1294), revision `2027d2f4e9810e5f0e78c91c079cfa0130c24695` | Primitive state planes, physical offsets, positive or negative strides, full 32-bit required/forbidden masks, stable row selection, explicit native admission | One shared count/gather core carries either `int[]` or `long[]` row identifiers. The original `selectNative` ABI is retained and `selectLongNative` is additive. |
| `m3_index_mask_window.c` / `M3IndexMaskWindow` | [M3JDK21 #1303](https://github.com/hsoliwal/M3jdk21/pull/1303), revision `e70058e73e4f42036815a24c2c22dac9dd948074` | Original and bounded JNI signatures, stable order, exact resume ordinal, independently validated output offsets, zero/contradictory mask admission | Each 128-state chunk records admitted relative offsets. JNI transfers only the enclosing admitted row span, stopping at the caller's output capacity. |
| `m3_signal_order_jni.c` / `M3SignalOrderNative` | Existing pinned receiving baseline `2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49` | Explicit opt-in, eight stable unsigned-byte radix passes, detached public order directories, Java scalar backend | No runtime source change. The new cross-owner contract compares this real JNI kernel, the existing scalar owner, the new `Sorts` LSD implementation, and independent comparison oracles. |

The selected native sources and the published C span instrumentation retain their Apache-2.0 per-file notices byte for byte. `NATIVE_INPUTS.json` in the canonical candidate provenance records the source Git blob, source SHA-256, preimage SHA-256, and exact target path. The sorting implementation's GPL/ClassPath notices remain in its own source files.

## Keys, indices and stable payloads

The existing signal owner canonicalizes NaNs for key comparison and encodes double total order as an unsigned long. `Sorts` compares signed long keys. XOR with `Long.MIN_VALUE` converts the unsigned ordering into the signed ordering without changing equality classes. Complementing that signed key gives descending order while preserving original equal-key occurrence order.

`M3SortsNativeParityContract` tests this conversion against `Double.compare` and `Long.compareUnsigned` oracles. It covers signed zeros, infinities, subnormal values, several NaN representations, equal groups, arbitrary unsigned 64-bit keys, opaque signed row labels and duplicate row labels. The public permutation tests cover nonzero source ranges, source-aligned masks, unchanged payload and mask arrays, detached exported indices, and immutable slice offsets. Exact signaling-NaN payload behavior is limited by the same platform copying semantics documented for the sorting API.

The native path-mask bridge preserves signed 64-bit row identifiers as `jlong`; it does not cast them into offsets. Physical offsets and path ordinals remain Java array coordinates. The original int API and the additive long API use the same stage predicate and checked path geometry. Java admission handles zero-stage and impossible-stage cases before allocating or dispatching, while maintaining explicit native-required refusal when specified by the owner contract.

## Bounded JNI work and failures

The window atom uses stack arrays of 128 elements and byte-sized relative offsets. It adds no native heap allocation, retained Java reference, pinned array, object callback, or native pointer exposed to callers. The exact next ordinal is preserved when the requested output fills within a chunk. `scanBounded` preserves the caller's admitted exclusive source boundary; its packed result supports resuming in a later call.

The path-mask bridge uses JNI Region calls and returns a fresh primitive array. Its count/gather core validates path arithmetic in 64-bit intermediates and checks each consumed physical offset against the relevant state plane. It detects changed match counts between its two passes. This is not a concurrent snapshot guarantee: the existing owner requires the caller to respect its input lifetime and mutation contract.

The unchanged signal JNI kernel copies its inputs into private native buffers, performs eight radix passes, copies results back, and frees all four work buffers. Public signal ordering passes detached temporary key/row arrays to this kernel, so user signal and mask arrays are not commit targets.

These existing native methods have no progress-monitor callback. They are not interrupted partway through a native call. Bounded source windows permit callers to check cancellation between calls. The new Java sorting API separately honors its borrowed monitor at its documented checkpoints and completes its callback-free payload commit after preparation. The parity test explicitly confirms that the existing signal JNI operation and an explicit `ProgressMonitor.NONE` sort preserve the thread's interrupt flag. It does not claim native cooperative cancellation.

## Verification

First run the sorting verifier to produce the actual `java.base` patch classes, then run this native verifier on the recipe-generated receiver:

```sh
python3 m3/verification/sorting/verify-native.py \
  --target /absolute/path/to/receiver \
  --preimage-tree /absolute/path/to/pinned/preimage \
  --sorts-patch /absolute/path/to/sorting/qualification/patch-classes \
  --java-home /absolute/path/to/jdk21 \
  --output /absolute/path/to/new/native/qualification
```

The preimage must contain the exact baseline `m3/collections` sources admitted by the canonical recipe. The output directory must be new or empty. The runner compiles all actual module main sources and the selected unchanged published standalone contracts; it does not provide substitute API, context, JNI, or collection classes.

The cross-owner Java contract is located at `m3/verification/sorting/src/test/java/com/m3/collections/M3SortsNativeParityContract.java`, within the existing canonical Java test-source layout. The runner checks Java21 strict compilation, generated JNI prototypes against the preimage, all nine collection libraries compiled from ten C units with C17 `-Wall -Wextra -Werror`, fatal UBSan, and exported native symbols. It executes the loaded-native standalone contracts with `-Xcheck:jni`, followed by the new signal/Sorts parity test and missing-native refusal controls in separate JVMs. Input hashes are checked again at completion. `EXECUTION.json` records each command, input, JNI prototype, library hash, log hash, and result.

The exact published span instrumentation is compiled with both the candidate window C and the pinned baseline. The candidate must pass and the baseline must fail its transferred-element assertion. The retained deterministic observations are one transferred row element instead of a full 128-element chunk for a one-result window, and 130 transferred elements instead of 300 in the selected cross-chunk case. Three injected Region-call failures verify bounded exception propagation in that instrumentation fixture. These are work-count and failure witnesses, not elapsed-time benchmarks or substitutes for real JVM tests.

Owning-module Maven tests, canonical recipe replay, full JDK image, jtreg, JCK, and other native platforms remain separately reported gates. A run with `--candidate` is explicitly labeled before-product qualification. Passing it does not establish canonical replay or receiver acceptance.
