<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
SPDX-License-Identifier: Apache-2.0 -->
# Current hsoliwal Lucene and OpenNLP JMOD lane

This contract precedes the driver implementation. The existing parent pack keeps
Lucene 9.10.0 / OpenNLP 2.4.0 compatibility custody. This separate lane accepts
sealed original Gradle/Maven build outputs from the current forks. Lucene's
current source and bytecode require Java 25. M3JDK21 cannot execute those classes.

## Source and artifact admission

Start from Lucene d1b7235f1ba1c1eea5b04a5b7e6554595a90719e and OpenNLP
368337311ad94b665fd178f8d3ced005a73b2370. OpenNLP's generic Providers lookup needs
the separately reviewed module-local addUses successor. Record that exact clean
successor commit in the lock. Lucene formatting-only successors keep the original
base_commit plus their exact current commit and verified ancestry in the lock. Never claim a remote head from an uncommitted patch.

Use original Gradle full JAR tasks, including their root Java25 implementation
classes. Build OpenNLP with its original Maven CPU toolkit reactor. The aggregate
preserves eleven API/runtime/ML/model/format/CLI/tools artifacts as
org.apache.opennlp.tools; it never distributes split packages in separate modules.
Every JAR path, Maven coordinate, SHA-256 and build receipt is explicit in the input
lock. Coordinates and filenames alone do not prove a source build. The caller
must retain actual commands, source snapshots and compiler/test outputs.

Source admission seals raw worktree SHA-256 values. A raw Git-blob mismatch is
accepted only for a complete LF-to-CRLF checkout declared by committed text=set
and eol=crlf attributes, with no filter or working-tree-encoding attribute. Native
git check-attr --source=<commit> reads those rules with system/global attributes
disabled. Active repository info/attributes rules conservatively refuse this
exception. Mixed endings, other byte changes, text=auto and core.autocrlf-only
conversion are refused. MANIFEST.json records each admitted conversion's raw
hash, committed blob and native attributes; SOURCE.sha256 retains the raw hashes.

The driver accepts additional genuine Lucene named modules and their complete
sealed dependency closure. It refuses automatic modules left without descriptors,
missing dependencies, split packages, duplicate names, unsafe archive paths and
bytecode newer than the selected release. The complete candidate includes all
thirty normal Lucene production modules and their admitted language resources.
Optional ONNX/native, UIMA, external morphology providers, pretrained models,
benchmark/test distributions and additional language resources need
separate input/rights/dependency admission; absence is never reported as coverage.

The two zlibsvm3.0.0 dependency JARs share implementation packages. They receive
one external owner, zlibsvm.core, while both original archive identities and
rights remain recorded separately. Its actual public libsvm dependency is
transitive. The lock explicitly seals both input JARs, the zlibsvm parent POM,
and the supporting OpenNLP LICENSE and NOTICE. The known resource-only
morfologik.ukrainian.search dictionary receives an empty module descriptor after
archive validation and a zero-class inventory; no bytecode dependency analysis
is claimed for that owner. Actual dictionary use is checked in the module image.
ClassGraph4.8.197 is included when receiving all model-resolver classes because
those class files reference it directly. Additional model JARs remain separately
admitted inputs, and their discoverability requires corresponding runtime tests.

## Ordered execution

1. Apply the qualified canonical Synexia hash-pinned recipes to exact preimages;
   retain first-run and unchanged replay receipts. Add module descriptors through
   the existing M3ModuleDescriptorRecipe. Check the existing receiver policy.
2. Run existing upstream formatting/checkstyle gates and Python syntax/whitespace
   checks. Provision immutable dependency caches separately, then qualify offline.
3. Build the unchanged fork algorithm owners through their original Gradle/Maven
   compile and JAR tasks. Use build.py compile to aggregate input JARs, obtain real
   jdeps dependency facts, apply descriptors through the canonical materializer,
   compile the existing capability source owner and additive adapters/probes, and
   invoke the existing jmod_pack.py --release 25 --jmods-only. It executes no tests
   and does not link or launch a product image.
4. Run focused original fork regression suites. Then build.py test runs archive
   refusal tests, borrowed-source/text/index/model checks, the custom SPI check,
   and a separate dependency probe against the real compiled modules. That probe
   uses original Lucene OpenNLP examples and checked-in models with the actual
   OpenNLP3 aggregate, Ukrainian dictionary stemming, and LibSVM predictions.
   It records exact inputs and output seals. An unchanged original Gradle test
   classpath still uses OpenNLP2.5.12 and is recorded as baseline evidence only.
5. build.py runtime requires unchanged compile/test receipts, links the explicit
   modules with jlink, validates/list modules, and repeats the behavioral probes
   in that image. Compiler integration remains a separate supplied-M3-compiler
   gate. No stock JDK test establishes internal M3String activation.

A corrective patch invalidates the candidate and restarts lint. Fresh output
paths are mandatory, outside source/JDK inputs. Inputs are checked again after
each boundary. Receipts report the actual finite tests; they do not promote the
ordered M3 receiving phase or claim whole-fork/full-JDK/platform/performance proof.

## Existing M3 ownership

The additive com.m3.nlpsearch module delegates to SourceArtifact.Span,
M3TextReaders, M3Tokenizers and actual Lucene/OpenNLP algorithms. There is no
tracked M3File class at the target pin: the requested file-family role uses the
existing immutable byte-source owner and is recorded as partial receiving parity.
M3String stays internal behind String; stable composite CharSequence adapters do
not flatten their input. The optional com.m3.nlpsearch.compiler consumer uses the
actual com.sun.source.util.M3AST PARSED projection and retains detached URI/seal/
UTF16 annotations only. See ../m3-bridge/README.md for lifetimes and failure behavior.

## Replay interface

The lock is JSON: source roots/commits, JDK25 home, required jdk21_home for fixtures, canonical recipe classpath,
eleven OpenNLP JAR records, named Lucene JAR records, external dependency JAR
records, zlibsvm_provenance records and source-build receipt records. Each file
record has path and sha256. The provenance object contains exactly parent_pom,
license_file and notice_file. Both zlibsvm input filenames retain their original
zlibsvm-core-3.0.0.jar and zlibsvm-api-3.0.0.jar identities; embedded coordinates
are independently validated by the merge owner.
Published module identities are read by the supplied JDK, not guessed from names.
The three phases use the same lock and output path:

```sh
python3 build.py compile --lock /absolute/INPUT_LOCK.json --out /absolute/fresh-pack
python3 build.py test --lock /absolute/INPUT_LOCK.json --out /absolute/fresh-pack
python3 build.py runtime --lock /absolute/INPUT_LOCK.json --out /absolute/fresh-pack
```

Use a frozen qualified materializer JAR and its dependency JARs in the recipe
classpath. Dynamic template resource directories are unnecessary for descriptor
materialization and would invalidate the input seal when edited. The recipe
classpath is a build-only canonical input. It is never copied into the
product modules or linked image. A runtime module depends only on the declared
JDK, M3 capability, Lucene and OpenNLP owners.

## Rights and promotion

Keep genuine source and notices with their upstream owners. Aggregated per-owner
manifests, licenses, notices and services have explicit META-INF/m3-donor custody.
New expression is Copyright 2026 Hitesh Soliwal and contributors, Apache-2.0.
Only already public M3 APIs/source are used. Reusable recipes and exact templates
remain in Synexia; this repository owns receiving integration and target tests.
No full M3 family parity, source-mirror completion or default-branch promotion is
implied by a successful finite qualification.

## Supplied Java25 distribution

The verified Temurin25 toolchain advertises linking from its runtime image and
does not ship a jmods directory. The receiver still creates real third-party
JMODs with jmod. jlink supplies system modules from the original runtime image;
M3 compiler API inspection uses jimage over lib/modules. Complete JDK regular
files and internal symlink identities are sealed before/after each phase. A
missing system JMOD directory is therefore not replaced with fabricated modules.
