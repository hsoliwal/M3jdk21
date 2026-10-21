# Exact measurement transport revision

The GitHub connector rejected the 27,707,405-byte measurement JSONL blob because
its request body exceeds 16 MiB. No final implementation commit or branch update
had occurred. The original five-file evidence bundle remains immutable.

Publish positive, ordered, record-boundary parts of at most 6 MiB. Each part is
ordinary exact UTF-8 measurement evidence. A manifest binds every part's length,
record count and SHA256, the original complete measurement file, and the four
unchanged companion files. A bounded restore adapter validates the complete part
set before creating a fresh destination, restores the original five-file bundle,
and checks its exact bytes. Run the unchanged evidence verifier on that restored
bundle. The original 1,092-file decoded evidence and product qualification do not
change. Keep the rejected upload diagnostic and revise only publication paths,
instructions and append-only provenance preparation.
