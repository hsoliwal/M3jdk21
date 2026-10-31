/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.fs;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.m3.fs.vfs.Capability;
import com.m3.fs.vfs.FileName;
import com.m3.fs.vfs.FileObject;
import com.m3.fs.vfs.FileType;
import com.m3.fs.vfs.impl.StandardFileSystemManager;
import com.m3.fs.vfs.provider.FileProvider;
import com.m3.fs.vfs.provider.UriParser;
import com.m3.fs.vfs.provider.zip.ZipFileSystem;

/**
 * Hierarchical metadata streams backed by the M3FS VFS providers.
 *
 * <pre>{@code
 * try (Stream<MFile> entries = MFiles.from(root)
 *         .filter(file -> file.isFile() && file.name().endsWith(".txt"))
 *         .descendInto(folder -> !folder.name().equals(".git"))
 *         .stream()) {
 *     entries.forEach(file -> System.out.println(file.relativeName()));
 * }
 * }</pre>
 *
 * <p>Always close the returned stream, including after short-circuit operations.
 * A terminal operation does not close it. Traversal defaults to depth first in
 * provider encounter order; breadth first is also available. A directory root
 * is excluded; a file root is emitted. Archive traversal requires an explicit
 * provider URI, for example {@code zip:file:///tmp/data.zip!/}, or an opt-in
 * {@link Plan#enterArchives(Predicate, int)} selection and layer budget.</p>
 *
 * <p>The facade requests metadata during traversal and opens payload streams only
 * through explicit content operations. Provider resolution, archive mounting,
 * and provider metadata methods can perform I/O according to their own contracts.
 * No filesystem snapshot, archive-record identity, or repeatable content is
 * implied. The original VFS {@code FileObject.close()} contract is unchanged.</p>
 */
public final class MFiles {
    private MFiles() {
    }

    /** Relative ordering of directory and mounted-layer descendants. */
    public enum TraversalOrder {
        /** Emit each entry and then its descendants before its siblings. */
        DEPTH_FIRST,
        /** Emit entries at a depth before entries at a greater depth. */
        BREADTH_FIRST
    }

    /** Creates an immutable plan without opening, resolving, or examining the path. */
    public static Plan from(Path root) {
        return new Plan(Objects.requireNonNull(root, "root"), null, entry -> true,
                name -> true, null, Integer.MAX_VALUE, TraversalOrder.DEPTH_FIRST, null, 0, Map.of());
    }

    /** Creates an immutable plan without resolving or validating a provider address. */
    public static Plan from(String providerAddress) {
        Objects.requireNonNull(providerAddress, "providerAddress");
        if (providerAddress.isEmpty()) {
            throw new IllegalArgumentException("providerAddress must not be empty");
        }
        return new Plan(null, providerAddress, entry -> true, name -> true, null,
                Integer.MAX_VALUE, TraversalOrder.DEPTH_FIRST, null, 0, Map.of());
    }

    /** Opens an owned stream for a local path. */
    public static Stream<MFile> stream(Path root) throws IOException {
        return from(root).stream();
    }

    /** Opens an owned stream for a provider address. */
    public static Stream<MFile> stream(String providerAddress) throws IOException {
        return from(providerAddress).stream();
    }

    /** Opens an owned stream, applies an output filter, and transforms each match. */
    public static <T> Stream<T> stream(Path root, Predicate<? super MFile> filter,
            Function<? super MFile, ? extends T> transform) throws IOException {
        Objects.requireNonNull(transform, "transform");
        return from(root).filter(filter).stream().map(transform);
    }

    /** Opens an owned provider stream, filters metadata, and transforms each match. */
    public static <T> Stream<T> stream(String providerAddress, Predicate<? super MFile> filter,
            Function<? super MFile, ? extends T> transform) throws IOException {
        Objects.requireNonNull(transform, "transform");
        return from(providerAddress).filter(filter).stream().map(transform);
    }

    /**
     * Computes a detached result while the selected entry's provider session is
     * owned by the current worker. A task may open content explicitly; this
     * interface does not require one read or full consumption. Close content
     * wrappers inside the callback so their completion observers run there.
     * Do not retain live entries, content streams, or provider objects for use
     * on another thread. Captured task and plan callback state must support
     * concurrent calls from independent sessions.
     *
     * @param <R> the detached result type, which may include null values
     */
    @FunctionalInterface
    public interface FileTask<R> {
        /** Computes one selected entry's result on its owning worker. */
        R apply(MFile file) throws IOException;
    }

    /**
     * Lazily maps selected entries using a borrowed executor and completion-order
     * results. No roots are iterated or providers opened until result demand.
     * The returned stream is sequential and does not split. Always close it,
     * including after short-circuit operations; closing never shuts down the
     * executor. Inline/caller-runs execution is rejected because it can block
     * the only result consumer.
     *
     * <p>At most {@code maxInFlight} work units are admitted, and at most that
     * many callbacks plus unconsumed results hold result credits. Each unit
     * opens and closes its own provider session on one worker. Local Path
     * subtrees may be reopened as independent units without sharing live VFS
     * objects. Archive layers and provider-address roots remain single-owner
     * regions; distinct roots can overlap. Provider factories are serialized
     * within this operation and must supply fresh instances. Provider child
     * arrays, archive indexes/caches/spooling, and task-owned memory are outside
     * the operation's O(maxInFlight) admission/result storage bound. Providers
     * may physically reread a container when opening archive entries.</p>
     *
     * <p>Checked provider/task failures become {@link UncheckedIOException};
     * runtime failures retain their identity. Failure and close interrupt active
     * workers, cancel queued units, and wait for actual worker cleanup. Tasks
     * and providers must cooperate with cancellation for this wait to finish.
     * Successful terminal exhaustion also waits for all sessions to close.
     * Individual results can precede a later session-close failure; commit a
     * complete generation only after exhaustion and successful stream close.
     * Closing or consuming this result stream from its own task is unsupported.
     *</p>
     *
     * <p>No thread-local progress scope is captured. Create a borrowed per-file
     * child on its worker and adapt it through {@link M3FsProgress#of}; use a
     * parent supporting concurrent callbacks or serialize those callbacks.
     * The task owns child completion. Plan encounter order governs each owned
     * region's enumeration, but global results follow callback completion,
     * including successful callback-content cleanup.</p>
     */
    public static <R> Stream<R> mapParallel(Iterable<? extends Plan> roots,
            ExecutorService executor, int maxInFlight, FileTask<? extends R> task) {
        Objects.requireNonNull(roots, "roots");
        Objects.requireNonNull(executor, "executor");
        Objects.requireNonNull(task, "task");
        if (maxInFlight < 1) {
            throw new IllegalArgumentException("maxInFlight must be positive");
        }
        Parallel<R> parallel = new Parallel<>(roots, executor, maxInFlight, task);
        return StreamSupport.stream(parallel, false).onClose(parallel::close);
    }

    /**
     * An immutable reusable traversal description. Its methods perform no provider
     * I/O. Every {@link #stream()} invocation owns a new manager and new provider
     * session. Captured predicates and provider suppliers are caller-owned objects;
     * their own mutable state is not copied.
     */
    public static final class Plan {
        private final Path path;
        private final String address;
        private final Predicate<? super MFile> include;
        private final Predicate<String> includeName;
        private final Predicate<? super MFile> descend;
        private final int maxDepth;
        private final TraversalOrder order;
        private final Predicate<? super MFile> enterArchive;
        private final int maxArchiveLayers;
        private final Map<String, Supplier<? extends FileProvider>> providers;

        private Plan(Path path, String address, Predicate<? super MFile> include,
                Predicate<String> includeName, Predicate<? super MFile> descend, int maxDepth,
                TraversalOrder order, Predicate<? super MFile> enterArchive, int maxArchiveLayers,
                Map<String, Supplier<? extends FileProvider>> providers) {
            this.path = path;
            this.address = address;
            this.include = include;
            this.includeName = includeName;
            this.descend = descend;
            this.maxDepth = maxDepth;
            this.order = order;
            this.enterArchive = enterArchive;
            this.maxArchiveLayers = maxArchiveLayers;
            this.providers = Collections.unmodifiableMap(new LinkedHashMap<>(providers));
        }

        /**
         * Adds an output predicate using logical AND. Rejected directories are
         * still traversed unless a descent predicate or depth boundary prunes them.
         */
        public Plan filter(Predicate<? super MFile> predicate) {
            Objects.requireNonNull(predicate, "predicate");
            return new Plan(path, address, entry -> include.test(entry) && predicate.test(entry),
                    includeName, descend, maxDepth, order, enterArchive, maxArchiveLayers, providers);
        }

        /**
         * Adds a cheap output filter over decoded base names using logical AND.
         * It runs before full metadata and size projection. A rejected name does
         * not prune descendants. An explicit directory or archive-selection
         * predicate can still require metadata for a rejected traversal boundary.
         */
        public Plan filterName(Predicate<String> predicate) {
            Objects.requireNonNull(predicate, "predicate");
            return new Plan(path, address, include, name -> includeName.test(name) && predicate.test(name),
                    descend, maxDepth, order, enterArchive, maxArchiveLayers, providers);
        }

        /**
         * Adds a case-sensitive name suffix filter. Each suffix accepts an optional
         * leading dot: {@code extensions(".txt", "json", "bin")}. Matching uses
         * the end of the decoded base name; {@code file.txt.bak} does not match
         * {@code txt}. The caller's array is copied. No suffixes selects no names.
         */
        public Plan extensions(String... extensions) {
            Objects.requireNonNull(extensions, "extensions");
            List<String> suffixes = new ArrayList<>(extensions.length);
            for (String extension : extensions) {
                Objects.requireNonNull(extension, "extension");
                String suffix = extension.startsWith(".") ? extension : "." + extension;
                if (suffix.length() < 2 || suffix.indexOf('/') >= 0 || suffix.indexOf('\\') >= 0) {
                    throw new IllegalArgumentException("extension must be a nonempty name suffix");
                }
                suffixes.add(suffix);
            }
            List<String> copied = List.copyOf(suffixes);
            return filterName(name -> {
                for (String suffix : copied) {
                    if (name.endsWith(suffix)) {
                        return true;
                    }
                }
                return false;
            });
        }

        /**
         * Adds a directory descent predicate using logical AND. It controls
         * children, not whether the directory itself is emitted. It applies to
         * the selected directory root too, whose relative name is empty. Symlinks
         * and directories at the depth boundary are never descended into.
         */
        public Plan descendInto(Predicate<? super MFile> predicate) {
            Objects.requireNonNull(predicate, "predicate");
            Predicate<? super MFile> combined = descend == null ? predicate
                    : entry -> descend.test(entry) && predicate.test(entry);
            return new Plan(path, address, include, includeName, combined, maxDepth,
                    order, enterArchive, maxArchiveLayers, providers);
        }

        /**
         * Sets the maximum emitted depth. The selected root is depth zero:
         * zero emits only a file root, one emits a directory's immediate children.
         * Negative depths are rejected. The default is {@link Integer#MAX_VALUE}.
         */
        public Plan maxDepth(int depth) {
            if (depth < 0) {
                throw new IllegalArgumentException("maxDepth must not be negative");
            }
            return new Plan(path, address, include, includeName, descend, depth,
                    order, enterArchive, maxArchiveLayers, providers);
        }

        /** Selects traversal order while preserving the provider's sibling order. */
        public Plan order(TraversalOrder traversalOrder) {
            return new Plan(path, address, include, includeName, descend, maxDepth,
                    Objects.requireNonNull(traversalOrder, "traversalOrder"), enterArchive, maxArchiveLayers, providers);
        }

        /** Selects breadth-first traversal. Its pending frontier can grow with tree width. */
        public Plan breadthFirst() {
            return order(TraversalOrder.BREADTH_FIRST);
        }

        /**
         * Opts into provider-supported archive layers selected independently of
         * output filters. The archive file is emitted before mounting is attempted
         * on later advancement. Mounting uses the manager's existing layered
         * provider SPI and can read or replicate archive content.
         *
         * <p>The budget counts automatic mounts below the selected root, including
         * a file root. Zero disables automatic mounting; negative budgets fail.
         * Existing layers in an explicit root URI do not consume this budget.
         * Archive contents add one global depth beneath their archive entry.
         * Directory layer roots are excluded but receive the descent predicate;
         * their relative names end in {@code !/}. Descendants use names such as
         * {@code outer.zip!/inner.zip!/file.txt}. Symlinks are never mounted.
         * Calling this method again replaces the previous selector and budget.</p>
         */
        public Plan enterArchives(Predicate<? super MFile> selector, int layers) {
            Objects.requireNonNull(selector, "selector");
            if (layers < 0) {
                throw new IllegalArgumentException("maxArchiveLayers must not be negative");
            }
            return new Plan(path, address, include, includeName, descend, maxDepth,
                    order, selector, layers, providers);
        }

        /**
         * Registers an additional VFS provider factory for each new session.
         * The supplier is invoked when a sequential or parallel session opens and
         * must return a fresh provider instance owned by that session. Built-in
         * scheme collisions are reported when the manager is opened. Replacing
         * a registration already present on this plan is rejected immediately.
         *
         * @param scheme a lower-case URI scheme
         * @param providerFactory a fresh provider factory
         * @return a new plan carrying the registration
         */
        public Plan withProvider(String scheme, Supplier<? extends FileProvider> providerFactory) {
            Objects.requireNonNull(scheme, "scheme");
            Objects.requireNonNull(providerFactory, "providerFactory");
            if (!scheme.matches("[a-z][a-z0-9+.-]*")) {
                throw new IllegalArgumentException("scheme must be a lower-case URI scheme");
            }
            if (providers.containsKey(scheme)) {
                throw new IllegalArgumentException("scheme is already registered on this plan");
            }
            Map<String, Supplier<? extends FileProvider>> updated = new LinkedHashMap<>(providers);
            updated.put(scheme, providerFactory);
            return new Plan(path, address, include, includeName, descend, maxDepth,
                    order, enterArchive, maxArchiveLayers, updated);
        }

        /** Maps this plan with the bounded ownership contract of {@link MFiles#mapParallel}. */
        public <R> Stream<R> mapParallel(ExecutorService executor, int maxInFlight,
                FileTask<? extends R> task) {
            return MFiles.mapParallel(List.of(this), executor, maxInFlight, task);
        }

        /**
         * Opens a sequential stream with its own VFS manager. Enumeration is
         * lazy after root resolution. The spliterator does not split; traversal
         * and callbacks should be used sequentially.
         * A directory is listed only when traversal reaches its children. The VFS
         * provider API returns a complete array for that directory; this facade
         * keeps one cursor per active array instead of materializing the tree or
         * allocating pending tasks for all siblings. Depth-first memory follows
         * active directory arrays; breadth-first also retains its pending frontier.
         *
         * <p>Opening and initial root-type errors are checked {@link IOException}s.
         * Later metadata projection and traversal errors become {@link UncheckedIOException}.
         * A traversal failure, or a callback failure during source advancement,
         * closes the session and suppresses cleanup failures onto the original error.
         * Failures in later buffered stages, such as a sorted comparator, require
         * the caller to close the stream; always use try-with-resources.
         * Normal exhaustion leaves the session
         * open so collected entries can be read until the stream is closed.</p>
         *
         * @return the resource-owning stream, which the caller must close
         * @throws IOException if initialization, resolution, or root metadata fail
         */
        public Stream<MFile> stream() throws IOException {
            Session session = new Session(new StandardFileSystemManager());
            try {
                session.manager.init();
                for (Map.Entry<String, Supplier<? extends FileProvider>> registration : providers.entrySet()) {
                    if (session.manager.hasProvider(registration.getKey())) {
                        throw new IOException("Provider scheme is already registered: " + registration.getKey());
                    }
                    FileProvider provider = Objects.requireNonNull(registration.getValue().get(),
                            "providerFactory returned null");
                    session.manager.addProvider(registration.getKey(), provider);
                }
                FileObject root = path == null ? session.manager.resolveFile(address)
                        : session.manager.toFileObject(path.toAbsolutePath().toFile());
                EntryMetadata rootMetadata = session.inspect(root);
                if (!rootMetadata.file() && !rootMetadata.directory() && !rootMetadata.link()) {
                    throw new IOException("The selected MFiles root does not exist");
                }
                Traversal traversal = new Traversal(session, this, root, rootMetadata);
                return StreamSupport.stream(traversal, false).onClose(traversal::close);
            } catch (IOException | RuntimeException | Error failure) {
                session.closeAfterFailure(failure);
                throw failure;
            }
        }
    }

    private record EntryMetadata(FileName fileName, String name, boolean file, boolean directory, boolean link) {
    }

    private record Layer(FileName rootName, String prefix, int mounts) {
    }

    private interface Task {
    }

    private record Node(FileObject file, int depth, boolean root, Layer layer, EntryMetadata metadata) implements Task {
    }

    private record Archive(Node node, EntryMetadata metadata, MFile projected) implements Task {
    }

    @FunctionalInterface
    private interface Fork {
        boolean offer(Node node);
    }

    /** One provider child array and one index; it never allocates pending nodes for all children. */
    private static final class ChildCursor implements Task {
        private final FileObject directory;
        private final int depth;
        private final Layer layer;
        private FileObject[] children;
        private int nextIndex;
        private boolean loaded;

        ChildCursor(FileObject directory, int depth, Layer layer) {
            this.directory = directory;
            this.depth = depth;
            this.layer = layer;
        }

        FileObject next() throws IOException {
            if (!loaded) {
                children = directory.getChildren();
                loaded = true;
            }
            if (nextIndex == children.length) {
                children = null;
                return null;
            }
            FileObject child = Objects.requireNonNull(children[nextIndex++], "Provider returned a null child");
            if (nextIndex == children.length) {
                children = null;
            }
            return child;
        }

        boolean hasRemaining() {
            return children != null;
        }
    }

    private static final class Traversal extends Spliterators.AbstractSpliterator<MFile> {
        private final Session session;
        private final Plan plan;
        private final Node initial;
        private final Fork fork;
        private final Runnable checkpoint;
        private final Consumer<Throwable> failureObserver;
        private final Deque<Task> tasks = new ArrayDeque<>();

        Traversal(Session session, Plan plan, FileObject root, EntryMetadata rootMetadata) {
            this(session, plan, new Node(root, 0, true, new Layer(root.getName(), "", 0), rootMetadata),
                    null, null, null);
        }

        Traversal(Session session, Plan plan, Node initial, Fork fork, Runnable checkpoint,
                Consumer<Throwable> failureObserver) {
            super(Long.MAX_VALUE, Spliterator.ORDERED | Spliterator.NONNULL);
            this.session = session;
            this.plan = plan;
            this.initial = initial;
            this.fork = fork;
            this.checkpoint = checkpoint;
            this.failureObserver = failureObserver;
            tasks.addFirst(initial);
        }

        @Override
        public Spliterator<MFile> trySplit() {
            return null;
        }

        private void schedule(Task task) {
            if (plan.order == TraversalOrder.DEPTH_FIRST) {
                tasks.addFirst(task);
            } else {
                tasks.addLast(task);
            }
        }

        private void mount(Archive archive) throws IOException {
            Node node = archive.node();
            MFile entry = archive.projected() == null
                    ? session.project(node.file(), node.root(), node.layer(), archive.metadata()) : archive.projected();
            if (!plan.enterArchive.test(entry) || !session.manager.canCreateFileSystem(node.file())) {
                return;
            }
            FileObject mounted = session.manager.createFileSystem(node.file());
            EntryMetadata metadata = session.inspect(mounted);
            Layer layer = new Layer(mounted.getName(), entry.relativeName() + "!/", node.layer().mounts() + 1);
            // A directory layer root is an invisible container at the archive entry's depth.
            // A single-content layer root is itself a child of the archive entry.
            int depth = metadata.directory() ? node.depth() : node.depth() + 1;
            tasks.addFirst(new Node(mounted, depth, true, layer, metadata));
        }

        @Override
        public boolean tryAdvance(Consumer<? super MFile> action) {
            Objects.requireNonNull(action, "action");
            try {
                session.requireTraversalOpen();
                while (!tasks.isEmpty()) {
                    if (checkpoint != null) {
                        checkpoint.run();
                    }
                    Task task = tasks.removeFirst();
                    if (task instanceof ChildCursor cursor) {
                        FileObject child = cursor.next();
                        if (cursor.hasRemaining()) {
                            tasks.addFirst(cursor);
                        }
                        if (child != null) {
                            tasks.addFirst(new Node(child, cursor.depth, false, cursor.layer, null));
                        }
                        continue;
                    }
                    if (task instanceof Archive archive) {
                        mount(archive);
                        continue;
                    }
                    Node node = (Node) task;
                    if (fork != null && node != initial && node.layer().mounts() == 0 && fork.offer(node)) {
                        continue;
                    }
                    EntryMetadata metadata = node.metadata() == null ? session.inspect(node.file()) : node.metadata();
                    boolean outputCandidate = !(node.root() && metadata.directory())
                            && plan.includeName.test(metadata.name());
                    boolean canDescend = metadata.directory() && !metadata.link() && node.depth() < plan.maxDepth;
                    MFile entry = outputCandidate || canDescend && plan.descend != null
                            ? session.project(node.file(), node.root(), node.layer(), metadata) : null;
                    if (canDescend && (plan.descend == null || plan.descend.test(entry))) {
                        ChildCursor cursor = new ChildCursor(node.file(), node.depth() + 1, node.layer());
                        if (node.root()) {
                            // Preserve this expansion's position within a breadth-first frontier.
                            tasks.addFirst(cursor);
                        } else {
                            schedule(cursor);
                        }
                    }
                    if (metadata.file() && !metadata.link() && node.depth() < plan.maxDepth
                            && plan.enterArchive != null && node.layer().mounts() < plan.maxArchiveLayers) {
                        schedule(new Archive(node, metadata, entry));
                    }
                    if (outputCandidate && plan.include.test(entry)) {
                        action.accept(entry);
                        return true;
                    }
                }
                return false;
            } catch (IOException failure) {
                tasks.clear();
                UncheckedIOException reported = new UncheckedIOException("MFiles provider traversal failed", failure);
                if (failureObserver != null) {
                    failureObserver.accept(reported);
                }
                session.closeAfterFailure(failure);
                throw reported;
            } catch (RuntimeException | Error failure) {
                tasks.clear();
                if (failureObserver != null) {
                    failureObserver.accept(failure);
                }
                session.closeAfterFailure(failure);
                throw failure;
            }
        }

        private void close() {
            tasks.clear();
            session.closeUnchecked();
        }
    }

    /** Only detached coordinates cross workers; each worker resolves its own VFS objects. */
    private record Location(String address, String relativeRootAddress, String prefix,
            int mounts, int depth, boolean root) {
        static Location of(Node node) {
            return new Location(node.file().getName().getURI(), node.layer().rootName().getURI(),
                    node.layer().prefix(), node.layer().mounts(), node.depth(), node.root());
        }
    }

    private static final class Parallel<R> extends Spliterators.AbstractSpliterator<R> {
        private final Iterable<? extends Plan> roots;
        private final ExecutorService executor;
        private final FileTask<? extends R> task;
        private final Object[] workers;
        private final Object[] results;
        private final Object factoryLock = new Object();
        private final Set<FileProvider> activeProviders = Collections.newSetFromMap(new IdentityHashMap<>());
        private Iterator<? extends Plan> rootIterator;
        private int active;
        private int credits;
        private int resultCount;
        private int head;
        private int tail;
        private boolean rootsExhausted;
        private boolean stopped;
        private boolean closed;
        private boolean failureReported;
        private Throwable failure;

        Parallel(Iterable<? extends Plan> roots, ExecutorService executor, int maximum,
                FileTask<? extends R> task) {
            super(Long.MAX_VALUE, Spliterator.ORDERED);
            this.roots = roots;
            this.executor = executor;
            this.task = task;
            workers = new Object[maximum];
            results = new Object[maximum];
        }

        @Override
        public Spliterator<R> trySplit() {
            return null;
        }

        @SuppressWarnings("unchecked")
        private Work worker(Object value) {
            return (Work) value;
        }

        private synchronized void requireRunning() {
            if (stopped || Thread.currentThread().isInterrupted()) {
                throw new CancellationException("MFiles parallel operation was cancelled");
            }
        }

        private synchronized void requireConsumer() {
            for (Object value : workers) {
                if (value != null && worker(value).owner == Thread.currentThread()) {
                    throw new IllegalStateException("An MFiles task cannot consume or close its own result stream");
                }
            }
        }

        private synchronized Work reserve(Plan plan, Location location) {
            if (stopped || active == workers.length) {
                return null;
            }
            for (int index = 0; index < workers.length; index++) {
                if (workers[index] == null) {
                    Work work = new Work(index, plan, location);
                    workers[index] = work;
                    active++;
                    return work;
                }
            }
            throw new IllegalStateException("MFiles parallel admission accounting failed");
        }

        private void dispatch(Work work) {
            work.submitter = Thread.currentThread();
            work.submitting = true;
            try {
                executor.execute(work);
            } catch (RuntimeException | Error rejected) {
                stop(rejected);
                throw rejected;
            } finally {
                work.submitting = false;
            }
        }

        /** The consumer alone advances roots, reserving a bounded batch before dispatch. */
        private void fillRoots() {
            synchronized (this) {
                if (stopped || rootsExhausted || active == workers.length) {
                    return;
                }
            }
            if (rootIterator == null) {
                rootIterator = Objects.requireNonNull(roots.iterator(), "roots.iterator()");
            }
            List<Work> admitted = new ArrayList<>();
            while (true) {
                synchronized (this) {
                    if (stopped || rootsExhausted || active == workers.length) {
                        break;
                    }
                }
                if (!rootIterator.hasNext()) {
                    synchronized (this) {
                        rootsExhausted = true;
                        notifyAll();
                    }
                    break;
                }
                Plan plan = Objects.requireNonNull(rootIterator.next(), "root plan");
                Work work = reserve(plan, null);
                if (work == null) {
                    break;
                }
                admitted.add(work);
            }
            for (Work work : admitted) {
                dispatch(work);
            }
        }

        private boolean fork(Plan plan, Node node) {
            Work work;
            synchronized (this) {
                // Explicit roots get admission priority; no unbounded pending subtree queue.
                if (!rootsExhausted || stopped || active == workers.length) {
                    return false;
                }
                work = reserve(plan, Location.of(node));
            }
            if (work == null) {
                return false;
            }
            dispatch(work);
            return true;
        }

        private synchronized void reserveResult() {
            while (credits == results.length && !stopped) {
                waitForChange();
            }
            requireRunning();
            credits++;
        }

        private synchronized void releaseResult() {
            credits--;
            notifyAll();
        }

        private synchronized void publish(R result) {
            requireRunning();
            results[tail] = result;
            tail = (tail + 1) % results.length;
            resultCount++;
            notifyAll();
        }

        private void apply(Session session, MFile file) {
            reserveResult();
            boolean published = false;
            try {
                R result = task.apply(file);
                closeCallbackContent(session);
                publish(result);
                published = true;
            } catch (IOException checked) {
                throw new UncheckedIOException("MFiles parallel task failed", checked);
            } finally {
                if (!published) {
                    releaseResult();
                }
            }
        }

        /** Close forgotten raw content on its owning worker before exposing a result. */
        private static void closeCallbackContent(Session session) throws IOException {
            List<Closeable> outstanding;
            synchronized (session) {
                outstanding = new ArrayList<>(session.content);
                session.content.clear();
            }
            Throwable problem = null;
            for (Closeable stream : outstanding) {
                try {
                    stream.close();
                } catch (IOException | RuntimeException | Error cleanup) {
                    problem = Session.accumulate(problem, cleanup);
                }
            }
            if (problem instanceof IOException checked) {
                throw checked;
            }
            if (problem != null) {
                throw unchecked(problem);
            }
        }

        /** Caller holds this monitor. Interruption remains visible after cleanup. */
        private void waitForChange() {
            try {
                wait();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                CancellationException cancelled = new CancellationException("MFiles parallel wait was interrupted");
                cancelled.initCause(interrupted);
                throw cancelled;
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super R> action) {
            Objects.requireNonNull(action, "action");
            synchronized (this) {
                if (closed) {
                    throw new IllegalStateException("The MFiles parallel result stream is closed");
                }
            }
            boolean deliveringFailure = false;
            try {
                requireConsumer();
                while (true) {
                    fillRoots();
                    R result;
                    synchronized (this) {
                        while (resultCount == 0 && failure == null && !stopped && active != 0) {
                            if (!rootsExhausted && active < workers.length) {
                                break;
                            }
                            waitForChange();
                        }
                        if (failure != null) {
                            deliveringFailure = true;
                            throw unchecked(failure);
                        }
                        if (stopped) {
                            throw new CancellationException("MFiles parallel result stream was closed");
                        }
                        if (resultCount == 0) {
                            if (rootsExhausted && active == 0) {
                                return false;
                            }
                            continue;
                        }
                        @SuppressWarnings("unchecked")
                        R value = (R) results[head];
                        result = value;
                        results[head] = null;
                        head = (head + 1) % results.length;
                        resultCount--;
                        credits--;
                        notifyAll();
                    }
                    action.accept(result);
                    return true;
                }
            } catch (RuntimeException | Error problem) {
                // An already recorded failure may acquire an unchecked delivery wrapper.
                stop(deliveringFailure ? null : problem);
                awaitWorkers();
                synchronized (this) {
                    if (failure != null) {
                        failureReported = true;
                        throw unchecked(failure);
                    }
                }
                throw problem;
            }
        }

        private static RuntimeException unchecked(Throwable problem) {
            if (problem instanceof IOException checked) {
                return new UncheckedIOException("MFiles parallel provider failed", checked);
            }
            if (problem instanceof RuntimeException unchecked) {
                return unchecked;
            }
            if (problem instanceof Error error) {
                throw error;
            }
            return new IllegalStateException("Unexpected MFiles parallel failure", problem);
        }

        private void stop(Throwable problem) {
            Object[] outstanding;
            synchronized (this) {
                if (problem != null) {
                    if (stopped && problem instanceof CancellationException) {
                        // The stop signal is expected; real cleanup failures attached to it are not.
                        for (Throwable cleanup : problem.getSuppressed()) {
                            recordFailure(cleanup);
                        }
                    } else {
                        recordFailure(problem);
                    }
                }
                stopped = true;
                for (int index = 0; index < results.length; index++) {
                    results[index] = null;
                }
                credits -= resultCount;
                resultCount = 0;
                head = 0;
                tail = 0;
                outstanding = workers.clone();
                notifyAll();
            }
            for (Object value : outstanding) {
                if (value != null) {
                    worker(value).cancel();
                }
            }
        }

        /** Caller holds this monitor. Repeated reporting must not duplicate cleanup evidence. */
        private void recordFailure(Throwable problem) {
            if (failure == null) {
                failure = problem;
            } else if (problem != failure) {
                // Causal relation is not identity; distinct cleanup errors retain their evidence.
                for (Throwable recorded : failure.getSuppressed()) {
                    if (recorded == problem) {
                        return;
                    }
                }
                failure.addSuppressed(problem);
            }
        }

        private void awaitWorkers() {
            boolean interrupted = false;
            synchronized (this) {
                requireConsumer();
                while (active != 0) {
                    try {
                        wait();
                    } catch (InterruptedException interruption) {
                        interrupted = true;
                    }
                }
            }
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }

        private void close() {
            synchronized (this) {
                closed = true;
            }
            stop(null);
            awaitWorkers();
            synchronized (this) {
                if (failure != null && !failureReported) {
                    failureReported = true;
                    throw unchecked(failure);
                }
            }
        }

        /** Future cancellation is only a signal; state 2 is actual cleanup completion. */
        private final class Work implements Runnable {
            private final int slot;
            private final Plan plan;
            private final Location location;
            private final FutureTask<Void> future;
            private final List<FileProvider> claimedProviders = new ArrayList<>();
            private Thread submitter;
            private boolean submitting;
            private Thread owner;
            private int state;

            Work(int slot, Plan plan, Location location) {
                this.slot = slot;
                this.plan = plan;
                this.location = location;
                future = new FutureTask<>(this::execute, null);
            }

            @Override
            public void run() {
                synchronized (Parallel.this) {
                    if (state != 0) {
                        return;
                    }
                    state = 1;
                    owner = Thread.currentThread();
                }
                try {
                    if (owner == submitter && submitting) {
                        stop(new RejectedExecutionException("MFiles parallel tasks require asynchronous execution"));
                    } else {
                        future.run();
                    }
                } finally {
                    synchronized (Parallel.this) {
                        state = 2;
                        owner = null;
                        workers[slot] = null;
                        active--;
                        Parallel.this.notifyAll();
                    }
                }
            }

            void cancel() {
                Thread executing;
                synchronized (Parallel.this) {
                    if (state == 2) {
                        return;
                    }
                    executing = owner;
                    if (state == 0) {
                        state = 2;
                        workers[slot] = null;
                        active--;
                        Parallel.this.notifyAll();
                    }
                }
                future.cancel(executing != null && executing != Thread.currentThread());
            }

            private void providers(Session session) throws IOException {
                session.manager.init();
                for (Map.Entry<String, Supplier<? extends FileProvider>> registration : plan.providers.entrySet()) {
                    requireRunning();
                    if (session.manager.hasProvider(registration.getKey())) {
                        throw new IOException("Provider scheme is already registered: " + registration.getKey());
                    }
                    FileProvider provider;
                    synchronized (factoryLock) {
                        requireRunning();
                        provider = Objects.requireNonNull(registration.getValue().get(), "providerFactory result");
                        if (!activeProviders.add(provider)) {
                            throw new IOException("Provider factory must return a fresh provider for each session");
                        }
                        claimedProviders.add(provider);
                    }
                    session.manager.addProvider(registration.getKey(), provider);
                }
            }

            private void execute() {
                Session session = null;
                Throwable problem = null;
                try {
                    requireRunning();
                    session = new Session(new StandardFileSystemManager());
                    providers(session);
                    requireRunning();
                    FileObject root = location != null ? session.manager.resolveFile(location.address())
                            : plan.path == null ? session.manager.resolveFile(plan.address)
                            : session.manager.toFileObject(plan.path.toAbsolutePath().toFile());
                    EntryMetadata metadata = session.inspect(root);
                    if (!metadata.file() && !metadata.directory() && !metadata.link()) {
                        throw new IOException("MFiles parallel root does not exist or has no supported type");
                    }
                    Layer layer = location == null ? new Layer(root.getName(), "", 0)
                            : new Layer(session.manager.resolveFile(location.relativeRootAddress()).getName(),
                                    location.prefix(), location.mounts());
                    Node initial = new Node(root, location == null ? 0 : location.depth(),
                            location == null || location.root(), layer, metadata);
                    Session owned = session;
                    Traversal traversal = new Traversal(session, plan, initial,
                            plan.path == null ? null : node -> fork(plan, node),
                            Parallel.this::requireRunning, Parallel.this::stop);
                    while (traversal.tryAdvance(file -> apply(owned, file))) {
                        requireRunning();
                    }
                } catch (IOException | RuntimeException | Error body) {
                    problem = body;
                    stop(body);
                } finally {
                    if (session != null) {
                        try {
                            session.close();
                        } catch (IOException | RuntimeException | Error cleanup) {
                            problem = Session.accumulate(problem, cleanup);
                        }
                    }
                    synchronized (factoryLock) {
                        for (FileProvider provider : claimedProviders) {
                            activeProviders.remove(provider);
                        }
                    }
                }
                if (problem != null) {
                    stop(problem);
                }
            }
        }
    }

    static final class Session {
        private final StandardFileSystemManager manager;
        private final Set<Closeable> content = Collections.newSetFromMap(new IdentityHashMap<>());
        private volatile boolean closed;

        Session(StandardFileSystemManager manager) {
            this.manager = manager;
        }

        EntryMetadata inspect(FileObject file) throws IOException {
            FileType type = file.getType();
            FileName fileName = file.getName();
            return new EntryMetadata(fileName, UriParser.decode(fileName.getBaseName()),
                    type.hasContent(), type.hasChildren(), file.isSymbolicLink());
        }

        MFile project(FileObject file, boolean root, Layer layer, EntryMetadata metadata) throws IOException {
            String relativeName = layer.prefix() + (root ? (metadata.directory() ? "" : metadata.name())
                    : UriParser.decode(layer.rootName().getRelativeName(metadata.fileName())));
            long size = metadata.file() ? file.getContent().getSize() : -1;
            URI uri;
            try {
                uri = file.getURI();
            } catch (IllegalArgumentException failure) {
                throw new IOException("Provider returned an invalid file URI", failure);
            }
            return new MFile(metadata.name(), relativeName, uri, metadata.fileName().getURI(),
                    metadata.file(), metadata.directory(), metadata.link(),
                    size < 0 ? OptionalLong.empty() : OptionalLong.of(size), file, this);
        }

        synchronized InputStream openInputStream(FileObject file, boolean isFile) throws IOException {
            requireContentOpen();
            if (!isFile) {
                throw new IOException("MFiles entry is not a readable file");
            }
            OwnedInputStream input = new OwnedInputStream(file.getContent().getInputStream(), this);
            content.add(input);
            return input;
        }

        synchronized OutputStream openOutputStream(FileObject file, boolean isFile, boolean append) throws IOException {
            requireContentOpen();
            if (!isFile) {
                throw new IOException("MFiles entry is not a writable content file");
            }
            OwnedOutputStream output = new OwnedOutputStream(file.getContent().getOutputStream(append), this);
            content.add(output);
            return output;
        }

        synchronized MFileObservation observe(FileObject file, String sourceAddress) throws IOException {
            requireContentOpen();
            file.refresh();
            FileType type = file.getType();
            boolean link = file.isSymbolicLink();
            int flags = MFileObservation.EXISTS_KNOWN;
            long size = 0;
            long modified = 0;
            if (type != FileType.IMAGINARY || link) {
                flags |= MFileObservation.EXISTS | MFileObservation.TYPE_KNOWN;
                if (type.hasContent()) {
                    flags |= MFileObservation.FILE;
                    long reportedSize = file.getContent().getSize();
                    if (reportedSize >= 0) {
                        flags |= MFileObservation.SIZE_KNOWN;
                        size = reportedSize;
                    }
                }
                if (type.hasChildren()) {
                    flags |= MFileObservation.DIRECTORY;
                }
                if (link) {
                    flags |= MFileObservation.SYMBOLIC_LINK;
                }
                if (type.hasAttributes() && file.getFileSystem().hasCapability(Capability.GET_LAST_MODIFIED)) {
                    long reportedModified = file.getContent().getLastModifiedTime();
                    // ZIP/JAR use ZipEntry.getTime(), whose -1 value means unknown.
                    // Other providers can report valid signed pre-epoch timestamps.
                    if (reportedModified != -1 || !(file.getFileSystem() instanceof ZipFileSystem)) {
                        flags |= MFileObservation.MODIFIED_KNOWN;
                        modified = reportedModified;
                    }
                }
            }
            MFileObservation.ChangeStamp stamp = file instanceof MFileChangeSource changes
                    ? changes.changeStamp() : MFileObservation.ChangeStamp.unavailable();
            return MFileObservation.metadata(sourceAddress, flags, size, modified, stamp);
        }

        private void requireContentOpen() throws IOException {
            if (closed) {
                throw new IOException("The owning MFiles stream is closed");
            }
        }

        private void requireTraversalOpen() {
            if (closed) {
                throw new IllegalStateException("The owning MFiles stream is closed");
            }
        }

        private synchronized void remove(Closeable stream) {
            content.remove(stream);
        }

        private void close() throws IOException {
            List<Closeable> outstanding;
            synchronized (this) {
                if (closed) {
                    return;
                }
                closed = true;
                outstanding = new ArrayList<>(content);
                content.clear();
            }
            Throwable failure = null;
            for (Closeable stream : outstanding) {
                try {
                    stream.close();
                } catch (IOException | RuntimeException | Error error) {
                    failure = accumulate(failure, error);
                }
            }
            try {
                manager.close();
            } catch (RuntimeException | Error error) {
                failure = accumulate(failure, error);
            }
            if (failure instanceof IOException checked) {
                throw checked;
            }
            if (failure instanceof RuntimeException unchecked) {
                throw unchecked;
            }
            if (failure instanceof Error error) {
                throw error;
            }
        }

        private static Throwable accumulate(Throwable previous, Throwable next) {
            if (previous == null) {
                return next;
            }
            if (previous != next) {
                previous.addSuppressed(next);
            }
            return previous;
        }

        private void closeUnchecked() {
            try {
                close();
            } catch (IOException failure) {
                throw new UncheckedIOException("Could not close MFiles content streams", failure);
            }
        }

        private void closeAfterFailure(Throwable original) {
            try {
                close();
            } catch (IOException | RuntimeException | Error failure) {
                if (failure != original) {
                    original.addSuppressed(failure);
                }
            }
        }
    }

    private static final class OwnedOutputStream extends OutputStream {
        private final OutputStream output;
        private final Session owner;
        private boolean closed;

        OwnedOutputStream(OutputStream output, Session owner) {
            this.output = output;
            this.owner = owner;
        }

        private void requireOpen() throws IOException {
            if (closed || owner.closed) {
                throw new IOException("The owning MFiles content stream is closed");
            }
        }

        @Override
        public synchronized void write(int value) throws IOException {
            requireOpen();
            output.write(value);
        }

        @Override
        public synchronized void write(byte[] bytes, int offset, int length) throws IOException {
            requireOpen();
            output.write(bytes, offset, length);
        }

        @Override
        public synchronized void flush() throws IOException {
            requireOpen();
            output.flush();
        }

        @Override
        public void close() throws IOException {
            try {
                synchronized (this) {
                    if (closed) {
                        return;
                    }
                    closed = true;
                    output.close();
                }
            } finally {
                owner.remove(this);
            }
        }
    }

    private static final class OwnedInputStream extends InputStream {
        private final InputStream input;
        private final Session owner;
        private boolean closed;
        private boolean closeFinished;
        private Throwable closeFailure;

        OwnedInputStream(InputStream input, Session owner) {
            this.input = input;
            this.owner = owner;
        }

        private void requireOpen() throws IOException {
            if (closed || owner.closed) {
                throw new IOException("The owning MFiles content stream is closed");
            }
        }

        @Override
        public synchronized int read() throws IOException {
            requireOpen();
            return input.read();
        }

        @Override
        public synchronized int read(byte[] bytes, int offset, int length) throws IOException {
            requireOpen();
            return input.read(bytes, offset, length);
        }

        @Override
        public synchronized long skip(long count) throws IOException {
            requireOpen();
            return input.skip(count);
        }

        @Override
        public synchronized int available() throws IOException {
            requireOpen();
            return input.available();
        }

        @Override
        public void close() throws IOException {
            try {
                synchronized (this) {
                    if (closed) {
                        // A monitoring wrapper may close after the session closed
                        // this reader. Failed primary cleanup must remain visible
                        // before that wrapper can publish completion evidence.
                        if (!closeFinished) throw new IOException("Primary MFiles input close is still in progress");
                        if (closeFailure instanceof IOException checked) throw checked;
                        if (closeFailure instanceof RuntimeException unchecked) throw unchecked;
                        if (closeFailure instanceof Error error) throw error;
                        return;
                    }
                    closed = true;
                    try {
                        input.close();
                    } catch (IOException | RuntimeException | Error failure) {
                        closeFailure = failure;
                        throw failure;
                    } finally {
                        closeFinished = true;
                    }
                }
            } finally {
                owner.remove(this);
            }
        }
    }
}
