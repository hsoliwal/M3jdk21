# A3 native/JNI file atoms

Status: candidate-only M3JDK21 absorption tooling.

## Purpose

A3 already inventories native OpenJDK files, but the first A3 application lane is Java-only because
OpenRewrite Java/LST is the truthful parser for Java source.

Native/HotSpot/JNI files require a different preparation rule:

1. classify them explicitly as native source;
2. keep each selected file as an exact content-addressed FILE atom;
3. replay the exact reviewed postimage with the existing hash-pinned PlainText recipe;
4. do not claim that PlainText replay understands C/C++ semantics;
5. require the native OpenJDK build, jtreg/runtime and JNI/HotSpot-specific proof before promotion.

This is the native counterpart of A3's Java FILE preparation, not a replacement C++ parser.

## Native source classification

The file-delta inventory recognizes these case-insensitive suffixes:

```text
.c .cc .cpp .cxx .h .hh .hpp .s .asm
```

This includes ordinary C/C++, headers and assembly used by HotSpot/JNI/platform code.

Changed native files receive explicit recipe lanes:

```text
SOURCE_SEALED_ADD_NATIVE
SOURCE_SEALED_NATIVE_PAIR
REVIEW_REMOVAL
```

The inventory TSV appends `native_source` after the existing columns so existing column positions
remain stable.

## Recipe generation

Use the precise native lane:

```bash
python3 m3/backports/generate_recipe_crates.py \
  --repo . \
  --release 27 \
  --paths-file target/selected-native-paths.txt \
  --include-native \
  --crate-size 1 \
  --out target/a3-native-crates
```

Every one-file native crate is emitted under the existing
`M3Jdk21HashPinnedTextSnapshotRecipe` resource root and has a deterministic name such as:

```text
jdk27-native-0001
jdk27-native-0002
```

The exact JDK21 preimage SHA-256 and donor postimage SHA-256 are recorded in the manifest.

## Backward compatibility

The historical `--include-text` option remains a broad non-Java opt-in. Native files selected only
through that legacy flag retain the historical generic `TEXT` candidate kind.

The new `--include-native` flag is what opts into first-class `NATIVE` candidate identity and
native-qualified crate names.

Java-only runs preserve their previous unqualified crate naming.

## Scope and semantics

A source-sealed native FILE atom proves only exact replay from an exact preimage.

It does not prove:

- C/C++ AST equivalence;
- ABI compatibility;
- JNI lifetime safety;
- HotSpot safepoint/GC correctness;
- compiler intrinsic correctness;
- platform portability;
- build success;
- performance improvement.

Those remain separate gates.

When a native change spans multiple coupled files, the one-file crates may be parallel preparation
leaves, but canonical admission must join them at the honest PACKAGE/MODULE/MULTI_MODULE scope.

## Future deeper atomization

Function-level native atomization should use a truthful C/C++ parser (for example clang tooling) and
remain separate from the byte-custody recipe. Until that parser is admitted and tested, native A3
uses exact source-sealed FILE atoms rather than regex-based pseudo-AST mutation.

## M3 pipeline

```text
A3Inv
  -> native_source classification
  -> donor compatibility review
  -> --include-native --crate-size 1
  -> source-sealed native FILE recipe atoms
  -> explicit scope DAG join
  -> configure
  -> make
  -> focused jtreg / native tests
  -> java -Xcheck:jni where applicable
  -> runtime/HotSpot proof
  -> benchmark only when performance is claimed
  -> serial promotion
```

OpenJDK remains the implementation/contract donor for JDK product changes. Challenge sites such as
LeetCode, HackerRank and GeeksforGeeks may contribute algorithm taxonomy, edge cases and benchmark
ideas, but their solution bodies are not copied into JDK native source.
