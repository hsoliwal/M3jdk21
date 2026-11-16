# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors

AC_DEFUN_ONCE([LIB_SETUP_SKIA],
[
  AC_ARG_WITH([skia], [AS_HELP_STRING([--with-skia],
      [build the owned jdk.m3.graphics native engine (auto, bundled or none) @<:@auto@:>@])])
  AC_ARG_WITH([gn], [AS_HELP_STRING([--with-gn],
      [path to pinned GN 2175 used to build the owned Skia sources])])
  AC_ARG_WITH([ninja], [AS_HELP_STRING([--with-ninja],
      [path to Ninja used to build the owned Skia sources])])
  AC_ARG_WITH([skia-ndk], [AS_HELP_STRING([--with-skia-ndk],
      [Android NDK used for the owned Skia native engine])])

  SKIA_ENABLED=false
  SKIA_GN=""
  SKIA_NINJA=""
  SKIA_PYTHON=""
  SKIA_NDK="$with_skia_ndk"
  SKIA_TARGET_OS=""
  SKIA_TARGET_CPU=""
  if test "x$with_skia" = x; then
    with_skia=auto
  fi
  if test "x$with_skia" = xauto; then
    # Keep the original Java-only module on JDK targets unsupported by this
    # native engine. Explicit bundled requests still diagnose such targets.
    case "$OPENJDK_TARGET_OS:$OPENJDK_TARGET_CPU" in
      linux:x86_64|linux:aarch64|linux:x86|linux:arm|\
      macosx:x86_64|macosx:aarch64|\
      windows:x86_64|windows:aarch64|windows:x86|\
      android:x86_64|android:aarch64|android:x86|android:arm|\
      ios:x86_64|ios:aarch64) with_skia=bundled ;;
      *) with_skia=none ;;
    esac
  fi
  if test "x$with_skia" = xbundled; then
    case "$OPENJDK_TARGET_OS" in
      linux) SKIA_TARGET_OS=linux ;;
      macosx) SKIA_TARGET_OS=mac ;;
      windows) SKIA_TARGET_OS=win ;;
      android) SKIA_TARGET_OS=android ;;
      ios) SKIA_TARGET_OS=ios ;;
      *) AC_MSG_ERROR([Skia does not support this target; use --with-skia=none for a Java-only graphics module]) ;;
    esac
    case "$OPENJDK_TARGET_CPU" in
      x86_64) SKIA_TARGET_CPU=x64 ;;
      aarch64) SKIA_TARGET_CPU=arm64 ;;
      x86) SKIA_TARGET_CPU=x86 ;;
      arm) SKIA_TARGET_CPU=arm ;;
      *) AC_MSG_ERROR([Skia does not support this CPU; use --with-skia=none for a Java-only graphics module]) ;;
    esac
    SKIA_SOURCE_DIR="$TOPDIR/src/jdk.m3.graphics/share/native/libm3graphics/skia"
    if test ! -f "$SKIA_SOURCE_DIR/BUILD.gn"; then
      AC_MSG_ERROR([The owned Skia source tree is missing: $SKIA_SOURCE_DIR])
    fi
    if test "x$with_gn" != x; then
      SKIA_GN="$with_gn"
      UTIL_FIXUP_EXECUTABLE(SKIA_GN)
    else
      UTIL_REQUIRE_PROGS(SKIA_GN, gn)
    fi
    if test "x$with_ninja" != x; then
      SKIA_NINJA="$with_ninja"
      UTIL_FIXUP_EXECUTABLE(SKIA_NINJA)
    else
      UTIL_REQUIRE_PROGS(SKIA_NINJA, ninja ninja-build)
    fi
    UTIL_REQUIRE_PROGS(SKIA_PYTHON, python3)
    if test "x$OPENJDK_BUILD_OS" = xwindows; then
      SKIA_PYTHON_OS=`$SKIA_PYTHON -c 'import os; print(os.name)'`
      if test "x$SKIA_PYTHON_OS" != xposix; then
        AC_MSG_ERROR([Skia source builds use the JDK POSIX path wrapper; select a POSIX python3 with SKIA_PYTHON])
      fi
    fi
    SKIA_GN_VERSION=`$SKIA_GN --version`
    case "$SKIA_GN_VERSION" in
      "2175 (b2afae"*) ;;
      *) AC_MSG_ERROR([Skia requires GN 2175 at b2afae122eeb6ce09c52d63f67dc53fc517dbdc8; found $SKIA_GN_VERSION]) ;;
    esac
    if test "x$SKIA_TARGET_OS" = xandroid; then
      if test "x$SKIA_NDK" = x; then
        AC_MSG_ERROR([Android Skia builds require --with-skia-ndk])
      fi
      UTIL_FIXUP_PATH(SKIA_NDK)
    fi
    SKIA_ENABLED=true
  elif test "x$with_skia" != xnone; then
    AC_MSG_ERROR([--with-skia must be auto, bundled or none; external Skija runtime artifacts are not supported])
  fi

  AC_SUBST(SKIA_ENABLED)
  AC_SUBST(SKIA_GN)
  AC_SUBST(SKIA_NINJA)
  AC_SUBST(SKIA_PYTHON)
  AC_SUBST(SKIA_NDK)
  AC_SUBST(SKIA_TARGET_OS)
  AC_SUBST(SKIA_TARGET_CPU)
])
