<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Dependency notices and source custody

TornadoVM source is received unchanged from 87a465f9185eb8e822f9532076fb4a5bc44fdf2a.
Its three original license files are retained here and in the source tree. The
receiver provider has separate Apache-2.0 LICENSE and NOTICE files.

Graal compiler 23.1.0 is GPLv2 with the Classpath Exception. The original compiler
JAR is the sealed input to Tornado's existing relocation build; the resulting
`tornado.graal` module is a distinct recorded build output. Graal collections,
word and truffle-compiler 23.1.0 use UPL 1.0. Original source copyright lines and
UPL text are retained. ASM 9.7 uses BSD-3-Clause; the exact source header notices
are retained, including their attribution. Commons Math 3.2 retains its original
Apache LICENSE and NOTICE. JUnit 4.13.2 is test support, not an installed runtime
module; its declared EPL 1.0 is recorded in the dependency lock.

The adjacent dependency-source-custody.json pins source archive URLs/hashes and
exact POM hashes. Raw source archives are available at those versioned Maven
Central URLs; the corresponding Graal release is oracle/graal vm-23.1.0. These
records distinguish prebuilt compiler support inputs from source-built Tornado
modules and do not assert that every Maven build tool was rebuilt from source.

The exact source reactor resolves Commons Math 3.2 through JMH. Its version is
sealed from that observed dependency graph; the earlier 3.6.1 assumption is not
an admitted dependency upgrade. ASM's original notices use line-comment headers;
`ASM_9_7_SOURCE_NOTICES.txt` preserves both unique headers across its 35 Java files.

No license in this integration replaces a dependency's original license.
