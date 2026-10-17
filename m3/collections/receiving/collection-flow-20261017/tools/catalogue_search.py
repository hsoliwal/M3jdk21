#!/usr/bin/env python3
"""Search the accumulated task catalogue by case-insensitive terms; no network or model."""
import json
import sys
from pathlib import Path

def main() -> None:
    terms = [term.casefold() for term in sys.argv[1:]]
    source = Path(__file__).resolve().parent.parent / "catalogue" / "algorithms.jsonl"
    for line in source.read_text(encoding="utf-8").splitlines():
        record = json.loads(line)
        if all(term in line.casefold() for term in terms):
            print(json.dumps(record, indent=2, ensure_ascii=False))

if __name__ == "__main__":
    main()
