// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
import com.m3.file.M3File;
import com.m3.file.M3FileSnapshot;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

/** Executable smoke check against the installed com.m3.file module. */
public final class M3FileModuleProbe {
    private M3FileModuleProbe() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("expected an owned test directory");
        Path root = Path.of(args[0]);
        Files.createDirectories(root);
        Path path = root.resolve("probe.class");
        if (Files.exists(path)) throw new IllegalArgumentException("probe path must be absent");
        M3File file = M3File.of(path);
        M3FileSnapshot absent = file.snapshot(8);
        Files.write(path, new byte[] {(byte) 0xca, (byte) 0xfe, (byte) 0xba, (byte) 0xbe, 1});
        M3FileSnapshot first = file.snapshot(8);
        require(first.changeFrom(absent), M3FileSnapshot.Change.CREATED);
        require(file.snapshot(8).changeFrom(first), M3FileSnapshot.Change.UNCHANGED);
        FileTime originalTime = first.metadata().orElseThrow().lastModified();
        Files.write(path, new byte[] {(byte) 0xca, (byte) 0xfe, (byte) 0xba, (byte) 0xbe, 2});
        Files.setLastModifiedTime(path, originalTime);
        M3FileSnapshot changed = file.snapshot(8);
        require(changed.changeFrom(first), M3FileSnapshot.Change.CONTENT_CHANGED);
        Files.setLastModifiedTime(path, FileTime.fromMillis(originalTime.toMillis() + 10_000));
        M3FileSnapshot touched = file.snapshot(8);
        require(touched.changeFrom(changed), M3FileSnapshot.Change.METADATA_CHANGED);
        Files.delete(path);
        require(file.snapshot(8).changeFrom(touched), M3FileSnapshot.Change.DELETED);
        if (!"com.m3.file".equals(M3File.class.getModule().getName())) {
            throw new AssertionError("M3File must load from its named product module");
        }
        System.out.println("PASS com.m3.file installed module: five source/class byte-delta states");
    }

    private static void require(M3FileSnapshot.Change actual, M3FileSnapshot.Change expected) {
        if (actual != expected) throw new AssertionError("expected " + expected + ", got " + actual);
    }
}
