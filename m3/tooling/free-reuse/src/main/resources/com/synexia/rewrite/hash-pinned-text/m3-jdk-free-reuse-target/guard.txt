#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
cd "$(dirname "$0")/.."
: "${M3_BOOT_JDK:?Set M3_BOOT_JDK to a private JDK 21 directory}"
mkdir -p m3/build/logs
args=(--with-boot-jdk="$M3_BOOT_JDK" --with-debug-level=fastdebug --with-jvm-variants=server --enable-headless-only --disable-warnings-as-errors --with-freetype=bundled)
if [[ -n "${M3_NATIVE_DEPS:-}" ]]; then
 args+=(--with-cups="$M3_NATIVE_DEPS" --with-fontconfig="$M3_NATIVE_DEPS" --with-alsa-include="$M3_NATIVE_DEPS/include" --with-alsa-lib="$M3_NATIVE_DEPS/lib/x86_64-linux-gnu" --x-includes="$M3_NATIVE_DEPS/include" --x-libraries="$M3_NATIVE_DEPS/lib/x86_64-linux-gnu" --with-extra-cflags="-I$M3_NATIVE_DEPS/include" --with-extra-cxxflags="-I$M3_NATIVE_DEPS/include")
fi
bash configure "${args[@]}" 2>&1 | tee m3/build/logs/native-configure.log
make images JOBS="${M3_JOBS:-5}" 2>&1 | tee m3/build/logs/native-build.log
