// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class RuntimeTest {
    @Test void requireTargetJavaAndActualJniAfterRecipeQualification() throws Exception {
        String seal=Fixture.seal();assertEquals(seal+"\n",Files.readString(Fixture.proof().resolve("SDK_OK")));
        Path task=Path.of(System.getProperty("m3.task")).toAbsolutePath().normalize();
        Process process=new ProcessBuilder(System.getProperty("m3.python"),task.resolve("qualify.py").toString(),"runtime","--root",Fixture.repo().toString(),"--proof",Fixture.proof().toString())
            .redirectOutput(Fixture.proof().resolve("runtime.stdout").toFile()).redirectError(Fixture.proof().resolve("runtime.stderr").toFile()).start();
        boolean ended=process.waitFor(12,TimeUnit.MINUTES);
        if(!ended){process.descendants().forEach(ProcessHandle::destroyForcibly);process.destroyForcibly();process.waitFor();}
        assertTrue(ended,"bounded runtime timeout");assertEquals(0,process.exitValue(),"see raw runtime stdout/stderr");
        assertEquals(seal+"\n",Files.readString(Fixture.proof().resolve("RUNTIME_OK")));assertEquals(seal,Fixture.seal());
    }
}
