// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.fs;
import com.m3.file.M3File;
import com.m3.file.M3StreamProgress;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
/** Executable host-input contract; native SQLite stage is verified separately. */
public final class M3HostFileInventoryProof {
    private static int checks;
    private static void check(boolean ok,String name){checks++;if(!ok)throw new AssertionError(name);}
    @FunctionalInterface interface Code { void run() throws Exception; }
    private static void expect(Class<? extends Throwable> error,Code code,String name){checks++;try{code.run();}catch(Throwable t){if(error.isInstance(t))return;throw new AssertionError(name+": "+t,t);}throw new AssertionError("missing "+name);}
    private static byte[] hash(String s) throws Exception {return MessageDigest.getInstance("SHA-256").digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    private static final M3HostFileInventory.Limits LARGE = new M3HostFileInventory.Limits(10000,12_000_000,1_000_000,12_000_000);
    private static M3HostFileInventory.Result scan(Path root,String policy,byte[] context,M3BuildState old,
        M3HostFileInventory.Limits limits, M3StreamProgress progress,
        M3HostFileInventory.ByteLayer layer) throws Exception {
        try(Stream<Path> stream=Files.walk(root)){
            return M3HostFileInventory.capture(root,stream.map(M3File::of),policy,p->true,context,old,limits,progress,layer);
        }
    }
    public static void main(String[] args) throws Exception {
        Path project=Files.createTempDirectory("m3-host-v2-");
        try{
            Files.createDirectories(project.resolve("module/src"));
            Files.createDirectories(project.resolve(".m3"));
            Files.createDirectories(project.resolve(".git"));
            Files.writeString(project.resolve("pom.xml"),"root");
            Files.writeString(project.resolve("module/pom.xml"),"child");
            Files.writeString(project.resolve("module/src/App.java"),"class App { int n=0; }");
            Files.writeString(project.resolve(".m3/temporary"),"ignored");
            byte[] profile=hash("maven-v2");
            AtomicLong bytes = new AtomicLong(), progress = new AtomicLong();
            var cold=scan(project,"all",profile,null,LARGE,progress::addAndGet,
                (file,buf,off,len)->bytes.addAndGet(len));
            check(cold.inventory().completeInventory(),"complete initial");
            check(cold.inventory().size()==3,"excluded control files");
            check(cold.delta().changedCount()==3,"all added");
            check(cold.observedBytes()==bytes.get(),"one-read callbacks");
            check(cold.observedBytes()==progress.get(),"progress equals bytes");
            check(cold.inventory().role(cold.inventory().find("module/pom.xml",M3BuildState.BUILD_INPUT))==M3BuildState.BUILD_INPUT,"nested pom");
            var warm=scan(project,"all",profile,cold.inventory(),LARGE,M3StreamProgress.none(),(f,b,o,l)->{});
            check(!warm.needsImpactAnalysis(),"unchanged warm scan");
            check(warm.delta().changedCount()==0,"warm no changes");
            Path java=project.resolve("module/src/App.java");
            FileTime prior=Files.getLastModifiedTime(java);
            Files.writeString(java,"class App { int n=1; }");
            Files.setLastModifiedTime(java,prior);
            var changed=scan(project,"all",profile,warm.inventory(),LARGE,M3StreamProgress.none(),(f,b,o,l)->{});
            check(changed.delta().changedCount()==1,"same mtime file changed");
            check(changed.modifiedRows().count()==1,"one modified mask");
            Files.delete(java);
            Files.writeString(project.resolve("module/src/New.java"),"class New {}");
            var renamed=scan(project,"all",profile,changed.inventory(),LARGE,M3StreamProgress.none(),(f,b,o,l)->{});
            check(renamed.delta().changedCount()==2,"add and remove");
            check(renamed.removedPaths().length==1,"removed identity");
            check(renamed.addedRows().count()==1,"one added identity");
            var different=scan(project,"new-policy",profile,renamed.inventory(),LARGE,M3StreamProgress.none(),(f,b,o,l)->{});
            check(!different.delta().comparable(),"scope change incomparable");
            check(different.removedPaths().length==0,"scope change no fabricated delete");
            expect(IOException.class,()->M3HostFileInventory.capture(project,Stream.of(M3File.of(project.resolve("pom.xml")),M3File.of(project.resolve("pom.xml"))),
                "duplicate",p->true,profile,null,LARGE,M3StreamProgress.none(),(f,b,o,l)->{}),"duplicate path");
            expect(IOException.class,()->M3HostFileInventory.capture(project,Stream.of(M3File.of(project.resolve("../outsider"))),
                "escape",p->true,profile,null,LARGE,M3StreamProgress.none(),(f,b,o,l)->{}),"out of root");
            expect(IOException.class,()->M3HostFileInventory.capture(project,Stream.of(M3File.of(project.resolve("pom.xml"))),
                "parser",p->true,profile,null,LARGE,M3StreamProgress.none(),(f,b,o,l)->{throw new IOException("simulated parser failure");}),"parser failure");
            expect(IOException.class,()->M3HostFileInventory.capture(project,Stream.of(M3File.of(project.resolve("pom.xml"))),
                "budget",p->true,profile,null,new M3HostFileInventory.Limits(1,116,1,1),M3StreamProgress.none(),(f,b,o,l)->{}),"file byte limit");
            expect(IllegalArgumentException.class,()->M3HostFileInventory.capture(project,Stream.of(M3File.of(project.resolve("pom.xml"))),
                "bad-context",p->true,new byte[31],null,LARGE,M3StreamProgress.none(),(f,b,o,l)->{}),"bad SHA");
            // Distinct policy identifiers must not collapse under replacement UTF-8.
            expect(IllegalArgumentException.class,()->M3HostFileInventory.capture(project,
                Stream.of(M3File.of(project.resolve("pom.xml"))),"policy-\uD800",p->true,
                profile,null,LARGE,M3StreamProgress.none(),(f,b,o,l)->{}),"malformed UTF16 selection");
            // A symlink hidden in a project-root ancestor must not escape source identity.
            Path alias=project.resolve("alias-directory");
            try {
                Files.createSymbolicLink(alias,project.resolve("module"));
                Path maskedRoot=alias.resolve("src");
                expect(IOException.class,()->M3HostFileInventory.capture(maskedRoot,
                    Stream.of(M3File.of(maskedRoot.resolve("New.java"))),"aliased-root",p->true,
                    profile,null,LARGE,M3StreamProgress.none(),(f,b,o,l)->{}),"root ancestor symlink");
            } catch (UnsupportedOperationException | SecurityException | IOException unsupported) {
                // A filesystem without symlink creation cannot run this check.
            }
            Files.deleteIfExists(alias);
            Path link=project.resolve("linked.java");
            try {
                Files.createSymbolicLink(link,project.resolve("module/src/New.java"));
                expect(IOException.class,()->scan(project,"links",profile,null,LARGE,M3StreamProgress.none(),(f,b,o,l)->{}),"symlink refusal");
            } catch (UnsupportedOperationException | SecurityException | IOException unsupported) {
                // Test hosts without symlink creation privileges still exercise other refusals.
            }
            Files.deleteIfExists(link);
            Random random=new Random(20261021);
            var paths=new ArrayList<Path>();
            for(int i=0;i<180;i++){
                Path extra=project.resolve(String.format("module/src/file-%04d.java",random.nextInt(10000)+10000*i));
                Files.writeString(extra,"class X"+i+" {}");paths.add(extra);
            }
            var randomScan=scan(project,"random-inputs",profile,null,LARGE,M3StreamProgress.none(),(f,b,o,l)->{});
            check(randomScan.inventory().size()==183,"random inventory count");
            for(int i=1;i<randomScan.inventory().size();i++)check(randomScan.inventory().path(i-1).compareTo(randomScan.inventory().path(i))<0,"stable sorted identities");
            System.out.println("M3_HOST_V2_SCOPED_PASS checks="+checks);
        }finally{
            try(var files=Files.walk(project)){var all=files.sorted((a,b)->b.getNameCount()-a.getNameCount()).toList();for(Path p:all)Files.deleteIfExists(p);}
        }
    }
}
