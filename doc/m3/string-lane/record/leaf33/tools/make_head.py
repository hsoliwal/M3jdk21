import sys
d='J:/m3-scratch/string-converge-20261117/'
head=open(d+'leaf31/String.head.java',encoding='utf-8',newline='').read()
case=open(d+'leaf31/case_family.txt',encoding='utf-8',newline='').read()
mine=open(d+'draft-encode/encode_family.txt',encoding='utf-8',newline='').read()
assert head.count(case)==1 and '\r' not in mine
i=head.index(case)+len(case)
open(d+'draft-encode/String.head.java','w',encoding='utf-8',newline='').write(head[:i]+mine+head[i:])
bad=[(n+1,len(l)) for n,l in enumerate(mine.split('\n')) if len(l)>100 or '\t' in l or any(ord(c)>126 for c in l)]
print('long/tab/nonascii lines:',bad)
