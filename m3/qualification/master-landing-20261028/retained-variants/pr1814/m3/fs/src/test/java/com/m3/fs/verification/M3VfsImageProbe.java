// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.fs.verification;

import com.m3.fs.M3FS;
import com.m3.fs.DatabaseFileProvider;
import com.m3.fs.M3ContentId;
import com.m3.fs.M3FsProgress;
import com.m3.fs.M3ObjectKind;
import com.m3.fs.M3ObjectRef;
import com.m3.fs.MFile;
import com.m3.fs.MFileCapture;
import com.m3.fs.MFileChangeSource;
import com.m3.fs.MFileContent;
import com.m3.fs.MFileDelta;
import com.m3.fs.MFileObservation;
import com.m3.fs.MFileShadow;
import com.m3.fs.MFileStreams;
import com.m3.fs.MFiles;
import com.m3.fs.vfs.FileObject;
import com.m3.fs.vfs.FileName;
import com.m3.fs.vfs.FileSystemManager;
import com.m3.fs.vfs.FileType;
import com.m3.fs.vfs.M3VFS;
import com.m3.fs.vfs.impl.StandardFileSystemManager;
import com.m3.fs.vfs.provider.GenericURLFileName;
import com.m3.fs.vfs.provider.url.UrlFileName;
import com.m3.fs.vfs.provider.url.UrlFileProvider;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.FileSystems;
import java.net.URI;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.logging.LogFactory;

/** Executed by the linked image as an independent named consumer of jdk.m3fs. */
public final class M3VfsImageProbe {
    private static final byte[] PAYLOAD = "M3FS VFS image payload\n".getBytes(StandardCharsets.UTF_8);

    private M3VfsImageProbe() {
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void read(FileSystemManager manager, String uri) throws Exception {
        try (FileObject file = manager.resolveFile(uri);
             InputStream input = file.getContent().getInputStream()) {
            require(Arrays.equals(PAYLOAD, input.readAllBytes()), "content mismatch for " + uri);
        }
    }

    private static void writeRead(FileSystemManager manager, String uri) throws Exception {
        try (FileObject file = manager.resolveFile(uri)) {
            file.getParent().createFolder();
            try (OutputStream output = file.getContent().getOutputStream()) {
                output.write(PAYLOAD);
            }
        }
        read(manager, uri);
    }

    private static MFileObservation observedContent(MFile file) throws Exception {
        MFileObservation metadata = file.observation();
        require(metadata.sourceKey().equals(file.sourceAddress()), "observation source address");
        require(metadata.presence() == MFileObservation.Presence.PRESENT, "observed file presence");
        require(metadata.content().isEmpty(), "metadata must not fabricate complete content");
        AtomicLong worked = new AtomicLong();
        AtomicInteger captures = new AtomicInteger();
        AtomicReference<MFileStreams.Completion> completed = new AtomicReference<>();
        AtomicReference<MFileCapture> captured = new AtomicReference<>();
        ByteArrayOutputStream shadowBytes = new ByteArrayOutputStream();
        MFileShadow observer = new MFileShadow() {
            @Override
            public void onBytes(byte[] bytes, int offset, int length) {
                require(completed.get() == null, "bytes cannot follow raw completion");
                shadowBytes.write(bytes, offset, length);
            }

            @Override
            public void onComplete(MFileStreams.Completion result) {
                require(captured.get() == null, "raw completion observers precede capture");
                require(completed.compareAndSet(null, result), "raw completion must be delivered once");
            }
        };
        MFileContent content = file.progressify(M3FsProgress.of(() -> false, worked::addAndGet))
                .shadow(observer).captureReads(value -> {
                    require(completed.get() != null, "capture requires successful raw completion");
                    require(captured.compareAndSet(null, value), "capture must be delivered once");
                    captures.incrementAndGet();
                });
        long openedAt = System.currentTimeMillis();
        try (InputStream input = content.openInputStream()) {
            require(completed.get() == null && captured.get() == null, "new input cannot be captured");
            require(Arrays.equals(PAYLOAD, input.readAllBytes()), "monitored facade content");
            require(completed.get() == null && captured.get() == null,
                    "EOF alone must not publish before successful close");
        }
        long closedAt = System.currentTimeMillis();
        MFileStreams.Completion completion = completed.get();
        MFileCapture capture = captured.get();
        require(completion != null && capture != null && captures.get() == 1,
                "clean input close must publish one completion and capture");
        require(completion.kind() == MFileStreams.CompletionKind.INPUT_EOF, "input completion kind");
        require(completion.byteCount() == PAYLOAD.length && worked.get() == PAYLOAD.length,
                "composed view counts bytes once");
        require(completion.layer().equals("input"), "default composed input layer");
        require(Arrays.equals(PAYLOAD, shadowBytes.toByteArray()), "input shadow content");
        require(completion.contentHash().equals(M3ContentId.digest(PAYLOAD)), "complete input SHA-256");
        require(capture.capturedAtMillis() >= openedAt && capture.capturedAtMillis() <= closedAt,
                "metadata capture time belongs to this read operation");
        require(capture.observation().sourceKey().equals(file.sourceAddress()), "capture source address");
        M3ObjectRef raw = capture.manifest().raw();
        require(raw.kind() == M3ObjectKind.RAW_BYTES && raw.size() == completion.byteCount()
                        && raw.id().equals(completion.contentHash()), "capture raw manifest binding");
        require(capture.observation().content().orElseThrow().equals(raw), "captured raw observation binding");
        require(capture.manifest().structures().isEmpty(), "a raw capture must not invent word or AST results");
        // The fixture supplies the complete selected file layer. Metadata and
        // read bytes remain separate evidence without a live-source stability claim.
        return capture.observation();
    }

    private static void selectedArchive(Path fixtures) throws Exception {
        try (var entries = MFiles.from(fixtures.resolve("payload.zip"))
                .extensions("txt").enterArchives(file -> file.name().endsWith(".zip"), 1).stream()) {
            var selected = entries.toList();
            require(selected.size() == 1, "selected ZIP layer must emit only its text entry");
            MFile entry = selected.getFirst();
            require(entry.relativeName().equals("payload.zip!/payload.txt"), "selected archive logical name");
            require(entry.uri().getScheme().equals("zip"), "selected archive provider URI");
            require(observedContent(entry).content().orElseThrow().size() == PAYLOAD.length,
                    "selected archive full entry observation");
        }
    }

    private static void urlNames(FileSystemManager core) throws Exception {
        // The donor parser requires a registered scheme in its real component
        // context. These temporary registrations test names only, without I/O
        // or enabling the omitted HTTP clients in the core profile.
        try (StandardFileSystemManager manager = new StandardFileSystemManager()) {
            manager.init();
            manager.addProvider(new String[] {"http", "https"}, new UrlFileProvider());
            GenericURLFileName encoded = (GenericURLFileName) manager.resolveURI(
                    "https://example.invalid:8443/base/caf\u00e9%20done%25.txt?token=a%2Bb&x=1");
            require(encoded.getQueryString().equals("token=a%2Bb&x=1"), "query bytes must be preserved");
            String utf8 = "https://example.invalid:8443/base/caf%C3%A9%20done%25.txt?token=a%2Bb&x=1";
            require(encoded.getURIEncoded(null).equals(utf8), "default URL charset must be UTF-8");
            require(encoded.getURIEncoded("UTF-8").equals(utf8), "explicit UTF-8 URL encoding");
            require(encoded.getURIEncoded("ISO-8859-1").equals(
                    "https://example.invalid:8443/base/caf%E9%20done%25.txt?token=a%2Bb&x=1"),
                    "explicit single-byte URL charset");
            GenericURLFileName percent = (GenericURLFileName) manager.resolveURI(
                    "http://example.invalid/literal%252Fvalue?x=a%20b");
            require(percent.getURIEncoded(null).equals("http://example.invalid/literal%252Fvalue?x=a%20b"),
                    "literal percent escaping must not become a path separator");
            FileName base = manager.resolveURI("https://example.invalid/base/");
            GenericURLFileName relative = (GenericURLFileName) manager.resolveName(base,
                    "../next/caf\u00e9%20x.txt?keep=a%2Bb");
            require(relative.getURIEncoded(null).equals(
                    "https://example.invalid/next/caf%C3%A9%20x.txt?keep=a%2Bb"),
                    "base-relative URL resolution and query preservation");
            UrlFileName hosted = new UrlFileName("http", "example.invalid", 8080, 80, null, null,
                    "/caf\u00e9%20x%25", FileType.FILE, "q=a%2Bb");
            require(hosted.getURIEncoded(null).equals("http://example.invalid:8080/caf%C3%A9%20x%25?q=a%2Bb"),
                    "direct UrlFileName with host");
            for (String host : new String[] {null, ""}) {
                UrlFileName hostless = new UrlFileName("jrt", host, -1, -1, null, null,
                        "/jdk.m3fs/notice%20test.txt", FileType.FILE, "v=1");
                require(hostless.getURIEncoded(null).equals("jrt:/jdk.m3fs/notice%20test.txt?v=1"),
                        "direct UrlFileName without host");
            }
        }
        require(!core.hasProvider("http") && !core.hasProvider("https"), "temporary URL context escaped into core");
    }

    private static void contentDefaultsAndNioPath() throws Exception {
        AtomicLong worked = new AtomicLong();
        MFileContent content = ((MFileContent) () -> new ByteArrayInputStream(PAYLOAD))
                .progressify(M3FsProgress.of(() -> false, worked::addAndGet));
        require(Arrays.equals(PAYLOAD, content.readAllBytes(PAYLOAD.length)), "bounded content default");
        require(worked.get() == PAYLOAD.length, "default preserves configured monitor");
        var runtime = FileSystems.getFileSystem(URI.create("jrt:/"));
        Path notice = runtime.getPath("/modules/jdk.m3fs/META-INF/m3fs-legal/commons-vfs2/NOTICE.txt");
        String source;
        try (var entries = MFiles.from(notice).stream()) {
            MFile file = entries.findFirst().orElseThrow();
            require(file.uri().equals(notice.toUri()), "native NIO URI preserved");
            require(file.readString(8192, StandardCharsets.UTF_8).contains("Apache Commons VFS"),
                    "linked-module NIO content");
            source = file.sourceAddress();
        }
        require(runtime.isOpen(), "caller NIO filesystem survives session close");
        try (var entries = MFiles.from(source).stream()) {
            require(entries.findFirst().orElseThrow().readString(8192, StandardCharsets.UTF_8)
                    .contains("Apache Commons VFS"), "NIO source address reopens same live instance");
        }
    }

    public static void main(String[] args) throws Exception {
        require(args.length == 1, "fixture directory required");
        Path fixtures = Path.of(args[0]);
        for (Class<?> type : new Class<?>[] {M3FS.class, MFiles.class, MFile.class, M3VFS.class,
                MFileContent.class, MFileStreams.class, MFileObservation.class, MFileDelta.class,
                MFileChangeSource.class, MFileCapture.class, MFileShadow.class, DatabaseFileProvider.class}) {
            require("jdk.m3fs".equals(type.getModule().getName()), "module identity: " + type);
        }
        for (String module : Set.of("commons.logging", "org.apache.commons.lang3", "org.apache.commons.io",
                                     "org.apache.commons.compress", "org.apache.commons.codec")) {
            require(ModuleLayer.boot().findModule(module).isPresent(), "dependency missing: " + module);
        }
        require("jdk.m3fs".equals(M3VFS.class.getModule().getName()), "VFS escaped the M3FS module");
        require(new TarArchiveEntry("payload.txt").getName().equals("payload.txt"), "transitive Compress API");
        FileSystemManager manager = M3VFS.getManager();
        try {
            ((StandardFileSystemManager) manager).setLogger(LogFactory.getLog(M3VfsImageProbe.class));
            urlNames(manager);
            for (String scheme : Set.of("file", "ram", "zip", "jar", "gz", "bz2", "tar", "tgz", "tbz2", "tmp", "res", "jrt", "m3db")) {
                require(manager.hasProvider(scheme), "compiled provider not registered: " + scheme);
            }
            for (String scheme : Set.of("ftp", "ftps", "http", "https", "http3", "http3s", "http4", "http4s", "http5", "http5s", "sftp")) {
                require(!manager.hasProvider(scheme), "omitted provider unexpectedly registered: " + scheme);
            }
            read(manager, fixtures.resolve("payload.txt").toUri().toString());
            writeRead(manager, "ram:///m3fs-vfs-image-probe/payload.txt");
            writeRead(manager, "tmp:///m3fs-vfs-image-probe/payload.txt");
            for (String scheme : Set.of("zip", "jar", "tar", "tgz", "tbz2")) {
                read(manager, scheme + ":" + fixtures.resolve("payload." + scheme).toUri() + "!/payload.txt");
            }
            read(manager, "gz:" + fixtures.resolve("payload.txt.gz").toUri());
            read(manager, "bz2:" + fixtures.resolve("payload.txt.bz2").toUri());
            // res resolves the module resource to its real jrt: URL and exercises the generic URL provider.
            try (FileObject notice = manager.resolveFile("res:META-INF/m3fs-legal/commons-vfs2/NOTICE.txt");
                 InputStream input = notice.getContent().getInputStream()) {
                require(new String(input.readAllBytes(), StandardCharsets.UTF_8).contains("Apache Commons VFS"),
                        "attributed module resource unavailable through res/URL");
            }
            try (var files = MFiles.from(fixtures.resolve("payload.txt")).stream()) {
                MFile file = files.findFirst().orElseThrow();
                require(file.isFile(), "facade local file kind");
                MFileObservation before = observedContent(file);
                MFileObservation after = observedContent(file);
                AtomicInteger callbacks = new AtomicInteger();
                MFileDelta delta = MFileDelta.between(before, after, value -> {
                    require(value.content() == MFileDelta.State.SAME, "observed complete content equality");
                    callbacks.incrementAndGet();
                });
                require(callbacks.get() == 1 && delta.before() == before && delta.after() == after,
                        "validated delta callback and immutable observation references");
                require(delta.contentEvidence() == MFileDelta.ContentEvidence.FULL_CONTENT_ID,
                        "content equality evidence must identify full observed bytes");
            }
            selectedArchive(fixtures);
            contentDefaultsAndNioPath();
        } finally {
            M3VFS.close();
        }
        System.out.println("M3FS_VFS_IMAGE_PROBE=PASS");
        System.out.println("MODULE=jdk.m3fs");
        System.out.println("CONSUMER_MODULE=" + M3VfsImageProbe.class.getModule().getName());
        System.out.println("GENERIC_URL_NAMES=PASS");
        System.out.println("MONITORED_CONTENT=PASS");
        System.out.println("COMPOSED_CONTENT_CAPTURE=PASS");
        System.out.println("OBSERVATION_DELTA=PASS");
        System.out.println("SELECTED_ARCHIVE_LAYER=PASS");
        System.out.println("CONTENT_DEFAULTS_AND_NIO_PATH=PASS");
        System.out.println("DATABASE_PROVIDER_REGISTRATION=PASS");
        System.out.println("NATIVE_SQLITE_PROBE=NOT_RUN");
        System.out.println("SOURCE_BUILT_M3JDK21=NOT_RUN");
    }
}
