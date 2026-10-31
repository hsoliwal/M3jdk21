# Original destination receiving fixture contract

This is bounded candidate tooling development against acquired destination `hsoliwal/M3jdk21@da958d00d24154c0db87beca0ec80a7df2b43b73`. It copies no Synexia production source or mastery implementation. Source export, whole-repository delivery, JNI/VM/JDK admission, and the owning migration-recipes module's 99% line/branch coverage gate remain separate and unexecuted by this fixture. No scoped coverage threshold replaces that gate.

## Epoch 2 freeze chronology

Epoch 1 remains unchanged at the parent directory: its exact test/POM, staging and verification receipts, log, and expanded prose contract are retained. Independent review found that comparing a descending map with `Map.of` iteration did not ensure distinct input enumeration orders. Epoch 1 therefore did not establish that finite enumeration obligation, despite its four passing tests.

This epoch changes only the enumeration check: two explicit `LinkedHashMap` inputs are constructed from the same frozen project, one sorted ascending and one descending. Their key lists must equal the exact orders below and must differ before either recipe runs. Fresh canonical composites are applied to both maps; both outputs are compiled, observed against the frozen oracle, checked for source/member preservation, and compared to each other and the independently established canonical result.

The revised test, unchanged POM, all six unchanged owners, this revised contract, and readable `revision.diff` are bound in this epoch's `STAGING_RECEIPT.json` before epoch 2 Maven starts. No epoch 1 evidence is overwritten or relabeled. All other corpus, owner and oracle expectations are unchanged.

## Corpus and exact expectations

The original project consists of two Java 21 files generated entirely by the test's `project()` method. That exact method is the byte-level corpus authority; this document describes its existing expectations.

`receiving/Arithmetic.java` contains a public observation method returning four values in this order, with four private static scalar-int leaves:

| Leaf | Initial body | Independent numeric oracle |
|---|---|---|
| `sum(int a, int b)` | `return a + b;` | Truncate the long sum to int. |
| `rotate(int a, int b)` | `return (a << b) \| (a >>> -b);` | `Integer.rotateLeft(a, b)`. |
| `mix(int a, int b)` | `return (~a * 17) + (b - 3);` | Truncate `(~(long) a) * 17L + b - 3L` to int. |
| `seeded(int a, int b)` | `int m3$pureIntAtom = a - b; return m3$pureIntAtom;` | Truncate the long difference to int. |

The fourth leaf is deliberately already atomized and lacks a pattern marker. The other three are raw eligible return expressions. Input pairs are the Cartesian product of `Integer.MIN_VALUE, -33, -1, 0, 1, 31, 32, 33, Integer.MAX_VALUE`, plus 32 `java.util.Random` pairs using seed `0x52454345495645L`: 113 pairs in total. The original is compiled once as a frozen differential oracle and is itself checked against the independent arithmetic model.

`receiving/Observer.java` is entirely outside the admitted private-static-int expression rewrite domain. Its source must remain byte-for-byte identical after each tested stage. The public `strings(int choice)` returns:

1. Java `null` for choice zero, otherwise `"value"`;
2. the distinct literal string `"null"`;
3. the code-looking string `"private static int fake(int a, int b) { return a + b; }"`;
4. the marker-looking string `"M3-IOP: PURE_INT_EXPRESSION /* return x+y; */"`;
5. an empty string;
6. `"A😀Z"`, which has four UTF-16 code units and three code points.

String arrays are compared without stringification at choices `0, 1, -1`, preserving null/string-null distinctions and the exact opaque payload. A separate public `next()` increments a private static counter. Two independently compiled projects must each start with their own counter state; calls yield `1, 2` in the first loader and `1` in the second.

## Retained owner schedules

Every step uses a fresh recipe factory product, parser, and execution context. Inputs are parsed with the existing OpenRewrite Java 21 parser; types and printed input text are checked. Actual recipe results may replace existing files only, preserving their paths. Every intermediate, including every no-change replay step, is compiled by unchanged destination `MemJava`, which runs javac with Java 21 release, annotation processing disabled, lint/warnings-as-errors, and isolated bytecode/class loaders.

| Schedule | Retained recipe sequence | Expected final shape |
|---|---|---|
| A | `M3AtomizePureIntReturnRecipe` | Four atom locals; three markers; pre-existing atom stays unmarked. |
| P | `M3PatternizePureIntAtomRecipe` | One atom local and one marker; raw leaves remain raw. |
| AP | Atomize, then Patternize | Four atom locals with four markers. |
| PA | Patternize, then Atomize | Exactly the same source map as AP. |

Each schedule must change at least one admitted leaf on its first sweep. Every individual step of a fresh second sweep must be unchanged. A and P must remain distinct, and A must differ from AP; their semantics remain equal to the original. No claim equates all four source shapes.

The actual `M3PureIntConvergenceRecipe` is also tested. Its declared children must be Atomize, Patternize, Document in that order. Each child intermediate is independently compiled and observed. Its final source map must equal direct execution of the retained composite, contain four documentation markers, remain unchanged under fresh composite replay, and be independent of two explicitly asserted project enumeration orders: ascending `[receiving/Arithmetic.java, receiving/Observer.java]` and descending `[receiving/Observer.java, receiving/Arithmetic.java]`. A fresh composite executes for each order, and each output is separately compiled and checked before final-source equality is asserted.

All compiled intermediate projects preserve declared method, field, and constructor surfaces. Source-file membership and opaque observer source remain exact. No new compiler, eligibility oracle, AST scanner, registry, scheduler, or product dependency is introduced.

## Negative controls and limits

- Malformed Java must be refused by the actual `MemJava` compiler with its existing compile-refusal diagnostic.
- Changing the valid `sum` source from addition to subtraction must be caught by the arithmetic oracle.
- Changing the valid opaque string `"null"` to `"changed"` is separately compiled successfully and must then be caught by the string observer, rather than being counted as a compiler rejection.

These four JUnit methods establish only this finite two-file fixture's behavior on the exact retained destination owners. They do not prove arbitrary expression equivalence, regex optimization, donor import, source-export approval, native/JNI behavior, complete module coverage, a built JDK, performance, or destination promotion.
