# JDK-8357439 — jcmd Bash completion

Upstream issue: **JDK-8357439, Add bash autocompletion for jcmd**.

Pinned upstream commit:

    openjdk/jdk@8549d1896054dd230ba3038c83bce23b10dcda22

The two JDK21 target preimages are absent:

- make/modules/jdk.jcmd/Copy.gmk
- src/jdk.jcmd/share/conf/bash-completion/jcmd

The merged M3JDK21 product files are byte-identical to the upstream Git blobs recorded in
manifest.tsv.

## Recipe custody

Target-specific replay owner:

    com.m3.rewrite.backport.M3Jdk8357439JcmdCompletionBackportRecipe

Generic exact text replay primitive:

    com.m3.rewrite.backport.M3Jdk21HashPinnedTextSnapshotRecipe

Crate:

    jdk27-jcmd-8357439

The crate is ABSENT -> exact postimage for both files. JUnit starts from an empty source set,
requires exactly two generated files, replays the result and requires a zero-diff fixed point,
and fails closed if a target path already exists with unexpected content.

## Compatibility

This packet adds build metadata and a Bash completion resource only. It does not change Java
grammar, class-file format, Java SE API behavior, HotSpot execution semantics, JNI/JVMTI
contracts, or existing jcmd commands.

On Windows the build rule deliberately does not install the Bash completion resource.

## Promotion evidence

Product materialization was merged earlier in PR #30. Recipe custody is a separate proof layer.

Required proof:

1. recipe JUnit;
2. backport manifest verifier;
3. Bash syntax check;
4. deterministic jcmd completion runtime smoke;
5. build-system acceptance for jdk.jcmd packaging;
6. second-pass recipe fixed point.

No new performance claim is made.
