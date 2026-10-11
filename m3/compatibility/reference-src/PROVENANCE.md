# Synexia Frozen UTF-16 compatibility references

These exact source files are retained only as compatibility-test references:

- Repository: https://github.com/hsoliwal/com.synexia
- Source commit: `64a2ea61c73b548413fed6686a9daeeb0b9b0564`
- `FrozenChars.java`: Git blob `75264c85b4da2cd7d9193d8e245364efb53aa936`
- `FrozenBytes.java`: Git blob `0e249745d0e96a1d9a8754a075f33cdf42e8fc0b`
- License: Apache-2.0; the exact license text from that source commit is retained as `LICENSE-APACHE-2.0.txt` (Git blob `29f81d812f3e768fa89638d1f72920dbfd1413a8`).
- The upstream root `NOTICE` is retained verbatim (Git blob `97bb5d49f8bd3bc573c19afe4ea2392a3d02704e`).

The exact `FrozenBytes` source also declares `addressed(...)`, whose return type
belongs to a larger Synexia subsystem. The isolated test compiles against a
fail-fast, test-only signature seam at
`m3/compatibility/test-support/com/synexia/indexstring/MIndexAddressedView.java`.
The contract test never invokes that method; this PR makes no claim about
addressed-view behavior and does not vendor its operation catalog or native
kernel.

The donor Java files are unmodified and are compiled against the M3 text module
by `m3/compatibility/check-synexia.py`. The test compares UTF-16 content,
hashes, and substring ranges only; it explicitly makes no public-API or
owner-equivalence claim. These references are outside `com.m3.text`,
`java.base`, and runtime artifacts. No Synexia corpus or lexicon payload is
included.
