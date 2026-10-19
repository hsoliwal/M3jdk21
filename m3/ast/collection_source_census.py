#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Read-only M3 collection source census, not an AST parse or promotion gate."""
from pathlib import Path
import hashlib
import sys

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / 'm3/collections/src/main/java/com/m3/collections'
FAMILIES = (
 ('ARRAY', ('Array','Lane','Segment','Arena','Slot')),
 ('LIST', ('List','Sequence')),
 ('SET', ('Set','Bloom')),
 ('MAP', ('Map',)),
 ('MULTIMAP', ('MultiMap',)),
 ('MULTISET', ('Multiset','Bag','Count')),
 ('SORTED', ('Sorted','Rank','Sort')),
 ('QUEUE_DEQUE', ('Queue','Deque','Stack','Priority','Heap')),
 ('TREE_GRAPH', ('Tree','Graph','Trie')),
 ('BIT_MASK', ('Mask','Bit')),
 ('CONCURRENT', ('Concurrent','Atomic','Locked','Mpmc','Spsc','Mpsc')),
 ('VIEW_ADAPTER', ('View','Adapter','Facade','Projection')),
 ('MEMORY_PAGING', ('Paged','Resident','Residency','Storage')),
)

def categories(name: str) -> list[str]:
    return [category for category, tokens in FAMILIES if any(t in name for t in tokens)]


def main() -> None:
    if not SOURCE.is_dir():
        raise SystemExit('FAIL: m3/collections full checkout required')
    files = sorted(SOURCE.glob('*.java'))
    if not files:
        raise SystemExit('FAIL: no collection source files')
    print('path\tsha256\tproblem_category_candidates\tL0_qualification')
    for path in files:
        digest = hashlib.sha256(path.read_bytes()).hexdigest()
        family = ','.join(categories(path.stem)) or 'UNCLASSIFIED_REVIEW'
        print(f'{path.relative_to(ROOT).as_posix()}\t{digest}\t{family}\tNOT_ATOMIZED_BY_THIS_CENSUS')
    print(f'# files={len(files)}; L0 qualified=0; source names only', file=sys.stderr)

if __name__ == '__main__':
    main()
