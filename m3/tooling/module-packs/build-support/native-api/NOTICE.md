<!-- Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 -->
# Native API source and distribution notices

Original API and generic C/C++ provider source:
MojoHaus Maven Native 1.1.0, commit
`59b9d76471f42265141e1edd3594f9dca156099a`,
https://github.com/mojohaus/maven-native.
The Codehaus copyright 2004 and MIT license apply to the original source
except where original file headers provide another license. The original
Ant-Contrib parser and file-set sources retain their Apache-2.0 headers and
copyrights. `DONOR_PINS.json` enumerates exact original paths and licenses.

`CompilerConfiguration` and `AbstractCompiler` have the additive caller-admitted
workset adaptation recorded in `ADAPTATIONS.json`. Its original MIT text stays
in place; additions are Copyright 2026 Hitesh Soliwal and contributors under
Apache-2.0. The new source-build/proof tooling and fixture have the same
first-party copyright and Apache-2.0 license. No other original source body is
changed. These combined source-built classes are an M3JDK21 received artifact,
not the unmodified upstream Maven binary release.

Plexus container-default 2.1.1 and component-annotations 2.2.0 retain the original
Codehaus/original-author Apache-2.0 notices. Plexus utils 4.1.0 and Apache XBean
Reflect 3.7 retain their JAR-embedded LICENSE and NOTICE texts. The selected
Plexus Classworlds 2.6.0 dependency is credited to The Codehaus; both original
`LICENSE.txt` (including its naming and endorsement conditions) and
`LICENSE-2.0.txt` are retained. No dependency has been renamed or relicensed.
Dependency artifact, POM, source-license and exact byte identities are recorded
in `DEPENDENCY_PINS.json`.

The selected original `com.m3.file` module keeps Copyright 2026 Hitesh Soliwal
and contributors and Apache-2.0. Its source and class identities come from the
preceding qualified product receipt; `FOUNDATION_PINS.json` binds that artifact.
No new file fingerprint owner is implemented here.

JNI headers copied from the selected SDK keep their original OpenJDK copyright
and GPL-2.0-with-Classpath-exception notices. The matching SDK's
`legal/java.base/LICENSE`, `ADDITIONAL_LICENSE_INFO` and `ASSEMBLY_EXCEPTION`
are retained with them. Native compiler binaries are host inputs, not bundled
or relicensed by this pack.
