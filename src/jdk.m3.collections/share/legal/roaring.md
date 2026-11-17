## RoaringBitmap (release 1.6.30 on Maven Central, plus one later commit)

### Origin

The Java sources under `providers/classes/jdk/m3/collections/roaring` of the module `jdk.m3.collections` are derived
from RoaringBitmap (https://github.com/RoaringBitmap/RoaringBitmap) at commit
`5268d7178913d3a9137fd3400736dd1b93309b62` (2026-11-14) of its branch `master`. The fork
https://github.com/hsoliwal/RoaringBitmap is identical to the library at that commit: nothing was changed in the fork.

The sources are those of the release published on Maven Central as version 1.6.30 (2026-11-14) plus one commit,
"Prevent run container iterators from rewinding in advanceIfNeeded" (#909), which changes `RunContainer` and
`buffer.MappeableRunContainer`: the sources jar of `RoaringBitmap` 1.6.30 differs from the main sources of this
commit in these two files only, and the sources jar of `bsi` 1.6.30 not at all. (The sources jar holds only the Java 8
`ArraysShim`; the Java 11 one is in the binary jar, under `META-INF/versions/11`.) The names in the library's GitHub
repository are one step behind Maven Central: the tag named `1.6.29` (`e0c9d3c4ac49`) is the state Maven Central has
as 1.6.30, the commit after it sets the version number to 1.6.30, and there is no tag `1.6.30`. Maven Central's
1.6.29 is the release before.

Taken are 114 sources of two of the repository's projects:

- `roaringbitmap`: its 105 main sources. The library has two versions of `ArraysShim`, and its jar is a multi-release
  jar: on Java 11 and later the version in `src/java11` replaces the one in `src/main`. The version from `src/java11`
  is the one here, as on any Java 21 run time. The version for Java 8 is not here.
- `bsi` (bit-sliced index): its 9 main sources, as the packages `jdk.m3.collections.roaring.bsi`, `.bsi.buffer` and
  `.bsi.longlong`.

The repository's other projects (`examples`, `fuzz-tests`, `jmh`, `real-roaring-dataset`, `simplebenchmark`) and the
tests are not taken.

RoaringBitmap is licensed under the Apache License, Version 2.0. The text below is the repository's file
`LICENSE-2.0.txt`, which names the copyright holders ("Copyright 2013-2016 the RoaringBitmap authors"). 44 of the 114
sources carry the header "(c) the authors Licensed under the Apache License, Version 2.0."; the other 70 (61 of
`roaringbitmap`, all 9 of `bsi`) carry no license header. The headers are unchanged. The repository's `AUTHORS` file,
"the official list of authors for copyright purposes", is reproduced below. The repository has no NOTICE file.

Two sources of `bsi` state another origin in their documentation and carry no header:

- `bsi.WritableUtils`: "copy from org.apache.hadoop.io.WritableUtils" (Apache Hadoop);
- `bsi.Pair`: "Copied from org.apache.hadoop.hbase.util.Pair" (Apache HBase).

Both projects are licensed under the Apache License, Version 2.0, and their files carry the header of the Apache
Software Foundation, which the copies in the library do not have. The attributions of their NOTICE files are
reproduced below.

The sources also point to other work without copying a file: the comparators of the bit-sliced index refer to
https://github.com/lemire/BitSliceIndex (five links, kept as they are); `longlong.Roaring64NavigableMap` refers to
`roaring64map.hh` of CRoaring (the 64-bit format and two private methods), to an article on java-performance.info,
and says of a private binary search "From Arrays.binarySearch (Comparator)";
`bsi.longlong.Roaring64NavigableMapSliceIndex` refers to `bsi64.go` of RoaringBitmap/roaring (its format);
`longlong.RoaringIntPacking` cites a Stack Overflow question for a one-line method.

### Changes made by Synexia (2026-11-17)

The changes were mechanical (com.synexia repository, recipe crate `inverse-package-relocation-20261116`,
`provider_pipeline.sh`):

- the package `org.roaringbitmap` is named `jdk.m3.collections.roaring`, in package and import declarations,
  qualified names and documentation. Five links in comments of `bsi` that hold the path `org/roaringbitmap` of
  another repository are not renamed;
- 12 top-level types that shared a source file with another type and are used from another file were moved to files
  of their own, with the header and the imports of the file they came from: the two character iterators of each of
  `ArrayContainer`, `BitmapContainer`, `RunContainer` and `buffer.MappeableBitmapContainer`, and two of the four of
  each of `buffer.MappeableArrayContainer` and `buffer.MappeableRunContainer` (the two `Raw...` ones are used in
  their own file only and stay). javac warns when such a type is used from another file, and the JDK build treats
  warnings as errors. Nothing inside a type changed;
- `@SuppressWarnings` was added to 42 top-level types for the lint categories the JDK build reports as errors;
- documentation, for doclint: one bare `<` was written as `&lt;` (`SuccinctRank`, in
  `Returns the number of integers <= x.`); in `bsi.longlong.Roaring64NavigableMapSliceIndex` nine `</br>` became
  `<br>` and one `</p>` that closes nothing was removed.

Compiled from the renamed, otherwise unedited sources and from these sources, every one of the 191 classes has the
same members and the same instructions.

### Exported packages

Exported are the four packages the library's own module descriptor exports (`roaring`, `roaring.buffer`,
`roaring.insights`, `roaring.longlong`) and the three packages of `bsi`, which has no descriptor.
`jdk.m3.collections.roaring.art` is not exported, as in the library. As in the library, the public class
`longlong.HighLowContainer` has types of that package in its signatures (`ContainerIterator`, `KeyIterator`,
`LeafNodeIterator`): code outside the module cannot name these types.

### Beside this provider

The module has two classes of its own named `RoaringBitmap` and `Roaring64Bitmap` in the package
`jdk.internal.m3.collections.bits`, which is not exported. They are scaffolds whose instance methods throw
`NotYetReceived`. Its exported package `jdk.m3.collections.bits` has `RoaringIntSet` and `RoaringLongSet`,
implementations of its own, and its exported package `jdk.m3.collections` has its own `IntIterator` and
`LongIterator`. None of these is this library or uses it.

### Notices of Apache Hadoop and Apache HBase (for `bsi.WritableUtils` and `bsi.Pair`)

```
Apache Hadoop
Copyright 2006 and onwards The Apache Software Foundation.

This product includes software developed at
The Apache Software Foundation (http://www.apache.org/).
```

```
Apache HBase
Copyright 2007-2026 The Apache Software Foundation

This product includes software developed at
The Apache Software Foundation (http://www.apache.org/).
```

### Authors

```
# This is the official list of authors for copyright purposes
Daniel Lemire,
Owen Kaser,
Samy Chambi,
Jon Alvarado,
Rory Graves,
Björn Sperber,
Seth Pellegrino,
Borislav Ivanov,
Gregory Ssi-Yan-Kai,
Galderic Puntí,
Navis Ryu,
Jerven Bolleman,
Keuntae Park,
Olaf Krische,
George Kankava,
Junyuan Zheng,
Przemek Piotrowski,
Olivier Peyrusse,
Richard Startin,
Erik Gorset,
Benoit Lacelle,
Yizhu Sun,
Amit Desai,
Simon Spero,
Rafael Telles,
Mehrez Douaihy,
Nuwan Sanjeewa Abeysiriwardana
```

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
      boilerplate notice, with the fields enclosed by brackets "[]"
      replaced with your own identifying information. (Don't include
      the brackets!)  The text should be enclosed in the appropriate
      comment syntax for the file format. We also recommend that a
      file or class name and description of purpose be included on the
      same "printed page" as the copyright notice for easier
      identification within third-party archives.

   Copyright 2013-2016 the RoaringBitmap authors

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
