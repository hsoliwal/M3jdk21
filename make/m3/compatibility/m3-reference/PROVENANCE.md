# Pinned Synexia frozen-text compatibility fixture

The two upstream source files below are copied byte-for-byte from the private
Synexia donor at commit `64a2ea61c73b548413fed6686a9daeeb0b9b0564`.
Their Apache-2.0 SPDX headers are retained, and `LICENSE-2.0.txt` and the
applicable attribution in `NOTICE.md` accompany them.

| Upstream path | Git blob SHA-1 |
|---|---|
| `synexia-indexstring/src/main/java/com/synexia/indexstring/FrozenChars.java` | `791fffa3f9938e5266663c1cb256cc4d00f8c0f5` |
| `synexia-indexstring/src/main/java/com/synexia/indexstring/FrozenBytes.java` | `22656cfaa3ccf0c6b214f214c92324d7f3ea55be` |
| `LICENSE` | `29f81d812f3e768fa89638d1f72920dbfd1413a8` |
| `NOTICE` (applicable attribution source) | `97bb5d49f8bd3bc573c19afe4ea2392a3d02704e` |

`check-synexia.py` recomputes each Git blob hash before compiling. The local
fixture removes a network dependency on unauthenticated raw URLs to a private
repository; it does not replace the pinned upstream identity.

This directory is test-only. The exact donor sources are compiled against M3
compatibility tests and are never included in shipped modules or runtime images.
The small `MIndexAddressedView.java` is a first-party compile-only seam for a
public method signature on `FrozenBytes`; it throws if invoked. Addressed-view
operations, native kernels, corpus data, and production APIs are outside this
fixture and are not claimed as tested.

To refresh the fixture, review the new upstream commit and license/NOTICE,
replace both exact snapshots, update the hashes and donor decision row, and rerun
the M3 compatibility workflow. Do not fetch private source from anonymous
`raw.githubusercontent.com` URLs during CI.
