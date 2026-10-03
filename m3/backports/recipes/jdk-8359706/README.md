# JDK-8359706 — Java 21 compatible open-file-descriptor diagnostics

Primary upstream issue: `JDK-8359706`, **Add file descriptor count to VM.info**.

Primary donor commit:

```text
openjdk/jdk@b0831572e2cd9dbff9ee2abcdf81a493ddcecc7e
```

Required immediate follow-up:

```text
JDK-8380236
openjdk/jdk@3a109f49feb19f313632be6a2aa24ba7d9b7269b
```

The follow-up fixes the macOS build by replacing a debug-only `assert` with `precond` so the
buffer-length parameter remains consumed in product builds. M3JDK21 admits the two commits as one
dependency-closed packet.

## Compatible leaf

This packet is diagnostics-only:

- Linux counts numeric entries in `/proc/self/fd` with a 50 ms bound;
- macOS uses a bounded `proc_pidinfo(PROC_PIDLISTFDS)` buffer;
- BSD non-macOS reports `unknown`;
- AIX/Windows provide compatibility stubs;
- `VM.info` and fatal error reports gain open-descriptor diagnostics;
- `TestJcmdSanity` requires numeric output on Linux/macOS.

No Java grammar, class-file, Java SE API, JNI/JVMTI ABI, heap layout, GC, JIT or persistent format is
changed.

## Recipe DAG

`M3Jdk8359706BackportRecipe` composes:

```text
M3Jdk21HashPinnedSnapshotRecipe(jdk27-open-fd-8359706-java)
  -> TestJcmdSanity.java

M3Jdk21HashPinnedTextSnapshotRecipe(jdk27-open-fd-8359706-text)
  -> AIX/BSD/Linux/Windows os files
  -> os.hpp
  -> vmError.cpp
```

All eight existing targets are bound to exact JDK21 SHA-256 preimages. JUnit requires the complete
eight-file replay, rejects drift and requires a zero-change second pass.

## Promotion gates

The packet remains `candidate-adapted` until:

- recipe JUnit passes;
- backport verifier passes;
- Linux release/server image builds;
- `TestJcmdSanity.java` passes on Linux;
- macOS build/test evidence exists for the bounded BSD path;
- fatal-error/VM.info diagnostic behavior remains compatible;
- diff/lint checks pass;
- second-pass recipe fixed point passes.

Do not claim cross-platform completion from Linux CI alone.
