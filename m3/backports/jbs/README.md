
## Pass 3 (2026-10-23): provisional disposition by component family

`jbs_classify.py` adds `provisional_disposition` to every row from the programme's own rules (JIT and GC holds,
javac and language holds, high-risk runtime and native families, additive tooling admitted, client libraries
rejected, the rest `candidate-adapted`); `disposition` stays UNCLASSIFIED until the reading pass confirms a row.
Counts: see `jbs_classify.counts`.
