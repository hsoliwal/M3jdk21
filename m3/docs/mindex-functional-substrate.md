# JDK-internal MIndex functional substrate

## Scope

This packet adds an opt-in implementation substrate under
`jdk.internal.mindex.function`. It does not modify public
`java.util.function`, `java.util.stream`, Optional, Collector, collection,
Spliterator or compiler contracts.

## Implemented constructs

- immutable int/long/double unary plans implementing JDK primitive functional interfaces;
- postfix primitive predicates implementing IntPredicate, LongPredicate and DoublePredicate;
- plan-to-plan compose/andThen/and/or/negate fusion;
- ordinary JDK lambda/method-reference fallback when one side is not an MIndex plan;
- immutable fused int/long/double map/filter/skip/limit pipelines;
- caller-reusable execution workspaces;
- count, sum, reduce, match, forEach and find-first terminals;
- encoded allocation-free int find-first plus caller-owned long/double result lanes;
- primitive range, iterate, scan and fold constructs;
- compensated sequential double summation.

## Recipe ownership

`M3MIndexFunctionalKernelRecipe` is the MODULE join. Its child is the existing
`M3HashPinnedJavaSnapshotRecipe` with crate `mindex-functional-kernel`.

The crate owns 13 independent Java FILE atoms, each admitted only as
`ABSENT -> exact SHA-256 reviewed postimage`. A second replay over the reviewed
postimage is a zero-change fixed point.

The composite recipe class, JUnit, scope registration and catalogue row are bootstrap/control
atoms and are intentionally not self-generated.

## JDK integration boundary

The first admission keeps Java SE source untouched. Later proof-gated recipes may target:

- java.util.function composition;
- primitive Stream pipeline and terminal lowering;
- OptionalInt/OptionalLong/OptionalDouble result lowering;
- PrimitiveIterator and Spliterator traversal;
- comparator/key extraction and collection searches;
- javac lambda attribution/lowering;
- invokedynamic/LambdaMetafactory recognition;
- VM/JIT intrinsics only after semantic and performance proof.

Unsupported, side-effecting or unproven constructs remain ordinary JDK code.
