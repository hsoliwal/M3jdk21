// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Result;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

/** Test input/proof custody only; transformations remain in the unchanged canonical recipe. */
final class Fixture {
    static final String RESOURCE = "/com/synexia/rewrite/mask-projection/";
    record Run(List<Result> results, List<Throwable> errors) { }
    private Fixture() { }
    static Path repo() { return Path.of(System.getProperty("m3.target.root")).toAbsolutePath().normalize(); }
    static Path proof() { return Path.of(System.getProperty("m3.proof")).toAbsolutePath().normalize(); }
    static String seal() throws Exception {
        String receipt = Files.readString(proof().resolve("SOURCE.sha256"));
        String root = M3GpuToolJoinManifest.sha256(receipt);
        if (!Files.readString(proof().resolve("LINT_OK")).equals(root+"\n")) throw new IllegalStateException("lint receipt missing or stale");
        for (String row : receipt.lines().toList()) {
            String[] c = row.split("  ",2);
            Path path = repo().resolve(c[1]).normalize();
            if (!path.startsWith(repo()) || !M3GpuToolJoinManifest.sha256(Files.readString(path)).equals(c[0])) throw new IllegalStateException("source drift: "+path);
        }
        return root;
    }
    static M3GpuToolJoinManifest manifest() {
        return M3GpuToolJoinManifest.load(RESOURCE,Fixture.class::getResourceAsStream,()->{});
    }
    static SourceFile source(String path,String text) {
        return PlainText.builder().sourcePath(Path.of(path)).text(text).build();
    }
    static List<SourceFile> inputs(int mask) throws Exception {
        List<SourceFile> result = new ArrayList<>();int bit=0;
        for (var target: manifest().targets()) {
            boolean after = target.text()!=null && (mask & 1<<bit++)!=0;
            if (target.addition() && !after) continue;
            String text;
            if (after) text=target.text();
            else if (target.text()==null) text=Files.readString(repo().resolve(target.path()));
            else try(var in=Fixture.class.getResourceAsStream(RESOURCE+"before/"+Path.of(target.path()).getFileName()+".txt")) {
                if(in==null)throw new IllegalStateException("missing frozen input");
                text=new String(in.readAllBytes(),StandardCharsets.UTF_8);
            }
            if(!M3GpuToolJoinManifest.sha256(text).equals(after?target.after():target.before()))throw new IllegalStateException("fixture drift");
            result.add(source(target.path(),text));
        }
        return result;
    }
    static Recipe named() throws Exception {
        String path="/META-INF/rewrite/m3-mask-projection.yml";
        try(var in=Fixture.class.getResourceAsStream(path)) {
            if(in==null)throw new IllegalStateException("missing named recipe");
            var loader=new YamlResourceLoader(in,java.net.URI.create("classpath:"+path),new java.util.Properties(),Fixture.class.getClassLoader());
            Recipe recipe=Environment.builder().load(loader).build().activateRecipes("com.synexia.m3.MaskProjectionRefinement");
            if(!recipe.validate().isValid())throw new IllegalStateException("invalid named recipe");
            return recipe;
        }
    }
    static Run run(List<SourceFile> inputs, Runnable checkpoint) throws Exception {
        var errors=new ConcurrentLinkedQueue<Throwable>();var context=new InMemoryExecutionContext(errors::add);
        if(checkpoint!=null)context.putMessage(M3GpuToolJoinRecipe.CHECKPOINT_MESSAGE,checkpoint);
        try {
            var results=named().run(new InMemoryLargeSourceSet(inputs),context).getChangeset().getAllResults();
            return new Run(results,List.copyOf(errors));
        } catch(RuntimeException failure) {errors.add(failure);return new Run(List.of(),List.copyOf(errors));}
    }
    static List<SourceFile> apply(List<SourceFile> inputs,Run run) {
        if(!run.errors().isEmpty())throw new IllegalStateException("SDK errors refuse publication");
        Map<Path,SourceFile> result=new TreeMap<>();for(var f:inputs)result.put(f.getSourcePath(),f);
        for(var change:run.results()) {if(change.getAfter()==null)throw new IllegalStateException("removal refused");result.put(change.getAfter().getSourcePath(),change.getAfter());}
        return new ArrayList<>(result.values());
    }
    static Map<String,String> texts(List<SourceFile> input) {
        Map<String,String> out=new TreeMap<>();for(var f:input)out.put(f.getSourcePath().toString(),f.printAll());return out;
    }
    static boolean failure(Run run,String fragment) {
        for(Throwable e:run.errors())for(Throwable f=e;f!=null;f=f.getCause())if(String.valueOf(f.getMessage()).contains(fragment))return true;
        return false;
    }
    static void emit(Path root,SourceFile source) throws Exception {
        Path p=root.resolve(source.getSourcePath()).normalize();
        if(!p.startsWith(root)||Files.exists(p))throw new IllegalStateException("unadmitted receiving path");
        Files.createDirectories(p.getParent());Files.writeString(p,source.printAll());
    }
}
