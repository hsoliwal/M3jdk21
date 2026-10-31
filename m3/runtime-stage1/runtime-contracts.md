# Coordinated String/VM integration boundary

All paths below are relative to the pinned OpenJDK repository. The manifest
seals their exact source bytes; it does not claim a complete dependency graph.

| Owner | Current contract and required work for discontiguous backing |
|---|---|
| `java/lang/String.java` | `@Stable private final byte[] value`, byte coder, cached hash/hashIsZero; length and many operations directly consume the entire contiguous array. A segment directory cannot be assigned to this field. Every direct backing read, construction, serialization and conversion path needs coordinated treatment. |
| `java/lang/StringLatin1.java`, `StringUTF16.java`, `StringCoding.java`, `StringConcatHelper.java` | Primitive-array algorithms, unit/byte lengths, endianness, trusted immutable construction and bulk copying. Splitting UTF16 surrogate pairs must not split encoding state. Flattening is the current safe boundary. |
| `hotspot/share/classfile/javaClasses.{hpp,cpp,inline.hpp}` | VM offset discovery requires `value` with byte-array signature. `value()` casts it to typeArrayOop; `length()` derives units from array length and coder. Introducing new fields alone cannot make null/segmented value safe. Hash and dedup flag access must remain coordinated. |
| `classfile/vmSymbols.hpp`, `vmIntrinsics.hpp` | String/helper signatures and intrinsic IDs bind Java methods to VM implementations. New representations need explicit eligibility/fallback rules, not a swapped classfile. |
| `opto/graphKit.cpp`, `opto/library_call.cpp`, `c1/c1_GraphBuilder.cpp` | Compiler lowering and String length/value/coder loads assume existing fields and array access. Any segment path needs matching guards, deoptimization and fallback semantics. Fastdebug interpreter success alone is insufficient. |
| `cpu/x86/macroAssembler_x86.cpp`, `stubGenerator_x86_64.cpp` | Platform intrinsics consume contiguous primitive memory. Segment dispatch or flattening must be established before entering these paths. Other architectures remain additional work. |
| `classfile/stringTable.cpp` | Intern equality/hash and canonical object identity must work for every representation, including literals, CDS and JNI-created Strings. Segment owner IDs are never substitutes for content equality. |
| `gc/shared/stringdedup/stringDedupTable.cpp` | Dedup reads/replaces equal byte arrays through java_lang_String. A segment owner needs tracing/barriers/lifetime and a separate eligibility contract; mutable global array pools are unsafe. |
| `cds/heapShared.cpp` | Archived objects, dedup flags and bootstrap heap ownership must be coordinated. New fields/owners change archive compatibility. Images must regenerate CDS archives. |
| `prims/jni.cpp` | UTF16/modified-UTF8 lengths, regions, critical and copied views require contiguous buffers or safe materialization with bounded lifetimes. No JNI pointer survives release. |

P1 retains every representation contract. Its single new construction is the
existing trusted `(byte[], coder)` constructor with an existing String's exact
immutable pair. No HotSpot source edit is necessary or claimed for this path.
The source gate refuses any unexpected VM change instead of suggesting that
unchanged HotSpot accepts a joined layout.

## Synexia owner reconciliation

The five real production owners in `synexia-pin.json` were fetched at PR7392
head `75b7e31199c4440e12c7c36f1746bf9b77f178a9` and compared byte-for-byte with
PR7388 `9c36c18a7778e6096148a3bbee8f2af7575138b4`: all are unchanged. PR7392
adds verification recipes/harnesses. Its reported 31 tests and 102 native plus
102 fallback calls are upstream evidence, not tests independently rerun here.

- `FrozenChars` owns immutable char arrays and range-sharing; `FrozenBytes` owns
  byte slices. Preserve their ingress copying and private backing boundaries.
- `LocalM3StringPiece` uses its existing Owner/generation/entryId/start/length
  contract over FrozenChars. `toString()` calls `asString()`, which materializes
  `frozenChars().toString()`. P1 cannot remove that first materialization copy;
  it can avoid copying its resulting String again in a singleton join.
- `MIndexJoinedChars` remains the weakly canonical joined body/range owner.
  Whole-join encoding handles surrogate and stateful charset seams; concatenating
  per-piece encoded arrays is not equivalent.
- `FrozenByteInterner` owns its bounded local lane, primitive indexes, LRU/MRU
  and policy-keyed lazy encoding. No duplicate pool is introduced in String.
- P0's `com.m3.text` types remain provisional byte-format experiments. Their UUID
  identities do not replace Synexia owners or turn local IDs into lexical keys.

## Next coherent segmented increment

An actual joined String representation needs one coordinated VM branch: define
trusted segment ingress, owner lifetime and content hashing; enumerate every
value/coder consumer; implement a verified flattening fallback before enabling
segment construction; coordinate interpreter/C1/C2/platform intrinsics, intern,
dedup, JNI, serialization and CDS; then enable a narrowly gated producer. Public
mutable CharSequence instances cannot be trusted as immutable owners. Mapped
file bytes need an immutable publication protocol before serving as live backing.

Acceptance must include bootstrap/failure injection, full upstream test targets,
GC/JNI/concurrency stress, allocation and retention bounds, and multiple platform
coverage. P1 is useful on its own and provides the source-bound gate; it does not
claim that this coordinated segmented implementation has been completed.
