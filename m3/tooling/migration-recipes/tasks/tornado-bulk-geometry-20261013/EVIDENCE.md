# TornadoVM bulk geometry repair — executed evidence

## Failure and exact input

Live target test preimage:
`m3/ports/tornadovm-bulk/src/test/java/com/m3/tornado/Rv32iBulkExecutorAdapterTest.java`

- Git blob: `2e3e56b8865178fe3de323dadc5ae366646fce73`
- SHA-256: `7184faf2b06a57f006a2a1e039aedd4dd82ea21d258a9369a88f3296ee8d33c0`
- Invalid assertion: `tornado.supports(task(4, 64))`.
- `BulkTask` requires `0 <= localWork <= workItems`; construction throws before `supports`.

## Corrected fixture

- Valid `4/4` and `64/64` provider selection.
- Explicit `4/64`, `4/5`, `0/0`, `4/-1` refusal.
- Positive CPU and Tornado admission geometries for 1, 2, 3, 4, 8, 63, 64, 65 workers.
- No production source, ABI, JNI library, scheduler priority or provider execution changed.
- Postimage SHA-256: `f0323ce99ce0ad5cab8b94e04252c72970e669f2098c1d8045d7e13693c716ff`.

## Executed local Java 21 gate

The current `src/java.base/share/classes/jdk/internal/vm/parallel/BulkTask.java`
was reproduced byte-for-byte from its published GitHub source; its standalone local
SHA-256 matched the exact canonical port manifest:
`c38cd88a1615df34391761e8a0bc7082eb0a3ca090706f04b15734b1249a75d1`.

Environment: OpenJDK 21.0.12.1, Linux x86_64. Executed:

```text
sha256sum --check                   PASS (exact BulkTask.java)
javac -Xlint:all -Werror
  --patch-module java.base=...      exit 0
javac -Xlint:all -Werror
  --patch-module java.base=...
  --add-exports ...                exit 0
java --patch-module java.base=...   PASS, 1 invalid + 16 valid geometries
java -Xint --patch-module ...       PASS, same geometry cases
```

The `-Xint` run is a repeatability check, not 16 additional distinct geometries.

## Recipe custody

Canonical recipe: `hsoliwal/com.synexia#10857`;
`com.synexia.rewrite.M3Jdk21TornadoBulkGeometryRepairRecipe`.

The target-owned receiver was committed before this test file changed.
The receiver uses the existing M3JDK
`M3Jdk21HashPinnedSnapshotRecipe`. The target postimage was reread and
matched the exact mirrored Synexia template Git blob.

## Open gates

Hosted exact-head Maven/JUnit, actual optional TornadoVM adapter execution,
source-built recipe scheduler fixed-point, full release-image/jtreg, GPU hardware
parity, and setup-inclusive throughput proof have **not** been established by this
focused local smoke. Keep the PR draft until the relevant jobs execute.
