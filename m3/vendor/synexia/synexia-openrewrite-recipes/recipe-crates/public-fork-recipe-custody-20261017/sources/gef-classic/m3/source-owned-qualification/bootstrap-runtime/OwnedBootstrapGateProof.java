// SPDX-License-Identifier: Apache-2.0
// Exercised against the owned, unchanged compiled bootstrap; no gate implementation copied.
import com.synexia.m3.bootstrap.M3BootstrapInventoryRecipe;
import com.synexia.m3.bootstrap.M3BootstrapTaskRecipe;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;

public final class OwnedBootstrapGateProof {
    private static String hash(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }

    private static void properties(Path path, String expected) {
        System.setProperty("m3.llm.taskCrateFile", path.toString());
        System.setProperty("m3.llm.taskCrateRoot", expected);
    }

    private static void refuse(String expectedMessage) {
        try {
            new M3BootstrapTaskRecipe().getRecipeList();
        } catch (IllegalArgumentException | IllegalStateException expected) {
            if (expectedMessage.equals(expected.getMessage())) {
                return;
            }
            throw new IllegalStateException("Wrong refusal: " + expected, expected);
        }
        throw new IllegalStateException("Gate accepted " + expectedMessage);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Exact proof scratch directory required");
        Path base = Path.of(args[0]);
        Path valid = base.resolve("gate-valid.json");
        Path empty = base.resolve("gate-empty.json");
        Path oversized = base.resolve("gate-oversized.bin");
        Files.writeString(valid, "{\"kind\":\"owned-GEF-recipe-proof\"}\n");
        Files.write(empty, new byte[0]);
        Files.write(oversized, new byte[4 * 1024 * 1024 + 1]);
        System.clearProperty("m3.llm.taskCrateFile");
        System.clearProperty("m3.llm.taskCrateRoot");
        refuse("M3 task-crate properties required");
        properties(valid, hash(valid));
        var task = new M3BootstrapTaskRecipe();
        var recipes = task.getRecipeList();
        if (task.directTargetFileMutationAuthority() || task.maxCycles() != 1 || recipes.size() != 1 ||
                !(recipes.getFirst() instanceof M3BootstrapInventoryRecipe inventory) || inventory.mutationAuthority()) {
            throw new IllegalStateException("Owned read-only composition contract drift");
        }
        properties(valid, hash(valid).toUpperCase(Locale.ROOT));
        if (new M3BootstrapTaskRecipe().getRecipeList().size() != 1) throw new IllegalStateException("Hex normalization drift");
        properties(valid, "0".repeat(64));
        refuse("M3_TASK_CRATE_SHA256");
        properties(empty, hash(empty));
        refuse("M3_TASK_CRATE_SIZE");
        properties(oversized, hash(oversized));
        refuse("M3_TASK_CRATE_SIZE");
        properties(base, "0".repeat(64));
        refuse("M3_TASK_CRATE_NOT_REGULAR");
        System.out.println("OWNED_BOOTSTRAP_GATE_PASS valid/read-only/uppercase/missing/hash-drift/empty/oversized/nonregular; ancestor lifetime and cross-platform refusal are not inferred");
    }
}
