<!-- SPDX-License-Identifier: Apache-2.0 -->
# Fork-first Apache CircularFifo M3 compatibility receipt

Original fork [hsoliwal/commons-collections](https://github.com/hsoliwal/commons-collections)
at `2d985c27e07bdcbb9516aea44b0dda3f3e487ced`; staged
package-only relocation in [fork PR #1](https://github.com/hsoliwal/commons-collections/pull/1)
commit `dd08e3a984fe619380aedac88b1b5055ff1b1753`. Canonical Synexia generator [#12290](https://github.com/hsoliwal/com.synexia/pull/12290)
source head `821816093f4c786d056a063d761007023c619b86`; public M3JDK21 pinned base `2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49`.

**This is a verification receiver, not a competing M3 runtime queue.**
All four original/relocated Apache complete Java sources are mirrored as
test-only reference custody under `reference/` and stay outside build
production source roots and JMOD. The one generated standalone Java21 oracle
under `src/test/java` compares original Apache, package-only relocated
Apache and the pre-existing M3CircularFifoQueue (state-mask/offset backend).
Three-source comparisons retain original Apache-2.0 headers, serial UID,
iteration, state masks, null/exception contracts and logical indexes.

The first product commit mirrors canonical recipe and source provenance.
The second commit installs ONLY the source-sealed test postimage. No
existing M3 source, JNI symbol, class/library public signature, POM,
java.util, or Synexia develop is mutated. CI must compile *real code* before
calling compatibility proven; no separate performance claim.
