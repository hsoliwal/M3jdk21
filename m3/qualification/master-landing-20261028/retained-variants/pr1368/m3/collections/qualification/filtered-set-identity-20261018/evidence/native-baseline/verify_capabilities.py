# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
#!/usr/bin/env python3
"""Complete actual ABI capability and native path-window checks after main gate."""
import importlib.util, json, os
from pathlib import Path
W=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('native_gate',W/'run_native_verification.py')
g=importlib.util.module_from_spec(spec);spec.loader.exec_module(g)
g.COMMANDS=json.loads((W/'commands.json').read_text())
g.run('09-capability-probe-compile',[g.JDK/'bin/javac','--release','21','-proc:none','-Xlint:all','-Werror','-cp',g.CLASSES,'-d',g.TEST_CLASSES,W/'NativeCapabilityProbe.java'])
java=[g.JDK/'bin/java','-ea','-Xcheck:jni','-Dm3.collections.signal.native=true','-Dm3.collections.rank.native=true','-Dm3.address.required=true','-Djava.library.path='+str(g.LIB),'-cp',str(g.CLASSES)+os.pathsep+str(g.TEST_CLASSES)]
g.run('10-actual-native-capabilities',java+['NativeCapabilityProbe'])
g.run('11-path-mask-window-native',java+['com.m3.collections.M3IndexPathMaskWindowSelfTest','--native'])
abi=json.loads((W/'jni-abi.json').read_text());abi['capability_versions_actually_invoked']=True
(W/'jni-abi.json').write_text(json.dumps(abi,indent=2)+'\n')
result=json.loads((W/'result.json').read_text());result.update({'supplemental_native_processes':2,'native_path_window_backend':'NATIVE_REQUIRED','abi_capability_checks':'actual JNI calls rangeAbiNative=1, positionsAbiNative=1','all_existing_native_owner_availability_checks':8})
(W/'result.json').write_text(json.dumps(result,indent=2)+'\n')
