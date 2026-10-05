This companion adds all 27 **physical outer Cia publication paths** to the immutable 234-path source census. Their exact source whitelist and explicit M3 delivery mapping are sealed by `plan.json`. It verifies both source and copied file bytes, SHA-256, Git blob and byte count for every path, and refuses missing, extra, duplicate or previously covered source paths.

The known universe is now **261 distinct source paths**. The added Cia outer files total **844,456 bytes**. They are separate from the Cia26 source increment and its internal archive entries already represented by the first census. This companion retains the first census's **25 pending publication additions**; it does not claim that those files were copied or remotely published.

| Input | SHA-256 |
|---|---|
| Prior `coverage.json` | `07df1ae51dd27de7ff4470c92864fdbbf4064e6403115d1baa9bd503ff162f26` |
| `PORTABLE-CIA-WHITELIST.json` | `fb283b1719522c78f0e385a8d07125e780bd7bdb8f0da6b5fa1928bf73b42991` |
| Cia receiving `MAPPING.json` | `c9c5011e73eafac8aaec5a12c60f005994e743fdf85e7df225b0b1a4c18f564b` |
| Append `plan.json` | `3a374228f39ddb9a6ff9f4c0817a7b9779d01042c9e471a07badf902e59c7566` |
| `append_coverage.py` | `d123d2aec12ca33606fdc9ff168f67d7a99c071c05abb11c3484cd1887e2c875` |

`coverage-append.json` contains all 27 exact source-to-receiving mappings and the complete 261-path name inventory. Each new row includes source SHA-256, size and Git blob; the original census is referenced by its immutable file seal and is not rewritten.

The same bounded read-only collector can append later Cic source/portable files and final consumer package or coverage-envelope files after their actual whitelists and mappings exist. Each new plan must name and hash a prior census, at most eight explicit segments, and the exact whitelist and mapping document for every segment. It does not guess filenames or expected counts, scan a repository, invoke candidate code, generate copies, or run producer/consumer gates.

For a physical segment, the mapping has `files` rows with `sourcePath`, `path` (or `receivingPath`), `file` (or `localPath`), `sha256`, `bytes`, and an optional Git blob. The source whitelist must declare the same source set exactly once, with explicit local bodies. Every delivery remains beneath `m3/migration/intake-20261005/current-752-ci/`.

For a later source entry represented inside a portable archive, a mapping row instead supplies `sourcePath`, `sha256`, `bytes`, `bundleFile`, `bundleSHA256`, `receivingBundle`, and `logicalPath`. The collector verifies the sealed bundle, part/member/shard identities, hash-addressed chunks and reconstructed logical file bytes against the original source body. References and omissions cannot stand in for file content. This optional branch is authored for later use; it was not needed or exercised for the 27 physical Cia copies.

Use a fresh plan and fresh output for each append:

```text
python append_coverage.py --plan <exact-plan.json> --plan-sha256 <sha256> --output <new-companion.json>
```

Final envelope files must already exist before they can be included in a later whitelist; a census cannot include its own as-yet-unwritten hash. Each companion names its predecessor and preserves its outstanding publication status. Content coverage does not confer consumer qualification, runtime acceptance or remote publication. No existing proof, candidate, archive or census was changed.
