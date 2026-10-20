#!/bin/bash
# usage: agg.sh <raw.tsv>  -- aggregate OwnedVsDonorBench rows over JVM runs: minimum of build_ms and query_ms
# (the machine is shared, so outside load only ever adds time), median of the allocation and retained columns,
# plus a runs column. Output on stdout in the bench's column order.
awk -F'\t' 'NR == 1 { print $0 "\truns"; next }
  { key = $1 FS $2 FS $3 FS $4 FS $5; if (!(key in seen)) { seen[key] = 1; order[++k] = key; unit[key] = $11 }
    c = ++cnt[key]; for (f = 6; f <= 10; f++) v[key, f, c] = $f }
  END { for (i = 1; i <= k; i++) { key = order[i]; line = key; n = cnt[key]
      for (f = 6; f <= 10; f++) { for (a = 1; a <= n; a++) s[a] = v[key, f, a]
        for (a = 1; a <= n; a++) for (b = a + 1; b <= n; b++) if (s[b] + 0 < s[a] + 0) { t = s[a]; s[a] = s[b]; s[b] = t }
        line = line FS (f <= 7 ? s[1] : s[int((n + 1) / 2)]) }
      print line FS unit[key] FS n } }' "$1"
