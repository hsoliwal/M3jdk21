# M3JDK21 mobile UI target: SWT, Swing and Skia (design and implementation boundary)

Status: **source-audited migration plan; Android and iOS runtimes are NOT implemented or qualified by this change**. This file does not create a new M3 toolkit or runtime layer.

## In-place product ownership

| Owner | Preserve | Mobile work that remains |
|---|---|---|
| `java.base` of M3JDK21 | Primitive array/offset/mask operations; collection, topology, projection and residency laws | Cross-built/included VM/runtime and approved JNI/native bindings; no Synexia runtime service |
| `java.desktop` of M3JDK21 | AWT/Swing/Java2D API signatures, layout, events, focus and look-and-feel | Mobile graphics/peer implementations; native text, IME and accessibility bridge; headless vs on-screen coverage |
| `org.eclipse.swt` original widget owners | Display/Shell/Control/GC/Tree/Table/List identity, listener order and disposal | Per-platform Android/iOS backends in SWT's original platform-specific source, using shared indexed backing data but not a shadow widget model |
| Skia/Skija/Skiko donors | Licensed rendering algorithms and native capability | Selected JNI bindings/context hosts for Android and iOS; no presumption Win32 WGL or GDI code ports directly |

M3JDK21's current `m3/collections` and `m3/viewport-core` are **proving grounds**, not a second runtime. Qualifying an operation means landing it at its owning JDK module and proving the resulting installed image. SWT's direct source is a separate Eclipse product with its own legal/API identity, and interoperates with the M3JDK21 runtime without requiring a downstream Synexia service.

## Current audited lineage

- `hsoliwal/eclipse.platform.swt` [PR #201](https://github.com/hsoliwal/eclipse.platform.swt/pull/201) contains intrinsic Tree/Table/List viewport mediation; [PR #202](https://github.com/hsoliwal/eclipse.platform.swt/pull/202) introduces drawn GC/M3/GPU planes.
- [SWT-on-Swing PR #231](https://github.com/hsoliwal/eclipse.platform.swt/pull/231) is merged into an Initiative 31 Swing prototype branch, not evidence of universal mobile support.
- [M3JDK21 PR #1403](https://github.com/hsoliwal/M3jdk21/pull/1403) provides the existing collection/viewports source provenance; [PR #1423](https://github.com/hsoliwal/M3jdk21/pull/1423) continues direct Swing adaptation.
- `M3RowHost`, `M3RowEvents` and `M3RowPainter` currently define toolkit adapter semantics; the Win32 `M3RowCanvas` relies on DIB/LOGFONT and `M3Gpu` on WGL, so Android/iOS need new platform render context hosts.
- Source-reuse candidates: JetBrains Skija and Skiko. Skiko publishes Android (JVM) and iOS (Kotlin/Native) paths; this does **not** imply a deployable Java 21 SWT/Swing runtime on iOS.

## Order of implementation (separately reviewed product PRs)

1. **Runtime admission:** verify M3JDK21 cross-compile/runtime execution options separately for Android (ART vs custom image) and iOS (signed native/AOT or otherwise platform-compliant runtime); document JNI/native-loading, distribution and Apple code-signing constraints. Do not assume Kotlin/Native runs Java bytecode.
2. **Rendering admission:** port the current row-canvas/backend behavior to Android Surface/Skia and iOS Metal/Skia, replacing GDI/WGL/uxtheme use while retaining M3 state masks and damage windows. Measure first-frame, text shaping and input-to-present latency.
3. **Widget input/lifecycle:** move the remaining stage-1 native behavior-plane duties to the owning SWT widget on a contract-by-contract basis; native touch, mouse, keyboard, scroll, focus, selection, IME, accessibility and disposal must match the declared semantics.
4. **Swing/AWT direct ownership:** implement or adapt peer/rendering mechanisms in the M3JDK21 `java.desktop` owners (not a jar layered over stock JDK), keeping Swing model/renderer and LookAndFeel answers and source compatibility.
5. **Proof and gates:** seeded official SWT/JDK public API differentials, scenario matrices per platform, pixel similarity metrics, 99% targeted line/branch coverage with exclusions, JNI load/unload/ownership tests, device/simulator CI, and licenses/attribution checks.

Don't relabel unrun mobile adapters as shipped. Continue per-file, recipe-first, replacing semantically meaningful private atoms while retaining observable contracts. This mobile plan is independent of the completed Java pipeline optimization in this PR.
