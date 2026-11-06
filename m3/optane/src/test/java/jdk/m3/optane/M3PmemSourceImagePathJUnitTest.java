// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.optane;

import static org.junit.jupiter.api.Assertions.*;
import jdk.m3.optane.M3PmemSourceImage;
import java.nio.file.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/5ecfe052562cdd1a7a6570068654c7f813fd0f47/m3-pmem-sdk/src/junit/java/test/PmemSourceImagePathJUnitTest.java blob 3a884062e928; recipe m3/optane/recipe/pmem-source-image-20261106/relocate.py v3
final class M3PmemSourceImagePathJUnitTest {
    @TempDir Path temporary;
    @Test void portableDriveAndStreamFormsRefuseBeforeAnySourceAccess() throws Exception {
        Path repository=Files.createDirectory(temporary.resolve("repo"));
        var limits=new M3PmemSourceImage.Limits(2,100,100,16);
        for(String unsafe:List.of("C:outside.java","C:/outside.java","dir/C:outside.java","x:y.java")) {
            Path destination=temporary.resolve("image");
            assertThrows(IllegalArgumentException.class,()->M3PmemSourceImage.pack(repository,List.of(unsafe),destination,limits));
            assertFalse(Files.exists(destination));
        }
    }
}
