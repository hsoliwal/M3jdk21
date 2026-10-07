# Native build evidence and reproduction

Base checkout: `hsoliwal/M3jdk21@890adb6410dab4606a4f26a942aed02fb2f55387`.
The foundation does not alter OpenJDK sources; this build validates the pinned VM
and provides a real fastdebug image on which to test the separate module.
It is not a VM-integrated joined String build.

This run used private Temurin 21+35 as boot JDK, GCC/G++ 14.2.0, GNU make and
Autoconf 2.72 on Debian 13 x86-64. Missing native tools/headers were downloaded
from Debian's signed repository and extracted under `/workspace/m3-build-deps`;
no system JDK was installed or replaced. CUPS image headers required a second
package after an incremental build failed on `cups/raster.h`. That dependency
was added and `make images JOBS=5` resumed successfully without cleaning objects.
The first failure and subsequent successful build logs are preserved in Library.

Equivalent configure/build entry point:

```sh
# Autoconf/m4 and compiler tools must already be on PATH; no installer runs here.
export M3_BOOT_JDK=/path/to/private/jdk-21+35
# Optional prefix with include/ and lib/x86_64-linux-gnu/ native dependencies:
export M3_NATIVE_DEPS=/path/to/extracted-deps/usr
./m3/build-openjdk.sh
export M3_JDK="$PWD/build/linux-x86_64-server-fastdebug/images/jdk"
./m3/build.sh
python3 m3/compatibility/check-synexia.py
python3 m3/verify-shared-mapping.py
```

The script configures fastdebug server, headless-only, bundled FreeType and
disabled warnings-as-errors. OpenJDK's Linux configure still required X11
headers/libraries in this configuration. The build emitted GCC 14 template
warnings. It created the JDK image and both CDS archive variants and exited 0.
No cleanup/rebuild was used to disguise the recovered dependency failure.

The built image runs the foundation's four configurations, the pinned Synexia
content/range comparison, and the separate String/regex/Unicode/JNI smoke harness.
Nine unmodified upstream String tests run with CompactStrings on/off. These are
direct-main tests, not a jtreg or tier-suite pass. Keep full jtreg, alternative
architectures and transparent joined String integration as separate gates.
