<!-- SPDX-License-Identifier: Apache-2.0 -->

# Public fork documentation index

[Back to the M3JDK21 guide](README.md)

This page is a navigation and status snapshot of 23 verified public `hsoliwal` forks, inspected on 14 October 2026. It points to 22 open draft documentation PRs and Lucene's existing public documentation. It does not claim that every repository, module, package or public API is fully documented.

The repository groupings are reading routes. They do not establish runtime dependencies, compatibility, common licensing, or implementation of a shared M3 architecture. Upstream means the GitHub parent shown for the fork; where the source repository differs, both are identified.

## How to read the evidence

- **Documentation snapshot:** each document link is pinned to the reviewed documentation commit. The live PR may advance. The source baseline recorded inside each guide remains authoritative for its source claims.
- **Inventory:** paths, descriptors, declarations or artifacts have been counted under that guide's explicit rules. A directory, namespace, Maven project, OSGi bundle, source set and package-manager artifact are different units; their totals must not be added into one completion percentage.
- **Reviewed scope:** source-backed explanations exist for the stated boundaries. Unless explicitly stated otherwise, semantics remain partial. A representative type does not document every member in its package.
- **Missing coverage:** unread bodies, unresolved identities and unexecuted checks stay visible. Document-only repositories do not acquire invented software packages.
- **CI:** separately observed GitHub results appear in [CI evidence](#ci-evidence). Source review, a documentation PR, an empty check list and a skipped job are not passing execution evidence.

## Reading routes

- [Runtime and acceleration](#runtime-and-acceleration): M3JDK21, TornadoVM and Dawn
- [Developer tools and catalogue](#developer-tools-and-catalogue): ChromeLLM, codexpro, OpenRewrite, Gitea and providers
- [Eclipse platform and widgets](#eclipse-platform-and-widgets): Platform, SDK assembly, SWT, UI, GEF Classic and Nebula
- [Initiative 31 research and prototypes](#initiative-31-research-and-prototypes): research documents, community record, PDE and LibreOffice/VCL experiment
- [Search and natural language](#search-and-natural-language): Lucene and the four OpenNLP forks

## Runtime and acceleration

### M3jdk21

[Public repository](https://github.com/hsoliwal/M3jdk21) · Parent: [openjdk/jdk21](https://github.com/openjdk/jdk21)

OpenJDK 21-derived runtime and independent M3 library/tooling boundaries.

- **Documentation:** [Guide](https://github.com/hsoliwal/M3jdk21/blob/eb6c8f7da7e1b274f03b079d16c208bf634e36f6/doc/m3-guide/README.md) · [Projects](https://github.com/hsoliwal/M3jdk21/blob/eb6c8f7da7e1b274f03b079d16c208bf634e36f6/doc/m3-guide/projects.md) · [JDK modules](https://github.com/hsoliwal/M3jdk21/blob/eb6c8f7da7e1b274f03b079d16c208bf634e36f6/doc/m3-guide/jdk-modules.md) · [M3 package contracts](https://github.com/hsoliwal/M3jdk21/blob/eb6c8f7da7e1b274f03b079d16c208bf634e36f6/doc/m3-guide/m3-packages.md) · [Package-directory catalogue](https://github.com/hsoliwal/M3jdk21/blob/eb6c8f7da7e1b274f03b079d16c208bf634e36f6/doc/m3-guide/packages/README.md) · [Descriptors](https://github.com/hsoliwal/M3jdk21/blob/eb6c8f7da7e1b274f03b079d16c208bf634e36f6/doc/m3-guide/descriptors.md) · [Indexed-string reference](https://github.com/hsoliwal/M3jdk21/blob/eb6c8f7da7e1b274f03b079d16c208bf634e36f6/doc/m3-guide/indexed-string-reference.md) · [Coverage](https://github.com/hsoliwal/M3jdk21/blob/eb6c8f7da7e1b274f03b079d16c208bf634e36f6/doc/m3-guide/coverage.md)
- **Reviewed scope:** Complete pinned tracked-tree accounting; all 70 direct JDK descriptors; 266 POM and 565 JPMS descriptor paths; 13,793 Java source-directory rows. Selected text/storage, precompute and indexed-string owner bodies have deeper contract notes.
- **Missing coverage:** The source-directory count is not a declared-package count. The namespace/project denominator, most package/member semantics, native/script ownership and target runtime qualification remain open.
- **Review:** [Draft PR #884](https://github.com/hsoliwal/M3jdk21/pull/884); documentation snapshot [`eb6c8f7da7e1b274f03b079d16c208bf634e36f6`](https://github.com/hsoliwal/M3jdk21/commit/eb6c8f7da7e1b274f03b079d16c208bf634e36f6)

### synexia-tornadovm

[Public repository](https://github.com/hsoliwal/synexia-tornadovm) · Parent: [beehive-lab/TornadoVM](https://github.com/beehive-lab/TornadoVM)

TornadoVM-based Java accelerator stack, with host orchestration, device backends and separate CPU-model experiments.

- **Documentation:** [Component guide](https://github.com/hsoliwal/synexia-tornadovm/blob/ca94352f6d0be944cc7be487553299752232e525/docs/COMPONENT_GUIDE.md) · [Module/package inventory](https://github.com/hsoliwal/synexia-tornadovm/blob/ca94352f6d0be944cc7be487553299752232e525/docs/COMPONENT_INVENTORY.md) · [Host/device lifecycle](https://github.com/hsoliwal/synexia-tornadovm/blob/ca94352f6d0be944cc7be487553299752232e525/docs/HOST_DEVICE_LIFECYCLE.md) · [Build/run guide](https://github.com/hsoliwal/synexia-tornadovm/blob/ca94352f6d0be944cc7be487553299752232e525/BUILD_AND_RUN.md)
- **Reviewed scope:** 30 POM projects and 19 JPMS descriptors; 287 named Java source-package locations corresponding to 276 owner/namespace identities. Selected graph, transfer, cleanup, native-dispatch and CPU-model contracts.
- **Missing coverage:** Only 15 identities have focused partial semantics; 261 remain inventory-only. Two nested CMake manifests, other language boundaries, API leaves, inline documentation and GPU/runtime qualification remain open.
- **Review:** [Draft PR #18](https://github.com/hsoliwal/synexia-tornadovm/pull/18); documentation snapshot [`ca94352f6d0be944cc7be487553299752232e525`](https://github.com/hsoliwal/synexia-tornadovm/commit/ca94352f6d0be944cc7be487553299752232e525)

### dawn

[Public repository](https://github.com/hsoliwal/dawn) · Parent: [google/dawn](https://github.com/google/dawn)

Native WebGPU implementation and Tint shader compiler; host API/wire/backend work is separate from device execution.

- **Documentation:** [Components](https://github.com/hsoliwal/dawn/blob/19dd31771c3747cea58a7ba736f952f3a0dc22be/docs/repository-components.md) · [Boundary inventory](https://github.com/hsoliwal/dawn/blob/19dd31771c3747cea58a7ba736f952f3a0dc22be/docs/component-inventory.md) · [Resource lifecycle](https://github.com/hsoliwal/dawn/blob/19dd31771c3747cea58a7ba736f952f3a0dc22be/docs/dawn/resource-lifecycle.md) · [Build guide](https://github.com/hsoliwal/dawn/blob/19dd31771c3747cea58a7ba736f952f3a0dc22be/docs/building.md)
- **Reviewed scope:** Pinned superproject inventory accounts for 431 build manifests in 175 directories and 66 first-party Go package directories. Selected buffer, queue, wire and Tint reader/IR/writer contracts.
- **Missing coverage:** 21 major first-party build-directory reviews are partial; 127 first-party directories and 27 third-party integration boundaries remain inventory-only. C++ namespace/target/API totals are unresolved; 67 gitlink contents are unexpanded. Go semantics and GPU/CTS qualification remain open.
- **Review:** [Draft PR #2](https://github.com/hsoliwal/dawn/pull/2); documentation snapshot [`19dd31771c3747cea58a7ba736f952f3a0dc22be`](https://github.com/hsoliwal/dawn/commit/19dd31771c3747cea58a7ba736f952f3a0dc22be)

## Developer tools and catalogue

### ChromeLLM

[Public repository](https://github.com/hsoliwal/ChromeLLM) · Parent: [xszwow/Chrome2api](https://github.com/xszwow/Chrome2api)

Chrome Gemini Nano/ChromeML HTTP wrapper alongside separate Java/native diffusion and media tools.

- **Documentation:** [Development](https://github.com/hsoliwal/ChromeLLM/blob/c469718a09719dc232fff55037a427ffac58b621/docs/FORK_DEVELOPMENT.md) · [Architecture/projects](https://github.com/hsoliwal/ChromeLLM/blob/c469718a09719dc232fff55037a427ffac58b621/docs/ARCHITECTURE_AND_PROJECTS.md) · [Java contracts](https://github.com/hsoliwal/ChromeLLM/blob/c469718a09719dc232fff55037a427ffac58b621/docs/JAVA_PACKAGE_CONTRACTS.md) · [Media lifecycle](https://github.com/hsoliwal/ChromeLLM/blob/c469718a09719dc232fff55037a427ffac58b621/docs/MEDIA_PIPELINE_CONTRACTS.md) · [Native/runtime contracts](https://github.com/hsoliwal/ChromeLLM/blob/c469718a09719dc232fff55037a427ffac58b621/docs/NATIVE_RUNTIME_CONTRACTS.md) · [Coverage](https://github.com/hsoliwal/ChromeLLM/blob/c469718a09719dc232fff55037a427ffac58b621/docs/DOCUMENTATION_COVERAGE.md)
- **Reviewed scope:** Six independently configured projects, 11 build/runtime/tooling modules, 14 main Java namespaces, six test namespaces and two Maven artifact identities. Java/test bodies, native bridge files and selected launcher/HTTP boundaries support the guides.
- **Missing coverage:** Windows runner callback/ABI semantics, binary provenance, generated/reference headers, full inline/API coverage and native/model/device behavior remain partial or unverified.
- **Review:** [Draft PR #12](https://github.com/hsoliwal/ChromeLLM/pull/12); documentation snapshot [`c469718a09719dc232fff55037a427ffac58b621`](https://github.com/hsoliwal/ChromeLLM/commit/c469718a09719dc232fff55037a427ffac58b621)

### codexpro

[Public repository](https://github.com/hsoliwal/codexpro) · Parent: [rebel0789/codexpro](https://github.com/rebel0789/codexpro)

Local MCP coding agent with a Node application, Java CPU sidecar, documentation site and separate analysis tooling.

- **Documentation:** [Architecture/projects](https://github.com/hsoliwal/codexpro/blob/730bc0a981cc8259b0502ddc280fc808e5b6e8ce/docs/architecture-and-projects.md) · [Module inventory](https://github.com/hsoliwal/codexpro/blob/730bc0a981cc8259b0502ddc280fc808e5b6e8ce/docs/module-inventory.md) · [MCP operation reference](https://github.com/hsoliwal/codexpro/blob/730bc0a981cc8259b0502ddc280fc808e5b6e8ce/docs/mcp-api-reference.md) · [Runtime contracts](https://github.com/hsoliwal/codexpro/blob/730bc0a981cc8259b0502ddc280fc808e5b6e8ce/docs/runtime-contracts.md) · [Workspace/analysis](https://github.com/hsoliwal/codexpro/blob/730bc0a981cc8259b0502ddc280fc808e5b6e8ce/docs/workspace-and-analysis-contracts.md) · [Compute contracts](https://github.com/hsoliwal/codexpro/blob/730bc0a981cc8259b0502ddc280fc808e5b6e8ce/docs/compute-contracts.md) · [Coverage](https://github.com/hsoliwal/codexpro/blob/730bc0a981cc8259b0502ddc280fc808e5b6e8ce/docs/documentation-coverage.md)
- **Reviewed scope:** Five configured projects, 73 source modules and five package identities; all 33 MCP registration sites have a compact reference. Selected runtime, workspace, analysis and sidecar contracts.
- **Missing coverage:** 63 category records are partial and 21 inventory-only; none reviewed-complete. Exhaustive schemas, API/inline semantics, harnesses, launcher/UI review and platform verification remain open. CI audit failure is recorded separately below.
- **Review:** [Draft PR #10](https://github.com/hsoliwal/codexpro/pull/10); documentation snapshot [`730bc0a981cc8259b0502ddc280fc808e5b6e8ce`](https://github.com/hsoliwal/codexpro/commit/730bc0a981cc8259b0502ddc280fc808e5b6e8ce)

### rewrite

[Public repository](https://github.com/hsoliwal/rewrite) · Parent: [openrewrite/rewrite](https://github.com/openrewrite/rewrite)

OpenRewrite source-refactoring framework with JVM, polyglot parser/RPC and auxiliary build boundaries.

- **Documentation:** [Architecture](https://github.com/hsoliwal/rewrite/blob/a6f3e685d8e06ddc6628370bc4eaf890231a758d/doc/architecture/README.md) · [Modules](https://github.com/hsoliwal/rewrite/blob/a6f3e685d8e06ddc6628370bc4eaf890231a758d/doc/architecture/modules.md) · [API contracts](https://github.com/hsoliwal/rewrite/blob/a6f3e685d8e06ddc6628370bc4eaf890231a758d/doc/architecture/api-contracts.md) · [Core packages](https://github.com/hsoliwal/rewrite/blob/a6f3e685d8e06ddc6628370bc4eaf890231a758d/doc/architecture/packages-core.md) · [JVM packages](https://github.com/hsoliwal/rewrite/blob/a6f3e685d8e06ddc6628370bc4eaf890231a758d/doc/architecture/packages-jvm.md) · [Format packages](https://github.com/hsoliwal/rewrite/blob/a6f3e685d8e06ddc6628370bc4eaf890231a758d/doc/architecture/packages-formats.md) · [Polyglot packages](https://github.com/hsoliwal/rewrite/blob/a6f3e685d8e06ddc6628370bc4eaf890231a758d/doc/architecture/packages-polyglot.md) · [Coverage](https://github.com/hsoliwal/rewrite/blob/a6f3e685d8e06ddc6628370bc4eaf890231a758d/doc/architecture/coverage.md)
- **Reviewed scope:** 34 explicitly configured Gradle modules plus separately identified build units; all 572 source-directory groups have inventory rows, 330 with partial responsibility summaries. Selected parser, recipe, visitor, table, RPC and DSL contracts.
- **Missing coverage:** 242 test/fixture/other groups remain inventory-only. Complete namespaces/public members, per-file semantics for 251 Python/TypeScript modules, inline reconciliation and executable verification remain open.
- **Review:** [Draft PR #5](https://github.com/hsoliwal/rewrite/pull/5); documentation snapshot [`a6f3e685d8e06ddc6628370bc4eaf890231a758d`](https://github.com/hsoliwal/rewrite/commit/a6f3e685d8e06ddc6628370bc4eaf890231a758d)

### gitea

[Public repository](https://github.com/hsoliwal/gitea) · Parent: [go-gitea/gitea](https://github.com/go-gitea/gitea)

Self-hosted Git service with repository hosting, review, collaboration and related application features.

- **Documentation:** [Architecture](https://github.com/hsoliwal/gitea/blob/4c5d87ac67eeed646a2e434c989dab8b32df749c/docs/source-architecture.md) · [Go package catalogue](https://github.com/hsoliwal/gitea/blob/4c5d87ac67eeed646a2e434c989dab8b32df749c/docs/package-catalog.md) · [Selected contracts](https://github.com/hsoliwal/gitea/blob/4c5d87ac67eeed646a2e434c989dab8b32df749c/docs/source-contracts.md) · [Tree/frontend map](https://github.com/hsoliwal/gitea/blob/4c5d87ac67eeed646a2e434c989dab8b32df749c/docs/source-tree.md) · [Coverage](https://github.com/hsoliwal/gitea/blob/4c5d87ac67eeed646a2e434c989dab8b32df749c/docs/source-coverage.md)
- **Reviewed scope:** Complete tracked-tree map; 387 Go-containing directories, including 125 representative source reviews. Go, frontend, Python tooling and contributed monitoring boundaries are distinguished.
- **Missing coverage:** 262 Go-directory implementations remain unread; all 246 frontend file candidates lack member review. Full HTTP/service lifecycle, settings, packaging, build-active packages and per-member semantics remain open.
- **Review:** [Draft PR #5](https://github.com/hsoliwal/gitea/pull/5); documentation snapshot [`4c5d87ac67eeed646a2e434c989dab8b32df749c`](https://github.com/hsoliwal/gitea/commit/4c5d87ac67eeed646a2e434c989dab8b32df749c)

### providers

[Public repository](https://github.com/hsoliwal/providers) · Parent: [api-evangelist/providers](https://github.com/api-evangelist/providers)

Jekyll provider catalogue with Python generation tools, data/templates and vendored browser assets; it is not an inference gateway.

- **Documentation:** [Architecture/coverage](https://github.com/hsoliwal/providers/blob/239175138e703872185236274041444f41d5eba4/documentation/ARCHITECTURE_AND_COVERAGE.md) · [Generator contracts](https://github.com/hsoliwal/providers/blob/239175138e703872185236274041444f41d5eba4/documentation/GENERATOR_CONTRACTS.md) · [Data/templates](https://github.com/hsoliwal/providers/blob/239175138e703872185236274041444f41d5eba4/documentation/DATA_AND_TEMPLATE_CONTRACTS.md) · [Validation/operations](https://github.com/hsoliwal/providers/blob/239175138e703872185236274041444f41d5eba4/documentation/VALIDATION_AND_OPERATIONS.md)
- **Reviewed scope:** Selected catalogue/generator contracts cover six first-party Python modules and five auxiliary executable boundaries; Monaco package identity and selected Liquid consumers are described.
- **Missing coverage:** The published inventory leaves the _providers subtree unresolved, so its record count and total repository count are unknown. Ten include bodies, three layouts, remaining schemas and full vendored API/browser behavior remain open.
- **Review:** [Draft PR #6](https://github.com/hsoliwal/providers/pull/6); documentation snapshot [`239175138e703872185236274041444f41d5eba4`](https://github.com/hsoliwal/providers/commit/239175138e703872185236274041444f41d5eba4)

## Eclipse platform and widgets

### eclipse.platform

[Public repository](https://github.com/hsoliwal/eclipse.platform) · Parent: [eclipse-platform/eclipse.platform](https://github.com/eclipse-platform/eclipse.platform)

Core Eclipse Platform frameworks and common services, including jobs and workspace/resource facilities.

- **Documentation:** [Reference home](https://github.com/hsoliwal/eclipse.platform/blob/6d3bbf2ae2c362713f94d9662d262fcdfa2cecc0/docs/source-reference/README.md) · [Contracts](https://github.com/hsoliwal/eclipse.platform/blob/6d3bbf2ae2c362713f94d9662d262fcdfa2cecc0/docs/source-reference/CONTRACTS.md) · [Coverage](https://github.com/hsoliwal/eclipse.platform/blob/6d3bbf2ae2c362713f94d9662d262fcdfa2cecc0/docs/source-reference/COVERAGE.md) · [Other sources](https://github.com/hsoliwal/eclipse.platform/blob/6d3bbf2ae2c362713f94d9662d262fcdfa2cecc0/docs/source-reference/OTHER-SOURCES.md)
- **Reviewed scope:** 137 metadata-backed project/module paths, 615 Java-bearing directories and 596 recognized named namespaces are mapped. Selected job cancellation/completion/join, scheduling-rule and workspace-operation contracts.
- **Missing coverage:** Complete project behavior, every package/member API, non-Java sources, effective build profiles and runtime/platform tests remain open. Reading all declarations is not a complete semantic review.
- **Review:** [Draft PR #8](https://github.com/hsoliwal/eclipse.platform/pull/8); documentation snapshot [`6d3bbf2ae2c362713f94d9662d262fcdfa2cecc0`](https://github.com/hsoliwal/eclipse.platform/commit/6d3bbf2ae2c362713f94d9662d262fcdfa2cecc0)

### eclipse.platform.releng.aggregator

[Public repository](https://github.com/hsoliwal/eclipse.platform.releng.aggregator) · Parent: [eclipse-platform/eclipse.platform.releng.aggregator](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator)

Eclipse SDK assembly and release-engineering superproject, including inherited parent configuration, products and sites.

- **Documentation:** [Reference home](https://github.com/hsoliwal/eclipse.platform.releng.aggregator/blob/98a846bf62648d30bb7871e3c8a61d6888761f25/docs/source-reference/README.md) · [Contracts](https://github.com/hsoliwal/eclipse.platform.releng.aggregator/blob/98a846bf62648d30bb7871e3c8a61d6888761f25/docs/source-reference/CONTRACTS.md) · [Coverage](https://github.com/hsoliwal/eclipse.platform.releng.aggregator/blob/98a846bf62648d30bb7871e3c8a61d6888761f25/docs/source-reference/COVERAGE.md) · [Other sources](https://github.com/hsoliwal/eclipse.platform.releng.aggregator/blob/98a846bf62648d30bb7871e3c8a61d6888761f25/docs/source-reference/OTHER-SOURCES.md)
- **Reviewed scope:** 49 metadata-backed project/module paths, 13 Java-bearing directories and 12 recognized namespaces; all 12 gitlink boundaries. Selected reactor/profile, test/performance and report-tool contracts.
- **Missing coverage:** Gitlink descendants are separate source boundaries. Full child semantics, all release/tool scripts, configured reactor behavior and SDK build/test verification remain open.
- **Review:** [Draft PR #6](https://github.com/hsoliwal/eclipse.platform.releng.aggregator/pull/6); documentation snapshot [`98a846bf62648d30bb7871e3c8a61d6888761f25`](https://github.com/hsoliwal/eclipse.platform.releng.aggregator/commit/98a846bf62648d30bb7871e3c8a61d6888761f25)

### eclipse.platform.swt

[Public repository](https://github.com/hsoliwal/eclipse.platform.swt) · Parent: [eclipse-platform/eclipse.platform.swt](https://github.com/eclipse-platform/eclipse.platform.swt)

Standard Widget Toolkit Java/native UI layer with platform variants and OSGi fragments.

- **Documentation:** [Architecture/coverage](https://github.com/hsoliwal/eclipse.platform.swt/blob/a781367aa2bc52df50fbb466e37522c54517ab86/docs/architecture/README.md) · [Module ledger](https://github.com/hsoliwal/eclipse.platform.swt/blob/a781367aa2bc52df50fbb466e37522c54517ab86/docs/architecture/module-ledger.md) · [Package ledger](https://github.com/hsoliwal/eclipse.platform.swt/blob/a781367aa2bc52df50fbb466e37522c54517ab86/docs/architecture/package-ledger.md) · [Lifecycle contracts](https://github.com/hsoliwal/eclipse.platform.swt/blob/a781367aa2bc52df50fbb466e37522c54517ab86/docs/architecture/lifecycle-contracts.md) · [Export boundaries](https://github.com/hsoliwal/eclipse.platform.swt/blob/a781367aa2bc52df50fbb466e37522c54517ab86/docs/architecture/export-boundaries.md)
- **Reviewed scope:** 34 descriptor-backed project/build units, 24 OSGi modules/fragments and 162 Java source-directory occurrences. Selected resource disposal, widget guards, Display handoff, event reentrancy, layout and JNI boundaries.
- **Missing coverage:** Platform source variants are not duplicate complete APIs. Exhaustive package/member/native semantics, export-only origins, inline reconciliation and host-specific UI/native qualification remain open.
- **Review:** [Draft PR #164](https://github.com/hsoliwal/eclipse.platform.swt/pull/164); documentation snapshot [`a781367aa2bc52df50fbb466e37522c54517ab86`](https://github.com/hsoliwal/eclipse.platform.swt/commit/a781367aa2bc52df50fbb466e37522c54517ab86)

### eclipse.platform.ui

[Public repository](https://github.com/hsoliwal/eclipse.platform.ui) · Parent: [eclipse-platform/eclipse.platform.ui](https://github.com/eclipse-platform/eclipse.platform.ui)

Eclipse UI frameworks including JFace, data binding, E4/workbench, commands and text-related owners.

- **Documentation:** [Architecture/coverage](https://github.com/hsoliwal/eclipse.platform.ui/blob/119b433438c6cca2e3d21365cf6054b7874a8d14/docs/architecture/README.md) · [Module ledger](https://github.com/hsoliwal/eclipse.platform.ui/blob/119b433438c6cca2e3d21365cf6054b7874a8d14/docs/architecture/module-ledger.md) · [Package ledger](https://github.com/hsoliwal/eclipse.platform.ui/blob/119b433438c6cca2e3d21365cf6054b7874a8d14/docs/architecture/package-ledger.md) · [Lifecycle contracts](https://github.com/hsoliwal/eclipse.platform.ui/blob/119b433438c6cca2e3d21365cf6054b7874a8d14/docs/architecture/lifecycle-contracts.md) · [Export boundaries](https://github.com/hsoliwal/eclipse.platform.ui/blob/119b433438c6cca2e3d21365cf6054b7874a8d14/docs/architecture/export-boundaries.md)
- **Reviewed scope:** 154 descriptor-backed project/build units, 143 OSGi modules/fragments and 767 Java source-directory occurrences. Selected viewer/provider teardown, resource registry, Window, workbench, realm and binding contracts.
- **Missing coverage:** Full source-set/package/member semantics, export-only provenance, all UI/state/thread interactions and native/platform tests remain open.
- **Review:** [Draft PR #10](https://github.com/hsoliwal/eclipse.platform.ui/pull/10); documentation snapshot [`119b433438c6cca2e3d21365cf6054b7874a8d14`](https://github.com/hsoliwal/eclipse.platform.ui/commit/119b433438c6cca2e3d21365cf6054b7874a8d14)

### gef-classic

[Public repository](https://github.com/hsoliwal/gef-classic) · Parent: [eclipse-gef/gef-classic](https://github.com/eclipse-gef/gef-classic)

Eclipse GEF Classic, Draw2D and Zest graphical editing/visualization frameworks.

- **Documentation:** [Reference home](https://github.com/hsoliwal/gef-classic/blob/cfb67edc9ce843b39704d97d650c8d5730e44bbf/docs/source-reference/README.md) · [Modules](https://github.com/hsoliwal/gef-classic/blob/cfb67edc9ce843b39704d97d650c8d5730e44bbf/docs/source-reference/MODULES.md) · [Packages](https://github.com/hsoliwal/gef-classic/blob/cfb67edc9ce843b39704d97d650c8d5730e44bbf/docs/source-reference/PACKAGES.md) · [Lifecycle](https://github.com/hsoliwal/gef-classic/blob/cfb67edc9ce843b39704d97d650c8d5730e44bbf/docs/source-reference/LIFECYCLE.md) · [Coverage](https://github.com/hsoliwal/gef-classic/blob/cfb67edc9ce843b39704d97d650c8d5730e44bbf/docs/source-reference/COVERAGE.md)
- **Reviewed scope:** 35 Eclipse project paths, 38 build/bundle/feature units and 159 declared named Java namespaces; six selected lifecycle contract groups.
- **Missing coverage:** Exhaustive package/API semantics, two export-only namespace origins, non-Java artifacts, UI/native tests and resource/performance verification remain open.
- **Review:** [Draft PR #8](https://github.com/hsoliwal/gef-classic/pull/8); documentation snapshot [`cfb67edc9ce843b39704d97d650c8d5730e44bbf`](https://github.com/hsoliwal/gef-classic/commit/cfb67edc9ce843b39704d97d650c8d5730e44bbf)

### nebula

[Public repository](https://github.com/hsoliwal/nebula) · Parent: [EclipseNebula/nebula](https://github.com/EclipseNebula/nebula)

Eclipse Nebula custom widgets and visualization components, grouped by their actual widget/component owners.

- **Documentation:** [Reference home](https://github.com/hsoliwal/nebula/blob/6fc58adda750c2af7ee7f20d436a29f49d7770df/docs/source-reference/README.md) · [Modules](https://github.com/hsoliwal/nebula/blob/6fc58adda750c2af7ee7f20d436a29f49d7770df/docs/source-reference/MODULES.md) · [Packages](https://github.com/hsoliwal/nebula/blob/6fc58adda750c2af7ee7f20d436a29f49d7770df/docs/source-reference/PACKAGES.md) · [Lifecycle](https://github.com/hsoliwal/nebula/blob/6fc58adda750c2af7ee7f20d436a29f49d7770df/docs/source-reference/LIFECYCLE.md) · [Coverage](https://github.com/hsoliwal/nebula/blob/6fc58adda750c2af7ee7f20d436a29f49d7770df/docs/source-reference/COVERAGE.md)
- **Reviewed scope:** 262 Eclipse project paths, 345 build/bundle/feature units and 334 declared named Java namespaces; eight selected lifecycle groups for Grid, Gallery, CDateTime and PShelf.
- **Missing coverage:** Full component/package/API semantics, 50 export-only namespace origins, 277 binary JAR interiors and embedded JavaScript/script behavior remain unreviewed. Native/UI behavior and resource budgets remain unverified.
- **Review:** [Draft PR #178](https://github.com/hsoliwal/nebula/pull/178); documentation snapshot [`6fc58adda750c2af7ee7f20d436a29f49d7770df`](https://github.com/hsoliwal/nebula/commit/6fc58adda750c2af7ee7f20d436a29f49d7770df)

## Initiative 31 research and prototypes

### initiative31-documents

[Public repository](https://github.com/hsoliwal/initiative31-documents) · Parent: [swt-initiative31/documents](https://github.com/swt-initiative31/documents)

Historical research reports and assessments for OS-agnostic SWT evaluation, with reader and evidence guides.

- **Documentation:** [Reader guide](https://github.com/hsoliwal/initiative31-documents/blob/cb51d12e6c31fb83fa0428ea954475747bf3976e/docs/START_HERE.md) · [Content map](https://github.com/hsoliwal/initiative31-documents/blob/cb51d12e6c31fb83fa0428ea954475747bf3976e/docs/CONTENT_MAP.md) · [Evidence/review](https://github.com/hsoliwal/initiative31-documents/blob/cb51d12e6c31fb83fa0428ea954475747bf3976e/docs/EVIDENCE_AND_REVIEW.md)
- **Reviewed scope:** Complete 85-file source-baseline inventory: 12 Markdown documents, 63 PNGs, eight presentations, one PDF and a license. All 12 Markdown documents were read; document groups and implementation references are distinguished.
- **Missing coverage:** Binary media contents/rendering, external availability and current prototype behavior remain unverified. Software packages and build modules are not applicable to this document-only baseline.
- **Review:** [Draft PR #2](https://github.com/hsoliwal/initiative31-documents/pull/2); documentation snapshot [`cb51d12e6c31fb83fa0428ea954475747bf3976e`](https://github.com/hsoliwal/initiative31-documents/commit/cb51d12e6c31fb83fa0428ea954475747bf3976e)

### initiative31-community

[Public repository](https://github.com/hsoliwal/initiative31-community) · Parent: [swt-initiative31/.github](https://github.com/swt-initiative31/.github)

Community profile, brainstorming record and navigation to the initiative's research and implementation repositories.

- **Documentation:** [Field guide](https://github.com/hsoliwal/initiative31-community/blob/18cf6874b2f3f9029366b78da7b8a291f1541b0b/docs/START_HERE.md) · [Content map](https://github.com/hsoliwal/initiative31-community/blob/18cf6874b2f3f9029366b78da7b8a291f1541b0b/docs/CONTENT_MAP.md)
- **Reviewed scope:** Complete seven-file source-baseline inventory: six Markdown documents and a license. Explains wishes versus reported results and the contribution destination.
- **Missing coverage:** Software modules/packages are not applicable to this document-only baseline. Rendered preview, historical external/contact availability and the separate prototypes' behavior are unverified.
- **Review:** [Draft PR #2](https://github.com/hsoliwal/initiative31-community/pull/2); documentation snapshot [`18cf6874b2f3f9029366b78da7b8a291f1541b0b`](https://github.com/hsoliwal/initiative31-community/commit/18cf6874b2f3f9029366b78da7b8a291f1541b0b)

### initiative31-eclipse.pde

[Public repository](https://github.com/hsoliwal/initiative31-eclipse.pde) · Parent: [swt-initiative31/eclipse.pde](https://github.com/swt-initiative31/eclipse.pde) · Source lineage: [eclipse-pde/eclipse.pde](https://github.com/eclipse-pde/eclipse.pde)

Initiative 31 fork of Eclipse Plug-in Development Environment tooling, including the API Tools lineage.

- **Documentation:** [Fork guide](https://github.com/hsoliwal/initiative31-eclipse.pde/blob/0bfea1ae5baa497bd814e63ccf43897bbe4cfaa1/docs/FORK_GUIDE.md)
- **Reviewed scope:** Published navigation guide describes source areas, Oomph checkout caveats, parent/Tycho/Maven/Jenkins settings and build/test entry points.
- **Missing coverage:** Only this published guide is indexed. A repository-wide project/module/package ledger and complete implementation/API semantics are not established by the linked documentation. Build, UI and compatibility checks remain unverified.
- **Review:** [Draft PR #1](https://github.com/hsoliwal/initiative31-eclipse.pde/pull/1); documentation snapshot [`0bfea1ae5baa497bd814e63ccf43897bbe4cfaa1`](https://github.com/hsoliwal/initiative31-eclipse.pde/commit/0bfea1ae5baa497bd814e63ccf43897bbe4cfaa1)

### initiative31-prototype-skiavcl-libreoffice

[Public repository](https://github.com/hsoliwal/initiative31-prototype-skiavcl-libreoffice) · Parent: [swt-initiative31/prototype-skiavcl-libreoffice](https://github.com/swt-initiative31/prototype-skiavcl-libreoffice) · Source lineage: [LibreOffice/core](https://github.com/LibreOffice/core)

LibreOffice-derived VCL extraction experiment, with historical native/Java bridge work and inherited VCL/Skia boundaries.

- **Documentation:** [Start here](https://github.com/hsoliwal/initiative31-prototype-skiavcl-libreoffice/blob/e877a33418481963c6b476bd2dde84e7249b2e1e/docs/fork-guide/START_HERE.md) · [Root/build index](https://github.com/hsoliwal/initiative31-prototype-skiavcl-libreoffice/blob/e877a33418481963c6b476bd2dde84e7249b2e1e/docs/fork-guide/MODULE_INDEX.md) · [Module manifests](https://github.com/hsoliwal/initiative31-prototype-skiavcl-libreoffice/blob/e877a33418481963c6b476bd2dde84e7249b2e1e/docs/fork-guide/MODULE_MANIFESTS.md) · [Prototype contracts](https://github.com/hsoliwal/initiative31-prototype-skiavcl-libreoffice/blob/e877a33418481963c6b476bd2dde84e7249b2e1e/docs/fork-guide/PROTOTYPE_CONTRACTS.md) · [VCL/Skia contracts](https://github.com/hsoliwal/initiative31-prototype-skiavcl-libreoffice/blob/e877a33418481963c6b476bd2dde84e7249b2e1e/docs/fork-guide/VCL_SKIA_CONTRACTS.md) · [Coverage](https://github.com/hsoliwal/initiative31-prototype-skiavcl-libreoffice/blob/e877a33418481963c6b476bd2dde84e7249b2e1e/docs/fork-guide/COVERAGE.md)
- **Reviewed scope:** Baseline superproject inventory accounts for 155,737 entries and 230 Module_*.mk candidate paths. Selected build, VCL/Skia helper and historical bridge bodies are reviewed with separate source revisions.
- **Missing coverage:** 228 candidate module bodies remain unreviewed; three gitlink descendants and downloaded archives are unexpanded. Language namespace/API totals, most rendering/widget/backend bodies and full experiment-wide semantics remain open. No GUI/GPU/native build qualification is implied.
- **Review:** [Draft PR #2](https://github.com/hsoliwal/initiative31-prototype-skiavcl-libreoffice/pull/2); documentation snapshot [`e877a33418481963c6b476bd2dde84e7249b2e1e`](https://github.com/hsoliwal/initiative31-prototype-skiavcl-libreoffice/commit/e877a33418481963c6b476bd2dde84e7249b2e1e)

## Search and natural language

### lucene

[Public repository](https://github.com/hsoliwal/lucene) · Parent: [apache/lucene](https://github.com/apache/lucene)

Apache Lucene Java text-search library.

- **Documentation:** [Existing README and documentation routes](https://github.com/hsoliwal/lucene/blob/1169cd2007d37c1e5354fdccf9aa7e4eef93d21a/README.md)
- **Reviewed scope:** Existing public README links contributor, build-system, migration and developer documentation. This index supplies navigation to that existing material only; no additional fork-documentation PR is indexed.
- **Missing coverage:** This entry does not establish an exhaustive fork-specific module/package inventory or source-contract review. Existing upstream documentation and badges retain their own revision and execution scope.
- **Existing-source snapshot:** [`1169cd2007d37c1e5354fdccf9aa7e4eef93d21a`](https://github.com/hsoliwal/lucene/commit/1169cd2007d37c1e5354fdccf9aa7e4eef93d21a); no new documentation contribution is represented by this entry

### opennlp

[Public repository](https://github.com/hsoliwal/opennlp) · Parent: [apache/opennlp](https://github.com/apache/opennlp)

Apache OpenNLP natural-language-processing tools and supporting API/model workflows.

- **Documentation:** [Fork guide](https://github.com/hsoliwal/opennlp/blob/2607b298af01371d822b33615fdc6803106cfeef/FORK_GUIDE.md) · [Package inventory](https://github.com/hsoliwal/opennlp/blob/2607b298af01371d822b33615fdc6803106cfeef/PACKAGE_INVENTORY.md)
- **Reviewed scope:** 23 POM projects, 154 production package directories and 136 test package directories. One representative type per production package and selected span, adaptive-state, stream, model and spelling contracts.
- **Missing coverage:** Structural accounting is broader than semantic review. All-member/inline coverage, caller/test reconciliation, training/evaluation and runtime verification remain open; proposed M3 integration is not implemented by these docs.
- **Review:** [Draft PR #3](https://github.com/hsoliwal/opennlp/pull/3); documentation snapshot [`2607b298af01371d822b33615fdc6803106cfeef`](https://github.com/hsoliwal/opennlp/commit/2607b298af01371d822b33615fdc6803106cfeef)

### opennlp-models

[Public repository](https://github.com/hsoliwal/opennlp-models) · Parent: [apache/opennlp-models](https://github.com/apache/opennlp-models)

Apache OpenNLP model artifacts and model-validation/training support.

- **Documentation:** [Fork guide](https://github.com/hsoliwal/opennlp-models/blob/5e3f4841b5a37f75fcc98c94038286769e65ecb2/FORK_GUIDE.md) · [Model inventory](https://github.com/hsoliwal/opennlp-models/blob/5e3f4841b5a37f75fcc98c94038286769e65ecb2/MODEL_INVENTORY.md)
- **Reviewed scope:** 153 POM projects and 145 model-bearing artifacts; the sole Java package's ModelValidator API and source-observed validation limits.
- **Missing coverage:** Artifact/resource identity is not model accuracy or acceptance. Per-language evaluation, complete native documentation, downloads/training and validation execution remain open.
- **Review:** [Draft PR #2](https://github.com/hsoliwal/opennlp-models/pull/2); documentation snapshot [`5e3f4841b5a37f75fcc98c94038286769e65ecb2`](https://github.com/hsoliwal/opennlp-models/commit/5e3f4841b5a37f75fcc98c94038286769e65ecb2)

### opennlp-addons

[Public repository](https://github.com/hsoliwal/opennlp-addons) · Parent: [apache/opennlp-addons](https://github.com/apache/opennlp-addons)

Apache OpenNLP add-on manual/documentation snapshot.

- **Documentation:** [Fork guide](https://github.com/hsoliwal/opennlp-addons/blob/a45c9097c7297db60d3e6eab4c65932298f9b939/FORK_GUIDE.md)
- **Reviewed scope:** Both POM projects and all five DocBook manual chapters were inspected. The snapshot contains no executable add-on modules or Java packages.
- **Missing coverage:** Manual build/rendering and example behavior were not executed. Historical manual coverage does not establish that an add-on implementation is present or validated.
- **Review:** [Draft PR #1](https://github.com/hsoliwal/opennlp-addons/pull/1); documentation snapshot [`a45c9097c7297db60d3e6eab4c65932298f9b939`](https://github.com/hsoliwal/opennlp-addons/commit/a45c9097c7297db60d3e6eab4c65932298f9b939)

### opennlp-sandbox

[Public repository](https://github.com/hsoliwal/opennlp-sandbox) · Parent: [apache/opennlp-sandbox](https://github.com/apache/opennlp-sandbox)

Apache OpenNLP experimental projects and examples, retaining the upstream non-production boundary.

- **Documentation:** [Fork guide](https://github.com/hsoliwal/opennlp-sandbox/blob/67ea350bd76a6acb3ea3c3f4e9858d93244ac52b/FORK_GUIDE.md) · [Package inventory](https://github.com/hsoliwal/opennlp-sandbox/blob/67ea350bd76a6acb3ea3c3f4e9858d93244ac52b/PACKAGE_INVENTORY.md)
- **Reviewed scope:** 21 Maven projects, 74 production and 35 test Java package directories, plus Python examples/cloud tooling. Selected storage, coreference, WSD/summary and gRPC boundaries.
- **Missing coverage:** Representative package summaries are partial; complete APIs/native docs, historical README reconciliation, server/client behavior, training and production suitability remain unverified.
- **Review:** [Draft PR #2](https://github.com/hsoliwal/opennlp-sandbox/pull/2); documentation snapshot [`67ea350bd76a6acb3ea3c3f4e9858d93244ac52b`](https://github.com/hsoliwal/opennlp-sandbox/commit/67ea350bd76a6acb3ea3c3f4e9858d93244ac52b)

## CI evidence

The following run conclusions were re-read from GitHub on 14 October 2026 and matched to the documentation snapshots above. They describe observed CI, not checks executed to write this index. A later commit needs its own evidence; in particular, adding this index advances M3JDK21 PR #884 beyond its `eb6c8f7` documentation snapshot.

| Repository / documentation snapshot | Observed result | What the result supports and leaves open |
| --- | --- | --- |
| ChromeLLM `c469718a` | [Java/native workflow](https://github.com/hsoliwal/ChromeLLM/actions/runs/38597205228): success | Java and Ubuntu/Windows native lanes completed. This is not Windows ChromeML model inference, OpenCL device qualification, full media parity or complete API semantics. |
| codexpro `730bc0a9` | [CI](https://github.com/hsoliwal/codexpro/actions/runs/38606395733): failure | All four Ubuntu/Windows × Node 20/24 jobs failed at dependency audit. Package smoke passed before audit. This is not a green build or dependency remediation. |
| gitea `4c5d87ac` | [compliance](https://github.com/hsoliwal/gitea/actions/runs/38609703745), [e2e workflow](https://github.com/hsoliwal/gitea/actions/runs/38609703783), [database workflow](https://github.com/hsoliwal/gitea/actions/runs/38609703915), [container dry-run workflow](https://github.com/hsoliwal/gitea/actions/runs/38609703856): success | Change detection and lint-on-demand ran. Code, end-to-end, database and container jobs were skipped by path filters. Workflow success therefore does not certify those skipped suites. |
| synexia-tornadovm `ca94352f` | [No-GPU reflection/CUDA codegen](https://github.com/hsoliwal/synexia-tornadovm/actions/runs/38590386872) and [CPU-model](https://github.com/hsoliwal/synexia-tornadovm/actions/runs/38590387115): success | Host CPU/reflection/code-generation evidence; not GPU hardware execution, all backend/library tests or full runtime acceptance. |
| rewrite `a6f3e685` | [CI](https://github.com/hsoliwal/rewrite/actions/runs/38586301753): startup_failure | No jobs were returned for the failed run. The available run evidence does not identify the startup cause or establish a passing build. |
| M3jdk21 `eb6c8f7d` | [Snapshot commit](https://github.com/hsoliwal/M3jdk21/commit/eb6c8f7da7e1b274f03b079d16c208bf634e36f6): zero check runs and zero status contexts; combined status pending | No passing CI claim for this snapshot. Check the live [PR #884](https://github.com/hsoliwal/M3jdk21/pull/884) separately for later commits. |
| Other indexed repositories | No exact-head passing result is established by this index | Read the linked draft's checks and its explicit verification limits. Historical badges, source test files and absent contexts do not substitute for a revision-matched result. |

## Continuing from this index

1. Open the repository's pinned guide and its coverage ledger; identify the actual project/module/package unit and source revision before extending a claim.
2. For an inventory-only or partial entry, read the remaining source bodies, native documentation, callers and test definitions. Preserve generated, fixture, vendor and external-submodule boundaries rather than removing them from the accounting.
3. Keep source semantics, documentation review, and executed verification separate. Record exact commit, environment, command, outcome and skipped gates for any new execution evidence.
4. Retain each repository's original upstream attribution, notices, contribution rules and per-file licensing. Inclusion here does not transfer ownership or grant new rights.
5. Refresh this index only from verified public repository/document/PR facts, keeping earlier source pins intact. An updated navigation row does not close the underlying coverage gaps.

No dependency installation, repository script, build, test, model inference, GPU execution or benchmark was run to create this index. This Markdown navigation addition does not alter source, build/workflow definitions, licenses, runtime gates or existing proof records.
