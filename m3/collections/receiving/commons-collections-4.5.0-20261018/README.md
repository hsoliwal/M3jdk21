<!-- SPDX-License-Identifier: Apache-2.0 -->
# Commons Collections source in the M3 namespace

Copyright 2026 Hitesh Soliwal and contributors. Receiving metadata is Apache-2.0; donor rights remain unchanged.

[The implementation directory](../../implementations/commons-collections-4.5.0/) contains all 904 actual files from the pinned 4.5.0 RC preparation revision: 359 main Java sources, 300 test sources, resources/build metadata and 182 serialized fixtures. FULL_SOURCE_BINDINGS.tsv records each original path/mode/blob/raw SHA-256 and its exact postimage. Original LICENSE/NOTICE and source headers remain.

The namespace is com.m3.collections.apache. All 359 main sources compile with javac --release8 on JDK21 into594 classfiles. Typed serialization recipes change578 owned name slots and preserve stored UIDs, opaque bytes and handle roles. A native proxy-format witness qualifies the observer correction; it does not validate all fixtures. Four missing test/helper classes leave the full182-fixture Java runtime test open.

The sole POM defaults to skipping install/deploy under original donor coordinates; defaults can be overridden. Full donor tests, a target-owned build, array/index/offset/mask conversion, stored-entry elimination and JNI remain open. The source keeps donor algorithms and sits outside the M3 reactor; receiving is not advanced. SOURCE_QUALIFICATION.json preserves review findings and their adjudication instead of claiming a clean model exit.
