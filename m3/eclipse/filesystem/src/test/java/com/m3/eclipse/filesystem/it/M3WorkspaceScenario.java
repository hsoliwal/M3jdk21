// Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0
package com.m3.eclipse.filesystem.it;

import com.m3.eclipse.filesystem.M3EclipseFileSystem;
import com.m3.fs.M3FS;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

/** Runs inside a real Equinox bundle with actual ResourcesPlugin workspace services. */
public final class M3WorkspaceScenario {
    private M3WorkspaceScenario() { }

    public static String[] run(String imagePath, boolean integrated) throws Exception {
        String[] completed = new String[15];
        int count = 0;
        check(Objects.equals(integrated ? "jdk.m3fs" : null, M3FS.class.getModule().getName()),
                "backend class must be owned by the selected packaging form");
        if (integrated) check(M3FS.class.getResource("M3FS.class").toString().startsWith("jrt:/jdk.m3fs/"),
                "linked mode must read the backend class from its runtime image");
        check(EFS.getFileSystem("m3fs") instanceof M3EclipseFileSystem, "extension registry must discover m3fs");
        M3EclipseFileSystem filesystem = (M3EclipseFileSystem) EFS.getFileSystem("m3fs");
        completed[count++] = "extension-discovery";
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("m3fs-resources-contract");
        try {
            IFileStore root = filesystem.create("workspace", Path.of(imagePath));
            check(root.equals(EFS.getStore(root.toURI())), "registered EFS URI must round-trip");
            for (String invalid : new String[] {"file:///x", "m3fs://WORK/x", "m3fs://workspace/a/../b",
                    "m3fs://workspace/a%2Fb/", "m3fs://workspace/x?query", "m3fs://workspace:9/x", "m3fs:/x",
                    "m3fs://workspace/%FF", "m3fs://workspace/%C0%AF", "m3fs://workspace/%ED%A0%80"}) {
                IFileStore refused = filesystem.getStore(java.net.URI.create(invalid));
                check(refused.getFileSystem().equals(EFS.getNullFileSystem()) && !refused.fetchInfo().exists(),
                        "invalid URI must produce the registered EFS null filesystem");
            }
            completed[count++] = "efs-uri-round-trip";
            IFileStore sourceStore = root.getChild("sources").mkdir(EFS.NONE, null);
            IFileStore destinationStore = root.getChild("destination").mkdir(EFS.NONE, null);
            project.create(null);
            project.open(null);
            IFolder sources = project.getFolder("sources");
            IFolder destination = project.getFolder("destination");
            sources.createLink(sourceStore.toURI(), IResource.NONE, null);
            destination.createLink(destinationStore.toURI(), IResource.NONE, null);
            check(sources.isLinked() && destination.isLinked(), "workspace folders must be linked");
            check(sources.getLocation() == null && sources.getLocationURI().equals(sourceStore.toURI()),
                    "Resources must retain the non-local m3fs URI");
            completed[count++] = "workspace-linked-folders";
            IFile file = sources.getFile("file.txt");
            file.create(stream("alpha"), IResource.NONE, null);
            check(read(file).equals("alpha"), "IFile create/read must cross EFS");
            completed[count++] = "workspace-create-read";
            file.appendContents(stream("+beta"), IResource.FORCE, null);
            check(read(file).equals("alpha+beta"), "IFile append must retain the original payload");
            file.setContents(stream("replacement"), IResource.FORCE, null);
            check(read(file).equals("replacement"), "IFile setContents must truncate");
            completed[count++] = "workspace-append-replace";
            IFile copied = destination.getFile("copied.txt");
            file.copy(copied.getFullPath(), IResource.NONE, null);
            check(read(copied).equals("replacement"), "Resources must copy between M3FS folders");
            completed[count++] = "m3fs-to-m3fs-copy";
            IFile local = project.getFile("local.txt");
            file.copy(local.getFullPath(), IResource.NONE, null);
            check(local.getLocation() != null && read(local).equals("replacement"), "M3FS-to-local copy");
            completed[count++] = "m3fs-to-local-copy";
            IFile returned = sources.getFile("returned.txt");
            local.copy(returned.getFullPath(), IResource.NONE, null);
            check(read(returned).equals("replacement"), "local-to-M3FS copy");
            completed[count++] = "local-to-m3fs-copy";
            IFile moved = destination.getFile("moved.txt");
            returned.move(moved.getFullPath(), IResource.NONE, null);
            check(!returned.exists() && read(moved).equals("replacement"), "Resources move");
            completed[count++] = "workspace-move";
            try (M3FS external = M3FS.open(Path.of(imagePath))) {
                external.createFile("/sources/external.txt", 0644);
                external.write("/sources/external.txt", 0, bytes("outside-workspace"));
                sources.refreshLocal(IResource.DEPTH_INFINITE, null);
                IFile externalFile = sources.getFile("external.txt");
                check(externalFile.exists() && read(externalFile).equals("outside-workspace"), "external create refresh");
                completed[count++] = "workspace-refresh-external-change";
                external.unlink("/sources/external.txt");
                sources.refreshLocal(IResource.DEPTH_INFINITE, null);
                check(!externalFile.exists(), "external delete refresh");
                completed[count++] = "workspace-delete-refresh";
                long milliseconds = 1_700_000_321_456L;
                external.setMetadata("/sources/file.txt", 0644, milliseconds * 1_000_000L, false, true);
                file.refreshLocal(IResource.DEPTH_ZERO, null);
                check(file.getLocalTimeStamp() == milliseconds, "native timestamp must project to Resources milliseconds");
                completed[count++] = "workspace-native-timestamp";
            }
            IFileStore snapshotRoot = filesystem.mount("snapshot", M3FS.openSnapshot(Path.of(imagePath)));
            IFolder snapshot = project.getFolder("snapshot");
            snapshot.createLink(snapshotRoot.getChild("sources").toURI(), IResource.NONE, null);
            file.setContents(stream("live-after-snapshot"), IResource.FORCE, null);
            check(read(snapshot.getFile("file.txt")).equals("replacement"), "snapshot keeps old payload");
            check(read(file).equals("live-after-snapshot"), "live image advances independently");
            completed[count++] = "snapshot-view-isolation";
            project.close(null);
            project.open(null);
            file = project.getFolder("sources").getFile("file.txt");
            check(read(file).equals("live-after-snapshot"), "workspace project reopen preserves URI consumption");
            completed[count++] = "workspace-project-reopen";
            project.close(null);
            filesystem.unmount("snapshot");
            filesystem.unmount("workspace");
            IFileStore reopened = filesystem.open("workspace", Path.of(imagePath), false);
            try (InputStream input = reopened.getChild("sources").getChild("file.txt").openInputStream(EFS.NONE, null)) {
                check(new String(input.readAllBytes(), StandardCharsets.UTF_8).equals("live-after-snapshot"),
                        "closed and reopened database retains the final content");
            }
            completed[count++] = "durable-image-reopen";
            check(count == completed.length, "all workspace contracts must execute");
            return completed;
        } finally {
            try {
                if (project.exists()) project.delete(false, true, null);
            } finally {
                filesystem.close();
            }
        }
    }

    private static byte[] bytes(String text) { return text.getBytes(StandardCharsets.UTF_8); }
    private static ByteArrayInputStream stream(String text) { return new ByteArrayInputStream(bytes(text)); }
    private static String read(IFile file) throws Exception {
        try (InputStream input = file.getContents()) { return new String(input.readAllBytes(), StandardCharsets.UTF_8); }
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

