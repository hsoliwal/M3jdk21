# M3JDK stage 3 — String implemented through MIndexString

Stage 3 keeps the public `java.lang.String` contract and makes `MIndexString` the experimental
canonical storage engine behind it.

## Absolute invariant

When `-XX:+UseM3StringStorage` is enabled and java.base bootstrap has completed:

```text
String
  -> canonical MIndexString scalar/tuple
       -> OS-mapped lexicon atom, when present
       -> VM-local weakly interned atom, for misses
       -> immutable atom/range tuple for concat/slice/repeat
```

Java primitive arrays do not resize. A contiguous Compact-String `byte[]` is a compatibility
projection, not the canonical joined representation.

The OS lexicon path is supplied with `-Djdk.mindex.lexicon=PATH`. The format is the existing
M3LEX001 read-only UTF-16LE image. The image is validated and mapped read-only after module
bootstrap. Absence/corruption fails closed to the VM-local interner.

JNI is **not required** for this model. Java reads mapped atoms directly and HotSpot reads the same
mapped address/atom tuple for VM/JNI compatibility paths. JNI remains a projection boundary when a
JNI API requires contiguous UTF-16 or modified UTF-8.

## VM-local misses

Content not found in the lexicon is admitted into a weak VM-local interner. Equal live scalar
content shares the exact canonical Compact-String byte array. Equal live tuple geometry shares one
MIndexString tuple body.

## Bootstrap exception

Strings created before MIndex activation retain the stock flat bootstrap representation. After
activation, structural operations lazily attach those values to canonical MIndex storage. Existing
final `String.value` arrays cannot be retroactively removed without violating the VM's Stable/final
contract; post-activation trusted construction and all joined/mapped values use canonical storage
directly. Constructor-wide source migration is a separate mechanical pass.

## Focused proof

Set `M3_TEST_JDK` to a built stage-3 image and run:

```bash
./m3/runtime-stage3/run-focused.sh
```

The test runs twice without loading any custom JNI library:

1. with a mapped lexicon fixture, proving lexicon hits use nonzero mapped addresses and no local
   byte array;
2. with a missing lexicon, proving identical misses converge on one VM-local canonical byte array.

Both lanes prove canonical joined tuple identity, slice reuse, precomputed hash behavior and normal
String projections.
