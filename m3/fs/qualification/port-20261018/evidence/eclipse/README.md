<!-- Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project; SPDX-License-Identifier: Apache-2.0 -->
# Eclipse acceptance against the full reconstructed backend

These compact receipts record **FULL_BACKEND_STAGING_ACCEPTANCE**, executed on
2026-10-19. They qualify the frozen Eclipse provider against the freshly built
M3JDK21 backend before canonical recipe application to the receiver checkout.
[The unified qualification record](../../QUALIFICATION.md) distinguishes this
stage from receiver reproduction and binds the remaining port evidence.

Each mode ran **14 native-backed provider tests** and **one real Equinox test
containing 15 named Eclipse Resources workspace checks**, with zero failures,
errors, or skips. The workspace checks are not 15 additional JUnit test cases.
The test logs retain their names and the actual test JVM home.

| Files | Evidence |
| --- | --- |
| normal-inputs.json / normal-result.json | Exact source and runtime input/output hashes for the backend installed as an OSGi bundle. |
| linked-inputs.json / linked-result.json | Exact linked-image inputs, cleared loader/Java environment policy, absence of the native-path override, and module/class-origin assertions. |
| normal-tests.txt / linked-tests.txt | Compact summaries derived from the executed reports, actual workspace output, and hashes of the full source reports. |

The provider and scenario JARs were byte-identical across modes. Linked tests
ran in the packaged image, imported `com.m3.fs` through the Equinox system
bundle, omitted the backend from installed OSGi bundles, and asserted
`jdk.m3fs` ownership plus a `jrt:/jdk.m3fs` class origin.

The JSON receipts preserve their staging paths and timestamps. The
`original_receipt_sha256` field identifies the uncurated input/result receipt;
the test summaries identify the original XML and Maven logs. The full executed
receipts remain in the task checkpoint. This directory contains no raw JVM
system-property dump, binaries, dependency cache, or Maven settings.

These are focused headless provider/Resources contracts. They do not represent
the entire Eclipse fork's Tycho suite, GUI or plugin coverage, platform mount
testing, a complete modified-JDK build, or performance/GC acceptance. See
[CAPABILITIES.md](../../CAPABILITIES.md) for those boundaries.

