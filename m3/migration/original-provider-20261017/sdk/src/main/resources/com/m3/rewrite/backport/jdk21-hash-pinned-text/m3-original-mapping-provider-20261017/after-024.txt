/* SPDX-License-Identifier: Apache-2.0 */
import com.m3.migration.ExactFileRecipe;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class RecipeContractTest {
    private static int checks;
    private static void require(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
    private static String hash(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
    }
    private static void refuses(Throwing action) throws Exception {
        checks++;
        try { action.run(); }
        catch (IOException | IllegalArgumentException expected) { return; }
        throw new AssertionError("expected refusal");
    }
    private interface Throwing { void run() throws Exception; }
    private static final class Fixture {
        final Path root = Files.createTempDirectory("m3-recipe-test-");
        final Path source = Files.createDirectory(root.resolve("source"));
        final Path target = Files.createDirectory(root.resolve("target"));
        final Path recipe = Files.createDirectory(root.resolve("recipe"));
        final Path plan = recipe.resolve("plan.tsv");
        Fixture() throws Exception {
            Files.createDirectories(target.resolve("m3"));
            Files.writeString(source.resolve("source.java"), "source revision\n");
            Files.writeString(target.resolve("m3/guard.java"), "target baseline\n");
            Files.writeString(recipe.resolve("first.txt"), "first\n");
            Files.writeString(recipe.resolve("second.txt"), "second\n");
            Files.writeString(plan, "M3-EXACT-FILE-RECIPE\t1\n"
                + "source\tsource.java\t" + hash("source revision\n") + "\n"
                + "guard\tm3/guard.java\t" + hash("target baseline\n") + "\n"
                + "add\tm3/new/first.java\t" + hash("first\n") + "\tfirst.txt\n"
                + "add\tm3/new/second.java\t" + hash("second\n") + "\tsecond.txt\n");
        }
    }
    public static void main(String[] args) throws Exception {
        Fixture f = new Fixture();
        var dry = ExactFileRecipe.apply(f.source, f.target, f.plan, true);
        require(dry.changed() == 2 && dry.dryRun(), "dry run count");
        require(!Files.exists(f.target.resolve("m3/new")), "dry run writes nothing");
        require(!Files.exists(f.target.resolve("m3/migration")), "dry run creates no state");
        var first = ExactFileRecipe.apply(f.source, f.target, f.plan, false);
        require(first.changed() == 2, "first apply");
        require(Files.readString(f.target.resolve("m3/new/first.java")).equals("first\n"), "exact bytes");
        var again = ExactFileRecipe.apply(f.source, f.target, f.plan, false);
        require(again.changed() == 0 && again.unchanged() == 2, "idempotence");
        require(ExactFileRecipe.rollback(f.target, f.plan).changed() == 2, "rollback owned files");
        require(!Files.exists(f.target.resolve("m3/new/first.java")), "removed only generated file");
        require(Files.readString(f.target.resolve("m3/guard.java")).equals("target baseline\n"), "guard preserved");

        f = new Fixture();
        Files.createDirectories(f.target.resolve("m3/new"));
        Files.writeString(f.target.resolve("m3/new/first.java"), "first\n");
        require(ExactFileRecipe.apply(f.source, f.target, f.plan, false).changed() == 1, "partial-state replay");
        require(ExactFileRecipe.rollback(f.target, f.plan).changed() == 1, "rollback ownership");
        require(Files.exists(f.target.resolve("m3/new/first.java")), "preexisting identical file retained");

        Fixture sourceDrift = new Fixture();
        Files.writeString(sourceDrift.source.resolve("source.java"), "changed\n");
        refuses(() -> ExactFileRecipe.apply(sourceDrift.source, sourceDrift.target, sourceDrift.plan, false));
        require(!Files.exists(sourceDrift.target.resolve("m3/new")), "source drift before writes");
        Fixture guardDrift = new Fixture();
        Files.writeString(guardDrift.target.resolve("m3/guard.java"), "changed\n");
        refuses(() -> ExactFileRecipe.apply(guardDrift.source, guardDrift.target, guardDrift.plan, false));
        Fixture snapshotDrift = new Fixture();
        Files.writeString(snapshotDrift.recipe.resolve("second.txt"), "tampered\n");
        refuses(() -> ExactFileRecipe.apply(snapshotDrift.source, snapshotDrift.target, snapshotDrift.plan, false));
        require(!Files.exists(snapshotDrift.target.resolve("m3/new")), "late snapshot drift before writes");
        Fixture late = new Fixture();
        Files.createDirectories(late.target.resolve("m3/new"));
        Files.writeString(late.target.resolve("m3/new/second.java"), "user work\n");
        refuses(() -> ExactFileRecipe.apply(late.source, late.target, late.plan, false));
        require(!Files.exists(late.target.resolve("m3/new/first.java")), "preflight every destination");
        require(Files.readString(late.target.resolve("m3/new/second.java")).equals("user work\n"), "never overwrite");

        Fixture changedOutput = new Fixture();
        ExactFileRecipe.apply(changedOutput.source, changedOutput.target, changedOutput.plan, false);
        Files.writeString(changedOutput.target.resolve("m3/new/second.java"), "target adaptation\n");
        refuses(() -> ExactFileRecipe.rollback(changedOutput.target, changedOutput.plan));
        require(Files.exists(changedOutput.target.resolve("m3/new/first.java")), "rollback preflight is all-or-refuse");

        for (String bad : new String[] {"../escape", "/absolute", "m3/../../escape", "m3/x\\y", "m3//x", "src/java.base/String.java"}) {
            Fixture path = new Fixture();
            Files.writeString(path.plan, Files.readString(path.plan).replace("m3/new/second.java", bad));
            refuses(() -> ExactFileRecipe.apply(path.source, path.target, path.plan, false));
        }
        Fixture duplicate = new Fixture();
        Files.writeString(duplicate.plan, Files.readString(duplicate.plan).replace("m3/new/second.java", "m3/new/first.java"));
        refuses(() -> ExactFileRecipe.apply(duplicate.source, duplicate.target, duplicate.plan, false));
        Fixture symlink = new Fixture();
        Path elsewhere = Files.createDirectory(symlink.root.resolve("elsewhere"));
        Files.createSymbolicLink(symlink.target.resolve("m3/new"), elsewhere);
        refuses(() -> ExactFileRecipe.apply(symlink.source, symlink.target, symlink.plan, false));
        try (var entries = Files.list(elsewhere)) {
            require(entries.findAny().isEmpty(), "symlink destination untouched");
        }
        Fixture linkedInput = new Fixture();
        Files.delete(linkedInput.source.resolve("source.java"));
        Files.createSymbolicLink(linkedInput.source.resolve("source.java"), linkedInput.target.resolve("m3/guard.java"));
        refuses(() -> ExactFileRecipe.apply(linkedInput.source, linkedInput.target, linkedInput.plan, false));
        Fixture noReceipt = new Fixture();
        refuses(() -> ExactFileRecipe.rollback(noReceipt.target, noReceipt.plan));
        Fixture ancestor = new Fixture();
        Files.writeString(ancestor.target.resolve("m3/block"), "not a directory");
        Files.writeString(ancestor.plan, Files.readString(ancestor.plan).replace("m3/new/second.java", "m3/block/second.java"));
        refuses(() -> ExactFileRecipe.apply(ancestor.source, ancestor.target, ancestor.plan, false));
        require(!Files.exists(ancestor.target.resolve("m3/new/first.java")), "nondirectory ancestor rejected in preflight");
        Fixture nested = new Fixture();
        Files.writeString(nested.plan, Files.readString(nested.plan).replace("m3/new/second.java", "m3/new/first.java/child.java"));
        refuses(() -> ExactFileRecipe.apply(nested.source, nested.target, nested.plan, false));
        require(!Files.exists(nested.target.resolve("m3/new/first.java")), "planned file/directory conflict rejected in preflight");
        Fixture interleaved = new Fixture();
        String three = Files.readString(interleaved.plan).replace("m3/new/second.java", "m3/new/first.java/child.java")
                + "add\tm3/new/first.java-extra\t" + hash("first\n") + "\tfirst.txt\n";
        Files.writeString(interleaved.plan, three);
        refuses(() -> ExactFileRecipe.apply(interleaved.source, interleaved.target, interleaved.plan, false));
        require(!Files.exists(interleaved.target.resolve("m3/new/first.java")), "intervening sort neighbor cannot hide planned ancestor");
        Fixture deleted = new Fixture();
        ExactFileRecipe.apply(deleted.source, deleted.target, deleted.plan, false);
        Files.delete(deleted.target.resolve("m3/new/second.java"));
        refuses(() -> ExactFileRecipe.apply(deleted.source, deleted.target, deleted.plan, false));
        require(!Files.exists(deleted.target.resolve("m3/new/second.java")), "target deletion is drift, not permission to recreate");
        Fixture locked = new Fixture();
        Files.createDirectories(locked.target.resolve("m3/migration/.state"));
        Path foreignLock = locked.target.resolve("m3/migration/.state/lock");
        Files.writeString(foreignLock, "other invocation");
        refuses(() -> ExactFileRecipe.apply(locked.source, locked.target, locked.plan, false));
        require(Files.readString(foreignLock).equals("other invocation"), "never remove another invocation's lock");
        Fixture reserved = new Fixture();
        Files.writeString(reserved.plan, Files.readString(reserved.plan).replace("m3/new/second.java", "m3/migration/.state"));
        refuses(() -> ExactFileRecipe.apply(reserved.source, reserved.target, reserved.plan, false));
        require(!Files.exists(reserved.target.resolve("m3/new/first.java")), "state directory is reserved");
        System.out.println("EXACT_RECIPE_PASS checks=" + checks);
    }
}
