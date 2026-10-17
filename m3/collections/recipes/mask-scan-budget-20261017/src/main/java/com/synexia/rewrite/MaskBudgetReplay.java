/* SPDX-License-Identifier: Apache-2.0 */
package com.synexia.rewrite;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CancellationException;

/** Calls the existing manifest kernel. This is a fixture runner, not the OpenRewrite scheduler. */
public final class MaskBudgetReplay {
    private static int checks;
    private static void check(boolean value) { checks++; if (!value) throw new AssertionError("recipe check "+checks); }
    private static void rejects(Runnable action) {
        try { action.run(); } catch (IllegalStateException expected) { checks++; return; }
        throw new AssertionError("expected rejection");
    }
    public static void main(String[] args) throws Exception {
        Path repo=Path.of(args[0]).toRealPath(),out=Path.of(args[1]).toAbsolutePath();
        if(Files.exists(out))throw new IllegalStateException("fresh output directory required");
        Path res=repo.resolve("m3/collections/recipes/mask-scan-budget-20261017/src/main/resources");
        String root="/com/synexia/rewrite/mask-scan-budget-20261017/";
        M3GpuToolJoinManifest.Resources loader=name->Files.newInputStream(res.resolve(name.substring(1)));
        var manifest=M3GpuToolJoinManifest.load(root,loader,()->{});var scan=manifest.scan();
        Map<String,String> actual=new LinkedHashMap<>();
        for(var t:manifest.targets())if(Files.exists(repo.resolve(t.path()))) {
            String hash=M3GpuToolJoinManifest.sha256(Files.readString(repo.resolve(t.path())));
            actual.put(t.path(),hash);scan.observe(t.path(),hash);
        }
        scan.requireAdmissible();
        var targets=new ArrayList<>(manifest.targets());
        for(int seed=0;seed<32;seed++) {
            Collections.shuffle(targets,new Random(seed));var replay=manifest.scan();
            for(var t:targets)replay.observe(t.path(),t.after());
            replay.requireAdmissible();
            for(var t:targets){check(!replay.missing(t.path()));check(replay.editing(t.path(),t.after()).equals(t));}
        }
        var owner=targets.stream().filter(t->!t.addition()).findFirst().orElseThrow();
        rejects(()->manifest.scan().requireAdmissible());
        rejects(()->{var s=manifest.scan();for(var t:targets)s.observe(t.path(),t.after());s.observe(owner.path(),owner.after());s.requireAdmissible();});
        rejects(()->{var s=manifest.scan();for(var t:targets)s.observe(t.path(),t.path().equals(owner.path())?"0".repeat(64):t.after());s.requireAdmissible();});
        rejects(()->{var s=manifest.scan();for(var t:targets)s.observe(t.path(),t.after());s.requireAdmissible();s.editing(owner.path(),"0".repeat(64));});
        rejects(()->M3GpuToolJoinManifest.load(root,loader,()->{throw new CancellationException();}));
        rejects(()->M3GpuToolJoinManifest.load(root,name->name.endsWith("manifest.tsv")?loader.open(name):new ByteArrayInputStream("bad".getBytes(StandardCharsets.UTF_8)),()->{}));
        int emitted=0;
        for(var t:manifest.targets())if(t.text()!=null){
            if(actual.containsKey(t.path()))scan.editing(t.path(),actual.get(t.path()));
            Path p=out.resolve(t.path());Files.createDirectories(p.getParent());Files.writeString(p,t.text(),StandardOpenOption.CREATE_NEW);emitted++;
        }
        System.out.println("MaskBudgetReplay checks="+checks+" orderings=32 emitted="+emitted);
    }
}
