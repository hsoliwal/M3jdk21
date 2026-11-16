## Bifurcan 0.2.0

### Origin

The Java sources under `providers/classes/jdk/m3/collections/bifurcan` of the module `jdk.m3.collections` are
derived from Bifurcan (https://github.com/lacuna/bifurcan) at commit `fd024881ad9ba6019ad3c468a52d4a3c2ff22115`
(2026-10-17) of its branch `master`: all 47 Java sources. That commit is the release tag `0.2.0`
(`99689b15bb42062fe4a92aa9aee0f85de081bbc2`) plus one commit that adds a deployment script, so the sources are those
of release 0.2.0: the 47 Java sources inside `bifurcan-0.2.0.jar` on Maven Central are the same files. The fork
https://github.com/hsoliwal/bifurcan is identical to the library at that commit: nothing was changed in the fork.
The tests, which are written in Clojure, and the benchmarks are not taken.

Bifurcan is licensed under the MIT License. The text below is the repository's file `LICENSE`, which has no copyright
line. The repository's `README.md` states the copyright:

```
Copyright © 2016-2020 Zachary Tellman

Distributed under the MIT License
```

No source carries a copyright line or a license header. The documentation of several structures names the papers
they follow (the hash maps of Steindorfer and Vinju, relaxed radix trees, the red-black tree of Germane, "Fast
Mergeable Integer Maps"); no source says that it was copied from other code.

### Changes made by Synexia (2026-11-17)

The changes were mechanical (com.synexia repository, recipe crate `inverse-package-relocation-20261116`,
`provider_pipeline.sh`):

- the package `io.lacuna.bifurcan` is named `jdk.m3.collections.bifurcan`;
- `@SuppressWarnings` was added to 34 top-level types and extended on 6 that had one, for the lint categories the JDK
  build reports as errors;
- a trailing blank was removed from one line (the repository's rule for Java sources, `.jcheck/conf`).

No top-level type was moved to another file, and no documentation comment was changed.

Compiled before and after these changes, every one of the 122 classes has the same members and the same
instructions.

### Exported packages

The library has no module descriptor; all four packages are exported (`bifurcan`, `bifurcan.hash`,
`bifurcan.nodes`, `bifurcan.utils`).

### License

```
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.```
