// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.ast;

import com.synexia.context.core.AbstractContext;
import com.synexia.context.core.ContextProgress;
import com.synexia.context.core.ContextProgressMonitor;
import com.synexia.context.event.IContextEvent.Change;
import com.synexia.context.event.IContextEvent.ChangeBits;
import com.synexia.context.event.IContextEvent.ChangeKind;
import com.synexia.indexstring.ExactStringIndexResolver;
import com.synexia.job.IProgressMonitor;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/**
 * Explicit single-source edit lifetime retaining the existing append-only AST pool.
 *
 * <p>Unchanged exact source reuses the same document without parsing. Changed source still uses
 * full-source javac parsing, but unchanged canonical atoms survive inside the same owner. This is
 * not an incremental parser, semantic verifier, executor or promotion authority.
 *
 * <p>This does not implement {@link MIndexASTPartialCache.Loader}: a growing shared pool must not
 * silently invalidate the ordinary LRU's retained-weight estimates. The isolated loader is unchanged.
 * A session retains only its current source/document plus the shared arena; clients may retain old
 * immutable documents. Close releases this session's ownership, not all clients' references.
 */
public final class MIndexASTEditSession extends AbstractContext<MIndexASTEditSession> implements AutoCloseable {
  /** Workload/lifetime bounds; these are not byte-precise Java heap limits. */
  public record Limits(int maximumSourceUtf16Units, long maximumAttemptedUtf16Units,
      int maximumParseAttempts) {
    public Limits {
      if (maximumSourceUtf16Units < 0 || maximumAttemptedUtf16Units < 0
          || maximumParseAttempts < 1) throw new IllegalArgumentException("invalid session limits");
    }
  }

  /** Snapshot statistics include attempted parses even when cancellation prevents publication. */
  public record Statistics(long sourceReads, int parseAttempts, long attemptedUtf16Units,
      long publishedVersions, long exactReuses, long failures, long cancellations,
      int retainedPoolAtoms, boolean closed) {}

  private final String sourceRef;
  private final int languageId;
  private final Limits limits;
  private MIndexASTPartialCache.SourceLoader sourceLoader;
  private MIndexASTParser parser;
  private MIndexASTPool pool;
  private MIndexASTDocument current;
  private String currentSource;
  private MIndexASTPartialCache.Key currentKey;
  private boolean closed;
  private boolean loading;
  private long sourceReads;
  private int parseAttempts;
  private long attemptedUtf16Units;
  private long publishedVersions;
  private long exactReuses;
  private long failures;
  private long cancellations;
  private MIndexASTDocument pendingClear;

  MIndexASTEditSession(String sourceRef, int languageId, Limits limits,
      MIndexASTPartialCache.SourceLoader sourceLoader) {
    this.sourceRef = MIndexASTPartialCache.Key.java21(sourceRef, "0".repeat(64)).sourceRef();
    if (languageId < 0) throw new IllegalArgumentException("negative languageId");
    this.languageId = languageId;
    this.limits = Objects.requireNonNull(limits, "limits");
    this.sourceLoader = Objects.requireNonNull(sourceLoader, "sourceLoader");
  }

  public String sourceRef() { return sourceRef; }
  public Limits limits() { return limits; }

  public MIndexASTDocument load(MIndexASTPartialCache.Key key) throws Exception {
    return loadWithContextProgress(key, null);
  }

  /**
   * Named progress entry point avoids ambiguity with the BooleanSupplier overload for null.
   * Null selects the existing no-op monitor. Begin/worked/done run outside the synchronized core;
   * cancellation queries run at its guarded checkpoints. Returned aliases survive later close.
   */
  public MIndexASTDocument loadWithProgress(MIndexASTPartialCache.Key key,
      IProgressMonitor monitor) throws Exception {
    IProgressMonitor progress = monitor == null ? IProgressMonitor.noop() : monitor;
    progress.beginTask("Load retained Java AST edit session", 1L);
    try {
      MIndexASTDocument result = load(key, progress::isCanceled);
      progress.worked(1L);
      return result;
    } finally {
      progress.done();
    }
  }

  /** Named ContextOS entry point; null resolves this source's current context monitor. */
  public MIndexASTDocument loadWithContextProgress(MIndexASTPartialCache.Key key,
      ContextProgressMonitor monitor) throws Exception {
    ContextProgressMonitor progress = ContextProgress.operation(
        monitor == null ? progressMonitor() : monitor, "Load retained Java AST edit session", 1L);
    progress.begin("Load retained Java AST edit session", 1L);
    try {
      MIndexASTDocument result = loadPublishing(key, () -> {
        progress.checkCancelled();
        return false;
      }, progress);
      progress.worked(1L);
      return result;
    } finally {
      progress.done();
    }
  }

  /**
   * Serial per-session load. Cancellation cannot preempt a source callback or javac parse; it is
   * checked immediately after they return, before publishing a new document. Attempts are never
   * refunded and append-only pool additions are not rolled back on failure/cancellation.
   */
  public MIndexASTDocument load(MIndexASTPartialCache.Key key,
      BooleanSupplier cancelled) throws Exception {
    return loadPublishing(key, cancelled, null);
  }

  private MIndexASTDocument loadPublishing(MIndexASTPartialCache.Key key,
      BooleanSupplier cancelled, ContextProgressMonitor monitor) throws Exception {
    MIndexASTDocument previous;
    MIndexASTDocument result;
    MIndexASTDocument cleared = null;
    long previousVersion;
    long version;
    try {
      synchronized (this) {
        previous = current;
        previousVersion = publishedVersions;
        try {
          result = loadLocked(key, cancelled);
          version = publishedVersions;
        } finally {
          // A source/cancellation callback may close reentrantly under the serial load lock.
          // Its clear notification must wait until the outer load releases that lock.
          if (!loading) {
            cleared = pendingClear;
            pendingClear = null;
          }
        }
      }
    } finally {
      if (cleared != null) publishClear(cleared);
    }
    if (version != previousVersion) {
      publish(previous, result, previousVersion, version,
          ChangeBits.SOURCE_CHANGED, monitor);
    }
    return result;
  }

  private MIndexASTDocument loadLocked(MIndexASTPartialCache.Key key,
      BooleanSupplier cancelled) throws Exception {
    ensureOpen();
    if (loading) throw new IllegalStateException("reentrant AST session load");
    Objects.requireNonNull(cancelled, "cancelled");
    loading = true;
    try {
      validateKey(Objects.requireNonNull(key, "key"));
      checkCancelled(cancelled);
      ensureOpen();
      sourceReads = Math.incrementExact(sourceReads);
      CharSequence loaded = Objects.requireNonNull(sourceLoader.load(key), "source");
      ensureOpen();
      String source = stableSource(loaded, cancelled);
      MIndexASTPartialCache.Key actual = MIndexASTPartialCache.Key.java21Source(sourceRef,
          new CheckedCharacters(source, cancelled));
      if (!actual.equals(key)) throw new IllegalArgumentException("source hash/spec mismatch");
      checkCancelled(cancelled);
      ensureOpen();
      if (currentKey != null && currentKey.equals(key)) {
        if (!currentSource.equals(source)) throw new IllegalArgumentException("source digest collision");
        exactReuses = Math.incrementExact(exactReuses);
        return current;
      }
      admitAttempt(source.length());
      if (parser == null) {
        pool = new MIndexASTPool(new ExactStringIndexResolver(), languageId);
        parser = new MIndexASTParser(pool);
      }
      MIndexASTDocument candidate = parser.parse(sourceRef, source);
      checkCancelled(cancelled);
      ensureOpen();
      if (!candidate.sourceUtf16Sha256Hex().equals(key.sourceUtf16Sha256())
          || candidate.root().astSpec().fingerprint() != key.astSpecFingerprint()) {
        throw new IllegalStateException("parser source/spec custody mismatch");
      }
      // Syntax errors remain partial documents with diagnostics, not permission to execute them.
      current = candidate;
      currentSource = source;
      currentKey = key;
      publishedVersions = Math.incrementExact(publishedVersions);
      return candidate;
    } catch (CancellationException failure) {
      cancellations = Math.incrementExact(cancellations);
      throw failure;
    } catch (InterruptedException failure) {
      Thread.currentThread().interrupt();
      cancellations = Math.incrementExact(cancellations);
      throw failure;
    } catch (Exception failure) {
      failures = Math.incrementExact(failures);
      throw failure;
    } finally {
      loading = false;
    }
  }

  /** Current published snapshot only; returns empty after close. */
  public synchronized Optional<MIndexASTDocument> current() { return Optional.ofNullable(current); }

  public synchronized Statistics statistics() {
    return new Statistics(sourceReads, parseAttempts, attemptedUtf16Units, publishedVersions,
        exactReuses, failures, cancellations, pool == null ? 0 : pool.size(), closed);
  }

  /** Close does not preempt a synchronized in-progress load or reclaim aliases held by clients. */
  @Override public void close() {
    MIndexASTDocument previous;
    synchronized (this) {
      if (closed) return;
      closed = true;
      previous = current;
      sourceLoader = null; parser = null; pool = null;
      current = null; currentSource = null; currentKey = null;
      if (loading) {
        pendingClear = previous;
        return;
      }
    }
    if (previous != null) publishClear(previous);
  }

  private void publishClear(MIndexASTDocument previous) {
    publish(previous, null, -1L, -1L, ChangeBits.NONE, null);
  }

  private void publish(MIndexASTDocument previous, MIndexASTDocument next,
      long previousVersion, long version, long detail, ContextProgressMonitor monitor) {
    notifyChangeListeners(new Change(this, ChangeKind.UPDATED,
        ChangeBits.UPDATED | ChangeBits.SNAPSHOT_REPLACED | detail,
        previousVersion, version, Set.of(sourceRef), previous, next, monitor));
  }

  private void validateKey(MIndexASTPartialCache.Key key) {
    if (!sourceRef.equals(key.sourceRef())) throw new IllegalArgumentException("foreign source reference");
    if (key.astSpecFingerprint() != MIndexLanguageSpecs.java21().astSpec().fingerprint()) {
      throw new IllegalArgumentException("foreign AST specification");
    }
  }

  private void admitAttempt(int units) {
    if (parseAttempts == limits.maximumParseAttempts()
        || units > limits.maximumAttemptedUtf16Units() - attemptedUtf16Units) {
      throw new IllegalStateException("AST edit-session parse budget exhausted");
    }
    parseAttempts++;
    attemptedUtf16Units += units;
  }

  private String stableSource(CharSequence loaded, BooleanSupplier cancelled) {
    checkCancelled(cancelled);
    int length = loaded.length();
    if (length < 0 || length > limits.maximumSourceUtf16Units()) {
      throw new IllegalArgumentException("AST edit-session source limit exceeded");
    }
    if (loaded instanceof String stable) return stable;
    char[] units = new char[length];
    for (int i = 0; i < length; i++) {
      if ((i & 4095) == 0) checkCancelled(cancelled);
      units[i] = loaded.charAt(i);
    }
    if (loaded.length() != length) throw new IllegalArgumentException("source length changed during snapshot");
    checkCancelled(cancelled);
    return new String(units);
  }

  private void ensureOpen() {
    if (closed) throw new IllegalStateException("AST edit session is closed");
  }
  private static void checkCancelled(BooleanSupplier cancelled) {
    if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) throw new CancellationException();
  }

  /** Reuses the cache's exact UTF16 hashing owner with bounded cancellation checkpoints. */
  private record CheckedCharacters(String source, BooleanSupplier cancelled) implements CharSequence {
    @Override public int length() { return source.length(); }
    @Override public char charAt(int index) {
      if ((index & 4095) == 0) checkCancelled(cancelled);
      return source.charAt(index);
    }
    @Override public CharSequence subSequence(int start, int end) { return source.subSequence(start, end); }
    @Override public String toString() { return source; }
  }
}
