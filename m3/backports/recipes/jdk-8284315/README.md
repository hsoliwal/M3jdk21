# JDK-8284315 — DocTrees element/type consistency

Status: **candidate-adapted, explicit LIBRARY_API bug-contract correction**.

Donor evidence is reviewed OpenJDK PR **#29547**, head:

`03e1198a5ae98cbbf94632160c9f4315a8c51cb5`

The donor PR was reviewed upstream and carried CSR JDK-8376955, but it was closed without merge.
M3JDK21 therefore treats this as a reviewed bug-fix donor candidate, **not** as a released-GA
backport obligation.

## Bug contract

Java 21's current implementation can return javac-internal elements for primitive/array doc
references. The candidate corrects the public DocTrees behavior:

- primitive and array references may have a non-null `TypeMirror`;
- they do not have a corresponding language-model `Element`;
- declared types and members retain their element/type behavior;
- explicit-module references are attributed and checked against the requested module;
- javadoc/doclint stop relying on the old primitive/array-symbol workaround.

This is intentionally observable API behavior. It is **not** admitted under the ordinary
contract-preserving FILE rule. The terminal packet requires explicit `LIBRARY_API` authority.

## Java 21 adaptation

The donor patch was written against newer javac internals. M3JDK21 retains the Java 21 helper
signatures:

- `findMethod(sym, memberName, paramTypes, boolean)`;
- `findField(sym, memberName)`.

The candidate also guards module-qualified type-only references before calling
`JavacElements.getModuleOf`, because Java 21 may have no language-model element for such a type.

## Recipe atoms

Eight independent file atoms are hash-pinned:

1. DocTrees API documentation/contract;
2. JavacTrees resolver behavior;
3. focused DocTrees jtreg;
4. javadoc CommentHelper consumer;
5. doclint Checker consumer;
6. javadoc TestSeeTag expectation;
7. doclint ReferenceTest source;
8. doclint ReferenceTest.out golden text.

`M3Jdk8284315BackportRecipe` owns the five semantic/focused-test Java atoms.
`M3Jdk8284315TestExpectationsRecipe` owns the three expectation atoms.
`M3Jdk8284315FullBackportRecipe` is the deterministic family entry point.

## Scope DAG

`packet.tsv` keeps every source edit at FILE scope, joins compiler and javadoc evidence at MODULE
scope, then requires one explicit LIBRARY_API join because the public DocTrees behavioral contract
is deliberately corrected.

Camel, Airflow and Drools projections may schedule the declared dependencies, but they have no
independent mutation or promotion authority.

## Required proof

Promotion remains forbidden until all of these are green:

1. both recipe JUnit suites;
2. packet DAG validation and identical Camel/Airflow/Drools semantic root;
3. OpenJDK Java 21 configure/build image;
4. focused jtreg:
   - `DocTreesElementTypeConsistencyTest.java`;
   - existing doctree `ReferenceTest.java`;
   - doclint `ReferenceTest.java`;
   - javadoc `TestSeeTag.java`;
5. diff/lint/static checks;
6. exact second-pass recipe fixed point;
7. explicit review of the LIBRARY_API behavior correction.

No performance claim is made.
