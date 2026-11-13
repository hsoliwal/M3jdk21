#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Refuse missing or ambiguous M3JDK21 source-pinned recipe dependencies.

This is a build-source admission aid, not a rewrite engine or an alternate
Java/JNI runtime. Maven's reactor remains responsible for compilation order.
"""
import argparse
import re
import subprocess
import sys
from pathlib import Path
from xml.etree import ElementTree as ET

NS = {'m': 'http://maven.apache.org/POM/4.0.0'}
MODULE = re.compile(r'[A-Za-z0-9_.-]+\Z')
PIN = '-m3-source-1'
ROOTS = {
    ('org.openrewrite', 'rewrite-java', '8.17.1-m3-source-1'): 'rewrite-java',
    ('org.openrewrite', 'rewrite-java-21', '8.17.1-m3-source-1'): 'rewrite-java-21',
    ('org.openrewrite', 'rewrite-test', '8.17.1-m3-source-1'): 'rewrite-test',
    ('org.junit.jupiter', 'junit-jupiter', '5.10.2-m3-source-1'): 'junit-jupiter-5.10.2',
}


def field(node, tag):
    return node.findtext('m:' + tag, namespaces=NS)


def load_inventory(root):
    vendor = root / 'm3/vendor/third_party'
    pom = ET.parse(vendor / 'pom.xml').getroot()
    modules = pom.findall('m:modules/m:module', NS)
    if not modules:
        raise ValueError('missing vendored source modules')
    inventory = {}
    for element in modules:
        folder = element.text or ''
        if not MODULE.fullmatch(folder):
            raise ValueError('unsafe source module name: ' + folder)
        path = vendor / folder
        project = ET.parse(path / 'pom.xml').getroot()
        key = (field(project, 'groupId'), field(project, 'artifactId'), field(project, 'version'))
        if not all(key) or key in inventory:
            raise ValueError('missing or duplicate source GAV: ' + repr(key))
        inventory[key] = (folder, project, path)
    return inventory


def closure(inventory, roots=ROOTS):
    for gav, folder in roots.items():
        if gav not in inventory or inventory[gav][0] != folder:
            raise ValueError('missing exact source-pinned recipe owner: ' + repr(gav))
    done, visiting = set(), set()

    def walk(gav):
        if gav in done:
            return
        if gav in visiting:
            raise ValueError('source dependency cycle: ' + repr(gav))
        if gav not in inventory:
            raise ValueError('unvendored source-pinned dependency: ' + repr(gav))
        visiting.add(gav)
        _, project, _ = inventory[gav]
        for dep in project.findall('m:dependencies/m:dependency', NS):
            target = (field(dep, 'groupId'), field(dep, 'artifactId'), field(dep, 'version'))
            if target[2] and target[2].endswith(PIN):
                walk(target)
        visiting.remove(gav)
        done.add(gav)

    for gav in roots:
        walk(gav)
    return sorted(done)


def admit(root, run_checks, roots=ROOTS):
    inventory = load_inventory(root)
    selected = closure(inventory, roots)
    for gav in selected:
        folder, _, path = inventory[gav]
        verifier = path / 'verify_source.py'
        if not verifier.is_file():
            raise ValueError('missing vendor source integrity verifier: ' + folder)
        if run_checks:
            subprocess.run((sys.executable, str(verifier)), cwd=root, check=True,
                           stdout=subprocess.DEVNULL)
    return selected, inventory


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, required=True)
    parser.add_argument('--verify-sources', action='store_true')
    args = parser.parse_args()
    selected, inventory = admit(args.root.resolve(), args.verify_sources)
    for gav in selected:
        print('\t'.join((*gav, inventory[gav][0])))
    print(f'Admitted {len(selected)} source modules for pinned M3JDK21 recipes',
          file=sys.stderr)


if __name__ == '__main__':
    main()
