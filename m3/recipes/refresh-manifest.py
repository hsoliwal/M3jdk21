#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
"""Maintainer-only: regenerate pins after reviewing source changes; not an apply step."""
import hashlib,json
from pathlib import Path
root=Path(__file__).resolve().parents[2]
runtime=['src/java.base/share/classes/java/lang/'+n+'.java' for n in ['String','StringLatin1','StringUTF16','StringCoding']]
runtime += ['src/hotspot/share/'+n for n in ['classfile/javaClasses.hpp','classfile/javaClasses.inline.hpp','opto/library_call.cpp','prims/jni.cpp']]
files=[]
for folder in ['m3/core','m3/docs','m3/lexicon','m3/compatibility']:
    files += [str(p.relative_to(root)) for p in (root/folder).rglob('*') if p.is_file()]
files += ['m3/README.md','m3/LICENSE','m3/NOTICE','m3/build.sh','m3/build-openjdk.sh','m3/verify-shared-mapping.py','m3/.gitignore','m3/recipes/apply.py','m3/recipes/test_recipe.py','m3/recipes/refresh-manifest.py','m3/evidence/constructor-pool-retirement.md','m3/evidence/build-result.json']
hashes=lambda paths:{p:hashlib.sha256((root/p).read_bytes()).hexdigest() for p in sorted(paths)}
(root/'m3/recipes/manifest.json').write_text(json.dumps({'schema':1,'upstream':'https://github.com/openjdk/jdk21','commit':'890adb6410dab4606a4f26a942aed02fb2f55387','unchanged_runtime_files':hashes(runtime),'new_files':hashes(files)},indent=2)+'\n')
