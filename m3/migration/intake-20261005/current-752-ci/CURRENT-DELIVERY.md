# Current delivery metadata

CURRENT-DELIVERY.json identifies the operative physical receiver selection, source mapping and finite census for this delivery. Use its `operative` relative paths under `delivery-envelope/archive-locator-v1/`.

All17 older envelopes remain byte-identical historical pre-upload metadata at their original paths. Their earlier physical map/census are retained as history; the operative map/census supersedes them for this delivery.

Only the redundant loose COPY-CUSTODY.json publication body is replaced by COPY-CUSTODY.ARCHIVE.json, and the two original connector-refusal records are added. The complete custody body remains in the unchanged archive; the locator and COPY_CUSTODY_ARCHIVE_RECORD.json bind its exact logical identity. All other main selection rows and all221 qualified paths are unchanged.

Both recorded connector failures were source-repository upload attempts. M3 receives their exact records as provenance under the shared request-size constraint; no separate failed M3 upload attempt is claimed.

The derived finite census has 614 paths: 387 declared producer-source paths plus 227 receiver provenance aliases. It replaces the historical pre-upload 612-path census for this physical delivery. The 28 outer envelope paths are excluded from that census. The source union has 642 paths, and the main receiver selection plus outer envelopes has 640; these are publication subsets, not repository-wide coverage.

The actual bounded full consumer-v5 PASS, seven historical trial dispositions, archive bytes and proof receipts remain unchanged. This metadata operation runs no proof gate, performs no remote write and grants no broader JDK product runtime admission. The outer source/receiver whitelist controls are outside their own files and the finite census.
