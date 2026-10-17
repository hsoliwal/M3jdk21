<!-- SPDX-License-Identifier: Apache-2.0 -->
# m3/tooling/jmod-recipes — every vendored library as a JMOD, descriptors written by OpenRewrite

Design: `m3/docs/m3sdk/m3-parallel-vcpu-design.md` §6 (SDK packaging as JMODs).

| Step | Tool | Output |
|---|---|---|
| 1. Named modules | `tools/jmod_image.py`: `jar --describe-module` (with `--release 21` for descriptors under `META-INF/versions`) over every Maven-built vendored jar; one version per module name; unresolved `requires` dropped | `JMOD_IMAGE.tsv` |
| 2. Split packages | `tools/split_packages.py`: packages per jar (release-21 view); merge groups | `SPLIT_PACKAGES.tsv`, `MERGE_GROUPS.tsv` |
| 3. Descriptor facts | `tools/descriptor_gen.py`: for jars without a descriptor, keep one version per artifact, drop jars whose packages a named module owns, merge each split-package group into one jar, then `jdeps --generate-module-info`. The jdeps output is normalized: `exports` of packages that contain only `package-info` are dropped, and so are `provides` clauses whose classes are absent | `DESCRIPTOR_MANIFEST.tsv` (module, artifacts, target, sha256), `DESCRIPTORS.tsv` |
| 4. Write descriptors | **OpenRewrite** `M3ModuleDescriptorRecipe` (a `ScanningRecipe` on the vendored rewrite-core 8.17.1) run by `M3ModuleDescriptorApply`. It writes each `module-info.java` into `m3/vendor/third_party-m3/descriptors/<module>/`. The upstream modules are never touched | receipt TSV (before/after sha256, `FIXED_POINT`) |
| 5. Package | `tools/jmod_pack.py`: multi-release jars are flattened to the release-21 view; each descriptor is compiled with `javac --patch-module`; then `jmod create`, `jlink java.se` + every vendored module, and `java --validate-modules` | `JMOD_PACK.tsv`, the image |

Recipe contract:
- A generated descriptor whose bytes differ from its pin is refused before anything is written.
- An absent target is created.
- A target that already holds the pinned bytes is left unchanged, so the second run makes 0 changes.
- A target with other bytes is never overwritten; the run reports `REFUSED` and exits with 3.

Tests (`mvn -o verify`): generate then fixed point, drift refusal, tampered-input refusal.

Evidence (`evidence/`, 2026-10-12, JDK 21.0.9, Windows):

| Item | Result |
|---|---|
| Named vendored modules (published descriptors) | 54, 31 of them multi-release flattened to the release-21 view |
| Jars without a descriptor | 33 descriptors written by the recipe. Merges: `org.openrewrite` = rewrite-core + rewrite-test, and `org.openrewrite.java` = rewrite-java + rewrite-java-21 (JPMS forbids their shared packages) |
| Older lines left out of the image | antlr4-runtime 4.11.1, jna 5.14.0, tomlj 1.1.1, checker-qual 3.37.0, slf4j-api 1.7.36 (see R2 convergence) |
| Recipe | first run: 33 added and fixed point; re-applying identical facts: 0 changes; changed facts on the old layer: `REFUSED` |
| Image | 119 modules = `java.se` closure + 87 vendored JMODs; `java --validate-modules` rc 0; 253 MB |

The jlink'ed image itself is a build output and is not committed.

## Safety repair after source-custody restoration

The 52 original PR #788 blobs and immutable historical evidence are pinned in
`evidence/SOURCE_CUSTODY_788.json`. The safety changes are a separate delta; the
2026-10-12 image evidence above does not qualify the changed tools.

All three Python entrypoints now require `--output-root` and
`--output-budget-bytes`. The owner must already exist, and `--out` must be a fresh,
absolute strict child. Occupied output is preserved and refused; nothing is
recursively deleted. Every effect checks the actual destination's free space
with an 8 GiB reserve after the declared allowance (at most 512 MiB). Archive
estimates charge expanded files and directories using the destination's measured
allocation unit; path escapes, Git paths and archive links are refused before
extraction. Full image construction may need a larger measured allowance and
remains unqualified under the current limit.

Windows uses the explicit `K:/codex-work/agent-temp` output lane required by this
workstation's storage policy. Other platforms use the caller's explicit owned
directory and their filesystem allocation unit; there is no alternate-drive
fallback. `JAVA_HOME` selects the JDK; the defaults are the original Windows JDK
21 path and `/usr/lib/jvm/java-21-openjdk-amd64` on other platforms. Module paths
use the platform separator. The POSIX policy branch is discriminated on Windows;
execution on a non-Windows runtime remains pending.

Descriptor manifests are bounded to 128 rows / 512 KiB and only name
`m3/vendor/third_party-m3/descriptors/<module>/module-info.java`. All targets,
generated payload size, reparse ancestors, and a fresh external TSV receipt are
validated before descriptor writes. The apply tool reserves 4 MiB, limits total
descriptor payload to 512 KiB, charges new files/directories by the destination's
measured allocation unit, and creates new files without truncating existing
ones. Original fixed point and drift refusal behavior remain covered by the
actual recipe tests. Concurrent hostile path replacement is not qualified by
these local filesystem checks.

Run the Python discriminators with `python -B tools/test_output_safety.py` and the
existing Java recipe tests using the exact pinned providers. Standard cached
rewrite-core execution is recorded separately from the source-suffixed Maven
provider closure. Full Maven/source-suffixed closure, jlink/image, non-Windows and
current-master whole-source fit remain explicit pending gates.
