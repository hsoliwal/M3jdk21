# Synexia to M3JDK receiver matrix

This is a status receipt for the complete Synexia lexicon/precompute surface.
It does not copy external datasets, promote open pull requests, or replace the
source manifest and field map. The authoritative source identity remains in:

- m3/lexicon/synexia-source-manifest.tsv
- m3/lexicon/synexia-precompute-field-map.tsv

The matrix makes the receiver boundary explicit for dictionary and frequency
facts, LangDex/Hugging Face bridge metadata, translation and grammar
projections, SI units, acronyms, numbers 0 through 10000, phrase rewrites,
spell/hash/prefix/token-frequency precomputes, proper names, titles, M3 string
facts, and external n-gram/Rapidex audits.

target_state is deliberately separate from hosted_state and payload_state.
An OPEN or OPEN_DRAFT receiver is not an admitted runtime. REFERENCE_ONLY and
NO_FIT rows intentionally have no M3JDK payload. A row reaches promotion only
after its source/license receipt, differential proof, terminal target workflows,
and review all pass.

Run:

    python m3/lexicon/verify-synexia-m3lexicons-receiver-matrix.py < m3/lexicon/synexia-m3lexicons-receiver-matrix.tsv

Expected proof:

    M3LEXICONS_RECEIVER_MATRIX_PASS rows=21 acronym_fields=3
