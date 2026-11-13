#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Mutants of the members restored by mergeloss case 01, scored by oracle MergelossInteropReference (kill = failures > 0).
usage: mutants.py WORKTREE/ SCRATCH/ (SCRATCH holds interop.cp, head-interop-classes/, oracle-classes/; JNI dll path via M3_ARRAYS_DLL)"""
import os, re, subprocess, sys
W, R = sys.argv[1], sys.argv[2]; I=W+'m3/collections/interop/src/main/java/jdk/m3/collections/interop/'
JAVA_BIN=os.path.join(os.environ.get('JAVA_HOME','C:/Program Files/Java/jdk-21'),'bin')
JAVAC=os.path.join(JAVA_BIN,'javac')
CP=open(R+'interop.cp').read().strip()
COLL=open(I+'M3LibraryCollectors.java',encoding='utf-8').read(); FAC=open(I+'M3LibraryCollections.java',encoding='utf-8').read()
M=[
 ('M1_putUniqueBidi_no_value_check','C','        if (map.containsValue(value)) throw new IllegalStateException("duplicate value: " + value);\n',''),
 ('M2_toApacheMultiSet_combiner_count1','C','left.add(entry.getElement(), entry.getCount());','left.add(entry.getElement(), 1);'),
 ('M3_toApacheBag_combiner_add1','C','left.add(element, right.getCount(element));','left.add(element);'),
 ('M4_toEclipseBag_combiner_drops_left','C','right.forEachWithOccurrences(left::addOccurrences); return left;','return right;'),
 ('M5_toGuavaMultiset_combiner_drops_right','C','Multiset<E>::add,\n                (left, right) -> { left.addAll(right); return left; });','Multiset<E>::add,\n                (left, right) -> { return left; });'),
 ('M6_toApacheListValuedMap_combiner_swapped','C','Supplier<ListValuedMap<K, V>> owner = M3LibraryCollections::apacheListValuedMap;\n        return Collector.of(owner, (map, item) -> map.put(keys.apply(item), values.apply(item)),\n                (left, right) -> { left.putAll(right); return left; });','Supplier<ListValuedMap<K, V>> owner = M3LibraryCollections::apacheListValuedMap;\n        return Collector.of(owner, (map, item) -> map.put(keys.apply(item), values.apply(item)),\n                (left, right) -> { right.putAll(left); return right; });'),
 ('M7_uniqueBidi_combiner_ignores_right','C','(left, right) -> { right.forEach((k, v) -> putUniqueBidi(left, k, v)); return left; });','(left, right) -> left);'),
 ('M8_eclipseOrderedMap_unordered_owner','F','OrderedMapAdapter.adapt(new M3LinkedHashMap<>())','OrderedMapAdapter.adapt(new java.util.HashMap<>())'),
 ('M9_toApacheSetValuedMap_combiner_swapped','C','Supplier<SetValuedMap<K, V>> owner = M3LibraryCollections::apacheSetValuedMap;\n        return Collector.of(owner, (map, item) -> map.put(keys.apply(item), values.apply(item)),\n                (left, right) -> { left.putAll(right); return left; });','Supplier<SetValuedMap<K, V>> owner = M3LibraryCollections::apacheSetValuedMap;\n        return Collector.of(owner, (map, item) -> map.put(keys.apply(item), values.apply(item)),\n                (left, right) -> { right.putAll(left); return right; });'),
 ('M10_apacheBag_factory_wrong_owner','F','public static <E> Bag<E> apacheBag() { return new M3ApacheBag<>(); }','public static <E> Bag<E> apacheBag() { return new org.apache.commons.collections4.bag.TreeBag<>(); }'),
]
env=dict(os.environ)
rows=[]
for name,which,old,new in M:
    d=R+'mutants/'+name+'/'; src=d+'src/jdk/m3/collections/interop/'; out=d+'classes'
    os.makedirs(src,exist_ok=True); os.makedirs(out,exist_ok=True)
    base=COLL if which=='C' else FAC
    assert base.count(old)==1, (name, base.count(old))
    fn='M3LibraryCollectors.java' if which=='C' else 'M3LibraryCollections.java'
    open(src+fn,'w',encoding='utf-8',newline='\n').write(base.replace(old,new))
    c=subprocess.run([JAVAC,'-J-Djava.io.tmpdir='+os.environ.get('TMP','.'),'--release','21','-nowarn','-d',out,'-cp',R+'head-interop-classes;'+CP,src+fn],capture_output=True,text=True,env=env)
    if c.returncode: rows.append((name,'COMPILE_ERROR',c.stderr[-300:])); continue
    r=subprocess.run([os.path.join(JAVA_BIN,'java'),'-Djava.io.tmpdir='+os.environ.get('TMP','.'),'-Dm3.arrays.native.path='+os.environ['M3_ARRAYS_DLL'],'-cp',out+';'+R+'head-interop-classes;'+R+'oracle-classes;'+CP,'jdk.m3.collections.interop.MergelossInteropReference','60','0x4D4C30315F524546'],capture_output=True,text=True,env=env)
    tail=[l for l in r.stdout.splitlines() if l.startswith('REFERENCE')]
    fails=[l.split()[1] for l in r.stdout.splitlines() if l.startswith('MEMBER') and not l.endswith('failures=0')]
    total=tail[-1] if tail else 'NO_SUMMARY rc=%d %s'%(r.returncode,r.stderr[-200:])
    killed = bool(tail) and not total.endswith('failures=0')
    if not tail: rows.append((name,'ERROR',total)); continue
    rows.append((name,'KILLED' if killed else 'SURVIVED',total+' members='+','.join(fails)))
for row in rows: print('\t'.join(row))
print('MUTANTS total=%d killed=%d survived=%d'%(len(rows),sum(r[1]=='KILLED' for r in rows),sum(r[1]=='SURVIVED' for r in rows)))
