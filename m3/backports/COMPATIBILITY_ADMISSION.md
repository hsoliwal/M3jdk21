# M3JDK21 complete compatible-backport law

Status: execution policy for the M3JDK21 downstream programme.

## Total donor universe

M3JDK21 does not curate only headline features.

For every released OpenJDK GA interval after JDK 21 and through the latest released Java version,
the donor universe is the complete upstream change history:

- every delivered JEP and its implementation commits;
- every non-JEP JBS fix and enhancement;
- javac/compiler fixes;
- HotSpot, GC, runtime and serviceability changes;
- core-library and security changes;
- JFR/JVMTI/JNI/native changes;
- tooling changes, including javac, javadoc, jlink, jpackage, jcmd, jdeps, keytool,
  launchers and build/test tooling;
- follow-up fixes to an earlier feature or backport;
- tests and build-system changes needed to prove the imported behavior.

The current released ceiling is JDK 27 GA. JDK 28 is not part of the released denominator until it
becomes GA. The pinned released intervals are therefore JDK 21 GA through JDK 27 GA.

## Backport obligation

Every upstream change enters the inventory.

The default state is not "ignore". The default state is:

```text
PENDING_COMPATIBILITY_PROOF
```

A change becomes a required M3JDK21 backport when it can be adapted while preserving the locked
Java 21 contract.

```text
upstream change
    -> inventory
    -> dependency closure
    -> Java 21 compatibility proof
    -> source-bound patch/OpenRewrite recipe
    -> diff
    -> lint
    -> compile as Java 21
    -> tests
    -> runtime/benchmark when applicable
    -> serial promotion
```

## Java 21 compatibility gate

A backport is compatible only when the resulting M3JDK21 build preserves the required Java 21
external contract. The gate includes, as applicable:

- Java 21 source grammar and source-version behavior;
- Java 21 javac type/attribution behavior except for bug fixes that restore or tighten conformance
  without introducing a later language feature;
- Java 21 class-file compatibility and accepted class-file versions;
- existing public/protected API and binary linkage unless an additive API is intentionally admitted;
- JVM/JNI/JVMTI/serialization/reflection behavior relied on by Java 21 programs;
- platform/runtime behavior that must remain available in JDK 21;
- buildability and tests under the pinned Java 21 toolchain.

A path name is only a review signal. Touching `javac`, the parser, attribution, HotSpot or a
compatibility-sensitive subsystem does not itself prove incompatibility.

## Exclusion rule

A change is excluded only when evidence shows that the change fundamentally requires a post-21
contract that M3JDK21 has chosen to keep locked, for example:

- new Java source syntax or language semantics that cannot be expressed as Java 21;
- a class-file format/version requirement that would make the runtime cease to be Java-21 compatible;
- deliberate removal of a Java 21 API, port, mode or behavior;
- a restriction whose purpose is to reject behavior Java 21 is required to continue accepting;
- a preview/incubator API whose contract is not stable enough to freeze into M3JDK21.

Where a larger upstream change contains both compatible and incompatible parts, split the patch.
The compatible leaf remains a backport candidate.

## javac rule

Do not reject or hold a fix merely because it touches `src/jdk.compiler`.

Compiler work is classified into two different questions:

1. Does this patch require a post-21 language/specification feature?
2. If not, can the compiler fix/tooling improvement compile and pass its tests under the Java 21
   contract?

A compatible javac bug fix, diagnostic improvement, performance improvement, documentation parser
fix, annotation-processing fix or tooling repair is in scope.

## Mechanical implementation

For recurring or structurally repeated Java-source adaptation, use a tested OpenRewrite recipe.
The recipe or recipe DAG must be proved by JUnit on Java 21 fixtures before broad application.

A one-off exact upstream patch may remain the canonical replay unit only when it is hash-pinned,
source-bound and independently verified. Repeated adaptations must be promoted into reusable
OpenRewrite substrate.

## Scope and promotion

Backport adaptation follows the M3 scope ladder:

```text
FILE -> VISIBILITY -> PACKAGE -> MODULE -> MULTI_MODULE -> LIBRARY_API
```

Use the narrowest scope that actually contains the change. Independent FILE candidates may execute
in parallel. Canonical promotion remains serial.

## Completion condition

The programme is not complete because all JEP rows were catalogued.

Completion requires:

1. the complete released upstream commit denominator is inventoried;
2. every commit has a compatibility decision or is explicitly pending proof;
3. every compatible JEP/fix/tooling change has a backport implementation or is already present by
   equivalence;
4. every accepted backport has provenance and verification evidence;
5. the full M3JDK21 build/test gates are green;
6. rerunning the inventory and admission process yields no unclassified compatible residue.
