<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->

# Frozen-text compatibility fixture source coverage

[Guide](README.md) · [Fixture contracts](frozen-text-compatibility-reference.md) · [Coverage ledger](coverage.md)

Public source pin: `c842fab8cf5b799257e3c9ea07f8baddde80db09`.
The unfiltered physical directory
`m3/compatibility/synexia-reference/src/com/synexia/indexstring` contains exactly
four Java files, all read in full. There is no `package-info.java` or
`module-info.java` in this fixture source tree. This denominator is two frozen
storage snapshots, one compile-only seam and one main test, not four production
storage owners. It does not include same-named retained-port/vendor roots.

Every explicit public callable is mapped below: **12 + 11 + 1 + 1 = 25**.
The count excludes package-private/private helpers, inherited methods, an
implicit constructor and enum-generated methods; those boundaries are separately
accounted for. Complete-body/source-contract review is distinct from executed
coverage, binary delivery, API equivalence and whole-repository qualification.
All repository code, tests, scripts, workflows, gates, examples, native/runtime
execution and benchmarks were **NOT_RUN** in this documentation pass.

## Complete four-source map

| Source | Role and complete-body route | Explicit public callables | Git blob SHA-1 |
| --- | --- | ---: | --- |
| [FrozenBytes.java](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/m3-reference/src/jdk/internal/m3/indexstring/FrozenBytes.java) | [Heap/direct byte owner, admission, sharing, retained bytes, hash/copy/error paths](frozen-text-compatibility-reference.md#frozenbytes-all-twelve-public-callables) | 12 | `22656cfaa3ccf0c6b214f214c92324d7f3ea55be` |
| [FrozenChars.java](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/m3-reference/src/jdk/internal/m3/indexstring/FrozenChars.java) | [UTF-16-unit owner, admission, ranges, hash/materialization/direct descriptors](frozen-text-compatibility-reference.md#frozenchars-all-eleven-public-callables) | 11 | `791fffa3f9938e5266663c1cb256cc4d00f8c0f5` |
| [MIndexAddressedView.java](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/m3-reference/src/jdk/internal/m3/indexstring/MIndexAddressedView.java) | [Throwing compile-only seam, private constructor and Kind enum](frozen-text-compatibility-reference.md#compile-only-addressed-seam) | 1 | `500605e34098965b95617c881bc14ada41dd5377` |
| [SynexiaMappedStorageTest.java](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/m3-reference/src/jdk/internal/m3/indexstring/M3MappedStorageTest.java) | [Direct-memory main test, private check helper and static counter](frozen-text-compatibility-reference.md#direct-memory-ownership-and-materialization) | 1 | `bf2ebd126499ea0c29199f2160c377d909b70781` |

The four source blobs also match the earlier package inventory at
`278b221e8365ebeb6095a3a5643b38a0564258b5`. Their physical directory identity and
current public bodies, rather than a filename prefix, establish this scope.

## Explicit public declaration ledger

Each link names the declaration at the pinned target revision. Contracts,
validation order, units, ownership and limitations are in the linked reference
sections above. These are explicit methods in source, not an API-compatibility
certificate or a count of passing tests.

| Source | Declaration and line | Contract route |
| --- | --- | --- |
| `FrozenBytes.java` | [`public static FrozenBytes copyOf(byte[] bytes)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/m3-reference/src/jdk/internal/m3/indexstring/FrozenBytes.java#L24) (L24) | [Contract](frozen-text-compatibility-reference.md#frozenbytes-all-twelve-public-callables) |
| `FrozenBytes.java` | [`public MIndexAddressedView addressed(MIndexAddressedView.Kind kind, java.nio.ByteOrder order)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/m3-reference/src/jdk/internal/m3/indexstring/FrozenBytes.java#L44) (L44) | [Contract](frozen-text-compatibility-reference.md#frozenbytes-all-twelve-public-callables) |
| `FrozenBytes.java` | [`public int length()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/m3-reference/src/jdk/internal/m3/indexstring/FrozenBytes.java#L48) (L48) | [Contract](frozen-text-compatibility-reference.md#frozenbytes-all-twelve-public-callables) |
| `FrozenBytes.java` | [`public int retainedBytes()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/m3-reference/src/jdk/internal/m3/indexstring/FrozenBytes.java#L50) (L50) | [Contract](frozen-text-compatibility-reference.md#frozenbytes-all-twelve-public-callables) |
| `FrozenBytes.java` | [`public boolean isDirect()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/m3-reference/src/jdk/internal/m3/indexstring/FrozenBytes.java#L51) (L51) | [Contract](frozen-text-compatibility-reference.md#frozenbytes-all-twelve-public-callables) |
| `FrozenBytes.java` | [`public FrozenBytes compact()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/m3-reference/src/jdk/internal/m3/indexstring/FrozenBytes.java#L53) (L53) | [Contract](frozen-text-compatibility-reference.md#frozenbytes-all-twelve-public-callables) |
| `FrozenBytes.java` | [`public int hash32()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/m3-reference/src/jdk/internal/m3/indexstring/FrozenBytes.java#L54) (L54) | [Contract](frozen-text-compatibility-reference.md#frozenbytes-all-twelve-public-callables) |
| `FrozenBytes.java` | [`public byte byteAt(int index)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/m3-reference/src/jdk/internal/m3/indexstring/FrozenBytes.java#L55) (L55) | [Contract](frozen-text-compatibility-reference.md#frozenbytes-all-twelve-public-callables) |
| `FrozenBytes.java` | [`public byte[] copy()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/m3-reference/src/jdk/internal/m3/indexstring/FrozenBytes.java#L59) (L59) | [Contract](frozen-text-compatibility-reference.md#frozenbytes-all-twelve-public-callables) |
| `FrozenBytes.java` | [`public FrozenBytes slice(int start, int end)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/m3-reference/src/jdk/internal/m3/indexstring/FrozenBytes.java#L65) (L65) | [Contract](frozen-text-compatibility-reference.md#frozenbytes-all-twelve-public-callables) |
| `FrozenBytes.java` | [`public void copyTo(int sourceStart, byte[] target, int targetStart, int count)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/m3-reference/src/jdk/internal/m3/indexstring/FrozenBytes.java#L72) (L72) | [Contract](frozen-text-compatibility-reference.md#frozenbytes-all-twelve-public-callables) |
| `FrozenBytes.java` | [`public ByteBuffer asReadOnlyBuffer()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/m3-reference/src/jdk/internal/m3/indexstring/FrozenBytes.java#L80) (L80) | [Contract](frozen-text-compatibility-reference.md#frozenbytes-all-twelve-public-callables) |
| `FrozenChars.java` | [`public static FrozenChars copyOf(char[] chars)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/m3-reference/src/jdk/internal/m3/indexstring/FrozenChars.java#L21) (L21) | [Contract](frozen-text-compatibility-reference.md#frozenchars-all-eleven-public-callables) |
| `FrozenChars.java` | [`public int hash32()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/m3-reference/src/jdk/internal/m3/indexstring/FrozenChars.java#L50) (L50) | [Contract](frozen-text-compatibility-reference.md#frozenchars-all-eleven-public-callables) |
| `FrozenChars.java` | [`public boolean isDirect()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/m3-reference/src/jdk/internal/m3/indexstring/FrozenChars.java#L51) (L51) | [Contract](frozen-text-compatibility-reference.md#frozenchars-all-eleven-public-callables) |
| `FrozenChars.java` | [`public int length()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/m3-reference/src/jdk/internal/m3/indexstring/FrozenChars.java#L52) (L52) | [Contract](frozen-text-compatibility-reference.md#frozenchars-all-eleven-public-callables) |
| `FrozenChars.java` | [`public char charAt(int index)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/m3-reference/src/jdk/internal/m3/indexstring/FrozenChars.java#L53) (L53) | [Contract](frozen-text-compatibility-reference.md#frozenchars-all-eleven-public-callables) |
| `FrozenChars.java` | [`public FrozenChars subSequence(int start, int end)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/m3-reference/src/jdk/internal/m3/indexstring/FrozenChars.java#L58) (L58) | [Contract](frozen-text-compatibility-reference.md#frozenchars-all-eleven-public-callables) |
| `FrozenChars.java` | [`public char[] copy()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/m3-reference/src/jdk/internal/m3/indexstring/FrozenChars.java#L65) (L65) | [Contract](frozen-text-compatibility-reference.md#frozenchars-all-eleven-public-callables) |
| `FrozenChars.java` | [`public void copyTo(int sourceStart, char[] target, int targetStart, int count)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/m3-reference/src/jdk/internal/m3/indexstring/FrozenChars.java#L71) (L71) | [Contract](frozen-text-compatibility-reference.md#frozenchars-all-eleven-public-callables) |
| `FrozenChars.java` | [`public CharBuffer asReadOnlyBuffer()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/m3-reference/src/jdk/internal/m3/indexstring/FrozenChars.java#L78) (L78) | [Contract](frozen-text-compatibility-reference.md#frozenchars-all-eleven-public-callables) |
| `FrozenChars.java` | [`public ByteBuffer directUtf16Bytes()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/m3-reference/src/jdk/internal/m3/indexstring/FrozenChars.java#L83) (L83) | [Contract](frozen-text-compatibility-reference.md#frozenchars-all-eleven-public-callables) |
| `FrozenChars.java` | [`public String toString()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/m3-reference/src/jdk/internal/m3/indexstring/FrozenChars.java#L96) (L96) | [Contract](frozen-text-compatibility-reference.md#frozenchars-all-eleven-public-callables) |
| `MIndexAddressedView.java` | [`public static MIndexAddressedView of(FrozenBytes owner, Kind kind, ByteOrder order)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/m3-reference/src/jdk/internal/m3/indexstring/MIndexAddressedView.java#L15) (L15) | [Contract](frozen-text-compatibility-reference.md#compile-only-addressed-seam) |
| `SynexiaMappedStorageTest.java` | [`public static void main(String[] args)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/m3-reference/src/jdk/internal/m3/indexstring/M3MappedStorageTest.java#L17) (L17) | [Contract](frozen-text-compatibility-reference.md#direct-memory-ownership-and-materialization) |

## Non-public, nested and generated-member closure

| Source | Remaining declared state/members and complete-body interpretation |
| --- | --- |
| `FrozenBytes.java` | Fields `heap`, `mapped`, `offset`, `length`, `retainedBytes`, `hash32` at L10–15; private assigning constructor L17–21; package-private `owned(byte[])` L29–32, `mapped(ByteBuffer,int,int)` L35–41 and both `hash` overloads L86–95. Admission/cursor/alias/hash trust is documented in [internal admission](frozen-text-compatibility-reference.md#internal-admission-and-hashing); no custom value equality, close or native declaration exists. |
| `FrozenChars.java` | Fields `data`, `utf16`, `offset`, `length`, `hash32` at L12–16; private assigning constructor L18–20; package-private ranged `copyOf(char[],int,int)` L30–36, `owned(char[])` L37–41, `mapped(FrozenBytes,int)` L43–48 and `hashUtf16(ByteBuffer)` L87–95. See [internal ranges](frozen-text-compatibility-reference.md#internal-ranges-and-direct-admission). `CharSequence` inherited defaults are not newly declared fixture implementations. |
| `MIndexAddressedView.java` | Public nested `Kind` at L11 has BYTE/CHAR/SHORT/INT/LONG. Private constructor L13. `of` is the single declared callable and always throws. Enum-generated `values`/`valueOf` and inherited enum methods are not additional explicit declarations. There are no working addressed operations behind the signature. |
| `SynexiaMappedStorageTest.java` | Static mutable counter `checks` L10 and private `check(boolean,String)` L12–15; helper increments then throws on false. No constructor is explicitly declared. Main ignores arguments, does not reset the counter and allocates direct memory; it is a test entry, not a storage factory or lifecycle API. |

## Actual test-to-contract map

| Read test / source lines | Assertions actually present | Explicit limits |
| --- | --- | --- |
| [SynexiaContractTest.main](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/M3ContractTest.java#L9-L26) | All 65,536 UTF-16 units in a three-unit sample; after source mutation, compares reference/product materialized content and full hash; compares every subrange's content | Does not independently assert original expected content; does not compare subrange hashes or prove API/owner equivalence. No byte-owner, ranged-copy overload, JNI or addressed coverage. |
| [SynexiaMappedStorageTest.main](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/m3-reference/src/jdk/internal/m3/indexstring/M3MappedStorageTest.java#L17-L77) | Six-unit allocateDirect sample; direct/read-only flags, full text/hash/units, every char subrange content/hash/directness, char copy independence, one offset copyTo, direct byte descriptor units/length, partial byte slice and heap compaction | No file mapping; retained writable alias is not mutated. No wrong-hash/admission/null/range refusal, full-size direct compact identity, addressed invocation, concurrency, unmapping or native execution. |

The `check` helper and comparison test explicitly throw `AssertionError`; the
harness also uses `-ea`. PASS/check-count strings are intended source outputs,
not new run results. The test names and messages do not enlarge their actual
assertion coverage. Neither an exhaustive single-unit loop nor all subranges of
one short sample establishes arbitrary text/encoding/concurrency equivalence.

## Pinned support evidence

These files were read completely for the bounded responsibility shown below.
They are support, not additional primary-source closure. The retained-port
comparison reads establish different physical definitions, not a fresh review
of the whole retained-port package.

| Public file | Evidence role | Git blob SHA-1 |
| --- | --- | --- |
| [m3/compatibility/m3-reference/PROVENANCE.md](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/m3-reference/PROVENANCE.md) | Public donor snapshot identity, test-only isolation, throwing seam, exclusions and refresh procedure | `400e30b64110faa62d872332093042549da8318d` |
| [m3/compatibility/m3-reference/NOTICE.md](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/m3-reference/NOTICE.md) | Applicable attribution and public lineage declaration; source of donor NOTICE identity, not that donor NOTICE itself | `f0f90fa78d87581bc8c227a7cb45bee60052e8d7` |
| [m3/compatibility/m3-reference/LICENSE-2.0.txt](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/m3-reference/LICENSE-2.0.txt) | Exact Apache-2.0 license bytes preserved; script checks its blob | `29f81d812f3e768fa89638d1f72920dbfd1413a8` |
| [m3/compatibility/check-m3.py](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/check-m3.py) | Two source/SPDX and license checks; release-21 compiler; separate reference classpath; two main invocations | `8a4e836c8411646131200a7e3f298f666330af42` |
| [m3/compatibility/M3ContractTest.java](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/M3ContractTest.java) | Actual differential content/range assertions and explicit API-equivalence disclaimer | `f864147e4e8f41ad990e05d5a2f0525f1fc3e05f` |
| [m3/build.sh](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/build.sh) | Core-only production source selection and separate tests/JAR output; fixture is not selected | `be764d854ce1d7686fd53da8953110a382ec1dec` |
| [.github/workflows/m3-foundation.yml](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/.github/workflows/m3-foundation.yml) | Declared foundation-build then compatibility step; path filters; separate mapping verification step | `573b248c82d8ea1f4d39fdc0c80ea88cf2a5605e` |
| [m3/ports/indexstring/src/main/java/jdk/internal/m3/indexstring/FrozenBytes.java](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/indexstring/src/main/java/jdk/internal/m3/indexstring/FrozenBytes.java) | Different retained-port body omits fixture addressed method | `f393350db66e0f75540a532b127df629641ad7ac` |
| [m3/ports/indexstring/src/main/java/jdk/internal/m3/indexstring/FrozenChars.java](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/indexstring/src/main/java/jdk/internal/m3/indexstring/FrozenChars.java) | Different retained-port body omits fixture package-private ranged copyOf overload | `e7cecf3e32eca949dcf6616455caab6c62fd909d` |

The public provenance reports donor revision
`64a2ea61c73b548413fed6686a9daeeb0b9b0564` for the two snapshots and license.
It reports original attribution-source NOTICE blob
`97bb5d49f8bd3bc573c19afe4ea2392a3d02704e`; the actual public fixture NOTICE is
`f0f90fa78d87581bc8c227a7cb45bee60052e8d7`, as listed above. Those are different
artifacts. No private donor source/NOTICE was fetched for this documentation.
`check-synexia.py` pins the two Java snapshots and license only; it does not
check the shim, mapped test or NOTICE body. Documentation hashes of those files
are review evidence, not new executable gates.

### Raw-byte SHA-256 ledger

SHA-256 below identifies the exact public bytes read, including the license's
line endings. Git blob SHA-1 above includes Git's blob header. Neither digest
establishes semantic correctness, licensing beyond its recorded scope, successful
execution or target receiving acceptance.

| File | Bytes / lines | SHA-256 |
| --- | ---: | --- |
| `m3/compatibility/m3-reference/src/jdk/internal/m3/indexstring/FrozenBytes.java` | 4288 / 96 | `a4c419c1d539189f004cdb72f009087dfee519cb39e5ee7bfc7faa043bf230f0` |
| `m3/compatibility/m3-reference/src/jdk/internal/m3/indexstring/FrozenChars.java` | 4424 / 99 | `5fe62df7d3f0d48f45326dbb17ce8ce8bce160aa9ffeafec5ec66c572a10deda` |
| `m3/compatibility/m3-reference/src/jdk/internal/m3/indexstring/MIndexAddressedView.java` | 573 / 18 | `36223e6e51474b7a8dbc5dacfa9a0c7563ad75905776280ac908a4c2445ae853` |
| `m3/compatibility/m3-reference/src/jdk/internal/m3/indexstring/M3MappedStorageTest.java` | 3847 / 78 | `56331cb98c2f539c2e17805a03d9a477542cba6fb7df1aa20e3f05ff24d0cf72` |
| `m3/compatibility/m3-reference/PROVENANCE.md` | 1700 / 29 | `7f5683751c72db91dac37ae83cb97e02882ad773040a88d47e9ad466ba303d55` |
| `m3/compatibility/m3-reference/NOTICE.md` | 809 / 11 | `da9f431faf05760ea4afa259f79917012c1e3c5c8f3a7a49fd2ad2a2d513eee1` |
| `m3/compatibility/m3-reference/LICENSE-2.0.txt` | 11558 / 201 | `1eb85fc97224598dad1852b5d6483bbcf0aa8608790dcc657a5a2a761ae9c8c6` |
| `m3/compatibility/check-m3.py` | 2107 / 33 | `78c646c4e5d1d16fc152cee917b892b27bd995eaae990007512b67fbb383c611` |
| `m3/compatibility/M3ContractTest.java` | 1340 / 27 | `b18f011344c6c0a392dc27e0557cb95f4366c62d726721e4620d560385fcaf66` |
| `m3/build.sh` | 5412 / 55 | `c7b954a6faa16cc8a0cb4a2d13a8032c43fb308a739b225362b6ac3fd84864ee` |
| `.github/workflows/m3-foundation.yml` | 3965 / 85 | `e3625a7f61ece03df4ac90102e45a299ed3f571ca889295da4899fcd8759d5ad` |
| `m3/ports/indexstring/src/main/java/jdk/internal/m3/indexstring/FrozenBytes.java` | 4037 / 91 | `db46ea5bd16183568d71552375f9ee61c29966902a14f5b15b70d8dd825fb17d` |
| `m3/ports/indexstring/src/main/java/jdk/internal/m3/indexstring/FrozenChars.java` | 3868 / 86 | `88a85b35a7c5c6d84b23fa734254c11e4492ecf87ed0c93b6f730ddbec7d15ba` |

## Reconciliation and remaining scope

The earlier [package-4 sample](m3-packages.md#package-4) and
[physical package inventory](packages/m3-01.md) retain their historical
source-sampled context. This continuation supplies complete four-body/member
routing and maps both the package-local test and external harness. It clarifies
that the fixture contains no joined-view implementation and that its addressed
seam always throws. Absence of package Javadoc remains explicit.

The [fixture reference](frozen-text-compatibility-reference.md#coverage-limits-and-continuation)
records remaining error, alias, resource, concurrency and execution cases.
The [retained-port guide](indexed-string-reference.md),
[internal mapped-storage guide](mapped-string-storage-reference.md) and
[bootstrap ownership guide](bootstrap-string-ownership.md) keep their separate
owners, pins and qualification scopes. This page changes none of their counts,
no donor/custody inventory, no public JDK contract and no receiving status.
Hosted CI, if later observed for a documentation commit, is separate exact-commit
evidence and does not retroactively turn this read-only review into an executed
fixture or runtime test pass.
