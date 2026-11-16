<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21: OS runtime capabilities before native acceleration

## Source ownership and backward compatibility

The first receiving owner is the **existing** `jdk.m3.io` module. This is
an additive public M3 companion API (`jdk.m3.os.OSCapabilities`), not a
new JDK distribution, a replacement for `java.nio`, or a daemon/wrapper.
The native probe uses a C17 JNI library `m3osprobe` built through the
existing `make/modules/jdk.m3.io/Lib.gmk` on Linux targets. The library
holds no file descriptors or io_uring state after a query. The process
snapshot retains two primitive arrays (one byte/status and one int/errno
per feature). O(1) feature queries allocate no entry objects.

**An OS name, kernel version, SDK number or JVM vendor alone cannot prove
native feature availability.** Linux `io_uring_setup` can be denied by
security policy; Windows `QueryIoRingCapabilities` depends on the running
host; sandboxed Android/iOS deployments differ from desktop Linux/macOS.
The default I/O backend is always **JDK_NIO**, regardless of probe status.
Native acceleration requires its own separately tested implementation,
permission checks, cancellation, lifetime ownership and conformance gates.

## Implemented and unimplemented OS feature rows

| OS family / modern release | Important platform capability | What this changes for M3JDK21 | State in this implementation |
|---|---|---|---|
| Linux 6.x, x86_64/ARM64 | `io_uring` asynchronous file/socket I/O | Future optional native async backend after full cancellation, ordering and fd-lifetime tests | JNI **probe only**, kernel- and sandbox-dependent |
| Linux 6.x | `pidfd_open`, `openat2`, `statx` | Future race-resistant process handles, path resolution and metadata in M3FS | JNI **read-only probes only** |
| Linux/Android | runtime page size, 4/16/64 KiB | M3 mmap/Optane/string/alignment and native heap rules must not hardcode 4 KiB | JNI measured page size **on Linux only**; Android **UNPROBED** |
| Windows 11 26H2, Server | IOCP, `QueryIoRingCapabilities` | Preserve IOCP fallback; optional ring backend if supported and independently qualified | Windows statuses **UNPROBED**, no Windows C/Win32 bridge |
| macOS 27, iOS 27 | `kqueue`, GCD, Metal/Core AI, VM and Apple silicon facilities | Event loop, native SWT/Cocoa widgets, on-device inference, VM diagnostics | Apple statuses **UNPROBED**; JNI/ObjC++ required |
| Android 17 | sandboxed kernel / 16 KiB-page device ecosystem / adaptive large-screen UI | Correct page geometry, signed native distribution, viewport and JNI lifecycle | Android statuses **UNPROBED**; ART is not the M3 standalone VM |
| FreeBSD 15 | native libc inotify interface, kqueue and jail descriptors | File watch, event ports and confined M3FS support | FreeBSD statuses **UNPROBED**; libc ABI qualification required |
| OpenBSD/NetBSD/AIX/illumos/Solaris | existing event, process, memory and sandbox facilities | Preserve JDK-supported interfaces and qualify native providers independently | No native backend; **UNPROBED** or NOT_APPLICABLE |

Non-exhaustive comparison sources (license and SDK review still required):
- Linux UAPI: https://github.com/torvalds/linux/blob/master/include/uapi/linux/io_uring.h
- Windows I/O rings: https://learn.microsoft.com/en-us/windows/win32/api/ioringapi/nf-ioringapi-queryioringcapabilities
- Windows 26H2: https://learn.microsoft.com/en-us/windows/whats-new/whats-new-windows-11-version-26h2
- Apple macOS 27: https://developer.apple.com/macos/whats-new/
- Android 17: https://android-developers.googleblog.com/2026/06/Android-17.html
- Android 16 16K page size: https://source.android.com/docs/whatsnew/android-16-release
- FreeBSD 15: https://www.freebsd.org/releases/15.0R/relnotes/

## Independent Linux Java21/JNI evidence

The shipped validation script compiles the new Java source *as a real
`jdk.m3.io` module*, generates its JNI header, compiles the native C17 file
with `-Werror` and runs source-built JIT, interpreter, native-disabled and
missing-library modes. It checks simulated OS families and page sizes,
errno distinctions, process snapshot reuse and concurrent readers, plus
512 repeat native probes and a descriptor-leak guard.

Observed on the test Linux host: reported page size **4096**, successful
actual `io_uring_setup` probe, no leaked descriptors. Another Linux host
may legitimately report DENIED or UNPROBED. No cross-OS result is claimed.

**Mandatory HOLD:** full OpenJDK reactor/build images, real jtreg/JCK,
platform Windows/macOS/Android/iOS/FreeBSD native compilation,
ABI/runtime linkage, JNI shutdown and accelerated I/O path tests, security
review, multi-platform CI and JMH/CPU/RSS are **NOT RUN**. Source-equivalent
Java compilation and isolated native probes alone do not prove a shippable JDK.
