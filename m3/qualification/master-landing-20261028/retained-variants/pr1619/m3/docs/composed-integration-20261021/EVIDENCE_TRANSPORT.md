<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->

# Documentary evidence transport

This metadata receipt has **72 physical text targets** representing the original
**82 logical evidence files**. The existing recipe admits 69 original files
directly. `EVIDENCE_TRANSPORT.json` stores the remaining 13 documents as UTF-8
strings: two Java probe listings, ten logs and one CSV table. This guide and
`extract_evidence.py` complete the physical target set.

The embedded Java listings are documentary data. They are not Java transformation
targets, runtime sources or build inputs. The existing recipe engine and its
source-root/format restrictions remain unchanged. Extraction does not compile,
execute or apply any embedded content to the product.

## Reconstruct the original evidence tree

Use Python 3.9 or later. Set `M3_ROOT` to the checkout containing this metadata and
`WORK_DIR` to an existing directory outside that checkout. The output directory
must not already exist:

```sh
python3 "$M3_ROOT/m3/docs/composed-integration-20261021/extract_evidence.py" \
  --source-root "$M3_ROOT" --output "$WORK_DIR/m3-evidence-original"
```

The helper checks the pinned logical inventory, every direct and embedded file,
safe paths, original byte counts, SHA-256 hashes, Git blob IDs and all four frozen
packet manifests **before creating the output directory**. It refuses an existing
output, an output inside the source root, input symlinks, inventory drift and
corrupt content. It writes only the new external evidence tree, using binary
writes to preserve the original UTF-8 bytes and newlines.

After successful extraction, use the original packet instructions under
`$WORK_DIR/m3-evidence-original/m3/docs/composed-integration-20261021`. References
there to `.java`, `.log` and `.csv` files name the reconstructed logical paths.
Runtime rerun commands still require the separately recorded product checkout,
toolchain and built images; reconstruction itself supplies no new runtime result.

## Exact custody

The frozen original selection SHA-256 is
`b8dbd94ee955bdf8a9283e553c00208e00725efd3a8872d8941ae7df3ccca830`.
Its 82 file records are preserved verbatim in `logical_files`, without its local
execution-location field. Their canonical JSON seal is
`3eb363680608829af4d6eee1c2104c817de3d59214252c508fdb299f8bd4af52`.

`direct_files` maps all 69 physical paths to their identical logical paths.
`embedded_assets` supplies each remaining original path, mode, size, SHA-256,
Git blob ID and exact content. All four original packet-manifest byte sequences
and member hashes remain unchanged. Those manifests describe the reconstructed
tree; they do not describe this physical transport layout. The new physical
metadata selection and its SDK receipt have their own seals and target count.

The first metadata representation was refused because the Java source-root gate
does not admit documentation probes and the text format gate excludes logs/CSV.
This explicit data representation retains that refusal and uses admitted text
formats without changing either gate. Source rights and notices remain attached
to their original contents; the transport metadata license does not relicense
embedded evidence. E1/E2 runtime results, failures and promotion limits are
unchanged.
