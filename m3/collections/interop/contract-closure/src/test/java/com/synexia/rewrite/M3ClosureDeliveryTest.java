/* SPDX-License-Identifier: Apache-2.0 */
package com.synexia.rewrite;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;

/** After-test delivery stages SDK Results, then qualifies their exact runtime bytes. */
final class M3ClosureDeliveryTest {
    static void requireProof(Path report) throws Exception {
        var factory=DocumentBuilderFactory.newInstance();factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities",false);factory.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
        var root=factory.newDocumentBuilder().parse(report.toFile()).getDocumentElement();
        if(!"8".equals(root.getAttribute("tests")))throw new IllegalStateException("missing proof inventory");
        for(String k:List.of("failures","errors","skipped"))if(!"0".equals(root.getAttribute(k)))throw new IllegalStateException("failed/skipped proof");
        Set<String>names=new HashSet<>();var cases=root.getElementsByTagName("testcase");
        for(int i=0;i<cases.getLength();i++){
            var e=(org.w3c.dom.Element)cases.item(i);
            if(!e.getAttribute("classname").equals("com.synexia.rewrite.M3ClosureRecipeTest")||!names.add(e.getAttribute("name")))throw new IllegalStateException("foreign/duplicate proof");
            if(e.getElementsByTagName("failure").getLength()+e.getElementsByTagName("error").getLength()+e.getElementsByTagName("skipped").getLength()!=0)throw new IllegalStateException("bad proof case");
        }
        if(!names.equals(M3ClosureRecipeTest.METHODS))throw new IllegalStateException("incomplete proof methods");
    }
    @Test void emitAndQualify() throws Exception {
        Path build=Path.of(System.getProperty("m3.build")).toAbsolutePath();
        requireProof(build.resolve("surefire-reports/TEST-com.synexia.rewrite.M3ClosureRecipeTest.xml"));
        Path output=build.resolve("sdk-output");assertFalse(Files.exists(output,LinkOption.NOFOLLOW_LINKS),"fresh stage required");
        var expected=M3ClosureRecipeTest.wanted();
        var actual=M3ClosureRecipeTest.outputs(M3ClosureRecipeTest.run(M3ClosureRecipeTest.named(),List.of()));assertEquals(expected,actual);
        assertTrue(M3ClosureRecipeTest.run(M3ClosureRecipeTest.named(),M3ClosureRecipeTest.parse(actual)).isEmpty());
        Files.createDirectory(output);
        for(var entry:actual.entrySet()){
            Path path=output.resolve(entry.getKey()).normalize();assertTrue(path.startsWith(output.resolve("m3/collections/interop")));
            Files.createDirectories(path.getParent());Files.writeString(path,entry.getValue(),StandardOpenOption.CREATE_NEW);
        }
        List<String>command=List.of("python3",System.getProperty("m3.task")+"/proof/verify_runtime.py", "--stage",output.toString(),
                "--receiver",System.getProperty("m3.receiver"),"--providers",System.getProperty("m3.providers"),"--cache",System.getProperty("m3.cache"));
        Path runtimeLog=build.resolve("runtime-verifier.log");
        Process p=new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(runtimeLog.toFile()).start();
        if (!p.waitFor(180, java.util.concurrent.TimeUnit.SECONDS)) { p.destroyForcibly(); throw new AssertionError("runtime verifier deadline exceeded: "+runtimeLog); }
        int exit=p.exitValue();assertEquals(0,exit,()->"emitted runtime qualification: "+runtimeLog);
        System.out.println("PASS runtime verifier; log="+runtimeLog);
        StringBuilder receipt=new StringBuilder("# Actual OpenRewrite Results; runtime qualification completed before this receipt.\n");
        for(var entry:actual.entrySet()){
            assertEquals(entry.getValue(),Files.readString(output.resolve(entry.getKey())));
            receipt.append(entry.getKey()).append('\t').append(M3ClosureRecipeTest.hash(entry.getValue())).append('\n');
        }
        Files.writeString(output.resolve("qualified-recipe-receipt.tsv"),receipt,StandardOpenOption.CREATE_NEW);
    }
}
