// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
import com.m3.file.M3FileScope;
import com.m3.file.M3FileSnapshot;
import java.nio.file.Files;
import java.nio.file.Path;

/** Consumer of the installed com.m3.file JMOD, with no patch-module arguments. */
public final class M3FileIntegratedImageProbe {
    private M3FileIntegratedImageProbe() { }
    public static void main(String[] args) throws Exception {
        Path path = Files.writeString(Path.of(args[0]), "alpha");
        try (var scope = new M3FileScope(1)) {
            var first = scope.file(path);
            if (!first.getClass().getModule().getName().equals("com.m3.file")) throw new AssertionError();
            if (first != scope.file(Path.of(args[0]))) throw new AssertionError();
            var before = first.snapshot(5);
            var metadata = first.observe();
            Files.writeString(path, "bravo");
            Files.setLastModifiedTime(path, metadata.metadata().orElseThrow().lastModified());
            if (first.snapshot(5).changeFrom(before) != M3FileSnapshot.Change.CONTENT_CHANGED) throw new AssertionError();
            try (var walk = first.walk(0, p -> true)) {
                if (walk.count() != 1) throw new AssertionError();
            }
        } finally { Files.deleteIfExists(path); }
        System.out.println("M3_FILE_INSTALLED_IMAGE_PASS");
    }
}
