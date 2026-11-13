# fix_cmake_endif.py <CMakeLists.txt>: declared build-file fix for the defect of commit 83d44d514d0 (master m3/arrays): the
# "Exact queries" if(BUILD_TESTING) block has no endif(); insert one after its endforeach() if the file is unbalanced. Idempotent.
import re, sys
p = sys.argv[1]
lines = open(p, encoding='utf-8').read().split('\n')
opens = sum(1 for l in lines if re.match(r'\s*if\(', l)); closes = sum(1 for l in lines if re.match(r'\s*endif\(', l))
if opens == closes:
    print('balanced: no fix'); sys.exit(0)
start = next(i for i, l in enumerate(lines) if 'Exact queries scan packed storage' in l)
end = next(i for i in range(start, len(lines)) if re.match(r'\s*endforeach\(', lines[i]))
lines.insert(end + 1, 'endif()  # closes if(BUILD_TESTING) of the exact-query checks (missing on master since 83d44d514d0; fixed by the arrays move)')
open(p, 'w', encoding='utf-8', newline='\n').write('\n'.join(lines))
print('inserted endif() after line', end + 1, 'if', opens, 'endif', closes + 1)
