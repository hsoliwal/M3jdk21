<!-- SPDX-License-Identifier: Apache-2.0 -->
# In-place M3JDK21 indexed viewport CPU/JNI projection

M3JDK21 itself is the Java/JDK/HotSpot/JNI product, not an M3 layer on top of another JDK.
This PR enhances the current `M3MaskViewportKernel` owner, adding a single
package-local primitive emission atom: ordered row IDs, source occurrence ordinals,
and all 32 state-mask bits for one selected visible page. CPU model/viewport projection
are authoritative; SWT, Nebula, GEF and RAP/RWT consume viewports to render and handle
input using their existing API contracts and backend-specific resources.

The existing JNI C file receives a purely additive optional batch symbol. No UI callback,
reference, predicate, theme, font, locale, or object payload crosses JNI. The old native
project() ABI and original public Java methods remain unchanged. Sources do not create
another row/object store and do not modify native widget lifecycle.

Canonical source-prepared recipe:
https://github.com/hsoliwal/com.synexia/tree/m3/viewport-frame-lanes-recipes-20261021-chatgpt/synexia-openrewrite-recipes/recipe-crates/m3-ast-build-reverse-20261017/datasets/m3-viewport-frame-20261021
All existing original-product preimages were rechecked byte-for-byte before receiving;
the four Java/C receiving outputs are copied unchanged from that dataset's templates.

Authoring proof: separate standalone Java21 28,847 CPU assertions and 40,847 JNI assertions
using -Xcheck:jni (not a full reactor proof). Exact-head original-class Java/JNI suite is
committed in this PR's workflow. Maven/OpenRewrite SDK result/fixed-point receipts and
full JDK, SWT/Nebula/GEF/RAP native, GPU, themes/RTL, accessibility, performance gates
must be qualified separately. Keep draft until those pass.
