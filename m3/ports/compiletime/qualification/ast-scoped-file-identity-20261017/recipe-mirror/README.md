<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Exact Synexia recipe proof mirror — NOT a canonical recipe owner

These files are **byte-identical** to the JDK21 variant of
[hsoliwal/com.synexia#12081](https://github.com/hsoliwal/com.synexia/pull/12081),
canonical source/recipe commit
`cc9f7956c91eb6a4b95696bfaafe3e1a74ed68ef`.
The original Git blobs for both preimages, the OpenRewrite descriptor,
the target manifest, the two postimages and the JUnit proof are enumerated in
`SOURCE_BLOBS.tsv`. This exact copy exists only to make the public
M3JDK21 PR reviewable without a private Synexia checkout. It is
not an independent M3JDK21 transformation engine, and it is not
on any product compilation or runtime classpath.

`verify_mirror.py` confirms the two source preimage SHA-256 digests,
manifest coverage/order and the exact Java postimage bytes in the
compiled product location. Run the **canonical** Maven/OpenRewrite SDK JUnit
suite in Synexia for real recipe fixed-point and drift-refusal proof:
`mvn -B -ntp -f synexia-openrewrite-recipes/pom.xml -Dtest=M3ASTScopedFileIdentity20261017RecipeTest test`.

OpenJDK/third-party rights and original first-party Apache-2.0 attribution
remain unchanged. No JNI code, binary, classpath dependency or JDK interface
is introduced by this proof mirror.
