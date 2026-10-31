<!--
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0
-->
# Post-landing #2076 source-sealed install + deploy quarantine

Original source PR: [#2076](https://github.com/hsoliwal/M3jdk21/pull/2076.patch),
HEAD `45f38829aa656709955eee03673a7fb1d2ee218e`.
Canonical original Maven/OpenRewrite recipe:
[Synexia #13807](https://github.com/hsoliwal/com.synexia/pull/13807.patch),
named `com.synexia.rewrite.M3InteropInstallDeployHold20261028`, stacked
on original canonical [#13780](https://github.com/hsoliwal/com.synexia/pull/13780).

This receiving commit is a **real two-parent source merge**:
first parent `master@c6f48af6be4dd20477887f09716cdb1a0a361e26`, second parent original source #2076
`45f38829aa656709955eee03673a7fb1d2ee218e`. The active product tree retains all the first parent's source
and changes **only four existing original interop POM/checker/test/README
files** plus this source receipt. The fifth original PR output, the
GitHub Actions file, is already identical at Git blob
`81c237f93da86aafa026099b9c799d0d99b09537`.

## Exact canonical before and after Git source seals

| Original M3JDK21 path | Before Git blob | Canonical / #2076 after Git blob |
| --- | --- | --- |
| `m3/collections/interop/pom.xml` | `3fc5cc0f0bee520675fea18a379c96b288c99084` | `529fa821fcb27a645795f6677270a9a44f4526e3` |
| `m3/collections/verification/no_external_runtime.py` | `aa7baed2aa9e4ff2aa675771def00a09ccc863f3` | `00b94bec1d828d5d716c9707844f97b6de1a6124` |
| `m3/collections/verification/test_no_external_runtime.py` | `2886c5d68444ee34e54a3352d1400a2e4fdb21a4` | `36b0428c202078f196b322d5e5d8d80313a5dd65` |
| `m3/collections/interop/README.md` | `2f7104f1a694e0392713a4fd62b0883ebba3d650` | `c04634bc2696fae873a63bd1dfe6eb54fc341453` |

All four canonical original target postimages match byte-identically in
private Synexia #13807's existing SHA-pinned resources
(`00-pom.xml.txt`, `01-checker.py.txt`, `02-tests.py.txt`, `03-readme.md.txt`).
No OpenRewrite parser/engine/YAML materializer is copied into M3JDK21.

## Executed original-source tests and contract bounds

- Independently reconstructed the new `no_external_runtime.py` and
  `test_no_external_runtime.py` as **byte-identical** product Git blobs
  `00b94bec1d828d5d716c9707844f97b6de1a6124` and
  `36b0428c202078f196b322d5e5d8d80313a5dd65`.
- `python3 -m py_compile` passed on these exact Python source files.
- Real `python3 -m unittest discover -p test_no_external_runtime.py -v`
  passed **16/16**, zero errors/failures, covering original eleven cases
  plus five negative checks: deploy-only skip, false/property install,
  duplicate install, install profile override, pluginManagement override.
- Compared the full active POM by ordered source line identity: every
  original POM line is retained in the postimage **in original order**;
  only the literal `maven-install-plugin:3.1.3 <skip>true</skip>` block
  is added alongside existing `maven-deploy-plugin:3.1.3 <skip>true</skip>`.
  Four direct provider `compile` dependencies remain Guava, Eclipse
  Collections, Spring Core and Apache Commons Collections.
- README changes are a truthful release-scope correction: ordinary Maven
  `verify` can run with donor libraries, but ordinary `install`/`deploy`
  now refuse to publish the optional research module.

## Explicit HOLD and no-loss constraints

This is **publication quarantine**, not zero-third-party-runtime source
convergence. Manual `install-file` / `deploy-file`, custom registries,
effective/transitive Maven graph, consumer classloading/JNI/ABI,
full original JDK/jtreg/JCK and vendor behavioral parity are not qualified.
The old #2076 body describing deploy-only quarantine is superseded by this
successor SHA-pinned head, and must not be used as release evidence.

This postimage does not delete any existing target source or overwrite
M3JDK21's current Java/JNI code; the original #2076 source ancestry is
permanently retained by the second parent. No squash/rebase/force push.
Hosted CI is checked separately; local Python tests do not claim full
Maven SDK or installed-JDK acceptance.
