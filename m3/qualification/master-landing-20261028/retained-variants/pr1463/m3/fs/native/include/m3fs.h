// SPDX-License-Identifier: Apache-2.0
#ifndef M3FS_H
#define M3FS_H

#include <stddef.h>
#include <stdint.h>

#if defined(_WIN32)
#define M3FS_EXPORT __declspec(dllexport)
#else
#define M3FS_EXPORT __attribute__((visibility("default")))
#endif

#ifdef __cplusplus
extern "C" {
#endif

#define M3FS_ABI_VERSION 1
/* Additive capability bits; the original ABI and database schema remain version 1. */
#define M3FS_CAP_CLONE_FILE 1U
#define M3FS_CAP_LIST_PAGE 2U
#define M3FS_CAP_READ_SNAPSHOT 4U
#define M3FS_CAP_WRITEV 8U
#define M3FS_CAP_NATIVE_MINDEX 16U
#define M3FS_CAP_NATIVE_REGEX 32U
#define M3FS_CAP_READV 64U
#define M3FS_CAP_BLOCK_SCRUB 128U
/*
 * Semantic feature bits describe filesystem behavior, not optional entry-point presence.
 * They are intentionally 64-bit so the superset feature plane can grow without changing ABI 1.
 */
#define M3FS_FEATURE_EXACT_BLOCK_DEDUP       (UINT64_C(1) << 0)
#define M3FS_FEATURE_SPARSE_FILES            (UINT64_C(1) << 1)
#define M3FS_FEATURE_BLOCK_CLONE_COW         (UINT64_C(1) << 2)
#define M3FS_FEATURE_HARD_LINKS              (UINT64_C(1) << 3)
#define M3FS_FEATURE_SYMLINKS                (UINT64_C(1) << 4)
#define M3FS_FEATURE_RETAINED_READ_SNAPSHOT  (UINT64_C(1) << 5)
#define M3FS_FEATURE_DIRECTORY_PAGING        (UINT64_C(1) << 6)
#define M3FS_FEATURE_SCATTER_GATHER_WRITE    (UINT64_C(1) << 7)
#define M3FS_FEATURE_WAL_CHECKPOINT          (UINT64_C(1) << 8)
#define M3FS_FEATURE_INTEGRITY_VERIFY        (UINT64_C(1) << 9)
#define M3FS_FEATURE_ORPHAN_BLOCK_GC         (UINT64_C(1) << 10)
#define M3FS_FEATURE_HOLE_PUNCH              (UINT64_C(1) << 11)
#define M3FS_FEATURE_ALLOCATED_RANGE_QUERY   (UINT64_C(1) << 12)

#define M3FS_FEATURE_SCATTER_GATHER_READ (UINT64_C(1) << 13)
#define M3FS_FEATURE_NATIVE_MINDEX (UINT64_C(1) << 14)
#define M3FS_FEATURE_NATIVE_REGEX (UINT64_C(1) << 15)
#define M3FS_FEATURE_BLOCK_SCRUB (UINT64_C(1) << 16)
#define M3FS_FEATURE_SET_METADATA (UINT64_C(1) << 17)
#define M3FS_FEATURE_ATOMIC_REPLACE (UINT64_C(1) << 18)
#define M3FS_FEATURE_CHECKED_INODE_IO (UINT64_C(1) << 19)

#define M3FS_CAP_PUNCH_HOLE 256U
#define M3FS_CAP_ALLOCATED_RANGES 512U
#define M3FS_CAP_SET_METADATA 1024U
#define M3FS_CAP_ATOMIC_REPLACE 2048U
#define M3FS_CAP_CHECKED_INODE_IO 4096U
#define M3FS_CAP_RENAME_NOREPLACE 8192U
#define M3FS_FEATURE_RENAME_NOREPLACE (UINT64_C(1) << 20)
#define M3FS_METADATA_MODE 1U
#define M3FS_METADATA_MTIME 2U
#define M3FS_WRITE_APPEND 1U
#define M3FS_WRITE_TRUNCATE 2U

#define M3FS_MAX_PAGE_ENTRIES 4096U
#define M3FS_MAX_WRITE_SEGMENTS 4096U
#define M3FS_MAX_READ_SEGMENTS 4096U
#define M3FS_SCRUB_BACKFILL_MISSING 1U

#define M3FS_BLOCK_SIZE 65536U
#define M3FS_MAX_PATH_BYTES 4096U
#define M3FS_MAX_NAME_BYTES 255U

typedef struct m3fs m3fs;

enum {
  M3FS_OK = 0,
  M3FS_NOT_FOUND = 1,
  M3FS_EXISTS = 2,
  M3FS_NOT_DIRECTORY = 3,
  M3FS_IS_DIRECTORY = 4,
  M3FS_NOT_EMPTY = 5,
  M3FS_INVALID = 6,
  M3FS_READ_ONLY = 7,
  M3FS_BUSY = 8,
  M3FS_CORRUPT = 9,
  M3FS_NOSPACE = 10,
  M3FS_IO = 11,
  M3FS_NOMEM = 12,
  M3FS_STALE = 13
};

enum {
  M3FS_KIND_FILE = 1,
  M3FS_KIND_DIRECTORY = 2,
  M3FS_KIND_SYMLINK = 3
};

enum {
  M3FS_OPEN_CREATE = 1,
  M3FS_OPEN_READ_ONLY = 2,
  M3FS_OPEN_READ_SNAPSHOT = 4
};

typedef struct m3fs_stat {
  uint64_t inode;
  uint64_t size;
  uint64_t generation;
  uint64_t mtime_ns;
  uint32_t kind;
  uint32_t mode;
  uint32_t nlink;
  uint32_t block_size;
} m3fs_stat;

typedef struct m3fs_stats {
  uint64_t inode_count;
  uint64_t dirent_count;
  uint64_t logical_bytes;
  uint64_t unique_blocks;
  uint64_t block_references;
  uint64_t physical_block_bytes;
} m3fs_stats;

/** Result of one complete deduplicated-block integrity scrub. */
typedef struct m3fs_scrub_report {
  uint64_t blocks_scanned;
  uint64_t checksums_verified;
  uint64_t checksums_missing;
  uint64_t checksums_mismatched;
  uint64_t checksums_backfilled;
} m3fs_scrub_report;

/** Borrowed byte segment for one synchronous scatter/gather write. */
typedef struct m3fs_iovec {
  const void *data;
  uint32_t length;
} m3fs_iovec;

/** Borrowed writable byte segment for one synchronous scatter/gather read. */
typedef struct m3fs_mut_iovec {
  void *data;
  uint32_t length;
} m3fs_mut_iovec;

typedef int (*m3fs_dir_callback)(
    void *context,
    const char *name,
    uint32_t name_bytes,
    const m3fs_stat *stat);

typedef int (*m3fs_range_callback)(
    void *context,
    uint64_t offset,
    uint64_t length);

M3FS_EXPORT int m3fs_abi_version(void);
M3FS_EXPORT uint64_t m3fs_feature_bits(void);
/* Optional symbol for ABI-1 clients: missing symbol means no extension capabilities. */
M3FS_EXPORT uint32_t m3fs_capabilities(void);
M3FS_EXPORT const char *m3fs_status_string(int status);

/*
 * READ_SNAPSHOT requires READ_ONLY and pins one SQLite read view during open. All reads
 * on that handle, including subsequent directory pages, share the view until close.
 * Failed lookups/rejected writes do not release it. Unexpected transaction loss returns IO;
 * the handle never silently adopts newer data. Long-lived snapshots retain WAL history.
 * They are not durable backups; do not replace the backing database/WAL/SHM under a handle.
 * Existing READ_ONLY alone retains its live, per-operation snapshot behavior.
 */
M3FS_EXPORT int m3fs_open(
    const char *database_path,
    uint32_t flags,
    m3fs **out);
M3FS_EXPORT void m3fs_close(m3fs *fs);

M3FS_EXPORT int m3fs_mkdir(
    m3fs *fs, const char *path, uint32_t mode);
M3FS_EXPORT int m3fs_create(
    m3fs *fs, const char *path, uint32_t mode);
M3FS_EXPORT int m3fs_write(
    m3fs *fs,
    const char *path,
    uint64_t offset,
    const void *data,
    uint32_t length,
    uint64_t *new_size);

/*
 * Atomically write the concatenation of zero or more borrowed byte segments.
 * Segment pointers are read only during this call and are never retained. All geometry is
 * validated before the transaction begins. Segment boundaries do not affect persisted bytes.
 */
M3FS_EXPORT int m3fs_writev(
    m3fs *fs,
    const char *path,
    uint64_t offset,
    const m3fs_iovec *segments,
    uint32_t segment_count,
    uint64_t *new_size);
M3FS_EXPORT int m3fs_read(
    m3fs *fs,
    const char *path,
    uint64_t offset,
    void *output,
    uint32_t capacity,
    uint32_t *read_bytes);

/* Fill borrowed writable segments in order under one read view. */
M3FS_EXPORT int m3fs_readv(
    m3fs *fs,
    const char *path,
    uint64_t offset,
    const m3fs_mut_iovec *segments,
    uint32_t segment_count,
    uint64_t *read_bytes);
/* Inode identity is compared within the same transaction as the I/O, including empty I/O. */
M3FS_EXPORT int m3fs_read_if_inode(
    m3fs *fs, const char *path, uint64_t expected_inode, uint64_t offset,
    void *output, uint32_t capacity, uint32_t *read_bytes);
/* APPEND chooses current EOF inside the write transaction; TRUNCATE resizes to offset first.
 * Flags are mutually exclusive. A mutation increments generation exactly once. */
M3FS_EXPORT int m3fs_write_if_inode(
    m3fs *fs, const char *path, uint64_t expected_inode, uint64_t offset,
    const void *data, uint32_t length, uint32_t flags, uint64_t *new_size);
M3FS_EXPORT int m3fs_set_metadata(
    m3fs *fs, const char *path, uint32_t mode, uint64_t mtime_ns, uint32_t flags);
/* Atomic content replacement preserves inode identity, hard links, and retained read views. */
M3FS_EXPORT int m3fs_replace(
    m3fs *fs, const char *path, const void *data, uint32_t length, uint64_t *new_size);
M3FS_EXPORT int m3fs_punch_hole(
    m3fs *fs, const char *path, uint64_t offset, uint64_t length);
M3FS_EXPORT int m3fs_allocated_ranges(
    m3fs *fs, const char *path, uint64_t offset, uint64_t length,
    m3fs_range_callback callback, void *context);

M3FS_EXPORT int m3fs_truncate(
    m3fs *fs, const char *path, uint64_t size);
M3FS_EXPORT int m3fs_unlink(
    m3fs *fs, const char *path);
M3FS_EXPORT int m3fs_rmdir(
    m3fs *fs, const char *path);
/* Fails with EXISTS if destination exists in the same namespace transaction. */
M3FS_EXPORT int m3fs_rename_noreplace(
    m3fs *fs, const char *from, const char *to);
M3FS_EXPORT int m3fs_rename(
    m3fs *fs, const char *from, const char *to);
M3FS_EXPORT int m3fs_link(
    m3fs *fs, const char *existing, const char *new_path);
M3FS_EXPORT int m3fs_symlink(
    m3fs *fs, const char *target, const char *link_path);
M3FS_EXPORT int m3fs_readlink(
    m3fs *fs,
    const char *path,
    char *output,
    uint32_t capacity,
    uint32_t *target_bytes);

M3FS_EXPORT int m3fs_stat_path(
    m3fs *fs, const char *path, m3fs_stat *out);
M3FS_EXPORT int m3fs_list(
    m3fs *fs,
    const char *path,
    m3fs_dir_callback callback,
    void *context);

/*
 * Create a distinct regular-file inode at a previously absent destination, copying mode/size
 * and sharing existing block references in one write transaction. Subsequent writes and
 * truncation remain independent; this is not a hard link. No payload BLOB is read or copied.
 * The result has nlink=1, generation=1 and a fresh mtime. Symlinks/directories are not cloned.
 * out_inode is required, set to zero on failure and valid only after successful commit.
 */
M3FS_EXPORT int m3fs_clone_file(
    m3fs *fs, const char *from, const char *to, uint64_t *out_inode);

/*
 * Bounded keyset scan in unsigned UTF-8 byte order. NULL/empty after_name starts a scan;
 * otherwise return names strictly greater than that component, even if it was deleted.
 * limit is 1..MAX_PAGE_ENTRIES. READ_SNAPSHOT handles retain one view across all pages.
 * Other handles take a fresh snapshot per call: concurrent changes can appear/disappear,
 * and renames can cause an inode to be encountered again under a new name.
 * A callback must not reenter this handle or retain borrowed pointers after returning.
 * Non-OK callback status aborts the call. Already emitted callbacks cannot be undone;
 * discard partial results on failure. Both required output counters are zero on failure.
 */
M3FS_EXPORT int m3fs_list_page(
    m3fs *fs, const char *path, const char *after_name, uint32_t limit,
    m3fs_dir_callback callback, void *context,
    uint32_t *entry_count, uint32_t *has_more);

M3FS_EXPORT int m3fs_checkpoint(m3fs *fs);
M3FS_EXPORT int m3fs_verify(m3fs *fs);

/*
 * Verify CRC32C sidecar integrity for every unique deduplicated block. Missing checksum rows are
 * reported but are not corruption. BACKFILL_MISSING requires a writable handle and inserts only
 * missing rows; it never overwrites an existing checksum and never repairs mismatched payload.
 */
M3FS_EXPORT int m3fs_scrub(
    m3fs *fs, uint32_t flags, m3fs_scrub_report *out);

M3FS_EXPORT int m3fs_get_stats(
    m3fs *fs, m3fs_stats *out);

#ifdef __cplusplus
}
#endif
#endif
