# Exploratory results, not final qualification

The exact predecessor reproduced missing Set DISTINCT metadata and a negative Collection.size after 2,147,483,664 occurrences.
An initial candidate repaired Set projections but its tests still rejected sorted metadata on a descending range. The recipe templates were corrected and resealed; no emitted file was patched.
An initial aggregate successor verification exceeded the outer tool time limit after completing its original large-count witness. It is NOT counted as a completed pass. A later fresh run completed all 32 commands; only that run is retained in LOCAL_RECEIPT.json and COMMANDS.log.
No elapsed-time sample is treated as a performance benchmark. No Maven executable or OpenRewrite dependencies were available locally; actual SDK qualification remains a separate hosted gate.
