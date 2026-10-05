# Pure-int atom documentation contract

Status: binding regression contract for the migration-recipe layer.

## Problem

The existing documenter appends an M3 Javadoc comment after any existing user Javadoc. Java associates
only the closest Javadoc comment with the declaration, so the generated M3 marker can silently replace
the user's effective compiler/Javadoc contract even when the original text remains in the file.

Compiler-visible documentation is part of the M3 contract envelope and cannot drift during a
FILE-local behavior-preserving transformation.

## Rule

For an admitted atomized pure-int method:

1. if no declaration Javadoc exists, retain the current generated M3 Javadoc behavior;
2. if user Javadoc already exists, preserve that Javadoc as the comment closest to the method;
3. place M3 semantic metadata immediately before the existing Javadoc as a non-Javadoc block comment;
4. repeated execution must make no further change;
5. the compiler's `DocTrees` view of the user documentation must remain unchanged.

This is a tooling-layer repair only. It changes no OpenJDK product source, public API, JNI ABI, or
coverage threshold.

## Executed verification

On the PR129 proof-kit toolchain plus the PR132 scope/atom closure:

- 42 focused JUnit tests passed;
- 0 failures, 0 errors, 0 skips;
- line coverage: 379/379 = 100%;
- branch coverage: 268/269 = 99.6283%;
- JaCoCo: `All coverage checks have been met.`

The compiler-backed regression uses Java 21 `DocTrees` to verify declaration-document association.
