#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""M3JDK21 receiver: compile the full tools and run them in an independent linked image."""
import argparse, hashlib, importlib.util, json, os, subprocess, sys
from pathlib import Path

def main():
 p=argparse.ArgumentParser();p.add_argument('--jdk-home',type=Path,required=True);p.add_argument('--out',type=Path,required=True);a=p.parse_args()
 root=Path(__file__).resolve().parents[4];home=a.jdk_home.resolve();out=a.out.resolve();out.mkdir(parents=True,exist_ok=False)
 owner=root/'m3/vendor/synexia/nlp-search';spec=importlib.util.spec_from_file_location('synexia_source_build',owner/'build.py');build=importlib.util.module_from_spec(spec);spec.loader.exec_module(build)
 # Build-only local view; all implementations are already vendored in this target.
 view=out/'donor';view.mkdir();(view/'third_party').symlink_to(root/'m3/vendor/third_party',target_is_directory=True)
 jars=build.compile_tools(view,out/'tools',home,root/'m3/vendor/third_party-m3/nlp-search')
 names={'slf4j-api-2.0.12':'org.slf4j','slf4j-simple':'org.slf4j.simple','opennlp-tools':'org.apache.opennlp.tools',**{'lucene-'+x:'org.apache.lucene.'+('analysis.common' if x=='analysis-common' else x) for x in ['core','analysis-common','queries','sandbox','queryparser']}}
 rows=['artifact\tversion\tjar\tmodule\trequires\tdisposition']+[f'{n}\t1\t{out/"tools"/(n+".jar")}\t{module}\t-\tJMOD' for n,module in names.items() if n!='opennlp-tools'];(out/'IMAGE.tsv').write_text('\n'.join(rows)+'\n')
 path='m3/tooling/module-packs/nlp-search/src/org.apache.opennlp.tools/module-info.java';source=root/path
 (out/'DESCRIPTORS.tsv').write_text('module\tartifacts\tunit_jars\ttarget\tsha256\tgenerated\n'+f'org.apache.opennlp.tools\topennlp-tools\t{out/"tools/opennlp-tools.jar"}\t{path}\t{hashlib.sha256(source.read_bytes()).hexdigest()}\t{source}\n')
 build.run([sys.executable,root/'m3/tooling/jmod-recipes/tools/jmod_pack.py','--jdk-home',home,'--image-tsv',out/'IMAGE.tsv','--manifest',out/'DESCRIPTORS.tsv','--layer-root',root,'--out',out/'pack','--add-modules','java.se,jdk.unsupported,jdk.management,jdk.compiler,jdk.jlink,jdk.jartool,jdk.jdeps'],out/'pack.log')
 image=out/'pack/image';mods=','.join(names.values());classes=out/'probe';classes.mkdir()
 build.run([image/'bin/javac','-source','21','-target','21','--add-modules',mods,'-d',classes,Path(__file__).parent/'fixtures/src/test/java/ToolsProbe.java'],out/'probe-compile.log')
 build.run([image/'bin/java','-Djava.util.logging.SimpleFormatter.format=%4$s: %5$s%n','-Dorg.slf4j.simpleLogger.defaultLogLevel=warn','--add-modules',mods,'--add-opens','org.apache.lucene.queryparser/org.apache.lucene.queryparser.classic=ALL-UNNAMED','-Xmx256m','-cp',classes,'ToolsProbe'],out/'image.behavior.log')
 if 'TOOLS_BEHAVIOR 1ebf2383e1199b0b9bc516852f02a727c8681cb36ec086a33252afb599dd900e' not in (out/'image.behavior.log').read_text():raise ValueError('frozen tool behavior')
 build.run([image/'bin/java','-m','org.apache.opennlp.tools/opennlp.tools.cmdline.CLI'],out/'image.cli.log')
 if 'TokenizerME' not in (out/'image.cli.log').read_text():raise ValueError('OpenNLP CLI provider missing')
 print('M3_FULL_TOOLS_PASS modules=8 runtimeDependencyOnSynexia=false')
if __name__=='__main__':main()
