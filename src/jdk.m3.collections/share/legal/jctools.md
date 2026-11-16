## JCTools (development state 5.0.0-SNAPSHOT)

### Origin

The Java sources under `providers/classes/jdk/m3/collections/jctools` of the module `jdk.m3.collections` are derived
from JCTools (https://github.com/JCTools/JCTools) at commit `8b7fb37eb41d36dcc2f66155a4344fe1c045fa40` (2026-11-11)
of its branch `master`. It is a development state, not a release: the build file says 5.0.0-SNAPSHOT; the newest
release is 4.0.7 (tag `v4.0.7`, on Maven Central for both projects), and this commit is 66 commits ahead of that tag
and 3 behind it. Against the sources jars of release 4.0.7 on Maven Central, this state has 35 more sources (23 in
`jctools-core`, 12 in `jctools-core-jdk11`), 127 that differ and 27 that are identical; no source of the release is
missing. The fork https://github.com/hsoliwal/JCTools
(`b9d0bbb7d547797ae6a0d951bb5f00058d84464b`) is two commits ahead of that commit; they change the root `pom.xml`
only, so the sources are the library's.

Taken are 189 sources of two of the repository's projects:

- `jctools-core`: all 137 main sources;
- `jctools-core-jdk11`: all 52 main sources, the queues built on `java.lang.invoke.VarHandle` (packages
  `queues.varhandle` and `queues.varhandle.unpadded`).

The projects `jctools-channels` and `jctools-experimental`, the build, benchmark and test projects and the tests are
not taken.

JCTools is licensed under the Apache License, Version 2.0. The text below is the repository's file `LICENSE`. The
repository has no NOTICE file. 167 of the 189 sources carry the Apache License header (119 of `jctools-core`, 48 of
`jctools-core-jdk11`); 22 carry no license header. No source carries a copyright line. The headers are unchanged.

### Origins the library states

- `maps`: the library's `README.md` lists "Cliff Click's NonBlockingHashMap and friends (NonBlockingHashMapLong,
  NonBlockingIdentityHashMap, NonBlockingHashSet, NonBlockingSetInt)". Seven sources of `maps` carry
  `@author Cliff Click` (`AbstractEntry`, `ConcurrentAutoTable`, `NonBlockingHashMap`, `NonBlockingHashMapLong`,
  `NonBlockingHashSet`, `NonBlockingIdentityHashMap`, `NonBlockingSetInt`). In JCTools they carry the Apache License
  header; the sources do not say under which terms they were taken.
- The documentation of the queues names the algorithms they follow: "a Java port of the MPSC algorithm as
  presented" and "an adaptation of the one put forward by D. Vyukov" (1024cores.net, named in 18 sources),
  FastFlow (sourceforge.net/projects/mc-fastflow, 12 sources) and the BQueue paper (6 sources). The sources name
  these as algorithms, not as copied code.

### Changes made by Synexia (2026-11-17)

The changes were mechanical (com.synexia repository, recipe crate `inverse-package-relocation-20261116`,
`provider_pipeline.sh`):

- the package `org.jctools` is named `jdk.m3.collections.jctools`;
- two package-private classes of `counters` have other names, because this repository's gate for names refuses a
  file name that ends in a version: `FixedSizeStripedLongCounterV6` is `FixedSizeStripedLongCounterCompareAndSwap`
  and `FixedSizeStripedLongCounterV8` is `FixedSizeStripedLongCounterGetAndAdd` (named after the `Unsafe`
  operation each uses). Their only user is `CountersFactory`, whose public methods, `createFixedSizeStripedCounterV6`
  and `createFixedSizeStripedCounterV8` among them, are unchanged;
- the nine `@org.osgi.annotation.bundle.Export` annotations of the package descriptions were removed with their
  imports: they produce OSGi manifest entries. The same nine packages are exported by the module descriptor;
- `@SuppressWarnings` was added to 120 of the 531 top-level types and extended on 6 of the 24 that had one, for the
  lint categories the JDK build reports as errors;
- documentation, for doclint, which the JDK build runs as errors: `<tt>` became `<code>` in 43 comments; 7 bare `<`,
  `>` or `&` were written as entities; 5 links `{@link BaseMpscLinkedArrayQueue}` became code text, in the `atomic`,
  `unpadded` and `varhandle` variants of `MpscBlockingConsumerArrayQueue`, where that class is in another package and
  not imported; 5 `<p>` that open no paragraph were removed;
- trailing blanks were removed from 168 lines in 41 files (the repository's rule for Java sources, `.jcheck/conf`).

No top-level type was moved to another file.

Compiled before and after these changes (the reference with the package and the two classes renamed, nothing else),
every one of the 578 classes has the same members and the same instructions. The library's nine `package-info`
classes exist only to carry the removed `@Export` annotation and have no counterpart here.

### What to know

Classes in `counters`, `maps`, `queues`, `queues.unpadded` and `util` use `sun.misc.Unsafe`; the module requires
`jdk.unsupported`. The queues in `queues.atomic` and `queues.varhandle` (with their `unpadded` packages) do not: they
use the atomic classes of `java.util.concurrent` and `VarHandle`.

### License

```
Apache License
                           Version 2.0, January 2004
                        http://www.apache.org/licenses/

   TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

   1. Definitions.

      "License" shall mean the terms and conditions for use, reproduction,
      and distribution as defined by Sections 1 through 9 of this document.

      "Licensor" shall mean the copyright owner or entity authorized by
      the copyright owner that is granting the License.

      "Legal Entity" shall mean the union of the acting entity and all
      other entities that control, are controlled by, or are under common
      control with that entity. For the purposes of this definition,
      "control" means (i) the power, direct or indirect, to cause the
      direction or management of such entity, whether by contract or
      otherwise, or (ii) ownership of fifty percent (50%) or more of the
      outstanding shares, or (iii) beneficial ownership of such entity.

      "You" (or "Your") shall mean an individual or Legal Entity
      exercising permissions granted by this License.

      "Source" form shall mean the preferred form for making modifications,
      including but not limited to software source code, documentation
      source, and configuration files.

      "Object" form shall mean any form resulting from mechanical
      transformation or translation of a Source form, including but
      not limited to compiled object code, generated documentation,
      and conversions to other media types.

      "Work" shall mean the work of authorship, whether in Source or
      Object form, made available under the License, as indicated by a
      copyright notice that is included in or attached to the work
      (an example is provided in the Appendix below).

      "Derivative Works" shall mean any work, whether in Source or Object
      form, that is based on (or derived from) the Work and for which the
      editorial revisions, annotations, elaborations, or other modifications
      represent, as a whole, an original work of authorship. For the purposes
      of this License, Derivative Works shall not include works that remain
      separable from, or merely link (or bind by name) to the interfaces of,
      the Work and Derivative Works thereof.

      "Contribution" shall mean any work of authorship, including
      the original version of the Work and any modifications or additions
      to that Work or Derivative Works thereof, that is intentionally
      submitted to Licensor for inclusion in the Work by the copyright owner
      or by an individual or Legal Entity authorized to submit on behalf of
      the copyright owner. For the purposes of this definition, "submitted"
      means any form of electronic, verbal, or written communication sent
      to the Licensor or its representatives, including but not limited to
      communication on electronic mailing lists, source code control systems,
      and issue tracking systems that are managed by, or on behalf of, the
      Licensor for the purpose of discussing and improving the Work, but
      excluding communication that is conspicuously marked or otherwise
      designated in writing by the copyright owner as "Not a Contribution."

      "Contributor" shall mean Licensor and any individual or Legal Entity
      on behalf of whom a Contribution has been received by Licensor and
      subsequently incorporated within the Work.

   2. Grant of Copyright License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      copyright license to reproduce, prepare Derivative Works of,
      publicly display, publicly perform, sublicense, and distribute the
      Work and such Derivative Works in Source or Object form.

   3. Grant of Patent License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      (except as stated in this section) patent license to make, have made,
      use, offer to sell, sell, import, and otherwise transfer the Work,
      where such license applies only to those patent claims licensable
      by such Contributor that are necessarily infringed by their
      Contribution(s) alone or by combination of their Contribution(s)
      with the Work to which such Contribution(s) was submitted. If You
      institute patent litigation against any entity (including a
      cross-claim or counterclaim in a lawsuit) alleging that the Work
      or a Contribution incorporated within the Work constitutes direct
      or contributory patent infringement, then any patent licenses
      granted to You under this License for that Work shall terminate
      as of the date such litigation is filed.

   4. Redistribution. You may reproduce and distribute copies of the
      Work or Derivative Works thereof in any medium, with or without
      modifications, and in Source or Object form, provided that You
      meet the following conditions:

      (a) You must give any other recipients of the Work or
          Derivative Works a copy of this License; and

      (b) You must cause any modified files to carry prominent notices
          stating that You changed the files; and

      (c) You must retain, in the Source form of any Derivative Works
          that You distribute, all copyright, patent, trademark, and
          attribution notices from the Source form of the Work,
          excluding those notices that do not pertain to any part of
          the Derivative Works; and

      (d) If the Work includes a "NOTICE" text file as part of its
          distribution, then any Derivative Works that You distribute must
          include a readable copy of the attribution notices contained
          within such NOTICE file, excluding those notices that do not
          pertain to any part of the Derivative Works, in at least one
          of the following places: within a NOTICE text file distributed
          as part of the Derivative Works; within the Source form or
          documentation, if provided along with the Derivative Works; or,
          within a display generated by the Derivative Works, if and
          wherever such third-party notices normally appear. The contents
          of the NOTICE file are for informational purposes only and
          do not modify the License. You may add Your own attribution
          notices within Derivative Works that You distribute, alongside
          or as an addendum to the NOTICE text from the Work, provided
          that such additional attribution notices cannot be construed
          as modifying the License.

      You may add Your own copyright statement to Your modifications and
      may provide additional or different license terms and conditions
      for use, reproduction, or distribution of Your modifications, or
      for any such Derivative Works as a whole, provided Your use,
      reproduction, and distribution of the Work otherwise complies with
      the conditions stated in this License.

   5. Submission of Contributions. Unless You explicitly state otherwise,
      any Contribution intentionally submitted for inclusion in the Work
      by You to the Licensor shall be under the terms and conditions of
      this License, without any additional terms or conditions.
      Notwithstanding the above, nothing herein shall supersede or modify
      the terms of any separate license agreement you may have executed
      with Licensor regarding such Contributions.

   6. Trademarks. This License does not grant permission to use the trade
      names, trademarks, service marks, or product names of the Licensor,
      except as required for reasonable and customary use in describing the
      origin of the Work and reproducing the content of the NOTICE file.

   7. Disclaimer of Warranty. Unless required by applicable law or
      agreed to in writing, Licensor provides the Work (and each
      Contributor provides its Contributions) on an "AS IS" BASIS,
      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
      implied, including, without limitation, any warranties or conditions
      of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A
      PARTICULAR PURPOSE. You are solely responsible for determining the
      appropriateness of using or redistributing the Work and assume any
      risks associated with Your exercise of permissions under this License.

   8. Limitation of Liability. In no event and under no legal theory,
      whether in tort (including negligence), contract, or otherwise,
      unless required by applicable law (such as deliberate and grossly
      negligent acts) or agreed to in writing, shall any Contributor be
      liable to You for damages, including any direct, indirect, special,
      incidental, or consequential damages of any character arising as a
      result of this License or out of the use or inability to use the
      Work (including but not limited to damages for loss of goodwill,
      work stoppage, computer failure or malfunction, or any and all
      other commercial damages or losses), even if such Contributor
      has been advised of the possibility of such damages.

   9. Accepting Warranty or Additional Liability. While redistributing
      the Work or Derivative Works thereof, You may choose to offer,
      and charge a fee for, acceptance of support, warranty, indemnity,
      or other liability obligations and/or rights consistent with this
      License. However, in accepting such obligations, You may act only
      on Your own behalf and on Your sole responsibility, not on behalf
      of any other Contributor, and only if You agree to indemnify,
      defend, and hold each Contributor harmless for any liability
      incurred by, or claims asserted against, such Contributor by reason
      of your accepting any such warranty or additional liability.

   END OF TERMS AND CONDITIONS

   APPENDIX: How to apply the Apache License to your work.

      To apply the Apache License to your work, attach the following
      boilerplate notice, with the fields enclosed by brackets "{}"
      replaced with your own identifying information. (Don't include
      the brackets!)  The text should be enclosed in the appropriate
      comment syntax for the file format. We also recommend that a
      file or class name and description of purpose be included on the
      same "printed page" as the copyright notice for easier
      identification within third-party archives.

   Copyright {yyyy} {name of copyright owner}

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

```
