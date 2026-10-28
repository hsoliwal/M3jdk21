#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Extend the existing source-built Apache pack with M3 source ingress and optional javac capture."""
import argparse
import hashlib
import importlib.util
import json
import os
import re
import shutil
import sys
import zipfile
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--jdk-home', type=Path, required=True)
    parser.add_argument('--out', type=Path, required=True)
    parser.add_argument('--with-compiler', action='store_true',
                        help='Also build jdk.m3.search.compiler; requires an actual M3AST-enabled JDK21')
    args = parser.parse_args()
    here = Path(__file__).resolve().parent
    root = here.parents[3]
    home = args.jdk_home.resolve(strict=True)
    out = args.out.resolve()
    if not re.search(r'^JAVA_VERSION="21(?:[.+"-])', (home / 'release').read_text(), re.MULTILINE):
        raise ValueError('Java 21 development toolchain required')
    if out.is_relative_to(home) or out == root or root.is_relative_to(out):
        raise ValueError('fresh build output must be outside the supplied JDK and not a source ancestor')
    for tool in ('java', 'javac', 'jar', 'jmod', 'jlink'):
        if not (home / 'bin' / tool).is_file():
            raise ValueError('development tool missing: ' + tool)
    if args.with_compiler:
        with zipfile.ZipFile(home / 'jmods/jdk.compiler.jmod') as archive:
            if 'classes/com/sun/source/util/M3AST.class' not in archive.namelist():
                raise ValueError('--with-compiler requires the real M3JDK M3AST compiler API')
    source_lock = json.loads((here / 'M3_SOURCE_LOCK.json').read_text())
    actual = {str(path.relative_to(here)): hashlib.sha256(path.read_bytes()).hexdigest()
              for directory in ('src', 'compiler-src', 'fixtures/src/test/java')
              for path in (here / directory).rglob('*.java')}
    if actual != source_lock:
        raise ValueError('M3 source/fixture input drift or incomplete coverage')
    out.mkdir(parents=True, exist_ok=False)
    owner = root / 'm3/vendor/synexia/nlp-search'
    spec = importlib.util.spec_from_file_location('synexia_source_build', owner / 'build.py')
    build = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(build)
    view = out / 'donor'
    view.mkdir()
    (view / 'third_party').symlink_to(root / 'm3/vendor/third_party', target_is_directory=True)
    jars = build.compile_tools(view, out / 'tools', home, root / 'm3/vendor/third_party-m3/nlp-search')
    names = {
        'slf4j-api-2.0.12': 'org.slf4j', 'slf4j-simple': 'org.slf4j.simple',
        'opennlp-tools': 'org.apache.opennlp.tools',
        **{'lucene-' + part: 'org.apache.lucene.' + part.replace('-', '.')
           for part in ('core', 'analysis-common', 'queries', 'sandbox', 'queryparser', 'analysis-opennlp')},
    }
    descriptor_path = 'm3/tooling/module-packs/nlp-search/src/org.apache.opennlp.tools/module-info.java'
    descriptor = root / descriptor_path
    # Reuse the existing descriptor for SDK compilation as well as JMOD assembly.
    # The original source-built JAR remains unchanged; this is a local compilation view.
    dependency = out / 'sdk-dependency'
    dependency.mkdir()
    info = dependency / 'classes'
    original_nlp = out / 'tools/opennlp-tools.jar'
    build.run([home / 'bin/javac', '--release', '21', '--module-path',
               os.pathsep.join(str(jar) for jar in jars if jar != original_nlp),
               '--patch-module', 'org.apache.opennlp.tools=' + str(original_nlp),
               '-d', info, descriptor], out / 'sdk-opennlp-descriptor.log')
    named_nlp = dependency / 'opennlp-tools.jar'
    shutil.copyfile(original_nlp, named_nlp)
    build.run([home / 'bin/jar', '--update', '--file', named_nlp,
               '--date=2026-10-18T00:00:00Z', '-C', info, 'module-info.class'],
              out / 'sdk-opennlp-jar.log')
    module_path = [named_nlp if jar == original_nlp else jar for jar in jars]
    sdk = [('jdk.m3.search', here / 'src/jdk.m3.search')]
    if args.with_compiler:
        sdk.append(('jdk.m3.search.compiler', here / 'compiler-src/jdk.m3.search.compiler'))
    for module, source in sdk:
        classes = out / 'sdk-classes' / module
        build.run([home / 'bin/javac', '-source', '21', '-target', '21', '-encoding', 'UTF-8',
                   '-Xlint:all,-module', '-Werror', '--module-path', os.pathsep.join(map(str, module_path)),
                   '-d', classes, *sorted(source.rglob('*.java'))], out / (module + '.compile.log'))
        jar = out / 'tools' / (module + '.jar')
        build.run([home / 'bin/jar', '--create', '--file', jar, '--date=2026-10-18T00:00:00Z',
                   '-C', classes, '.'], out / (module + '.jar.log'))
        module_path.append(jar)
        names[module] = module
    rows = ['artifact\tversion\tjar\tmodule\trequires\tdisposition']
    rows += [f'{name}\t1\t{out / "tools" / (name + ".jar")}\t{module}\t-\tJMOD'
             for name, module in names.items() if name != 'opennlp-tools']
    (out / 'IMAGE.tsv').write_text('\n'.join(rows) + '\n')
    (out / 'DESCRIPTORS.tsv').write_text(
        'module\tartifacts\tunit_jars\ttarget\tsha256\tgenerated\n'
        + f'org.apache.opennlp.tools\topennlp-tools\t{original_nlp}\t{descriptor_path}\t'
        + hashlib.sha256(descriptor.read_bytes()).hexdigest() + f'\t{descriptor}\n')
    build.run([sys.executable, root / 'm3/tooling/jmod-recipes/tools/jmod_pack.py',
               '--jdk-home', home, '--image-tsv', out / 'IMAGE.tsv',
               '--manifest', out / 'DESCRIPTORS.tsv', '--layer-root', root, '--out', out / 'pack',
               '--legal-notices', here / 'legal', '--add-modules',
               'java.se,jdk.unsupported,jdk.management,jdk.compiler,jdk.jlink,jdk.jartool,jdk.jdeps'],
              out / 'pack.log')
    image = out / 'pack/image'
    modules = ','.join(names.values())
    classes = out / 'probe'
    classes.mkdir()
    fixtures = here / 'fixtures/src/test/java'
    checks = ['ToolsProbe', 'M3SourceSearchChecks', 'SyntheticModelsProbe']
    if args.with_compiler:
        checks.append('M3ASTSearchChecks')
    build.run([image / 'bin/javac', '-source', '21', '-target', '21', '--add-modules', modules,
               '-d', classes, *[fixtures / (check + '.java') for check in checks]], out / 'probe-compile.log')
    markers = {'ToolsProbe': 'TOOLS_BEHAVIOR 1ebf2383e1199b0b9bc516852f02a727c8681cb36ec086a33252afb599dd900e',
               'M3SourceSearchChecks': 'M3_SOURCE_SEARCH_PASS',
               'SyntheticModelsProbe': 'NLP_SYNTHETIC_MODEL_BRIDGE_PASS',
               'M3ASTSearchChecks': 'M3_AST_SEARCH_PASS'}
    for check in checks:
        log = out / (check + '.log')
        build.run([image / 'bin/java', '-XX:-OmitStackTraceInFastThrow',
                   '-Dorg.slf4j.simpleLogger.defaultLogLevel=warn', '--add-modules', modules,
                   '--add-opens', 'org.apache.lucene.queryparser/org.apache.lucene.queryparser.classic=ALL-UNNAMED',
                   '-Xmx256m', '-cp', classes, check,
                   *([] if check == 'ToolsProbe' else [out / (check + '-work')])], log)
        if markers[check] not in log.read_text():
            raise ValueError('missing runtime proof: ' + check)
    build.run([image / 'bin/java', '-m', 'org.apache.opennlp.tools/opennlp.tools.cmdline.CLI'],
              out / 'image.cli.log')
    if 'TokenizerME' not in (out / 'image.cli.log').read_text():
        raise ValueError('OpenNLP CLI provider missing')
    (out / 'SOURCE_INTEGRATION.json').write_text(json.dumps({
        'schema': 'M3_NLP_SOURCE_JMOD_PROOF_1', 'library_modules': 9, 'sdk_modules': len(sdk),
        'jdk_release_sha256': hashlib.sha256((home / 'release').read_bytes()).hexdigest(),
        'm3ast_enabled': args.with_compiler, 'runtime_dependency_on_synexia': False,
        'source_inputs': source_lock,
        'jmod_sha256': {p.name: hashlib.sha256(p.read_bytes()).hexdigest()
                        for p in sorted((out / 'pack/jmods').glob('*.jmod'))},
        'executed_probes': checks,
        'scope': 'selected source modules on the supplied JDK; no whole-M3JDK VM or performance claim',
    }, indent=2, sort_keys=True) + '\n')
    print(f'M3_FULL_TOOLS_PASS modules={len(names)} runtimeDependencyOnSynexia=false')
    print(f'M3_SOURCE_JMODS_PASS libraryModules=9 sdkModules={len(sdk)} m3ast={args.with_compiler}')


if __name__ == '__main__':
    main()
