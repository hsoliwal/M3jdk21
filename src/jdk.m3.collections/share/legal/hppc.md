## HPPC 0.13.0

### Origin

The Java sources under `providers/classes/jdk/m3/collections/hppc` of the module `jdk.m3.collections` are derived
from HPPC, High Performance Primitive Collections (https://github.com/carrotsearch/hppc), release 0.13.0, except one:
546 of the 547 sources are the 546 Java sources of the sources jar `com.carrotsearch:hppc:0.13.0` on Maven Central
(2026-11-04; its SHA-1 is the one Maven Central publishes for it), without its module descriptor, and the 547th,
`ArraySizingStrategies.java`, is Synexia's (see "Growth policy superset by Synexia" below). The fork
https://github.com/hsoliwal/hppc is identical to the library; its head `cce6bb7f5ddfe8319bdbb7056cbea657fe91614a` is
the tag `0.13.0` plus one commit that changes `CHANGES.txt` and `build-options.properties` only.

525 of the 546 derived sources are generated: the library's build writes them from 26 templates
(`hppc/src/main/templates`) with its own template processor (ANTLR and Velocity) and does not keep them in the
repository. They are taken from the sources jar rather than generated again here. That the jar holds the sources of
the release was checked: its 21 other sources are byte for byte the files of the repository, and compiled with
javac 21 the 546 sources give the 1,040 classes of the released jar `hppc-0.13.0.jar` with the same members and the
same instructions, except the order in which the constructors of the anonymous classes in the 40
`SortedIteration...HashMap` classes store their synthetic fields (the enclosing instance and captured variables),
and, in 282 classes, extra entries of the debug line-number tables in the release. Both are code generation of the
newer javac that built the release, not the sources.

HPPC is licensed under the Apache License, Version 2.0. The text below is the repository's file `LICENSE.txt`, which
names the copyright holder ("Copyright 2010-2013, Carrot Search s.c., Boznicza 11/56, Poznan, Poland"). The 21
hand-written sources carry the header "Copyright (C) 2010-2026 Carrot Search s.c. and contributors / All rights
reserved. / Refer to the full license file "LICENSE.txt"", which points to the Apache License of the repository; the
525 generated sources carry none (the templates have none). These headers are unchanged, except that the header of
`ArraySizingStrategy.java` has a modification notice (two lines), which records Synexia's modification. `ArraySizingStrategies.java`
carries Synexia's header (Apache License 2.0 or Eclipse Public License 2.0). The repository's `NOTICE.txt` is
reproduced below.

Origins the library states:

- `NOTICE.txt`: "HPPC borrowed code, ideas or both from" Apache Lucene, fastutil and Koloboke, all under the Apache
  License;
- `RamUsageEstimator` is "Mostly forked from Lucene tag releases/lucene-solr/8.5.1" and `Accountable` "Partly forked
  from" the same tag.

Origins found by comparison, which no header states (they are among the 21 hand-written sources, whose headers name
Carrot Search only):

- `BitUtil` is Apache Lucene's `org.apache.lucene.util.BitUtil` (8.5.1: 98% of its tokens, with Lucene's comment
  "see LUCENE-2221");
- `BitSet` and `BitSetIterator` follow Lucene's `OpenBitSet` and `OpenBitSetIterator` of the 3.x line (3.0.3: 63% and
  about 95% of their tokens, with Lucene's documentation);
- `PlaModel`, on which the PGM indexes (`...PgmIndex`) are built, is a translation of `add_point` of
  `OptimalPiecewiseLinearModel` in the C++ PGM-index (https://github.com/gvinciguerra/PGM-index,
  `include/pgm/piecewise_linear_model.hpp`; Apache License 2.0; "Copyright (c) 2018 Giorgio Vinciguerra"): the same
  branches, local names and comments. The PGM index classes cite the paper and https://pgm.di.unipi.it/;
- `XorShift128P` follows `xorshift128plus.c` (cited; public domain), and its `nextInt(int)` is the algorithm of
  OpenJDK's `java.util.Random.nextInt(int)`.

The Lucene classes carry the header of the Apache Software Foundation in Lucene; HPPC's copies do not. The
attributions of the NOTICE files of the Lucene versions they come from are reproduced below.

### Changes made by Synexia (2026-11-17)

The changes were mechanical (com.synexia repository, recipe crate `inverse-package-relocation-20261116`,
`provider_pipeline.sh`; the run is in `PROVIDER_RUNS.md` there):

- the package `com.carrotsearch.hppc` is named `jdk.m3.collections.hppc`;
- `@SuppressWarnings` was added to 141 of the 546 top-level types and extended on 28 of the 29 that had one
  (`AbstractObjectCollection` unchanged), for the lint categories the JDK build reports as errors.

These mechanical changes moved no top-level type to another file and changed no documentation comment and no white
space.

Compiled before and after these changes, every one of the 1,040 classes has the same members and the same
instructions.

### Growth policy superset by Synexia (2026-11-17)

`ArraySizingStrategy` is now annotated `@FunctionalInterface` and has default `ramBytesAllocated()` and
`ramBytesUsed()` (the object header), so `grow` is its only abstract method and a lambda is a strategy. Its new
static factories are a superset of the array-list growth policies of the module's collection libraries: `hppc()`
(the existing `BoundedProportionalArraySizingStrategy.DEFAULT_INSTANCE`), `arrayList()`, `fastList()`,
`immutableCollectionBuilder()` and `gapList()`; the last four return the constants of the new package-private enum
`ArraySizingStrategies` (Synexia's file). `ArraySizingStrategies` re-expresses the growth arithmetic of
`java.util.ArrayList` with `jdk.internal.util.ArraysSupport.newLength` (OpenJDK), Eclipse Collections
`FastList.sizePlusFiftyPercent`/`ensureCapacity`, Guava `ImmutableCollection.Builder.expandedCapacity` and Brownies
Collections `CapacityHelper.calculateCapacity`. `grow`, `BoundedProportionalArraySizingStrategy` and the HPPC
containers are unchanged. The class documentation comment of `ArraySizingStrategy` is rewritten for the new role.
`ArraySizingStrategy.java` retains its original copyright and license and records this modification in its header.
`arrayList()` and `fastList()` keep a length that already holds what is needed (2026-11-17, second round), as their
lists do not grow then.

### Exported packages

The seven packages the library's own module descriptor exports, which are all of its packages: `hppc`,
`hppc.comparators`, `hppc.cursors`, `hppc.internals`, `hppc.predicates`, `hppc.procedures`, `hppc.sorting`. The
descriptor's `requires java.logging` is required by this module already. Its `requires static jdk.management` is not
carried: `RamUsageEstimator` reaches `com.sun.management.HotSpotDiagnosticMXBean` by reflection only. In a default
run the module `jdk.management` is there and the estimates equal the library's; without it, both assume uncompressed
references and class pointers and overestimate sizes on a default 64-bit JVM.

### Notice of the library

```
ACKNOWLEDGEMENT
===============

HPPC borrowed code, ideas or both from:

 * Apache Lucene, https://lucene.apache.org/
   (Apache license)
 * Fastutil, https://fastutil.di.unimi.it/
   (Apache license)
 * Koloboke, https://github.com/leventov/Koloboke
   (Apache license)
```

### Notices of Apache Lucene

For `RamUsageEstimator`, `Accountable` and `BitUtil` (Lucene 8.5.1, `META-INF/NOTICE.txt` of
`lucene-core-8.5.1-sources.jar`):

```
Apache Lucene
Copyright 2001-2020 The Apache Software Foundation

This product includes software developed at
The Apache Software Foundation (http://www.apache.org/).
```

For `BitSet` and `BitSetIterator` (Lucene 3.0.3, `META-INF/NOTICE.txt` of `lucene-core-3.0.3-sources.jar`; the rest
of that file names parts of Lucene that are not here):

```
Apache Lucene
Copyright 2006 The Apache Software Foundation

This product includes software developed by
The Apache Software Foundation (http://www.apache.org/).
```

### Notice of PGM-index (for `PlaModel`)

```
Copyright (c) 2018 Giorgio Vinciguerra.

Licensed under the Apache License, Version 2.0
```

### License

```

                                 Apache License
                           Version 2.0, January 2004
                        https://www.apache.org/licenses/

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
      boilerplate notice, with the fields enclosed by brackets "[]"
      replaced with your own identifying information. (Don't include
      the brackets!)  The text should be enclosed in the appropriate
      comment syntax for the file format. We also recommend that a
      file or class name and description of purpose be included on the
      same "printed page" as the copyright notice for easier
      identification within third-party archives.

   Copyright 2010-2013, Carrot Search s.c., Boznicza 11/56, Poznan, Poland

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       https://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

```
