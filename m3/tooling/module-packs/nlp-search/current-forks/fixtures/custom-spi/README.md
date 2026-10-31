<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
SPDX-License-Identifier: Apache-2.0 -->
# Generic OpenNLP provider lookup from the linked module

These three modules exercise the real donor Providers implementation with a
caller-defined SPI. They are runtime fixtures, not a replacement NLP provider.
The probe requires the provider module explicitly so it is resolved before
lookup. No fixture module declares a static uses clause for this SPI.

Each module uses the conventional <module>/src/main/java source owner, including
its module-info.java. This layout keeps the new fixture inside the existing
Java snapshot recipe's admitted source domain. The layout changes only paths;
all six Java source files retain their exact bytes. This documentation update
precedes the corresponding review-template move.

Run only after the root coordinator's ordered gates permit compilation and
runtime. Compile against the generated named org.apache.opennlp.tools artifact
and its actual dependency modules. For example, with task-specific variables
set to the supplied JDK, this directory, the completed module path and a fresh
output directory:

```sh
"$probe_jdk/bin/javac" -Xlint:all -Werror \
  --module-path "$opennlp_modules" --module-source-path "$probe_sources/*/src/main/java" \
  -d "$probe_output" \
  -m opennlp.fixture.spi,opennlp.fixture.provider,opennlp.fixture.probe
"$opennlp_image/bin/java" --module-path "$probe_output" \
  -m opennlp.fixture.probe/fixture.probe.CustomSpiProbe
```

The module path for compilation contains the generated modular artifacts. The
module-source-path wildcard is one quoted literal argument expanded by javac,
with probe_sources naming this fixtures/custom-spi directory. The
runtime image already contains the JMOD-built org.apache.opennlp.tools and its
closure; do not also place donor JARs or donor stubs on its runtime module path.
The actual compiler release and runtime image version belong in the root receipt.
This fixture source uses Java 21 APIs. Compiling with --release against a linked
image can hide additional system modules behind ct.sym; use the explicit generated
module path shown above, or the image compiler without --release when available.

Expected successful output is OPENNLP_CUSTOM_SPI_MODULE_PASS. The probe verifies
named-owner identity, absence of a predeclared uses clause, first dynamic lookup,
the provider's result, runtime canUse admission, descriptor immutability, repeated
lookup and the donor's trust/disabled-class filters. It intentionally fails with
the unadjusted donor when packaged as an explicit module. A classpath run cannot
satisfy its first check. No execution is claimed by adding this fixture.
