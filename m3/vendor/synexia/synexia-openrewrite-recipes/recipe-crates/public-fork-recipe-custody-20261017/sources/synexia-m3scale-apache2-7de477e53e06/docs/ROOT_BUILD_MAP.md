# Root Build Map

Baseline `de0d06ce13d502b279a767c5636bc4154c21585f`. This is an exhaustive Maven manifest inventory for the selected paths, not a successful effective reactor or API-coverage claim. Descriptions below are **declared build intent**, not runtime verification. The complete manifest is authoritative for dependency versions, executions, profiles and exclusions. All fetched manifest byte sizes match their pinned tree entries.

[Guide](REPOSITORY_GUIDE.md) · [Coverage](COVERAGE.md)

## pom.accelerator-adoption.xml

[Source manifest](../pom.accelerator-adoption.xml) · well-formed XML; Maven not run

- Artifact: `com.synexia.m3scale:m3-accelerator-adoption-verification`; packaging: jar
- Declared intent: Isolated Java 21 proof that existing M3 hashing hot paths preserve semantics with the optional accelerator present.
- Source configuration: `src/main/java`, `accelerator/src/main/java`
- Release values appearing in configuration: `21`
- Include selectors: `com/synexia/m3scale/acceleration/**/*.java`, `com/synexia/m3scale/accelerator/**/*.java`, `com/synexia/m3scale/atom/Hashing.java`, `com/synexia/m3scale/atom/FileAtom.java`, `com/synexia/m3scale/atom/SimilarityCluster.java`, `com/synexia/m3scale/atom/SimHashClusterer.java`, `com/synexia/m3scale/refactor/Hashing.java`, `com/synexia/m3scale/refactor/SimHash64.java`; resource includes and compiler includes are distinguished in the linked XML

## pom.accelerator.xml

[Source manifest](../pom.accelerator.xml) · well-formed XML; Maven not run

- Artifact: `com.synexia.m3scale:m3-java-accelerator`; packaging: jar
- Declared intent: Verified Java 21 batch hashing and SimHash primitives with CPU authority and optional TornadoVM/JCuda CUDA backends.
- Source configuration: `accelerator/src/main/java`, `accelerator/src/tornadovm/java`, `accelerator/src/jcuda/java`
- Release values appearing in configuration: `21`
- Profiles: `tornadovm`, `jcuda`, `gpu`

## pom.atomize.xml

[Source manifest](../pom.atomize.xml) · well-formed XML; Maven not run

- Artifact: `com.synexia.m3scale:synexia-m3scale`; packaging: jar (default)
- Source configuration: `src/main/java`
- Release values appearing in configuration: `21`
- Include selectors: `com/synexia/m3scale/atom/**/*.java`; resource includes and compiler includes are distinguished in the linked XML

## pom.atomizer.xml

[Source manifest](../pom.atomizer.xml) · well-formed XML; Maven not run

- Artifact: `com.synexia.m3scale:m3-atomizer-maven-plugin`; packaging: maven-plugin
- Declared intent: CPU-only deterministic Java atomization, structural clustering, donor-backed convergence, entropy/volume, and contract proof gates.
- Source configuration: `src/main/java`, `${project.basedir}/src/main/java/com/synexia/m3scale/atom`, `${project.basedir}/src/main/java/com/synexia/m3scale/intern`
- Release values appearing in configuration: `21`
- Include selectors: `com/synexia/m3scale/atom/**/*.java`, `com/synexia/m3scale/intern/**/*.java`; resource includes and compiler includes are distinguished in the linked XML

## pom.bigtable.xml

[Source manifest](../pom.bigtable.xml) · well-formed XML; Maven not run

- Artifact: `com.synexia.m3scale:m3-bigtable-inventory`; packaging: jar (default)
- Source configuration: `src/main/java`
- Release values appearing in configuration: `21`
- Include selectors: `com/synexia/m3scale/inventory/FileEntry.java`, `com/synexia/m3scale/catalog/IGlobalFileCatalog.java`, `com/synexia/m3scale/progress/M3ProgressMonitor.java`, `com/synexia/m3scale/inventory/table/**/*.java`; resource includes and compiler includes are distinguished in the linked XML

## pom.camel-gpu-bridge.xml

[Source manifest](../pom.camel-gpu-bridge.xml) · well-formed XML; Maven not run

- Artifact: `com.synexia.m3scale:m3-camel-gpu-bridge-check`; packaging: jar (default)
- Source configuration: `${project.basedir}/src/main/java`, `${project.basedir}/src/camel-gpu/java`, `${project.basedir}/src/camel-gpu-test/java`
- Release values appearing in configuration: `21`
- Include selectors: `com/synexia/m3scale/compute/gpu/**/*.java`; resource includes and compiler includes are distinguished in the linked XML

## pom.chrome-workflow.xml

[Source manifest](../pom.chrome-workflow.xml) · well-formed XML; Maven not run

- Artifact: `com.synexia.m3scale:m3-chrome-workflow`; packaging: jar (default)
- Source configuration: `${project.basedir}`
- Release values appearing in configuration: `21`
- Include selectors: `LICENSE`, `NOTICE`, `integrations/chrome-workflow/src/main/java/com/synexia/m3scale/merge/ChromeAiInventorySource.java`, `integrations/chrome-workflow/src/main/java/com/synexia/m3scale/merge/ChromeAiNativeWorkflow.java`, `integrations/chrome-workflow/src/main/java/com/synexia/m3scale/merge/ChromeAiWorkflow.java`, `integrations/chrome-workflow/src/main/java/com/synexia/m3scale/merge/ChromeAiWorkflowMain.java`, `integrations/chrome2api/src/main/java/com/synexia/cpu/camel/fabric/chrome/Chrome2ApiAudioGrant.java`, `integrations/chrome2api/src/main/java/com/synexia/cpu/camel/fabric/chrome/Chrome2ApiClient.java`, `integrations/chrome2api/src/main/java/com/synexia/cpu/camel/fabric/chrome/Chrome2ApiCodec.java`, `integrations/chrome2api/src/main/java/com/synexia/cpu/camel/fabric/chrome/Chrome2ApiMessage.java`, `integrations/chrome2api/src/main/java/com/synexia/cpu/camel/fabric/chrome/Chrome2ApiOptions.java`, `integrations/chrome2api/src/main/java/com/synexia/cpu/camel/fabric/chrome/Chrome2ApiReceipt.java`, `integrations/chrome2api/src/main/java/com/synexia/cpu/camel/fabric/chrome/Chrome2ApiResponses.java`, `integrations/chrome2api/src/main/java/com/synexia/cpu/camel/fabric/chrome/Chrome2ApiTextModel.java`, `integrations/chrome2api/src/main/java/com/synexia/cpu/camel/fabric/chrome/Chrome2ApiTransport.java`, `integrations/chrome2api/src/main/java/com/synexia/m3scale/chrome2api/Chrome2ApiAssets.java`, `integrations/chrome2api/src/main/java/com/synexia/m3scale/chrome2api/Chrome2ApiCli.java`, `integrations/chrome2api/src/main/java/com/synexia/m3scale/chrome2api/Chrome2ApiHelper.java`, `integrations/chrome2api/src/main/java/com/synexia/nanobrowser/Json.java`, `integrations/chrome2api/src/main/java/com/synexia/nanobrowser/TextModel.java`, `src/main/java/com/synexia/m3scale/inventory/FileEntry.java`, `src/main/java/com/synexia/m3scale/merge/ChromeAiDocument.java`, `src/main/java/com/synexia/m3scale/merge/ChromeAiDocumentHost.java`, `src/main/java/com/synexia/m3scale/merge/ChromeAiDocumentMain.java`, `src/main/java/com/synexia/m3scale/merge/ChromeAiPresets.java`, `src/main/java/com/synexia/m3scale/merge/ChromeAiQuotaAudit.java`, `src/main/java/com/synexia/m3scale/merge/ChromeAiQuotaPlanner.java`, `src/main/java/com/synexia/m3scale/merge/ChromeAiSchema.java`, `src/main/java/com/synexia/m3scale/merge/ChromeAiSummaryChain.java`, `src/main/java/com/synexia/m3scale/merge/LlamaCppJson.java`; resource includes and compiler includes are distinguished in the linked XML

## pom.chrome2api.xml

[Source manifest](../pom.chrome2api.xml) · well-formed XML; Maven not run

- Artifact: `com.synexia.m3scale:m3-chrome2api-helper`; packaging: jar (default)
- Source configuration: no explicit source-directory override in this manifest; Maven defaults or inherited configuration apply
- Release values appearing in configuration: `21`
- Include selectors: `com/synexia/m3scale/merge/ConflictRegion.java`, `com/synexia/m3scale/merge/ai/Chrome2ApiPatchAdvisor.java`, `com/synexia/m3scale/merge/ai/PatchAdvice.java`, `com/synexia/m3scale/merge/ai/PatchAdviceDecision.java`, `com/synexia/m3scale/merge/ai/PatchAdvisor.java`, `com/synexia/m3scale/merge/ai/PatchDifferenceAtom.java`, `com/synexia/m3scale/merge/ai/PatchHashing.java`, `com/synexia/m3scale/merge/ai/PatchPromptRenderer.java`, `com/synexia/m3scale/runtime/forge/Chrome2ApiCli.java`, `com/synexia/m3scale/runtime/forge/Chrome2ApiGuardedClient.java`, `com/synexia/m3scale/runtime/forge/Chrome2ApiSetup.java`, `com/synexia/m3scale/runtime/forge/ForgeJson.java`; resource includes and compiler includes are distinguished in the linked XML

## pom.contract-ladder.xml

[Source manifest](../pom.contract-ladder.xml) · well-formed XML; Maven not run

- Artifact: `com.synexia.m3scale:m3-contract-ladder-maven-plugin`; packaging: maven-plugin
- Declared intent: Java 21 FILE to API mechanical proof ladder with deterministic caller/callee impact.
- Source configuration: `src/main/java`, `${project.basedir}/m3-maven-plugin/src/main/java`
- Release values appearing in configuration: `21`
- Include selectors: `com/synexia/m3scale/refactor/**/*.java`, `com/synexia/m3scale/progress/**/*.java`, `com/synexia/m3scale/maven/AbstractM3Mojo.java`, `com/synexia/m3scale/maven/MojoProgressMonitor.java`, `com/synexia/m3scale/maven/M3ScaleMojo.java`; resource includes and compiler includes are distinguished in the linked XML

## pom.convergence.donors.xml

[Source manifest](../pom.convergence.donors.xml) · well-formed XML; Maven not run

- Artifact: `com.synexia.m3scale:m3-convergence-maven-plugin`; packaging: maven-plugin
- Declared intent: Donor-backed Lucene and PMD CPD candidate discovery with deterministic M3 rescoring.
- Source configuration: `src/main/java`
- Release values appearing in configuration: `21`
- Include selectors: `com/synexia/m3scale/atom/**/*.java`; resource includes and compiler includes are distinguished in the linked XML

## pom.convergence.xml

[Source manifest](../pom.convergence.xml) · well-formed XML; Maven not run

- Artifact: `com.synexia.m3scale:m3-convergence-validation`; packaging: jar (default)
- Source configuration: `src/main/java`
- Release values appearing in configuration: `21`
- Include selectors: `com/synexia/m3scale/inventory/FileEntry.java`, `com/synexia/m3scale/catalog/IGlobalFileCatalog.java`, `com/synexia/m3scale/catalog/InMemoryCatalog.java`, `com/synexia/m3scale/dedup/ClusterType.java`, `com/synexia/m3scale/dedup/DuplicateCluster.java`, `com/synexia/m3scale/dedup/DedupEngine.java`, `com/synexia/m3scale/convergence/**/*.java`, `com/synexia/m3scale/merge/**/*.java`; resource includes and compiler includes are distinguished in the linked XML

## pom.eclipse-help.xml

[Source manifest](../pom.eclipse-help.xml) · well-formed XML; Maven not run

- Artifact: `com.synexia.m3scale:eclipse-help-webapp`; packaging: war
- Source configuration: `${project.basedir}/src/main/java`, `${project.basedir}/src/servlet/java`
- Release values appearing in configuration: `21`
- Include selectors: `com/synexia/m3scale/guide/eclipse/XmlSupport.java`, `com/synexia/m3scale/guide/eclipse/help/**/*.java`; resource includes and compiler includes are distinguished in the linked XML

## pom.gitea-donors.xml

[Source manifest](../pom.gitea-donors.xml) · well-formed XML; Maven not run

- Artifact: `com.synexia.m3scale:m3-gitea-donor-runtime`; packaging: jar (default)
- Source configuration: `src/main/java`
- Release values appearing in configuration: `21`
- Include selectors: `com/synexia/m3scale/runtime/**/*.java`, `com/synexia/m3scale/git/**/*.java`, `com/synexia/m3scale/progress/**/*.java`; resource includes and compiler includes are distinguished in the linked XML

## pom.guide.xml

[Source manifest](../pom.guide.xml) · well-formed XML; Maven not run

- Artifact: `com.synexia.m3scale:m3-guided-dag-validation`; packaging: jar (default)
- Source configuration: `src/main/java`
- Release values appearing in configuration: `21`
- Include selectors: `com/synexia/m3scale/guide/**/*.java`, `com/synexia/m3scale/progress/**/*.java`; resource includes and compiler includes are distinguished in the linked XML

## pom.indexast.xml

[Source manifest](../pom.indexast.xml) · well-formed XML; Maven not run

- Artifact: `com.synexia.m3scale:m3-index-ast-validation`; packaging: jar (default)
- Source configuration: `src/indexast/java`, `${project.basedir}/src/main/java`
- Release values appearing in configuration: `21`
- Include selectors: `com/synexia/m3scale/indexast/partial/**/*.java`, `com/synexia/m3scale/immutable/**/*.java`, `com/synexia/m3scale/performance/StringTokenInterner.java`, `com/synexia/m3scale/progress/M3ProgressMonitor.java`, `com/synexia/m3scale/refactor/Hashing.java`, `com/synexia/m3scale/reuse/ReuseArtifact.java`, `com/synexia/m3scale/reuse/ReuseArtifactKey.java`, `com/synexia/m3scale/reuse/ReuseArtifactStore.java`, `com/synexia/m3scale/reuse/FileReuseArtifactStore.java`, `com/synexia/m3scale/reuse/SqliteReuseArtifactStore.java`, `com/synexia/m3scale/reuse/TieredReuseArtifactStore.java`, `com/synexia/m3scale/transcompile/partial/PartialAst.java`, `com/synexia/m3scale/transcompile/partial/PartialAstDiagnostic.java`, `com/synexia/m3scale/transcompile/partial/PartialAstKind.java`, `com/synexia/m3scale/transcompile/partial/PartialAstNode.java`, `com/synexia/m3scale/transcompile/partial/PartialAstStatus.java`, `com/synexia/m3scale/transcompile/partial/SourceRange.java`; resource includes and compiler includes are distinguished in the linked XML

## pom.inventory-merge.xml

[Source manifest](../pom.inventory-merge.xml) · well-formed XML; Maven not run

- Artifact: `com.synexia.m3scale:m3-inventory-merge`; packaging: jar (default)
- Source configuration: no explicit source-directory override in this manifest; Maven defaults or inherited configuration apply
- Release values appearing in configuration: `21`
- Include selectors: `com/synexia/m3scale/inventory/FileEntry.java`, `com/synexia/m3scale/catalog/IGlobalFileCatalog.java`, `com/synexia/m3scale/progress/M3ProgressMonitor.java`, `com/synexia/m3scale/inventory/table/**/*.java`, `com/synexia/m3scale/inventory/bridge/**/*.java`, `com/synexia/m3scale/merge/ConflictChoice.java`, `com/synexia/m3scale/merge/MergeWorkspace.java`, `com/synexia/m3scale/merge/GitMergeCandidates.java`, `com/synexia/m3scale/merge/MergeInventoryMatrix.java`, `com/synexia/m3scale/merge/Java21MergeGate.java`, `com/synexia/m3scale/merge/AcceptBothConsolidator.java`, `com/synexia/m3scale/merge/IdenticalMethodRepair.java`; resource includes and compiler includes are distinguished in the linked XML

## pom.inventory-wide.xml

[Source manifest](../pom.inventory-wide.xml) · well-formed XML; Maven not run

- Artifact: `com.synexia.m3scale:m3-wide-inventory-validation`; packaging: jar (default)
- Declared intent: Java 21 Bigtable/HBase-inspired inventory and Lucene search validation.
- Source configuration: `src/main/java`, `${project.basedir}/src/main/java/com/synexia/m3scale/inventory`, `${project.basedir}/src/main/java/com/synexia/m3scale/compute`, `${project.basedir}/src/main/java/com/synexia/m3scale/progress`, `${project.basedir}/m3-maven-plugin/src/main/java/com/synexia/m3scale/maven`, `m3-maven-plugin/src/main/java`
- Release values appearing in configuration: `21`
- Include selectors: `com/synexia/m3scale/inventory/**/*.java`, `com/synexia/m3scale/compute/**/*.java`, `com/synexia/m3scale/progress/**/*.java`, `com/synexia/m3scale/maven/AbstractM3Mojo.java`, `com/synexia/m3scale/maven/MojoProgressMonitor.java`, `com/synexia/m3scale/maven/InventoryBuildMojo.java`, `com/synexia/m3scale/maven/FingerprintPackMojo.java`, `com/synexia/m3scale/maven/InventorySearchMojo.java`, `com/synexia/m3scale/maven/InventoryCompactMojo.java`, `com/synexia/m3scale/maven/InventoryExportMojo.java`; resource includes and compiler includes are distinguished in the linked XML

## pom.inventory.xml

[Source manifest](../pom.inventory.xml) · well-formed XML; Maven not run

- Artifact: `com.synexia.m3scale:m3-inventory-index`; packaging: jar (default)
- Source configuration: `integrations/inventory/lucene/src/main/java`, `integrations/inventory/lucene/src/test/java`, `integrations/inventory/hbase/src/main/java`, `integrations/inventory/hadoop/src/main/java`, `integrations/inventory/spark/src/main/java`, `integrations/inventory/merge/src/test/java`, `integrations/inventory/merge/src/main/java`
- Release values appearing in configuration: `21`
- Profiles: `hbase`, `hadoop`, `spark`, `merge`
- Include selectors: `com/synexia/m3scale/inventory/FileEntry.java`, `com/synexia/m3scale/inventory/search/InventorySearchQuery.java`, `com/synexia/m3scale/inventory/search/InventorySearchResult.java`, `com/synexia/m3scale/inventory/search/CanonicalInventorySearch.java`, `com/synexia/m3scale/catalog/IGlobalFileCatalog.java`, `com/synexia/m3scale/catalog/index/**/*.java`, `com/synexia/m3scale/merge/AcceptBothConsolidator.java`, `com/synexia/m3scale/merge/GitMergeCandidates.java`, `com/synexia/m3scale/merge/MergeWorkspace.java`, `com/synexia/m3scale/merge/Java21MergeGate.java`, `com/synexia/m3scale/merge/ConflictChoice.java`, `com/synexia/m3scale/progress/M3ProgressMonitor.java`; resource includes and compiler includes are distinguished in the linked XML

## pom.mechanical.xml

[Source manifest](../pom.mechanical.xml) · well-formed XML; Maven not run

- Artifact: `com.synexia.m3scale:m3-mechanical-parent`; packaging: pom
- Declared intent: Standalone additive build; does not replace the existing Cognix parent or runtime.
- Source configuration: no explicit source-directory override in this manifest; Maven defaults or inherited configuration apply
- Release values appearing in configuration: `21`
- Declared module paths: `mechanical`, `mechanical-maven-plugin`; profile activation still matters

## pom.merge-ai.xml

[Source manifest](../pom.merge-ai.xml) · well-formed XML; Maven not run

- Artifact: `com.synexia.m3scale:m3-merge-ai-validation`; packaging: jar (default)
- Declared intent: Isolated Java 21 verification for deterministic patch atoms and optional Chrome Nano advice.
- Source configuration: `src/main/java`, `${project.basedir}/src/main/java/com/synexia/m3scale/merge`
- Release values appearing in configuration: `21`
- Include selectors: `com/synexia/m3scale/merge/**/*.java`; resource includes and compiler includes are distinguished in the linked XML

## pom.nano.xml

[Source manifest](../pom.nano.xml) · well-formed XML; Maven not run

- Artifact: `com.synexia.m3scale:m3-nano-patch-advisor`; packaging: jar (default)
- Source configuration: `src/main/java`
- Release values appearing in configuration: `21`
- Include selectors: `com/synexia/m3scale/merge/**/*.java`, `com/synexia/m3scale/nano/**/*.java`; resource includes and compiler includes are distinguished in the linked XML

## pom.neural-render.xml

[Source manifest](../pom.neural-render.xml) · well-formed XML; Maven not run

- Artifact: `com.synexia.m3scale:neural-render-optional`; packaging: jar
- Declared intent: Standalone Java 21 build for JNA and ONNX diffusion adapters.
- Source configuration: `${project.basedir}/src/main/java`, `${project.basedir}/src/jna/java`, `${project.basedir}/src/tornado/java`, `${project.basedir}/src/onnx/java`
- Release values appearing in configuration: `21`
- Profiles: `jna`, `tornado`, `onnx`, `onnx-cuda`
- Include selectors: `com/synexia/m3scale/neural/render/**/*.java`; resource includes and compiler includes are distinguished in the linked XML

## pom.reactor.xml

[Source manifest](../pom.reactor.xml) · well-formed XML; Maven not run

- Artifact: `com.synexia.m3scale:synexia-m3scale-reactor`; packaging: pom
- Declared intent:  Aggregates the core JAR, Maven plugin, and verified hybrid runtime without changing core artifact packaging. 
- Source configuration: no explicit source-directory override in this manifest; Maven defaults or inherited configuration apply
- Declared module paths: `m3-pageable-tree/pom.xml`, `m3-index-dag/pom.xml`, `m3-index-string/pom.xml`, `m3-index-object/pom.xml`, `m3-index-ast/pom.xml`, `gpu-runtime/pom.xml`, `m3-index-ast-accelerator/pom.xml`, `m3-index-framework/pom.xml`, `m3-immutable-pool/pom.xml`, `pom.xml`, `m3-maven-plugin/pom.xml`, `hybrid-runtime/pom.xml`, `m3-object-projection/pom.xml`, `m3-object-signal/pom.xml`; profile activation still matters

## pom.refactor-plugin.xml

[Source manifest](../pom.refactor-plugin.xml) · well-formed XML; Maven not run

- Artifact: `com.synexia.m3scale:m3-refactor-maven-plugin`; packaging: maven-plugin
- Declared intent: Java 21 compiler-first mechanical refactoring with contract gates and structural clustering.
- Source configuration: `src/main/java`
- Release values appearing in configuration: `21`
- Include selectors: `com/synexia/m3scale/refactor/**/*.java`; resource includes and compiler includes are distinguished in the linked XML

## pom.refactor.xml

[Source manifest](../pom.refactor.xml) · well-formed XML; Maven not run

- Artifact: `com.synexia.m3scale:synexia-m3scale`; packaging: jar (default)
- Source configuration: `src/main/java`
- Release values appearing in configuration: `21`
- Include selectors: `com/synexia/m3scale/progress/**/*.java`, `com/synexia/m3scale/refactor/**/*.java`, `com/synexia/m3scale/merge/**/*.java`, `com/synexia/m3scale/nano/**/*.java`; resource includes and compiler includes are distinguished in the linked XML

## pom.related-superset.xml

[Source manifest](../pom.related-superset.xml) · well-formed XML; Maven not run

- Artifact: `com.synexia.m3scale:m3-related-chat-superset-validation`; packaging: jar (default)
- Declared intent: Java 21 integration proof for atomization, convergence, advisory atoms, merge repair and progress.
- Source configuration: `src/main/java`, `${project.basedir}/src/main/java/com/synexia/m3scale/atom`, `${project.basedir}/src/main/java/com/synexia/m3scale/advisor`, `${project.basedir}/src/main/java/com/synexia/m3scale/progress`, `${project.basedir}/src/main/java/com/synexia/m3scale/merge`, `${project.basedir}/src/main/java/com/synexia/m3scale/transpile`
- Release values appearing in configuration: `21`
- Include selectors: `com/synexia/m3scale/atom/**/*.java`, `com/synexia/m3scale/advisor/**/*.java`, `com/synexia/m3scale/progress/M3ProgressMonitor.java`, `com/synexia/m3scale/progress/LoggingProgressMonitor.java`, `com/synexia/m3scale/merge/**/*.java`, `com/synexia/m3scale/transpile/**/*.java`, `com/synexia/m3scale/runtime/git/**/*.java`; resource includes and compiler includes are distinguished in the linked XML

## pom.runtime-truth.xml

[Source manifest](../pom.runtime-truth.xml) · well-formed XML; Maven not run

- Artifact: `com.synexia.m3scale:m3-runtime-truth-proof`; packaging: jar (default)
- Source configuration: `src/main/java`
- Release values appearing in configuration: `21`
- Include selectors: `com/synexia/m3scale/M3Runtime.java`, `com/synexia/m3scale/axiom/Axiom.java`, `com/synexia/m3scale/axiom/AxiomViolation.java`, `com/synexia/m3scale/catalog/IGlobalFileCatalog.java`, `com/synexia/m3scale/inventory/FileEntry.java`, `com/synexia/m3scale/packet/*.java`, `com/synexia/m3scale/pass/*.java`, `com/synexia/m3scale/progress/*.java`, `com/synexia/m3scale/report/OutputArtifact.java`, `com/synexia/m3scale/report/ReportWriter.java`, `com/synexia/m3scale/verify/*.java`; resource includes and compiler includes are distinguished in the linked XML

## pom.tornado.xml

[Source manifest](../pom.tornado.xml) · well-formed XML; Maven not run

- Artifact: `com.synexia.m3scale:m3-cpu-tornado-validation`; packaging: jar (default)
- Source configuration: `src/main/java`, `m3-tornado-accelerator/src/main/java`, `m3-tornado-accelerator/src/test/java`
- Release values appearing in configuration: `21`
- Include selectors: `com/synexia/m3scale/compute/**/*.java`, `com/synexia/m3scale/progress/M3ProgressMonitor.java`, `com/synexia/m3scale/tornado/**/*.java`; resource includes and compiler includes are distinguished in the linked XML
- Exclude selectors: `**/TornadoRuntimeSmoke.java`

## pom.transcompile-antlr4.xml

[Source manifest](../pom.transcompile-antlr4.xml) · well-formed XML; Maven not run

- Artifact: `com.synexia.m3scale:synexia-m3scale-partial-antlr4`; packaging: jar (default)
- Source configuration: `src/antlr4/java`, `${project.basedir}/src/antlr4/antlr`, `${project.basedir}/src/main/java`, `${project.basedir}/m3-index-string/src/main/java`, `${project.basedir}/mechanical/src/main/java`
- Release values appearing in configuration: `21`
- Include selectors: `com/synexia/m3scale/indexstring/**/*.java`, `com/synexia/m3scale/immutable/**/*.java`, `com/synexia/m3scale/intern/**/*.java`, `com/synexia/m3scale/progress/**/*.java`, `com/synexia/m3scale/mechanical/Digests.java`, `com/synexia/m3scale/mechanical/transpile/*.java`, `com/synexia/m3scale/refactor/Hashing.java`, `com/synexia/m3scale/transcompile/*.java`, `com/synexia/m3scale/performance/**/*.java`, `com/synexia/m3scale/refactor/SimHash64.java`, `com/synexia/m3scale/reuse/CompactAstShapePool.java`, `com/synexia/m3scale/reuse/PackedPartialAst.java`, `com/synexia/m3scale/reuse/ReuseArtifact.java`, `com/synexia/m3scale/reuse/ReuseArtifactKey.java`, `com/synexia/m3scale/reuse/ReuseArtifactStore.java`, `com/synexia/m3scale/reuse/SqliteReuseArtifactStore.java`, `com/synexia/m3scale/reuse/FileReuseArtifactStore.java`, `com/synexia/m3scale/reuse/TieredReuseArtifactStore.java`, `com/synexia/m3scale/transcompile/IntermediateRepresentation.java`, `com/synexia/m3scale/transcompile/LanguageId.java`, `com/synexia/m3scale/transcompile/TranscompileDocument.java`, `com/synexia/m3scale/transcompile/TranscompileFrontend.java`, `com/synexia/m3scale/transcompile/partial/**/*.java`, `com/synexia/m3scale/transcompile/antlr4/**/*.java`; resource includes and compiler includes are distinguished in the linked XML

## pom.transcompile-jdt.xml

[Source manifest](../pom.transcompile-jdt.xml) · well-formed XML; Maven not run

- Artifact: `com.synexia.m3scale:synexia-m3scale-partial-jdt`; packaging: jar (default)
- Source configuration: `src/jdt/java`, `${project.basedir}/src/main/java`
- Release values appearing in configuration: `21`
- Include selectors: `com/synexia/m3scale/intern/**/*.java`, `com/synexia/m3scale/progress/**/*.java`, `com/synexia/m3scale/refactor/Hashing.java`, `com/synexia/m3scale/transcompile/partial/**/*.java`, `com/synexia/m3scale/transcompile/jdt/**/*.java`; resource includes and compiler includes are distinguished in the linked XML

## pom.transcompile.xml

[Source manifest](../pom.transcompile.xml) · well-formed XML; Maven not run

- Artifact: `com.synexia.m3scale:synexia-m3scale`; packaging: jar (default)
- Source configuration: `src/main/java`, `${project.basedir}/m3-index-string/src/main/java`, `${project.basedir}/src/indexast/java`, `${project.basedir}/m3-index-string/src/test/java`
- Release values appearing in configuration: `21`
- Include selectors: `com/synexia/m3scale/indexstring/**/*.java`, `com/synexia/m3scale/indexast/**/*.java`, `com/synexia/m3scale/immutable/**/*.java`, `com/synexia/m3scale/intern/**/*.java`, `com/synexia/m3scale/performance/**/*.java`, `com/synexia/m3scale/progress/**/*.java`, `com/synexia/m3scale/refactor/**/*.java`, `com/synexia/m3scale/reuse/**/*.java`, `com/synexia/m3scale/transcompile/**/*.java`; resource includes and compiler includes are distinguished in the linked XML
- Exclude selectors: `**/TranscompileSelfTestMain.java`

## pom.transpile.xml

[Source manifest](../pom.transpile.xml) · well-formed XML; Maven not run

- Artifact: `com.synexia.m3scale:m3-transcompile-validation`; packaging: jar (default)
- Source configuration: `src/main/java`
- Release values appearing in configuration: `21`
- Include selectors: `com/synexia/m3scale/transpile/**/*.java`; resource includes and compiler includes are distinguished in the linked XML

## pom.transpiler.xml

[Source manifest](../pom.transpiler.xml) · well-formed XML; Maven not run

- Artifact: `com.synexia.m3scale:m3-mechanical-transpiler`; packaging: jar
- Declared intent: Deterministic Java 21 source-to-source compiler fabric with pinned external donor adapters.
- Source configuration: no explicit source-directory override in this manifest; Maven defaults or inherited configuration apply
- Release values appearing in configuration: `21`
- Include selectors: `com/synexia/m3scale/transpile/**/*.java`; resource includes and compiler includes are distinguished in the linked XML

## pom.xml

[Source manifest](../pom.xml) · well-formed XML; Maven not run

- Artifact: `com.synexia.m3scale:synexia-m3scale`; packaging: jar
- Declared intent: M3-SCALE v3.0 — Mechanical \| Memory-Safe \| Multi-Pass \| Horizontal Scale runtime. Axiom enforcement, task packet contracts, inventory/catalog/dedup, deterministic sharding, multi-pass refinement, verify gates, and output contract generation.
- Parent locator: `../cognix-parent/pom.xml` (resolution not verified)
- Source configuration: `src/main/java`, `${project.basedir}/src/tornado/java`, `${project.build.directory}/generated-sources/tornado`, `${project.basedir}/src/tornado-test/java`, `${project.basedir}/src/camel-gpu/java`
- Release values appearing in configuration: `21`
- Declared module paths: `m3-tornado-accelerator`; profile activation still matters
- Profiles: `m3-tooling-superset`, `gpu-tornado`, `camel-gpu-bridge`, `m3-tornado-crc`

