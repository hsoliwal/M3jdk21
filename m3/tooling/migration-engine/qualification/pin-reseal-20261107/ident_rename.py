"""ident_rename.py <in> <out> : rename the Java identifier token synexia -> m3 outside comments and string literals
(the #2635 rule for the half-renamed m3-import tests). Prints the token count and the output sha256."""
import hashlib
import re
import sys

TOKEN = re.compile(r'//[^\n]*|/\*.*?\*/|"(?:\\.|[^"\\])*"|\bsynexia\b', re.S)
text = open(sys.argv[1], 'rb').read().decode('utf-8')
count = 0


def rep(m):
    global count
    if m.group(0) == 'synexia':
        count += 1
        return 'm3'
    return m.group(0)


out = TOKEN.sub(rep, text).encode('utf-8')
open(sys.argv[2], 'wb').write(out)
print(f"tokens={count} sha256={hashlib.sha256(out).hexdigest()}")
