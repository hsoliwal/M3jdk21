# M3Scale source-backed repository guide

Source baseline: `de0d06ce13d502b279a767c5636bc4154c21585f` (`develop`), inspected 14 October 2026. This guide separates observed implementation, declared build intent and unresolved work. It is a partial semantic documentation pass over a complete tracked-tree inventory. No repository program, build, test, installer or transformation was executed.

## Navigation

- [Core runtime and mechanical contracts](CORE_CONTRACTS.md)
- [Secondary module contracts](SECONDARY_MODULE_CONTRACTS.md)
- [Root Maven build lanes](ROOT_BUILD_MAP.md)
- [Module/fixture/alternate Maven manifests](MODULE_BUILD_MAP.md)
- Java source-boundary inventories: [root](JAVA_BOUNDARIES_ROOT.md), [modules](JAVA_BOUNDARIES_MODULES.md), [custody/evidence](JAVA_BOUNDARIES_CUSTODY.md)
- [Native and script boundaries](NATIVE_AND_SCRIPTS.md)
- [Coverage, blockers and remaining work](COVERAGE.md)

## What this repository contains

The root [README](../README.md) identifies a bounded M3Scale release surface. The inspected code contains task-packet and reporting infrastructure, inventory/catalog interfaces, multiple mechanical transformation lanes, optional integration sources and several index/storage libraries. These components do not form a single automatically enabled runtime. Selecting a POM, source set, profile and dependency classpath is part of selecting the contract.

The full recursive tree has 5,894 entries: 4,414 files, 1,479 directories and one gitlink. It contains 3,004 Java files in 407 Java-containing directories, 85 Maven POM-shaped manifests (80 outside alternate custody), and nine CMake configurations. There are 177 distinct namespace labels in the representative-declaration inventory, including one default-package label. This is not a claim that every Java declaration has been reconciled or every directory is a deployable module.

## Build ownership and activation

[`pom.xml`](../pom.xml) declares `com.synexia.m3scale:synexia-m3scale` as a JAR with Java release 21. It relies on the external Cognix parent at `../cognix-parent/pom.xml`, inherited `${revision}` and managed dependencies, plus first-party artifacts such as `m3-index-dag` and `synexia-common`. A clean standalone checkout does not prove those prerequisites are available.

[`pom.reactor.xml`](../pom.reactor.xml) aggregates fourteen explicit module paths including the core JAR, Maven plugin, selected indexes, GPU/hybrid runtime and object projection/signal. It does not aggregate every source directory or every alternative POM. [`pom.mechanical.xml`](../pom.mechanical.xml) is an additive, independent reactor for `mechanical` and `mechanical-maven-plugin`, enforcing JDK 21 and Maven `[3.9.9,4)`. Root `pom.*.xml` files select additional source slices and optional profiles; read their [build map](ROOT_BUILD_MAP.md) before treating a class as active.

The root GPU profile `gpu-tornado` adds generated and `src/tornado` sources and preview settings; `camel-gpu-bridge` adds separate Camel bridge sources. Optional TornadoVM, JCuda, JNI, ONNX, servlet, JDT, ANTLR, Chrome/model and distributed-storage integrations require their actual SDK/runtime/dependency lanes. No hardware, model endpoint or native library was tested here. Managed plugin declarations alone are not executed checks.

## Current blockers and important distinctions

1. [`mechanical/pom.xml`](../mechanical/pom.xml) is not well-formed XML: a second `<project>` starts before the first build/project closes. [`camel-runtime/pom.xml`](../camel-runtime/pom.xml) has an unmatched dependency closing tag. This was confirmed against complete pinned contents and matching tree byte sizes by static XML parsing. The files remain unchanged
2. [`IInventoryScanner`](../src/main/java/com/synexia/m3scale/inventory/IInventoryScanner.java) contains two identical `scanBasicBatch(List<Path>, String, BatchCompute)` signatures. This is a source-observed compile blocker, not a compiler run receipt
3. Two physical trees contain different implementations under `com.synexia.m3scale.mechanical`, including duplicate fully qualified `Workspace` and `ClusterIndex` types. Do not combine their source roots or describe their promotion contracts as interchangeable
4. The root `M3Runtime` performs real configured verification, but most named transformation passes currently produce completion records without implementing the named transformation. A completed pass label is not proof that code changed
5. Exact hashes, structure hashes, SimHash clusters, lexical similarity and classfile identity are distinct evidence. None implies universal behavioral equivalence or authorizes source deletion
6. `.m3/all-branches-both-pass3-20260919/alternates` contains preserved alternatives, not automatically active source. `.merge-preserved/pr-205.gitlink` points to commit `911033d8d560841ca7ae89148b298b76be9e7d61`; there is no tracked `.gitmodules` mapping. The linked content is unresolved and not claimed recursively documented

## Operational entry points

The repository's [AGENTS.md](../AGENTS.md) documents `mvn -B -T1 --fail-fast -f pom.mechanical.xml install` on JDK 21/Maven 3.9.9+, followed by mechanical goals such as analyze/verify/apply. That command is an existing documented workflow, **currently blocked by the malformed child POM at this baseline**, and was not run. It must not be presented as a successful setup recipe.

The [root build](../pom.xml) is a separate lane with external parent requirements. A prospective `mvn -f pom.reactor.xml verify` still depends on resolving all selected module inputs and source blockers. Individual valid POM slices may be independently usable, but this pass does not certify them. Consult the manifest maps rather than assuming one command validates the entire tree.

Library APIs, CLI entry points, Maven plugin goals, scripts and native libraries have different effects. Inventory operations may create index/report files; verification may launch arbitrary host-configured processes; promotion may write source files or publish a snapshot depending on the lane. Read the exact contract before use. Documentation publication does not run these entry points.

## Licenses, provenance and review

Repository metadata reports a private non-fork repository; that does not prove absence of donor material or original authorship. Access remains unchanged. Refer to [LICENSE](../LICENSE), [NOTICE](../NOTICE), [COPYRIGHT.md](../COPYRIGHT.md), [AUTHORS.md](../AUTHORS.md) and [THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md). This pass does not relicense, transplant or publish raw reference material.

The guide is reviewable Markdown only. Build scripts, source, workflows, license files, preserved alternatives and native code remain unchanged. Missing semantic coverage is retained in [the explicit queue](COVERAGE.md), rather than silently excluded or counted as complete.
