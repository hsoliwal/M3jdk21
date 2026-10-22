// SPDX-License-Identifier: Apache-2.0
// Adapter of the existing official recipe output/fixedpoint/wrongpath proof harness.
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.Validated;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainTextParser;

public final class GefBootstrapRuntimeRecipeProof {
    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    public static void main(String[] args) throws Exception {
        require(args.length == 1, "exact sealed recipe directory required");
        Path base = Path.of(args[0]);
        Path path = Path.of(".m3/openrewrite-recipes/pom.xml");
        String before = Files.readString(base.resolve("before.txt"), StandardCharsets.UTF_8);
        String expected = Files.readString(base.resolve("after.txt"), StandardCharsets.UTF_8);
        var context = new InMemoryExecutionContext(error -> { throw new IllegalStateException(error); });
        var parser = PlainTextParser.builder().build();
        var source = parser.parseInputs(List.of(Parser.Input.fromString(path, before)), null, context).findFirst().orElseThrow();
        require(source.printAll().equals(before), "original byte print drift");
        Recipe recipe;
        Path yaml = base.resolve("recipe.yaml");
        try (var stream = Files.newInputStream(yaml)) {
            recipe = Environment.builder().load(new YamlResourceLoader(stream, yaml.toUri(), null))
                    .build().activateRecipes("com.synexia.recipe.GefBootstrapSlf4jRuntime");
        }
        require(recipe.validateAll().stream().allMatch(Validated::isValid), "recipe validation failed");
        var changes = recipe.run(new InMemoryLargeSourceSet(List.of(source)), context).getChangeset().getAllResults();
        require(changes.size() == 1, "exact one source change required");
        var after = changes.getFirst().getAfter();
        require(after != null && after.getSourcePath().equals(path) && after.printAll().equals(expected), "exact postimage mismatch");
        Files.write(base.resolve("observed-after.txt"), after.printAll().getBytes(StandardCharsets.UTF_8));
        require(recipe.run(new InMemoryLargeSourceSet(List.of(after)), context).getChangeset().getAllResults().isEmpty(), "fixedpoint failed");
        require(recipe.run(new InMemoryLargeSourceSet(List.of(source.withSourcePath(Path.of("wrongpath/pom.xml")))), context)
                .getChangeset().getAllResults().isEmpty(), "wrong path changed");
        var wrongPre = parser.parseInputs(List.of(Parser.Input.fromString(path, before + "\n<!-- source drift -->\n")), null, context).findFirst().orElseThrow();
        // FindAndReplace matches a literal substring. The whole-text source preflight is the separate admission boundary.
        var wrongChanges = recipe.run(new InMemoryLargeSourceSet(List.of(wrongPre)), context).getChangeset().getAllResults();
        require(wrongChanges.size() == 1 && !wrongChanges.getFirst().getAfter().printAll().equals(expected),
                "wrong preimage must fail the sealed exact-output discriminator");
        System.out.println("OFFICIAL_RECIPE_PASS exact-postimage/fixedpoint/wrongpath; wrong-preimage fails exact-output; PlainText only, original owning JUnit red baseline retained separately");
    }
}
