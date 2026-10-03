// SPDX-License-Identifier: Apache-2.0
package com.m3.pack;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class M3PackToolTest {
    @TempDir Path temp;

    @Test void verifiesStagesAndExecutesRealArchiveCommands() throws Exception {
        Path jar = M3ArchiveFixtures.jar(temp.resolve("value.jar"),
                M3ArchiveFixtures.classes(temp, "example.locked"), false);
        Path lock = M3ArchiveFixtures.lock(temp, M3ArchiveFixtures.row(jar, "example.locked"));
        M3PackTool tool = new M3PackTool();
        assertEquals(1, tool.verify(lock).size());
        Path stagedLock = tool.stage(lock, temp.resolve("staged"));
        assertEquals(tool.verify(lock).getFirst().sha256(), tool.verify(stagedLock).getFirst().sha256());
        assertThrows(IOException.class, () -> tool.stage(lock, temp.resolve("staged")));
        M3PackTool.main(new String[] {"verify", lock.toString(), "example.locked"});
        M3PackTool.main(new String[] {"stage", lock.toString(), temp.resolve("command-stage").toString()});
        assertThrows(IllegalArgumentException.class, () -> M3PackTool.main(new String[0]));
        assertThrows(IllegalArgumentException.class, () -> M3PackTool.main(new String[] {"unknown", "x", "y"}));
        assertThrows(IllegalStateException.class, () -> M3PackTool.requireJava21(22));
        M3PackTool.requireJava21(21);
        Path empty = M3ArchiveFixtures.lock(temp, "");
        assertTrue(tool.verify(empty).isEmpty());
        M3PackTool.main(new String[] {"verify", empty.toString(), "jdk.jcmd,jdk.jfr"});
    }

    @Test void refusesIdentityDriftInvalidRowsTraversalAndDuplicateModules() throws Exception {
        Path jar = M3ArchiveFixtures.jar(temp.resolve("value.jar"),
                M3ArchiveFixtures.classes(temp, "example.locked"), false);
        String row = M3ArchiveFixtures.row(jar, "example.locked");
        String hash = M3ModuleInspector.sha256(jar);
        M3PackTool tool = new M3PackTool();
        for (String bad : new String[] {"", "wrong\n"}) {
            Path lock = Files.writeString(temp.resolve("bad.tsv"), bad);
            assertThrows(IllegalArgumentException.class, () -> tool.verify(lock));
        }
        for (String badRow : new String[] {
                "no-fields\n", "value.jar\tnot-a-hash\texample.locked\n",
                "../value.jar\t" + hash + "\texample.locked\n",
                jar.toAbsolutePath() + "\t" + hash + "\texample.locked\n",
                "value.jar\t" + "0".repeat(64) + "\texample.locked\n",
                row.replace("example.locked", "example.wrong"), row + row}) {
            Path lock = M3ArchiveFixtures.lock(temp, badRow);
            assertThrows(IllegalArgumentException.class, () -> tool.verify(lock));
        }
        Path lock = M3ArchiveFixtures.lock(temp, row);
        M3PackTool drift = new M3PackTool((source, target) -> Files.writeString(target, "corrupted"));
        assertThrows(IOException.class, () -> drift.stage(lock, temp.resolve("drifted")));
        Path outside = Files.createTempFile("m3-outside", ".jar");
        try {
            Files.createSymbolicLink(temp.resolve("outside.jar"), outside);
            Path escaped = M3ArchiveFixtures.lock(temp, "outside.jar\t" + hash + "\texample.locked\n");
            assertThrows(IllegalArgumentException.class, () -> tool.verify(escaped));
        } finally {
            Files.delete(outside);
        }
    }

    @Test void stagesRealJmodWithoutChangingItsContents() throws Exception {
        M3ArchiveFixtures.classes(temp, "example.nativepack");
        Path jmod = temp.resolve("nativepack.jmod");
        Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "jmod").toString(),
                "create", "--class-path", temp.resolve("classes-example.nativepack").toString(),
                "--module-version", "1.0", jmod.toString()).inheritIO().start();
        assertEquals(0, process.waitFor());
        Path lock = M3ArchiveFixtures.lock(temp, M3ArchiveFixtures.row(jmod, "example.nativepack"));
        Path result = new M3PackTool().stage(lock, temp.resolve("jmods"));
        M3PackTool.main(new String[] {"verify", result.toString(), "example.nativepack"});
    }
}
