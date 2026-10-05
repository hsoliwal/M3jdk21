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

## Verification

The focused regression must:

- execute the real `M3DocumentPureIntAtomRecipe`;
- verify rendered comment ordering;
- use Java 21 `DocTrees` to prove the existing user summary/tags remain the declaration Javadoc;
- verify an undocumented atom still receives generated M3 Javadoc;
- prove second-pass fixed point;
- run under the existing 99% line/branch JaCoCo gate without exclusions.
