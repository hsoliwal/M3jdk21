# JDK-8359706 — Java 21 update-line open-file-descriptor diagnostics

Issue: `JDK-8359706`, **VM.info and hs_err_pid Logs Now Report Current Open File Descriptors**.

## Preferred donor lineage

This packet does not re-adapt the JDK 27 implementation from scratch. OpenJDK already backported the
feature into the Java 21 update line:

```text
openjdk/jdk21u-dev@3da873b1aa23c64e47a5da3352b7b48e8c2c74cd
  Backport-of: openjdk/jdk@b0831572e2cd9dbff9ee2abcdf81a493ddcecc7e
```

That backport is immediately dependency-closed with the macOS build repair:

```text
openjdk/jdk21u-dev@f696cb16462f0201a10540ca36049472a79eba94
  JDK-8380236
  Backport-of: openjdk/jdk@3a109f49feb19f313632be6a2aa24ba7d9b7269b
```

The repair is mandatory. Importing only JDK-8359706 would knowingly reproduce the macOS build
failure fixed by JDK-8380236.

## Compatibility

The change is diagnostic/serviceability only:

- Linux counts `/proc/self/fd` entries with a bounded 50ms scan;
- macOS uses `proc_pidinfo` with bounded caller-owned scratch;
- AIX and Windows provide non-counting stubs;
- `VM.info` and fatal error reports expose the count on supported platforms;
- the existing Java SE API, Java source grammar, class-file version, JNI/JVMTI API and persistent
  formats are unchanged.

M3JDK21 adapts only insertion locations to the current JDK21 source layout. The diagnostic
semantics are taken from the Java-21 update-line donor.

## Recipe ownership

`M3Jdk8359706BackportRecipe` composes:

```text
M3Jdk21HashPinnedTextSnapshotRecipe(jdk21u-open-fds-8359706-text)
  -> 7 HotSpot/platform C++/header targets

M3Jdk21HashPinnedSnapshotRecipe(jdk21u-open-fds-8359706-java)
  -> test/jdk/sun/tools/jcmd/TestJcmdSanity.java
```

Every existing target is bound to its exact current M3JDK21 SHA-256 preimage and exact reviewed
postimage. A stale preimage fails closed. The exact postimage is a second-pass fixed point.

## Promotion gates

The packet remains candidate-adapted until all applicable gates pass:

1. recipe JUnit and manifest/hash proof;
2. M3 backport catalogue/DAG verification;
3. Linux HotSpot/JDK image build;
4. `TestJcmdSanity.java` jtreg on Linux;
5. macOS build proof or equivalent trusted update-line evidence plus later target build;
6. fatal-error/VM.info diagnostic smoke where available;
7. diff/lint;
8. fixed-point readback;
9. serial promotion.

Do not infer whole-JDK compatibility from the Java-21 donor alone.
