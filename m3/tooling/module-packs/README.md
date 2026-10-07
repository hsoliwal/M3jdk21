# M3 module-pack inventory and image assembly

Status: implementation packet following COMMUNITY-0 / PR #51. No upstream JavaFX or
whole-JDK admission is implied by a successful local diagnostics fixture.

## Contract and authority

This TOOLING_PACK consumes real Java-21 JAR/JMOD descriptors, checks selected dependency
closure and builds opt-in images. It does not install into JAVA_HOME or download at runtime.
The host JDK and third-party archives remain read-only. All generated work stays in a new,
explicit output directory. An occupied output is refused; failed runs are retained as evidence.

The source authority is m3/docs/community-capability-admission.md. The backport and
name-mapping authorities remain unchanged. The new exported tooling API has LIBRARY_API authority; reactor registration has
MULTI_MODULE authority. These are additive, approved tooling surfaces, not FILE-only
refactors. Existing JDK and library contracts remain locked.

## Semantic atoms and pattern/IOP roles

- M3ModuleArtifact: immutable descriptor/provenance value, InventoryRecord role.
- M3ModuleInspector: archive, effective-class-version, signature, native/resource and
  checksum inspection; Adapter/Recognizer role. No source rewriting or class loading.
- M3ModuleGraph: named-root closure, static-requires handling, cycle and split-package
  validation; Specification/GraphValidator role.
- M3ModuleInventory: deterministic CLI projection, Facade role.
- M3InventoryModulePackRecipe: read-only OpenRewrite DataTable projection of the same
  kernel; Adapter role, not a second module registry.
- build_pack.py: bounded assembler using the selected JDK's javac/jar/jmod/jlink tools.
  No algorithm implementation or third-party identity is copied or renamed.

A valid descriptor is necessary but not proof of native ABI, reflection or application
compatibility. Native paths and service declarations are inventory facts, not admission.
Signed modular JARs fail closed rather than stripping signatures. Automatic modules are
not linkable. Static requires do not silently expand the runtime closure.

## Run

Maven control: `mvn -B -ntp -f m3/tooling/module-packs/pom.xml clean verify`.
Kernel requires Java 21 only; the separate recipe module carries OpenRewrite dependencies.
JaCoCo gates >=99% line and branch coverage; no exclusions. Do not claim the gate passed
unless the report and successful check actually exist.

Offline executable checks:
`python3 m3/tooling/module-packs/verify.py --jdk /path/to/jdk21 --out /new/proof/dir`

Diagnostics image:
`python3 m3/tooling/module-packs/build_pack.py --jdk /path/to/jdk21 --out /new/image/dir`

JavaFX image:
add `--javafx-lock /path/to/reviewed-lock.json`. Lock schema/example is in
javafx-lock.example.json. Replace placeholders with reviewed artifacts; placeholders
are deliberately invalid. All external module bytes, source/licensing and target
platform must be pinned. The three required JMODs are javafx.base, javafx.graphics,
javafx.controls. Use a real display or execute the build under xvfb-run.

The assembler packages original M3 smoke/tool classes only. It does not redistribute
unreviewed JavaFX binaries or infer license approval from a download URL. It preserves
the upstream archive identities. The resulting local image must be rebuilt from
the actual M3JDK21 image for final M3 distribution admission.

## Proof and limits

The same standard-Java behavioral cases run from the offline harness and JUnit wrapper.
The harness is not called JUnit. Maven/JaCoCo/OpenRewrite execution is reported separately.
Diagnostic proof checks JMOD descriptions, graph closure, linked module availability,
JFR recording/readback, and native jcmd invocation. JavaFX proof launches the toolkit
and renders a control; missing artifacts/display/native libraries cause failure.

COMMUNITY-1 kernel, COMMUNITY-2 JavaFX assembler and COMMUNITY-4 diagnostics are tracked
separately. COMMUNITY-3 still requires real backport build/jtreg receipts. Dependency
download or build failures never turn into accepted candidates or zero community demand.
