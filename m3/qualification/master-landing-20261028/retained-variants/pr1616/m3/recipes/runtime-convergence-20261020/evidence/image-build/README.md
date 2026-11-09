<!-- Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 -->
# Actual M3JDK21 image configuration attempt

The complete image build is blocked by host prerequisites. No matched image was
produced, and no source, phase/status record, branch, or root staging was modified.

## Exact attempt

From the isolated `configure-attempt` directory:

```sh
bash /workspace/scratch/53bcd62c244c/M3JDK21/configure \
  --with-boot-jdk=/tmp/m3jdk21-toolchain-20261020/jdk-21.0.12.1+1 \
  --with-debug-level=fastdebug --with-jvm-variants=server \
  --enable-headless-only --with-freetype=bundled --with-num-cores=4
```

The normal repository configure wrapper exited1 before generating its runnable
configure script: Autoconf is not installed/on PATH. The complete diagnostic is
`configure-attempt/configure.log`. The generated-configure source is not checked
into this OpenJDK tree; make/autoconf/configure requires real Autoconf generation.
The selected boot JDK is verified Temurin21.0.12.1+1, not a substituted M3 image.

The command follows the documented fastdebug server/headless route and preserves
warnings-as-errors. The historical m3/build-openjdk.sh also passes
--disable-warnings-as-errors; that switch was intentionally not used in this
strict attempt. No guard, compiler policy or native contract was weakened.

## Further observed prerequisites and capacity

GCC/G++13.3.0 and GNU make are available. Autoconf, m4 and pkg-config are absent.
Standard checked CUPS, X11, ALSA and fontconfig development headers are absent.
The pinned OpenJDK build guide identifies these Linux prerequisites; headless
configuration does not remove X11 or ALSA requirements. External FreeType headers
are also absent, but this attempt explicitly selects bundled FreeType.

The host exposes nine logical CPUs and9.7GiB RAM. The filesystem has roughly5.9GiB
free before hydrating HotSpot and most JDK source modules. The pinned build guide
specifies at least6GB free for an x86 build. Thus the current disk margin is narrow
even before obtaining missing sources/dependencies; no safe full-build time or
space acceptance is asserted. No unrelated workspace files or processes were
inspected, cleaned or interrupted to obtain space.

The sparse source inventory and exact free-byte count are recorded in ENVIRONMENT.json.
No full source hydration or large speculative image build was attempted after the
actual configure prerequisite failure. To continue requires real Autoconf/m4 and
Linux development packages, complete matching source modules, and adequate build
storage. A normally configured strict `make images` is the next build gate once
those prerequisites are available; invoking make without a generated configuration
would not supply useful runtime evidence.

## Pin and documentation

The starting production pin was d6d76b563050fc13c4fa79bc494edf29f1614b37. While
working, master advanced to73f59218dc4ef1a3ef79aad990e77273db2a2d60 with three
documentation changes only. A direct Git comparison proves src, make, configure
and Makefile identical between these pins; the configure attempt applies to both.

Read the current AGENTS.md, porting invariant, native-build.md, new common JNI/native
platform contract and the pinned doc/building.md. The latter was fetched as its
exact Git blob without checkout/source changes and is retained locally as building.md.
The current common contract keeps platform implementation, compilation/linking,
checked-JNI execution and full installed-image qualification distinct. This
configuration refusal does not advance any of those gates or the STRING phase.
