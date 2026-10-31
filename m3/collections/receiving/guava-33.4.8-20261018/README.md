<!-- SPDX-License-Identifier: Apache-2.0 -->
# Guava source in the M3 namespace

Copyright 2026 Hitesh Soliwal and contributors. Receiving metadata is Apache-2.0; donor rights remain unchanged.

[The implementation directory](../../implementations/guava-33.4.8/) contains all 3,356 actual donor files, including JRE, Android, GWT, tests, resources and build metadata. FULL_SOURCE_BINDINGS.tsv maps every original path/mode/blob/raw SHA-256 to its postimage. This is a complete source namespace pass over the pinned revision; file counts do not count implemented contracts.

The owned common and publicsuffix packages are renamed. External Truth/Jimfs imports retain their provider namespaces. The complete JRE profile compiles on the donor Java 8/9 baselines using JDK 21; 166 finite source-aware checks cover helper/classpath coexistence. All 14 POMs default to skipping install/deploy under the original coordinates, and both OSGi rules exclude the internal suffix helpers. Those defaults are explicitly overridable.

The source retains donor storage algorithms. Complete Maven, Android/GWT and external-provider linkage, runtime contracts, per-file array/index/offset/mask storage conversion, stored-entry elimination and JNI remain open. This source is outside the M3 reactor and does not advance JDK receiving. See TARGET_DISPOSITION.json and SOURCE_QUALIFICATION.json.
