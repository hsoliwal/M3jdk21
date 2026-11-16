<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 OS-native donor convergence: capability-to-owner register

**Scope: all JDK implementations plus all relevant OS/runtime capabilities.**
This register is open-ended and does not claim complete integration.
Java 21 source, binary, behavioral compatibility and existing M3 APIs remain
the baseline. Native selections must preserve cancellation, ownership,
security policy, ordering, exceptions and cleanup. A vendor/OS/version name
is **never** evidence that an optimization is available in the process.

## Existing primary owners and guarded opportunities

| Domain / receiving owner | Platforms and native donor features | Required gate and fallback |
| --- | --- | --- |
| M3FS, NIO, channels | Linux io_uring, openat2, statx, inotify/fanotify; Windows IOCP, QueryIoRingCapabilities, USN change journal; Apple kqueue/FSEvents/DispatchIO; FreeBSD/NetBSD/OpenBSD kqueue; illumos event ports; AIX asynchronous I/O | Actual feature probe and permission; strict fd/handle lifetime, ordering, cancellation, file renames and atomic writes; existing JDK NIO default |
| M3FS directories and archives | Linux mount features and statx, Windows reparse points and directory notification, macOS APFS clones, FreeBSD ZFS snapshots | Do not infer semantics from filesystem labels; test symlinks, rename pairs, lock ordering, mmap truncation and cross-filesystem moves |
| M3 String / M3 Collections / M3FS memory | Linux native page geometry, huge pages, memory pressure/cgroups, NUMA; Windows NUMA/VirtualAlloc2 and memory priority; Apple memory-pressure dispatch; Android ARM64 16-KiB pages; AIX large pages | Physical vs virtual page distinction; page-aligned mapping and JNI; avoid hardcoded 4096, no mandatory native memory. Per-owner allocated-bytes/retained-RSS oracle |
| M3 scheduler / structured parallelism | Linux futex_waitv, scheduler constraints; Windows WaitOnAddress and EcoQoS; Apple Dispatch QoS; Android thermal/battery limits; BSD event filters | Do not replace Java monitor/lock/virtual-thread semantics; explicit cancellation, fairness, wakeup ordering, Java 21 / JNI / scheduler source tests |
| SWT / JFace / Nebula / SwingX / NetBeans viewport | Windows native HWND/DirectComposition; Apple AppKit, UIKit, Metal GPU family detection; Android native UI and accessibility; Wayland/X11; mobile compositors | Preserve each toolkit's own API, native widget ownership, selection, focus, accessibility, UI-thread rules. DOOP array-backed viewport+overscan; no mandatory replacement toolkit |
| JIT/AOT/native compiler and JNI | Linux executable-memory permissions, Apple hardened runtime and code signing, Windows code integrity, Android ART vs standalone Java runtime, AIX/FreeBSD executable-memory rules | No JIT assumptions on sandboxed iOS; isolate interpreter/AOT backends; JNI ABI and packaging tests separately per OS/CPU |
| Security and process ownership | Linux pidfd, seccomp, Landlock and openat2; Windows job objects/AppContainer; Apple sandbox/entitlements; Android SELinux/app sandbox; FreeBSD Capsicum; OpenBSD pledge/unveil | Never use a probe to bypass policy. Detect denial separately from absence and provide a safe fallback |

The first additive product implementation is `jdk.m3.os.OSCapabilities`,
in the existing `jdk.m3.io` module, with a small, array-backed immutable
snapshot and no per-feature entry objects. It currently provides
**real native probes on Linux** for four syscalls and page geometry.
Windows/Apple/Android/BSD/AIX/illumos API rows are **UNPROBED** until an
independent native binding has been built and device-tested. These API
rows do **not** turn on an I/O, scheduling or GPU backend.

## High-priority qualification sequence

1. **Runtime page geometry:** use measured kernel page size where a
   platform backend exists; test 4 KiB, 16 KiB, 64 KiB where supported.
   Android's 16 KiB transition affects ELF program-header alignment,
   JNI libraries, mmap offsets, internal fragmentation and per-viewport
   memory budgets.
2. **Asynchronous I/O:** qualify existing NIO fallback first. Independently
   test queue teardown, cancellation, callbacks, resource limits,
   bounded memory, EINTR and sandbox denials. Never infer actual ring
   support from a release number.
3. **Native UI/compute:** SWT, JFace, Nebula, Swing/AWT, SwingX and NetBeans
   retain upstream public contracts, while platform-specific rendering,
   accessibility, Metal/DirectX/Vulkan and array-backed viewport engines
   may use optional native implementations.
4. **Power and memory pressure:** separate foreground latency requirements
   from background indexing. Adopt OS scheduler/memory hints only when the
   user task and owner permit them; do not make QoS hints part of Java
   semantics.
5. **Platform fleet:** Linux x86-64 / AArch64; Windows x86-64 / ARM64;
   macOS Apple Silicon / Intel; iOS/iPadOS ARM64 and Android ARM64;
   FreeBSD, OpenBSD, NetBSD, illumos, Solaris, AIX, IBM i, QNX,
   embedded/RTOS and WebAssembly/WASI where legitimate Java runtime
   ports exist. Keep each as a separately qualified target; never
   conflate Linux host JNI and Android ART or standalone mobile JVM.

## Mandatory evidence for any implementation promotion

- **DISCOVER → SOURCE HASH → LICENSE/PROVENANCE → RECIPE → DIFF →
  COMPILE → TEST → BENCHMARK → PLATFORM QUALIFY**.
- Exact source/blob hashes before/after; ABI descriptors and JNI headers;
  deterministic atomicity/ordering/cancellation and functional parity.
- JNI runtime platform library loading and native image/device packaging.
  Simulated classification is NOT proof of a Windows, Apple or Android backend.
- Record page size, privilege/sandbox state, kernel/API result code, ABI
  version, fault-injection and fallback, then measured CPU, heap/RSS and
  energy on representative hardware.
- Do not relicense source copied from OpenJDK/vendor GPL lineages as
  Apache-2.0; keep per-file grants and retained NOTICE statements.

## Platform primary references (architecture, not imported source)

- Linux io_uring policy: https://docs.kernel.org/admin-guide/sysctl/kernel.html
- Linux futex2: https://docs.kernel.org/userspace-api/futex2.html
- Windows I/O-ring runtime query: https://learn.microsoft.com/en-us/windows/win32/api/ioringapi/nf-ioringapi-queryioringcapabilities
- Windows EcoQoS and memory priorities: https://learn.microsoft.com/en-us/windows/win32/api/processthreadsapi/nf-processthreadsapi-setthreadinformation
- Apple memory pressure events: https://developer.apple.com/documentation/dispatch/dispatch_source_type_memorypressure
- Apple Metal 4 runtime API: https://developer.apple.com/documentation/metal/understanding-the-metal-4-core-api
- Android 16 KiB native page size: https://developer.android.com/guide/practices/page-sizes
- FreeBSD Capsicum: https://docs.freebsd.org/en/books/handbook/security/
- AIX AIO: https://www.ibm.com/docs/en/aix/7.3?topic=concepts-asynchronous-io-subsystem

No third-party implementation bodies, solutions, external source files or
vendor-copyright headers have been copied into this register.
