// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.text.PlainText;

/** Splits JEP 497 released-state paths into private whole-file and shared semantic-recipe lanes. */
public final class M3Jep497SharedOwnerSplitRecipe
        extends ScanningRecipe<M3Jep497SharedOwnerSplitRecipe.Inventory> {
    private static final String ROOT =
            "/com/m3/rewrite/backport/jep497-shared-owner-split-20261007/";

    private static final Map<String, Target> TARGETS = Map.of(
            ".github/workflows/m3-jep497-mldsa-inventory.yml",
            new Target("workflow.before.yml","workflow.after.yml",
                    "11425741b788db3806e3c71cfcf02327f6bcc9b6",
                    "771c834dbfb5f0a7930eb8f91a3df583fa58699f"),
            "m3/backports/recipes/jep-497-mldsa/README.md",
            new Target("README.before.md","README.after.md",
                    "aa337ea74e20be8e94dd048f8e573a9ab1ef0777",
                    "6d40957c80c46b78cafb34d5f0403590c20626a9"),
            "m3/backports/recipes/jep-497-mldsa/MATERIALIZE_PATHS.txt",
            new Target("MATERIALIZE.before.txt","MATERIALIZE.after.txt",
                    "c5d456d93c3ea10bde4cb9e8ab1a0860f5876548",
                    "0adcadd7ccef2fa2101140ec8ddb3f51dae38182"),
            "m3/backports/recipes/jep-497-mldsa/SHARED_OWNER_PATHS.txt",
            new Target(null,"SHARED_OWNER_PATHS.txt","ABSENT",
                    "c55b5bafbe58dbe71f0eb4951382d23b36a4226d"));

    static final class Inventory { final Map<String,String> seen=new HashMap<>(); }
    private record Target(String beforeResource,String afterResource,String beforeBlob,String afterBlob) {}

    @Override public String getDisplayName() { return "Split JEP 497 shared owners"; }
    @Override public String getDescription() {
        return "Moves shared security owners out of whole-file GA materialization into targeted semantic recipe work.";
    }
    @Override public Set<String> getTags() {
        return Set.of("m3","jdk21","jep-497","shared-owner","semantic-recipe","file-atomic","candidate-only");
    }
    @Override public int maxCycles() { return 1; }

    @Override public Inventory getInitialValue(ExecutionContext context) {
        TARGETS.forEach((path,target)->{
            if(!"ABSENT".equals(target.beforeBlob())) requireBlob(target.beforeResource(),target.beforeBlob());
            requireBlob(target.afterResource(),target.afterBlob());
        });
        return new Inventory();
    }

    @Override public TreeVisitor<?,ExecutionContext> getScanner(Inventory inventory) {
        return new TreeVisitor<Tree,ExecutionContext>() {
            @Override public Tree preVisit(Tree tree,ExecutionContext context) {
                if(!(tree instanceof SourceFile source)) return tree;
                stopAfterPreVisit();
                String path=normalized(source.getSourcePath());
                Target target=TARGETS.get(path);
                if(target==null) return tree;
                if(!(source instanceof PlainText)) throw new IllegalStateException("not PlainText: "+path);
                String blob=gitBlob(source.printAll());
                if(!blob.equals(target.beforeBlob())&&!blob.equals(target.afterBlob()))
                    throw new IllegalStateException("JEP497 split drift: "+path);
                if(inventory.seen.putIfAbsent(path,blob)!=null)
                    throw new IllegalStateException("duplicate JEP497 split target: "+path);
                return tree;
            }
        };
    }

    @Override public Collection<? extends SourceFile> generate(Inventory inventory,ExecutionContext context) {
        List<SourceFile> generated=new ArrayList<>();
        for(var entry:TARGETS.entrySet()) {
            if(!inventory.seen.containsKey(entry.getKey())) {
                if(!"ABSENT".equals(entry.getValue().beforeBlob()))
                    throw new IllegalStateException("missing JEP497 split target: "+entry.getKey());
                generated.add(PlainText.builder().sourcePath(Path.of(entry.getKey()))
                        .text(resource(entry.getValue().afterResource())).build());
            }
        }
        return generated;
    }

    @Override public TreeVisitor<?,ExecutionContext> getVisitor(Inventory inventory) {
        return new TreeVisitor<Tree,ExecutionContext>() {
            @Override public Tree preVisit(Tree tree,ExecutionContext context) {
                if(!(tree instanceof SourceFile source)) return tree;
                stopAfterPreVisit();
                String path=normalized(source.getSourcePath());
                Target target=TARGETS.get(path);
                if(target==null) return tree;
                String current=gitBlob(source.printAll());
                if(current.equals(target.afterBlob())) return tree;
                if(!current.equals(target.beforeBlob()))
                    throw new IllegalStateException("JEP497 split changed after scan: "+path);
                return PlainText.builder().sourcePath(source.getSourcePath())
                        .text(resource(target.afterResource())).build()
                        .withId(source.getId()).withMarkers(source.getMarkers())
                        .withFileAttributes(source.getFileAttributes()).withCharset(source.getCharset())
                        .withCharsetBomMarked(source.isCharsetBomMarked()).withChecksum(null);
            }
        };
    }

    public boolean productSourceMutationAuthority(){return false;}
    public boolean promotionAuthority(){return false;}
    static String before(String path){Target t=require(path);if("ABSENT".equals(t.beforeBlob()))throw new IllegalArgumentException("absent preimage");return resource(t.beforeResource());}
    static String after(String path){return resource(require(path).afterResource());}
    static Set<String> targetPaths(){return Set.copyOf(TARGETS.keySet());}
    private static Target require(String path){Target t=TARGETS.get(path);if(t==null)throw new IllegalArgumentException(path);return t;}
    private static void requireBlob(String res,String expected){if(!gitBlob(resource(res)).equals(expected))throw new IllegalStateException("resource drift: "+res);}
    private static String resource(String name){
        try(var in=M3Jep497SharedOwnerSplitRecipe.class.getResourceAsStream(ROOT+name)){
            if(in==null)throw new IllegalStateException("missing resource: "+name);
            return new String(in.readAllBytes(),StandardCharsets.UTF_8);
        }catch(IOException e){throw new IllegalStateException(e);}
    }
    static String gitBlob(String text){
        byte[] bytes=Objects.requireNonNull(text).getBytes(StandardCharsets.UTF_8);
        byte[] prefix=("blob "+bytes.length+"\0").getBytes(StandardCharsets.UTF_8);
        try{MessageDigest d=MessageDigest.getInstance("SHA-1");d.update(prefix);d.update(bytes);return HexFormat.of().formatHex(d.digest());}
        catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    private static String normalized(Path path){return path.normalize().toString().replace('\\','/');}
}
