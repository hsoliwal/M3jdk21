## Vavr (development state 2.0.0-SNAPSHOT)

### Origin

The Java sources under `providers/classes/jdk/m3/collections/vavr` of the module `jdk.m3.collections` are derived from
Vavr (https://github.com/vavr-io/vavr) at commit `9fb5dab2dd5b8bc0c475ce43feebbd8803729416` (2026-11-15) of its
branch `main`. It is a development state, not a release: the build file says 2.0.0-SNAPSHOT. The newest release on
Maven Central is 1.1.0 (2026-10-24), made from another line of development: this commit is 184 commits ahead of the
tag `v1.1.0` and 69 behind it, and against the sources jar of 1.1.0, 96 main sources differ, one was added (the
module descriptor) and five were removed (the four `GwtIncompatible` annotations and `Patterns`, see below). The fork
https://github.com/hsoliwal/vavr (`3663be4a8c00bfb9eed7db19b14addf10a36b6c7`) is two commits ahead of that commit;
they change the root `pom.xml` only, so the sources are the library's.

Taken are 97 sources of the project `vavr`:

- its 66 main sources (`src/main/java`) without the module descriptor;
- its 30 generated sources that the repository holds (`src-gen/main/java`, written by the library's Scala generator,
  which was not run again);
- `Patterns.java`, which the library's build generates from the class `$` with its own annotation processor
  (`io.vavr.match.PatternsProcessor` of `io.vavr:vavr-match-processor:1.0.0`, Apache License 2.0, found on the class
  path during the build). It was generated here with that processor from these sources; the result is the same file,
  byte for byte, as the `Patterns.java` in the sources jar of release 1.1.0. It is the pattern API used with
  `API.Match` (`$Some`, `$None`, `$Tuple2`, `$Cons`, `$Success`, ...).

The tests, the benchmarks, the generator and the two `readme-contributing.md` files are not taken.

Vavr is licensed under the Apache License, Version 2.0. The text below is the repository's file `LICENSE`. The
repository has no NOTICE file. 92 of the 97 sources carry the header "Copyright 2014-2026 Vavr, https://vavr.io" with
the Apache License; the four `package-info.java` and the generated `Patterns.java` carry none. The headers are
unchanged.

The documentation names the work some structures follow: Okasaki's "Purely Functional Data Structures" (`List`,
`Queue`, `Stream`), Kazu Yamamoto's Haskell red-black tree and the papers of Okasaki and Kahrs (`RedBlackTree`),
Blelloch, Ferizovic and Sun, Scala's `ConcatIterator` and `RedBlackTree`, Scalaz's `Validation`, and a Stack Overflow
answer (`FutureImpl`, `Promise`). No source says that it was copied from other code.

### Changes made by Synexia (2026-11-17)

The changes were mechanical (com.synexia repository, recipe crate `inverse-package-relocation-20261116`,
`provider_pipeline.sh`; the run is in `PROVIDER_RUNS.md` there):

- the package `io.vavr` is named `jdk.m3.collections.vavr`;
- the package-private class `$` is named `PatternExtractors`: a file named `$.java` cannot be a source of the JDK
  build, whose make files read `$` as a variable. Only its declaration and its uses qualified with its package (the
  generated `Patterns` calls `io.vavr.$::Tuple2` and so on) were renamed; the public methods named `$` of `API` are
  unchanged;
- the annotations `@Patterns` (1) and `@Unapply` (20) of `io.vavr.match.annotation` were removed from that class with
  their two imports: they are the input of the processor above and have no effect at run time (retention `CLASS`);
- the annotations of JSpecify were removed with their imports, as for the Guava provider: 4,833 annotations
  (`@Nullable` 4,821, `@NonNull` 8, `@NullMarked` 4) and 97 imports in 93 files;
- `@SuppressWarnings` was added to two top-level types for the lint categories the JDK build reports as errors:
  `collection.HashArrayMappedTrie` (`unchecked`) and `concurrent.Future` (`deprecation`: it uses the deprecated
  `concurrent.Task` in a signature, and javac reports that without a source position). Two types had one already;
- trailing blanks were removed from 56 lines in 26 files (the repository's rule for Java sources, `.jcheck/conf`).

No top-level type was moved to another file.

Compiled before and after these changes (the reference with the two renames and the removed vavr-match annotations,
nothing else), every one of the 435 classes has the same members and the same instructions.

### Exported packages

The four packages the library's own module descriptor exports: `vavr`, `vavr.collection`, `vavr.control`,
`vavr.concurrent` (all of its packages). The descriptor's `requires static io.vavr.match` and
`requires static org.jspecify` name the annotation modules removed above; nothing else is required.

### What to know

`concurrent.Future` uses virtual threads by default (`Executors.newVirtualThreadPerTaskExecutor()`). The interface
`concurrent.Task` is deprecated in the library.

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
