# Existing M3JDK21 frame-page Java fallback — clean recipient

This draft is a **source-identical product receiving extraction** from
[M3JDK21 #1884](https://github.com/hsoliwal/M3jdk21/pull/1884), at
`facc2e13d0c658c96c9335bf7cb5d9c8f54bae44`, onto
`master` `51de1f76e8b61f2d808792affc75aa3f69ee4943`.
The former review contains unrelated Eclipse/workflow work as well as the
viewport fallback; this focused branch only receives the existing Java
kernel result, an original-owner deterministic Java test, and an unchanged
qualification receipt. The product's C JNI implementation and package-
private `M3MaskViewportNative` declarations are already present in master
and remain **byte-for-byte unchanged**.

## Canonical source
Original Synexia convergence dataset
`synexia-openrewrite-recipes/recipe-crates/m3-ast-build-reverse-20261017/datasets/vpt-frame-fallback-20261027`
named `com.synexia.m3.ViewportFrameFallback20261027`, using the
original `M3HashPinnedJavaSnapshotRecipe`.

The retained receiving receipt records **authentic Java21 OpenRewrite**
`J.CompilationUnit`, 5 passing `M3TaskRecipeTest` tests, two actual
SDK Results, zero-change recipe replay and source-drift refusals. It
also discloses that an **adapted offline Maven POM** was used (the original
canonical POM was not run byte-identically) and that the original
`receive.py` was not executed in that proof. This PR is therefore kept
draft pending independent product CI and current-source review.

## Exact product source custody

- Baseline product owner blob `5966bffacc730b3ef7102d615a2331a42ae4a22b`.
- SDK-produced Java fallback owner blob `aefcdb933cbc2caa122a527180e20fda8e2a284d`.
- Source-identical original Java frame oracle blob
  `db58a90031370a5cf8dd9039dbce6306817dde46`.
- Existing unchanged product native Java owner
  `9cdfab35cf719ccf26f61ee61e1ae10fa98eb9ad`.
- Existing unchanged product native C owner
  `f29d081e28ba8ccc4516bd28c09e738b7a408a76`.

The new hosted gate compiles **actual M3JDK21 Java classes**, executes
the 35,080-check deterministic frame reference in normal and interpreted
modes, and reruns the original kernel/page Java contracts with exact
observations. This is Java fallback correctness only; it does not establish
new native JNI throughput, Qt/SWT/Nebula/RWT/GEF UI behavior, full
JDK/HotSpot/jtreg, GPU or performance improvements.

No public JDK type, wrapper, second model, new rendering control, native
export or JNI declaration is introduced. Neither `master` nor the
historical convergence draft is rebased or force-pushed.
