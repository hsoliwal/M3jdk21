#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Qualify actual compiler evidence or the explicit M3 interpreter boundary."""
import argparse
import json
from pathlib import Path
import xml.etree.ElementTree as ET


def qualify(path, compiler, kernel, m3_interpreter=False):
    if compiler not in ('c1', 'c2'):
        raise ValueError('Expected compiler must be c1 or c2')
    root = ET.parse(path).getroot()
    if root.tag != 'hotspot_log' or root.find('hotspot_log_done') is None:
        raise ValueError('Complete HotSpot compilation log required')
    arguments = (root.findtext('vm_arguments/args') or '').split()
    properties = dict(line.split('=', 1) for line in
                      (root.findtext('vm_arguments/properties') or '').splitlines()
                      if '=' in line)
    nodes = list(root.iter('nmethod'))
    m3_enabled = '-XX:+UseM3StringStorage' in arguments
    if m3_interpreter:
        # arguments.cpp deliberately overrides a requested compiler while the
        # experimental String representation lacks compiler support.
        requested = '-XX:TieredStopAtLevel=1' if compiler == 'c1' else '-XX:-TieredCompilation'
        if not m3_enabled or requested not in arguments:
            raise ValueError('M3 flag and requested compiler must be present')
        if properties.get('java.vm.info') != 'interpreted mode' or nodes:
            raise ValueError('M3 String storage must retain its interpreter boundary')
        return {'mode': 'm3-interpreter', 'requested_compiler': compiler,
                'kernel': kernel, 'compiled_kernels': 0}
    if m3_enabled:
        raise ValueError('M3 interpreter execution is not compiled-kernel evidence')
    count = sum(node.get('compiler') == compiler and
                node.get('method', '').startswith(kernel + ' ') for node in nodes)
    if not count:
        raise ValueError('No compiled ' + compiler + ' kernel evidence: ' + kernel)
    return {'mode': 'compiled', 'compiler': compiler, 'kernel': kernel,
            'compiled_kernels': count}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('log', type=Path)
    parser.add_argument('compiler', choices=('c1', 'c2'))
    parser.add_argument('kernel')
    parser.add_argument('--m3-interpreter', action='store_true')
    args = parser.parse_args()
    print(json.dumps(qualify(args.log, args.compiler, args.kernel, args.m3_interpreter), sort_keys=True))
