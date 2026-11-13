#!/bin/bash
# classify.sh [gate out dir]: classify master/branch differing lines of the CHECKSUMS_TO_JDK differential by invalid-call shape
O=${1:-$HOME/crcg}
for v in 1.17.1 1.22.1 relocated; do for seed in 1 2 3; do
  paste -d'\t' $O/diff_master_${v}_$seed.txt $O/diff_branch_${v}_$seed.txt | awk -F'\t' '$1!=$2' > $O/pairs_${v}_$seed.tsv
  echo "$v seed $seed: differing=$(wc -l < $O/pairs_${v}_$seed.tsv) without_bad_call=$(grep -vc 'bad(' $O/pairs_${v}_$seed.tsv)"
done; done
cat $O/pairs_*_*.tsv | python3 -c '
import re,sys,collections
c=collections.Counter()
for line in sys.stdin:
    m,b=line.rstrip("\n").split("\t")
    for (name,mv),(_,bv) in zip(re.findall(r"(crc32c?)=(\S+)",m),re.findall(r"(crc32c?)=(\S+)",b)):
        if mv==bv: continue
        mm=re.search(r"bad\((-?\d+),(-?\d+)\)=(\w+)",mv); bb=re.search(r"bad\((-?\d+),(-?\d+)\)=(\w+)",bv)
        off,ln=int(mm.group(1)),int(mm.group(2))
        shape=("off<0" if off<0 else "")+(" len<0" if ln<0 else "")+(" len=0" if ln==0 else "")
        c[(name,shape.strip() or "off+len>length",mm.group(3),bb.group(3))]+=1
for k,v in sorted(c.items()): print(v,*k)
'
