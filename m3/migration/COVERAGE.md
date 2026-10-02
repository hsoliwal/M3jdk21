# MIndex → M3 migration coverage

Generated from `manifest.json`; do not edit status rows by hand.

Source baseline: `6df9df8d8f42111239013ee941723ec37f97ba6e` (`develop`)
Target master baseline: `8bb6215372e07712f1fdf5a0cb912af495007b19`
Route-C runtime candidate: `3776d6e674d6c9b04539ca24aca2504aa88d4a57`

## Status summary

| Status | Count |
| --- | ---: |
| implemented-tested | 0 |
| implemented-unverified | 2 |
| partial | 7 |
| proposed | 4 |
| blocked | 2 |
| excluded | 0 |

## Capability mappings

| ID | Capability | Kind | Status | Source → target |
| --- | --- | --- | --- | --- |
| `M3-TEXT-EXPLICIT-001` | Route A explicit immutable String-shaped value | adapter | **implemented-unverified** | `com.synexia.indexstring.MIndexString` → `com.m3.text.M3Text` |
| `M3-TEXT-SLICE-001` | UTF-16 range view and substring contract reconciliation | consolidation | **implemented-unverified** | `com.synexia.indexstring.SubMIndexString, com.synexia.mindex.SubMIndexString` → `com.m3.text.M3Text#substring` |
| `M3-TEXT-TUPLE-001` | Canonical tuple bodies and resolver-scoped interning | specialization | **partial** | `com.synexia.indexstring.MIndexStringIntern, com.synexia.indexstring.IndexStringTuple` → `com.m3.text.M3Text.WeakInterner, java.lang.MIndexString` |
| `M3-COMPILER-001` | Route B compiler-lowering contract target | adapter | **partial** | `com.synexia.indexstring.MIndexStringCompilerRuntime` → `com.m3.text.M3TextCompilerRuntime` |
| `M3-RUNTIME-STRING-001` | Route C modified complete JDK String storage | specialization | **partial** | `com.synexia.indexstring.MIndexString, com.synexia.indexstring.shared.SharedArrayPool` → `java.lang.MIndexString, java.lang.String` |
| `M3-STRUCT-ATOM-001` | Primitive canonical atom store for structural domains | pending | **proposed** | `com.synexia.indexstring.MIndexAtomStore` → `—` |
| `M3-STRUCT-AST-001` | Parser-neutral immutable AST over canonical atoms | pending | **blocked** | `com.synexia.indexstring.MIndexAst` → `—` |
| `M3-INTERACTION-001` | Precomputed role x role x mode interaction classifier | pending | **proposed** | `com.synexia.indexstring.MIndexInteraction` → `—` |
| `M3-STRUCT-DAG-001` | Immutable indexed DAG over canonical atoms | pending | **blocked** | `com.synexia.indexstring.MIndexDag` → `—` |
| `M3-MATINDEX-001` | Whole-string MatIndex pool/handle and prepared views | pending | **proposed** | `com.synexia.indexstring.MatIndexString` → `—` |
| `M3-RESOLVER-001` | Frozen-first resolver, exact local novelty and encoder admission | specialization | **partial** | `com.synexia.indexstring.HierarchicalIndexResolver, com.synexia.indexstring.HybridIndexEncoder, com.synexia.indexstring.IndexInternedLiteralNamespace, com.synexia.indexstring.SharedArrayIndexResolver` → `com.m3.text.M3Text admission` |
| `M3-DICTIONARY-001` | Packed/mapped dictionary and immutable mapped payload generations | specialization | **partial** | `com.synexia.indexstring.PackedIndexLexicon, com.synexia.indexstring.IndexDictionary, com.synexia.indexstring.MappedIndexDictionary, com.synexia.indexstring.MIndexMappedArrays, com.synexia.indexstring.MIndexMappedPrecomputation` → `java.lang.MIndexStringPool mapped reader` |
| `M3-JOINED-001` | Joined immutable chars/bytes, frozen ranges and descriptor canonicalization | dependency-reuse | **partial** | `com.synexia.indexstring.FrozenChars, com.synexia.indexstring.FrozenBytes, com.synexia.indexstring.MIndexJoinedChars, com.synexia.indexstring.MIndexJoinedBytes, com.synexia.indexstring.MIndexJoinedStorageIntern` → `com.m3.text.JoinedM3StringPiece` |
| `M3-PRECOMPUTE-001` | Derived String facts, shared text index and prepared joined search | specialization | **partial** | `com.synexia.indexstring.MIndexStringPrecomputation, com.synexia.indexstring.SharedArrayTextIndex, com.synexia.indexstring.MIndexJoinedNativeSearch` → `java.lang.MIndexString hash facts` |
| `M3-NATIVE-001` | JNI/native resolver and descriptor acceleration | pending | **proposed** | `com.synexia.indexstring.nativebridge.MIndexNativeIntern, com.synexia.indexstring.MIndexNativeResolver` → `—` |

## Visible gaps

- `M3-TEXT-EXPLICIT-001` — **implemented-unverified**: verification, inventory closure, or promotion still pending
- `M3-TEXT-SLICE-001` — **implemented-unverified**: verification, inventory closure, or promotion still pending
- `M3-TEXT-TUPLE-001` — **partial**: verification, inventory closure, or promotion still pending
- `M3-COMPILER-001` — **partial**: verification, inventory closure, or promotion still pending
- `M3-RUNTIME-STRING-001` — **partial**: Current Synexia source advanced after the runtime pin; StringJoiner OOME failures remain; compiled segmented/JIT mode, full jtreg/JCK and live SA attach unverified
- `M3-STRUCT-ATOM-001` — **proposed**: verification, inventory closure, or promotion still pending
- `M3-STRUCT-AST-001` — **blocked**: Blocked by atom-store/resolver layering
- `M3-INTERACTION-001` — **proposed**: verification, inventory closure, or promotion still pending
- `M3-STRUCT-DAG-001` — **blocked**: Blocked by atom-store layering
- `M3-MATINDEX-001` — **proposed**: Full MatIndex dependency closure not enumerable through current private-repo code index
- `M3-RESOLVER-001` — **partial**: Frozen-first/shared resolver integration into Route A pending
- `M3-DICTIONARY-001` — **partial**: verification, inventory closure, or promotion still pending
- `M3-JOINED-001` — **partial**: verification, inventory closure, or promotion still pending
- `M3-PRECOMPUTE-001` — **partial**: Current word/pattern/precompute PRs postdate Route-C pin
- `M3-NATIVE-001` — **proposed**: Active source branches contain incompatible ABI-v3 symbol supersets; numeric ABI alone is insufficient

The source connector did not expose a complete private-repository directory/code index, so this report is a source-grounded seeded inventory, not yet the exhaustive closure required for completion.

A destination file is not proof of migration completion. Exact contracts and exact-candidate evidence remain mandatory.
