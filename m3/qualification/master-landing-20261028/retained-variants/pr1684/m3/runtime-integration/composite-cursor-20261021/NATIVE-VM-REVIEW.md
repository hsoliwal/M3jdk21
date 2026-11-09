# Independent JNI, VM and backing-owner review

Baseline: `hsoliwal/M3jdk21` commit `a4415af418a9b0f21a06cd5801d238577fbb6eb7`. This review reads source and frozen candidate postimages; it does not establish that the candidate builds or runs in a matched VM. `native-vm-review-receipt.json` binds the five reviewed candidate files and `native-vm-mindex-coverage.tsv` binds the 25 supplementary baseline production files by SHA-256.

## Decision on the frozen five-file JNI candidate

No newly introduced blocking JNI contract or lifetime defect was identified in the reviewed change. The decision is limited to the two production edits and their three test files; target-runtime qualification remains required.

### Compatibility shadows

`src/java.base/share/native/libjava/String.c` adds `m3_check_shadow_range` before an empty return or array allocation. Both entry points have already rejected null, negative start/length, and invalid byte coders. The new check uses `start > source_length || length > source_length - start`; short-circuit evaluation and those nonnegative arguments prevent signed overflow in the subtraction. A valid character range therefore also makes the later `start + length` safe.

Byte-shadow output-size checking still precedes the new source-range check, preserving the explicit oversized-byte-array `OutOfMemoryError`. Valid empty calls still allocate fresh arrays. The existing bulk `getBytes` / `getChars` callbacks, returned array ownership, and exception checks are preserved. Invalid empty ranges now throw instead of returning an array, and invalid huge ranges may fail earlier: these are explicit private-native defect repairs, not claims of identical malformed-call behavior.

`M3StringShadowRangeTest.java` checks independent UTF-16 values, native-endian compact bytes, surrogate seams, slices, fresh array identity and isolation, null/negative/coder errors, empty out-of-range calls, and positive-overflow coordinates. Its target VM run is still pending in the reviewed evidence.

### GetStringCritical donor atom

An independent GitHub connector read of donor commit `5d2bbd7401053eba2f64efe0a71dfb367ce357e6` returned full-file git blob `0e1de0e62b03e24d7e07e3b2b9cd7faab6071f0c` for `src/hotspot/share/prims/jni.cpp`. Computing the git blob from the complete candidate gives the same value. The requested source excerpt and origin are preserved in `jni-donor-source-read.json`.

The only production difference from baseline replaces the character-by-character loop in `jni_GetStringCritical` with the existing `java_lang_String::copy_chars`. This matches PR 1629 exactly. The donor's canonical recipe identity is separately recorded in `CRITICAL_DONOR.json`: Synexia commit `037075d86ddfa7ccbaf2b77fa628db8468ac1e47`, crate `m3jdk21-native-text-io-20261020`.

`javaClasses.cpp` already implements the reused copier for flat Latin-1/UTF-16, native/mapped atoms, packed Immediate owners, and intersecting clipped tuple ranges. It does not add Java allocation or a Java callback during this native copy. The JNI atom leaves the existing copy-versus-pin branch, native allocation, null-on-allocation-failure result, zero terminator, `isCopy`, and `ReleaseStringCritical` policy unchanged. No retained pointer, new canonical payload, or ABI is introduced.

The new bridge in `libM3StringCriticalFirstTest.c` reads only the logical length before its first content access via `GetStringCritical`. Between acquisition and release it performs a bounded local UTF-16 digest loop, with no JNI call, allocation, or blocking operation. It releases before allocating the result array. The Java fixture checks expected char-array digests for scalar, composed and clipped inputs, lengths around seams, split/unpaired surrogates, reacquisition, and retained slices after GC pressure. The stock-VM log in `evidence/host-jni-compile-reference.json` is useful fixture evidence and explicitly has `m3Fixtures=0`; it is not target M3 execution.

Two limitations remain explicit. The target test requires at least one M3-backed fixture but does not independently assert every intended owner kind and clipped tuple shape. Its native-content oracle is a 64-bit digest, not exhaustive returned-buffer comparison. Also, the donor intentionally preserves the pre-existing signed-int `s_len + 1` native-allocation expression; an `Integer.MAX_VALUE` logical String needs a separate overflow review/test. Neither limitation is newly introduced by the atom, and neither is described as repaired here.

## Coverage and current runtime boundary

The specified direct-symbol search finds **48** baseline production files. The prior Java table covers **23** of them and additionally includes `M3FrozenLexiconIndex.java`; the supplementary TSV covers the remaining **25** with an exact set-equality check. Registry rows are labelled as registry evidence, not implemented consumer behavior. This does not enumerate every transitive String consumer; the companion collections catalogue evaluates those routes separately.

The enabled configuration remains an experiment:

- `runtime/globals.hpp` defaults `UseM3StringStorage` to false.
- `runtime/arguments.cpp` forces interpreter mode after argument parsing, disables optimized String concatenation and shared spaces, and rejects requested JVMCI, string deduplication, CDS archive operations and JFR options.
- `classfile/vmIntrinsics.cpp` disables selected String/Builder/Buffer intrinsics; `opto/stringopts.cpp` separately refuses legacy C2 concatenation. These are safeguards, not completed M3 compiler lowering.
- `services/diagnosticCommand.cpp` refuses dynamic CDS archive operations; `jfrRecorder.cpp` disables/refuses dynamic recorder creation as well as startup use. `javaClasses.cpp` excludes M3 Strings/values from heap archiving.
- `stringDedupTable.cpp` skips M3-backed Strings so their empty layout sentinel is not deduplicated as content.

The review identified a precise existing serviceability gap: `OopUtilities.m3OwnerCharAt` handles atom kind 1 and tuple kind 2, then throws. The valid Immediate owner kind 3 is not decoded, although the VM accessors already decode it. This candidate leaves that file unchanged; complete SA support must not be claimed.

The backing/index review also records an unfinished integration: `M3ChunkCursor` is consumed by `M3RunTable` and `M3RegExpCompiler`, but neither baseline nor candidate `M3String.CodeUnitCursor` implements it. The continuation improvement therefore does not complete the chunk-interface adapter. `M3StringBacking` default loops, mapped backing views, `M3TQ` preparation and RX7 candidate loops are separate routes, not automatic cursor consumers. RX7 verification still consults authoritative `Pattern`; search images remain data-only. Mapped backing `close()` closes channels and rejects subsequent backing access; it is not an explicit immediate unmap of every retained page/view.

Matched VM build, enabled/disabled target runs, CompactStrings variants, JNI checking, owner/GC lifetime, allocation-pressure and platform mapping gates remain pending or separately recorded. No stock-reference result, source inventory, donor history, or model traversal count substitutes for those gates.
