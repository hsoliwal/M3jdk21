/* ******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.paradigm.context;

import jdk.internal.m3.collections.PlaceholderLogger;

import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import jdk.internal.m3.paradigm.annotation.ISpec;
import jdk.internal.m3.paradigm.annotation.Model;
import jdk.internal.m3.paradigm.annotation.Stability;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;


/**
 * IFileWatcher - File system change monitoring via Java NIO WatchService.
 *
 * <h2>Architecture</h2>
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │  FILE WATCHER PATTERN                                                   │
 * ├─────────────────────────────────────────────────────────────────────────┤
 * │                                                                         │
 * │  IFileWatcher (interface)                                               │
 * │    │                                                                    │
 * │    ├── watch(Path)           - Start watching directory                 │
 * │    ├── unwatch(Path)         - Stop watching directory                  │
 * │    ├── onCreated(Consumer)   - File created callback                    │
 * │    ├── onModified(Consumer)  - File modified callback                   │
 * │    ├── onDeleted(Consumer)   - File deleted callback                    │
 * │    ├── start()               - Start monitoring                         │
 * │    └── stop()                - Stop monitoring                          │
 * │                                                                         │
 * │  Implementation uses Java NIO WatchService:                             │
 * │    WatchService  - Native OS event-based monitor (inotify/FSEvents)     │
 * │    WatchKey      - Registration token for watched directory             │
 * │    WatchEvent    - Represents a single file system event                │
 * │                                                                         │
 * └─────────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * IFileWatcher watcher = IFileWatcher.create(Duration.ofSeconds(1))
 *     .watch(Path.of("/data/input"))
 *     .filter(f -> f.getName().endsWith(".csv"))
 *     .onCreated(file -> process(file))
 *     .onModified(file -> reprocess(file))
 *     .onDeleted(file -> cleanup(file))
 *     .start();
 *
 * // Later...
 * watcher.stop();
 * }</pre>
 *
 * <h2>Why Java NIO WatchService?</h2>
 * <ul>
 *   <li>Native: Uses OS-level file system events (inotify, FSEvents, etc.)</li>
 *   <li>Efficient: No polling overhead on supported platforms</li>
 *   <li>Pure Java 21: No external dependencies</li>
 *   <li>Virtual threads: Callbacks execute on JDK 21 virtual threads</li>
 * </ul>
 *
 * @since ContextOS 1.0
 * @see java.nio.file.WatchService
 */
@ISpec(
    value = "File system change monitoring via Java NIO WatchService - pure Java 21, no dependencies",
    constraints = {
        "Uses native OS events: inotify (Linux), FSEvents (macOS), ReadDirectoryChangesW (Windows)",
        "Thread-safe: can be used in concurrent environments",
        "Supports JDK 21 virtual threads for callbacks",
        "Pure Java 21 implementation, no external dependencies"
    },
    invariants = {
        "watch(path) returns this for fluent chaining",
        "start() begins monitoring, stop() ends it",
        "isRunning() reflects current monitoring state"
    },
    examples = {
        "IFileWatcher.create(Duration.ofSeconds(1))",
        "watcher.watch(path).onCreated(f -> process(f)).start()",
        "IFileWatcher.createVirtual()"
    },
    model = Model.ModelType.SONNET,
    stability = Stability.Level.STABLE
)
/**
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/os/IFileWatcher.java}
 * blob {@code 1caaab0047f44adf4f390b927bdaa1de176fcf94} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch 2 (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface IFileWatcher {

    /** File event types. */
    enum EventType {
        CREATED,
        MODIFIED,
        DELETED,
        DIRECTORY_CREATED,
        DIRECTORY_MODIFIED,
        DIRECTORY_DELETED
    }

    /**
     * File event record.
     *
     * @param type the event type
     * @param file the affected file
     */
    record FileEvent(EventType type, File file) {
        public Path path() {
            return file.toPath();
        }

        public boolean isDirectory() {
            return type == EventType.DIRECTORY_CREATED ||
                   type == EventType.DIRECTORY_MODIFIED ||
                   type == EventType.DIRECTORY_DELETED;
        }
    }

    // --- CONFIGURATION ---

    /**
     * Add a directory to watch.
     *
     * @param directory the directory path
     * @return this for fluent chaining
     */
    IFileWatcher watch(Path directory);

    /**
     * Remove a directory from watching.
     *
     * @param directory the directory path
     * @return this for fluent chaining
     */
    IFileWatcher unwatch(Path directory);

    /**
     * Set file filter for events.
     *
     * @param filter the file filter
     * @return this for fluent chaining
     */
    IFileWatcher filter(FileFilter filter);

    // --- CALLBACKS ---

    /**
     * Register callback for file creation.
     *
     * @param callback the callback
     * @return this for fluent chaining
     */
    IFileWatcher onCreated(Consumer<File> callback);

    /**
     * Register callback for file modification.
     *
     * @param callback the callback
     * @return this for fluent chaining
     */
    IFileWatcher onModified(Consumer<File> callback);

    /**
     * Register callback for file deletion.
     *
     * @param callback the callback
     * @return this for fluent chaining
     */
    IFileWatcher onDeleted(Consumer<File> callback);

    /**
     * Register callback for all events.
     *
     * @param callback the callback receiving FileEvent
     * @return this for fluent chaining
     */
    IFileWatcher onEvent(Consumer<FileEvent> callback);

    // --- LIFECYCLE ---

    /**
     * Start the file watcher.
     *
     * @return this for fluent chaining
     * @throws IOException if monitoring cannot be started
     */
    IFileWatcher start() throws IOException;

    /**
     * Stop the file watcher.
     *
     * @return this for fluent chaining
     */
    IFileWatcher stop();

    /**
     * Check if watcher is running.
     *
     * @return true if running
     */
    boolean isRunning();

    // --- FACTORY ---

    /**
     * Create a file watcher with default polling interval (5 seconds).
     *
     * @return new file watcher
     */
    static IFileWatcher create() {
        return create(Duration.ofSeconds(5));
    }

    /**
     * Create a file watcher with specified polling interval.
     *
     * @param interval polling interval for WatchService.poll()
     * @return new file watcher
     */
    static IFileWatcher create(Duration interval) {
        return new NioFileWatcher(interval, false);
    }

    /**
     * Create a file watcher using JDK 21 virtual threads.
     * Callbacks execute on virtual threads for better scalability.
     *
     * @return new file watcher with virtual threads
     */
    static IFileWatcher createVirtual() {
        return createVirtual(Duration.ofSeconds(5));
    }

    /**
     * Create a file watcher using JDK 21 virtual threads.
     *
     * @param interval polling interval for WatchService.poll()
     * @return new file watcher with virtual threads
     */
    static IFileWatcher createVirtual(Duration interval) {
        return new NioFileWatcher(interval, true);
    }

    // --- IMPLEMENTATION ---

    /** Java NIO WatchService based implementation with optional virtual threads. */
    class NioFileWatcher implements IFileWatcher {
        private static final PlaceholderLogger log = PlaceholderLogger.of(NioFileWatcher.class);

        private final Duration pollInterval;
        private final boolean useVirtualThreads;
        private final Map<Path, WatchKey> watchKeys;
        private final List<Consumer<File>> onCreatedCallbacks;
        private final List<Consumer<File>> onModifiedCallbacks;
        private final List<Consumer<File>> onDeletedCallbacks;
        private final List<Consumer<FileEvent>> onEventCallbacks;
        private final ExecutorService callbackExecutor;
        private WatchService watchService;
        private Thread watchThread;
        private FileFilter fileFilter;
        private volatile boolean running;

        public NioFileWatcher(Duration interval, boolean useVirtualThreads) {
            this.pollInterval = interval;
            this.useVirtualThreads = useVirtualThreads;
            this.watchKeys = new ConcurrentHashMap<>();
            this.onCreatedCallbacks = new CopyOnWriteArrayList<>();
            this.onModifiedCallbacks = new CopyOnWriteArrayList<>();
            this.onDeletedCallbacks = new CopyOnWriteArrayList<>();
            this.onEventCallbacks = new CopyOnWriteArrayList<>();
            this.running = false;

            // JDK 21: Virtual threads for callbacks
            this.callbackExecutor = useVirtualThreads
                    ? Executors.newVirtualThreadPerTaskExecutor()
                    : Executors.newCachedThreadPool();
        }

        @Override
        public IFileWatcher watch(Path directory) {
            if (!Files.isDirectory(directory)) {
                throw new IllegalArgumentException("Not a directory: " + directory);
            }

            try {
                if (watchService == null) {
                    watchService = FileSystems.getDefault().newWatchService();
                }
                WatchKey key = directory.register(watchService,
                        ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
                watchKeys.put(directory, key);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to watch directory: " + directory, e);
            }
            return this;
        }

        @Override
        public IFileWatcher unwatch(Path directory) {
            WatchKey key = watchKeys.remove(directory);
            if (key != null) {
                key.cancel();
            }
            return this;
        }

        @Override
        public IFileWatcher filter(FileFilter filter) {
            this.fileFilter = filter;
            return this;
        }

        @Override
        public IFileWatcher onCreated(Consumer<File> callback) {
            onCreatedCallbacks.add(callback);
            return this;
        }

        @Override
        public IFileWatcher onModified(Consumer<File> callback) {
            onModifiedCallbacks.add(callback);
            return this;
        }

        @Override
        public IFileWatcher onDeleted(Consumer<File> callback) {
            onDeletedCallbacks.add(callback);
            return this;
        }

        @Override
        public IFileWatcher onEvent(Consumer<FileEvent> callback) {
            onEventCallbacks.add(callback);
            return this;
        }

        @Override
        public IFileWatcher start() throws IOException {
            if (running) {
                return this;
            }

            if (watchService == null) {
                watchService = FileSystems.getDefault().newWatchService();
            }

            running = true;

            // Start watch thread (use virtual thread if configured)
            Runnable watchLoop = this::runWatchLoop;
            if (useVirtualThreads) {
                watchThread = Thread.ofVirtual().name("file-watcher").start(watchLoop);
            } else {
                watchThread = Thread.ofPlatform().name("file-watcher").start(watchLoop);
            }

            return this;
        }

        @Override
        public IFileWatcher stop() {
            if (!running) {
                return this;
            }

            running = false;

            // Interrupt watch thread
            if (watchThread != null) {
                watchThread.interrupt();
            }

            // Close watch service
            if (watchService != null) {
                try {
                    watchService.close();
                } catch (IOException ignored) {
                    log.trace("Cleanup error", ignored);
                }
                watchService = null;
            }

            // Shutdown callback executor
            callbackExecutor.shutdown();
            try {
                if (!callbackExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    callbackExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                callbackExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }

            // Clear watch keys
            watchKeys.values().forEach(WatchKey::cancel);
            watchKeys.clear();

            return this;
        }

        @Override
        public boolean isRunning() {
            return running;
        }

        // NFL: OS file watch polling loop — external I/O iterator — loop preserved per M3 §3
        private void runWatchLoop() {
            while (running) {
                try {
                    WatchKey key = watchService.poll(pollInterval.toMillis(), TimeUnit.MILLISECONDS);
                    if (key == null) {
                        continue;
                    }

                    Optional<Path> dirOpt = findDirectory(key);
                    if (dirOpt.isEmpty()) {
                        key.cancel();
                        continue;
                    }
                    Path dir = dirOpt.get();

                    // NFL: WatchKey event iteration — external OS event source — loop preserved per M3 §3
                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();

                        // Skip overflow events
                        if (kind == StandardWatchEventKinds.OVERFLOW) {
                            continue;
                        }

                        @SuppressWarnings("unchecked")
                        WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                        Path relativePath = pathEvent.context();
                        Path fullPath = dir.resolve(relativePath);
                        File file = fullPath.toFile();

                        // Apply filter
                        if (fileFilter != null && !fileFilter.accept(file)) {
                            continue;
                        }

                        // Dispatch event
                        dispatchEvent(kind, file);
                    }

                    // Reset key to receive further events
                    boolean valid = key.reset();
                    if (!valid) {
                        watchKeys.values().remove(key);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (ClosedWatchServiceException e) {
                    break;
                }
            }
        }

        private Optional<Path> findDirectory(WatchKey key) {
            return watchKeys.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(key))
                    .map(Map.Entry::getKey)
                    .findFirst();
        }

        private void dispatchEvent(WatchEvent.Kind<?> kind, File file) {
            boolean isDirectory = file.isDirectory();

            if (kind == ENTRY_CREATE) {
                EventType type = isDirectory ? EventType.DIRECTORY_CREATED : EventType.CREATED;
                if (!isDirectory) {
                    dispatchCallbacks(onCreatedCallbacks, file);
                }
                fireEvent(new FileEvent(type, file));
            } else if (kind == ENTRY_MODIFY) {
                EventType type = isDirectory ? EventType.DIRECTORY_MODIFIED : EventType.MODIFIED;
                if (!isDirectory) {
                    dispatchCallbacks(onModifiedCallbacks, file);
                }
                fireEvent(new FileEvent(type, file));
            } else if (kind == ENTRY_DELETE) {
                EventType type = isDirectory ? EventType.DIRECTORY_DELETED : EventType.DELETED;
                if (!isDirectory) {
                    dispatchCallbacks(onDeletedCallbacks, file);
                }
                fireEvent(new FileEvent(type, file));
            }
        }

        private void fireEvent(FileEvent event) {
            onEventCallbacks.forEach(cb ->
                    callbackExecutor.execute(() -> cb.accept(event)));
        }

        private void dispatchCallbacks(List<Consumer<File>> callbacks, File file) {
            callbacks.forEach(cb ->
                    callbackExecutor.execute(() -> cb.accept(file)));
        }
    }
}
