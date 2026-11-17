## Brownies Collections 0.9.24

### Origin

The Java sources under `providers/classes/jdk/m3/collections/brownies` of the module `jdk.m3.collections` are derived
from Brownies Collections (https://github.com/magicwerk/brownies-collections), release 0.9.24: commit
`3ce3db5c1103cda9cb7ae617a448040468347ed8` (2026-01-09, "Release 0.9.24"), which carries the library's tag `0.9.24`.
The fork https://github.com/hsoliwal/brownies-collections is identical to the library at that commit: nothing was
changed in the fork. All 104 main sources are taken; the tests are not.

Brownies Collections is licensed under the Apache License, Version 2.0. The text below is the repository's file
`LICENSE.md`. The repository has no NOTICE file. The headers of the sources are unchanged. By their headers the 104
sources are:

- 88 with a copyright line of Thomas Mauch (2010: 2, 2012: 22, 2013: 37, 2014: 21, 2015: 3, 2023: 3) and the Apache
  License header;
- `helper.SortedLists`: "Copyright (C) 2010 The Guava Authors", Apache License header. Its first lines say it was
  copied from `com.google.common.collect.SortedLists` of Guava because that class is not public. The eight classes
  `helper.primitive.BooleanBinarySearch`, `ByteBinarySearch`, `CharBinarySearch`, `DoubleBinarySearch`,
  `FloatBinarySearch`, `IntBinarySearch`, `LongBinarySearch` and `ShortBinarySearch` follow the same Guava class
  (its documentation text and its comments are in them); they are among the 88 and name Thomas Mauch only;
- `helper.MutableInt`: the header of the Apache Software Foundation ("Licensed to the Apache Software Foundation
  (ASF) under one or more contributor license agreements. See the NOTICE file distributed with this work ..."). Its
  first lines say it was copied from `org.apache.commons.lang3.mutable.MutableInt` of Apache Commons Lang and
  "slightly adapted to work as standalone class". The attribution of the NOTICE file of Apache Commons Lang is
  reproduced below;
- 14 without a copyright line or license header: the five `package-info.java`, and nine merge sort classes
  (`helper.MergeSort` and `helper.primitive.BooleanMergeSort`, `ByteMergeSort`, `CharMergeSort`, `DoubleMergeSort`,
  `FloatMergeSort`, `IntMergeSort`, `LongMergeSort`, `ShortMergeSort`). The first lines of each of the nine say "This
  file has been copied from http://thomas.baudel.name/Visualisation/VisuTri/inplacestablesort.html". That page says
  "This code is litteraly copied from the C++ stl library and translated in Java" and "Copyright Thomas Baudel,
  1998-2006"; it has no license statement (read 2026-11-17). Brownies Collections distributes these files as part
  of the library under its Apache License; neither the files nor the page state terms of their own. The code has
  the structure of the C++ Standard Template Library's merge without a buffer (`__merge_without_buffer`: the names
  `first_cut`, `second_cut`, `len11`, `len22`, lower and upper bound, rotation). The page does not name the STL
  implementation it translated; the STL of Hewlett-Packard and Silicon Graphics, from which the common
  implementations descend, carries the two notices reproduced below, which ask that they appear in all copies. Whether
  they apply to this translation is a question for the owner; they are reproduced here so that nothing is missing if
  they do.

Two more origins are not stated in a header (the third, Guava's `SortedLists` for the eight binary search classes, is
named above). They are among the 88 sources that name Thomas Mauch only:

- the tree node of `BigList` (`BigList.BlockNode`) and of the eight primitive big lists (`primitive.BooleanBigList`
  to `primitive.ShortBigList`, their `...BlockNode` classes) is adapted from `TreeList.AVLNode` of Apache Commons
  Collections: its documentation ("Implements an AVLNode ...", the "faedelung" flags) and its comments are those of
  that class, which carries the header of the Apache Software Foundation (Apache License, Version 2.0). The
  attribution of the NOTICE file of Apache Commons Collections is reproduced below;
- `KeyCollectionAsMap.equals` is, by its own comment, "Copied from AbstractMap" (`java.util.AbstractMap`), and the
  private class `helper.GapLists.CollectorImpl` follows `java.util.stream.Collectors.CollectorImpl`. Method
  documentation in `IList` and the primitive `I...List` classes repeats text of the `java.util` documentation. The
  origin is the JDK (OpenJDK: GNU General Public License, version 2, with the Classpath exception, the license of
  this repository).

The library does not say which release of Guava, Apache Commons Lang or Apache Commons Collections it copied from.

### Notice of Apache Commons Lang (for `helper.MutableInt`)

```
Apache Commons Lang
Copyright 2001-2026 The Apache Software Foundation

This product includes software developed at
The Apache Software Foundation (https://www.apache.org/).
```

### Notices of the Standard Template Library (for the nine merge sort classes)

As in `libstdc++-v3/include/bits/stl_algo.h` of GCC:

```
Copyright (c) 1994
Hewlett-Packard Company

Permission to use, copy, modify, distribute and sell this software
and its documentation for any purpose is hereby granted without fee,
provided that the above copyright notice appear in all copies and
that both that copyright notice and this permission notice appear
in supporting documentation.  Hewlett-Packard Company makes no
representations about the suitability of this software for any
purpose.  It is provided "as is" without express or implied warranty.


Copyright (c) 1996
Silicon Graphics Computer Systems, Inc.

Permission to use, copy, modify, distribute and sell this software
and its documentation for any purpose is hereby granted without fee,
provided that the above copyright notice appear in all copies and
that both that copyright notice and this permission notice appear
in supporting documentation.  Silicon Graphics makes no
representations about the suitability of this software for any
purpose.  It is provided "as is" without express or implied warranty.
```

### Notice of Apache Commons Collections (for the tree node of the big lists)

```
Apache Commons Collections
Copyright 2001-2026 The Apache Software Foundation

This product includes software developed at
The Apache Software Foundation (https://www.apache.org/).
```

### Changes made by Synexia (2026-11-17)

The changes were mechanical (com.synexia repository, recipe crate `inverse-package-relocation-20261116`,
`provider_pipeline.sh`):

- the package `org.magicwerk.brownies.collections` is named `jdk.m3.collections.brownies`;
- 8 top-level types that shared a source file with another type were moved to files of their own, with the header
  and the imports of the file they came from: `primitive.IBooleanListable`, `IByteListable`, `ICharListable`,
  `IDoubleListable`, `IFloatListable`, `IIntListable`, `ILongListable` and `IShortListable`, each from the file of its
  `I...List` class. javac warns when such a type is used from another file, and the JDK build treats warnings as
  errors. Nothing inside a type changed;
- `@SuppressWarnings` was added to 71 top-level types and extended on 5 of the 8 that had one, for the lint
  categories the JDK build reports as errors;
- white space, for the repository's rule for Java sources (`.jcheck/conf`: no tab, no trailing blank, no carriage
  return): the line ends of 79 files were CR LF (71) or mixed (8) and are LF; tabs became spaces at four-column
  stops and trailing blanks went, in 99 files (24,263 lines);
- documentation, for doclint, which the JDK build runs as errors:
  - `<tt>` became `<code>` in 107 comments;
  - 24 links that do not resolve became code text, eight each: `{@link #asDeque}` in the primitive `...GapList`
    classes, which have no `asDeque`; `{@link Key1List}` in the primitive `...BigList` classes and `{@link BigList}`
    in the primitive `I...List` classes, where the type is in another package and not imported;
  - 80 `@see` references that do not resolve are kept as text, `@see "..."`, in the primitive classes:
    `#indexOf(Object)` and `#lastIndexOf(Object)` (24 each; the primitive class has the method with its primitive
    type), `List#toArray()` and `List#toArray(Object[])` (16 each; `List` is not imported);
  - 144 `@param` tags that name no parameter were removed with their text: in the primitive variants the tag of a
    type parameter lost its name, and tags of parameters the primitive method does not have remained (for example
    `@param comparator`);
  - 9 `<p>` that open no paragraph were removed.

Compiled from the renamed, otherwise unedited sources and from these sources, every one of the 240 classes has the
same members and the same instructions.

### Exported packages

The library has no module descriptor; its manifest exports all five packages, and all five are exported here.

### Known properties of the library, unchanged here

- The interfaces `primitive.I...Listable` are package-private and appear in public signatures of the public
  `I...List` classes: code outside the package cannot name them.
- A copy of a `BigList` or of a primitive `...BigList` (`copy()`) is not independent of the list. Sorting the copy of
  a small list sorts the list as well (up to 950 elements with the default block size of 1000, in an independent
  test): after `BigList.create(3, 1, 2).copy().sort(null)` the list reads `[1, 2, 3]` (`GapList` keeps
  `[3, 1, 2]`). After many inserts and removes at random places (300,000
  and 100,000 in the test) a copy cannot be sorted: `IntBigList.copy().sort()` fails with
  `ArrayIndexOutOfBoundsException`, `BigList.copy().sort(null)` with `NullPointerException`; an independent test found
  such a copy unreadable before any sort. The list itself stays readable and sortable. The library's classes,
  compiled from its unchanged sources, behave the same in the same tests.
- An independent random test of 322,493 calls found no difference between the library and this provider, and met
  further defects that both have (a `KeyCollection` that cannot be iterated after `removeIf` or `retainAll` in some
  states, among others). This list is not complete.
- `primitive.GapListPrimitives` is in the sources of the tag and is here, but it is not in the released jar
  (`brownies-collections-0.9.24.jar` on Maven Central); nothing else in the library uses it.

### Beside this provider

The module has classes of its own named `BigList` and `LongBigList`, as the library has, in its own packages. They are
not this library and do not use it.

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

   Copyright [yyyy] [name of copyright owner]

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
