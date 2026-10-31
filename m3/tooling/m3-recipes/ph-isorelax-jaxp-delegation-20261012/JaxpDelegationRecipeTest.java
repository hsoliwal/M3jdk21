// Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Regression suite for the source-bound JAXP delegation recipe. */
public final class JaxpDelegationRecipeTest {
    private JaxpDelegationRecipeTest() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("source and output paths required");
        Path input = Path.of(args[0]).toAbsolutePath().normalize();
        Path output = Path.of(args[1]).toAbsolutePath().normalize();
        String before = Files.readString(input, StandardCharsets.UTF_8);
        String candidate = JaxpDelegationRecipe.apply(before);
        require(!candidate.equals(before), "first application must repair the source");
        require(candidate.contains("@Override\n  public boolean isExpandEntityReferences ()"),
                "standard JAXP getter must delegate");
        require(candidate.contains("@Override\n  public void setExpandEntityReferences (final boolean expandEntityRef)"),
                "standard JAXP setter must delegate");
        require(candidate.contains("return isExpandEntityReferences ();"),
                "singular getter compatibility alias must remain");
        require(candidate.contains("setExpandEntityReferences (expandEntityRef);"),
                "singular setter compatibility alias must remain");
        require(JaxpDelegationRecipe.apply(candidate).equals(candidate), "second pass must be a fixed point");
        refuse(() -> JaxpDelegationRecipe.apply(candidate.replace(
                "public boolean isExpandEntityReferences ()",
                "public boolean isExpandEntityReferencess ()")));
        require(Files.readString(input, StandardCharsets.UTF_8).equals(before),
                "recipe must not mutate its input file");
        Files.createDirectories(output.getParent());
        Files.writeString(output, candidate, StandardCharsets.UTF_8);
        System.out.println("JAXP_DELEGATION_RECIPE_PASS fixed_point=true input_immutable=true drift_refused=true");
    }

    private static void refuse(Checked action) throws Exception {
        try {
            action.run();
            throw new AssertionError("expected source drift refusal");
        } catch (IllegalArgumentException expected) {
            // Expected fail-closed result.
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @FunctionalInterface private interface Checked { void run() throws Exception; }
}
