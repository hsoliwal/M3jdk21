<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Borrowed source and notices

This SDK wraps existing Synexia source-sealed OpenRewrite owners. The following
files are imported verbatim and keep their original SPDX notices:

- M3GpuToolJoinRecipe.java, Git blob 1c1b1b568d369f688f6953161636bf36b484bf5a.
- M3GpuToolJoinManifest.java, Git blob 38df5f97bbeec8a8a8cfc77091d56bda925c07f2.
- pom.xml, Git blob d79867bcc09ad6329e12418e15907e54b8ab025f.

They are Apache-2.0 Synexia files, reused from the previously qualified guard SDKs
d183c3c0210eeba355182f4c0e13b1357eae19c3 and
be298fee59b8ed7fe4b0cce5e4f17f5f9e0f1f81. Maven coordinates and plugin versions
are unchanged. The thin adapter, explicit named YAML activation and custody tests
adapt that existing mechanism; no new rewrite engine or native harness is added.

Five authentic hsoliwal/M3jdk21 sources from immutable receiver commit
adc10e96af417655a7f1fc61b07c0ca8789c83da are retained byte-for-byte as before/out
KEEP fixtures. Their existing Apache-2.0, copyright and adaptation notices remain
inside every complete source body. They cover M3StateRelation.java,
M3DotStarRelations.java, M3StateRelation.c, m3_state_relation.h and the existing
M3RelationPortProof.java. No kernel, proof or runtime interface is rewritten.

The historical C audit fixture, Git blob
5b30f71d8ed878b3199225b9512b75a090ff896c, also retains its complete original
notices. It is a non-target refusal fixture. Prior execution of that historical
translation unit does not qualify the different current C owner.

The counter/XML verifier mechanics are adapted from the existing lexical SDK
verifier f16bcaa268a03e9d397329e4977ee6946796fcef and test
215959642d18a069886e493c7ac7623793e5b453. The named KEEP scheduling and refusal
mechanics are also derived from M3TextMetadataCurrentOwnerRecipeTest
977527278e3761cb31365dd75b8f5883c177463a. These Synexia files are Apache-2.0.

License text: https://www.apache.org/licenses/LICENSE-2.0
