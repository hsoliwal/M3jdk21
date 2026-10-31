// SPDX-License-Identifier: Apache-2.0
#include "m3fs.h"
#include "m3fs_internal.h"

#include <sqlite3.h>

#include <errno.h>
#include <limits.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

#if defined(_WIN32)
#include <windows.h>
typedef CRITICAL_SECTION m3_mutex;
static int m3_mutex_init(m3_mutex *mutex) {
  InitializeCriticalSection(mutex);
  return 0;
}
static void m3_mutex_destroy(m3_mutex *mutex) { DeleteCriticalSection(mutex); }
static void m3_mutex_lock(m3_mutex *mutex) { EnterCriticalSection(mutex); }
static void m3_mutex_unlock(m3_mutex *mutex) { LeaveCriticalSection(mutex); }
#else
#include <pthread.h>
typedef pthread_mutex_t m3_mutex;
static int m3_mutex_init(m3_mutex *mutex) {
  return pthread_mutex_init(mutex, NULL);
}
static void m3_mutex_destroy(m3_mutex *mutex) { (void)pthread_mutex_destroy(mutex); }
static void m3_mutex_lock(m3_mutex *mutex) { (void)pthread_mutex_lock(mutex); }
static void m3_mutex_unlock(m3_mutex *mutex) { (void)pthread_mutex_unlock(mutex); }
#endif

#define M3_SCHEMA_VERSION 1
#define M3_ROOT_INODE UINT64_C(1)

struct m3fs {
  sqlite3 *db;
  int read_only;
  int read_snapshot;
  sqlite3_stmt *read_block;
  sqlite3_stmt *read_range;
  sqlite3_stmt *lookup_child;
  sqlite3_stmt *stat_inode;
  sqlite3_stmt *insert_block;
  sqlite3_stmt *find_block;
  sqlite3_stmt *link_block;
  sqlite3_stmt *touch_inode;
  sqlite3_stmt *directory_page;
  sqlite3_stmt *insert_integrity;
  sqlite3_stmt *read_integrity;
  unsigned char *block_scratch;
  m3_mutex mutex;
};

typedef struct m3_name {
  const char *data;
  uint32_t bytes;
} m3_name;

static const char *const M3_SCHEMA_SQL =
    "PRAGMA foreign_keys=ON;"
    "PRAGMA journal_mode=WAL;"
    "PRAGMA synchronous=FULL;"
    "CREATE TABLE IF NOT EXISTS fs_meta("
    " key TEXT PRIMARY KEY,"
    " value BLOB NOT NULL"
    ") WITHOUT ROWID;"
    "CREATE TABLE IF NOT EXISTS inodes("
    " id INTEGER PRIMARY KEY,"
    " kind INTEGER NOT NULL CHECK(kind IN (1,2,3)),"
    " mode INTEGER NOT NULL,"
    " size INTEGER NOT NULL CHECK(size>=0),"
    " nlink INTEGER NOT NULL CHECK(nlink>=0),"
    " generation INTEGER NOT NULL CHECK(generation>=1),"
    " mtime_ns INTEGER NOT NULL CHECK(mtime_ns>=0)"
    ");"
    "CREATE TABLE IF NOT EXISTS dirents("
    " parent INTEGER NOT NULL REFERENCES inodes(id) ON DELETE CASCADE,"
    " name BLOB NOT NULL CHECK(length(name)>0 AND length(name)<=255),"
    " child INTEGER NOT NULL REFERENCES inodes(id),"
    " PRIMARY KEY(parent,name)"
    ") WITHOUT ROWID;"
    "CREATE INDEX IF NOT EXISTS dirents_child ON dirents(child);"
    "CREATE TABLE IF NOT EXISTS blocks("
    " id INTEGER PRIMARY KEY,"
    " data BLOB NOT NULL UNIQUE CHECK(length(data)=65536)"
    ");"
    "CREATE TABLE IF NOT EXISTS file_blocks("
    " inode INTEGER NOT NULL REFERENCES inodes(id) ON DELETE CASCADE,"
    " block_no INTEGER NOT NULL CHECK(block_no>=0),"
    " block_id INTEGER NOT NULL REFERENCES blocks(id),"
    " PRIMARY KEY(inode,block_no)"
    ") WITHOUT ROWID;"
    "CREATE INDEX IF NOT EXISTS file_blocks_block ON file_blocks(block_id);"
    "CREATE TABLE IF NOT EXISTS symlinks("
    " inode INTEGER PRIMARY KEY REFERENCES inodes(id) ON DELETE CASCADE,"
    " target BLOB NOT NULL CHECK(length(target)>0 AND length(target)<=4096)"
    ");"
    "CREATE TRIGGER IF NOT EXISTS file_blocks_gc_delete "
    "AFTER DELETE ON file_blocks BEGIN "
    " DELETE FROM blocks WHERE id=OLD.block_id "
    "   AND NOT EXISTS(SELECT 1 FROM file_blocks WHERE block_id=OLD.block_id);"
    "END;"
    "CREATE TRIGGER IF NOT EXISTS file_blocks_gc_update "
    "AFTER UPDATE OF block_id ON file_blocks WHEN OLD.block_id<>NEW.block_id BEGIN "
    " DELETE FROM blocks WHERE id=OLD.block_id "
    "   AND NOT EXISTS(SELECT 1 FROM file_blocks WHERE block_id=OLD.block_id);"
    "END;"
    "INSERT OR IGNORE INTO fs_meta(key,value) VALUES('schema_version',1);"
    "INSERT OR IGNORE INTO fs_meta(key,value) VALUES('block_size',65536);"
    "INSERT OR IGNORE INTO inodes(id,kind,mode,size,nlink,generation,mtime_ns)"
    " VALUES(1,2,493,0,1,1,0);";

/*
 * Additive ABI-1 integrity sidecar. Schema version remains 1 because inode/dentry/block identity
 * and every original table/column contract are unchanged.
 */
static const char *const M3_INTEGRITY_SCHEMA_SQL =
    "CREATE TABLE IF NOT EXISTS block_integrity("
    " block_id INTEGER PRIMARY KEY REFERENCES blocks(id) ON DELETE CASCADE,"
    " crc32c INTEGER NOT NULL CHECK(crc32c>=0 AND crc32c<=4294967295)"
    ") WITHOUT ROWID;";

static void m3_crc32c_table(uint32_t table[256]) {
  for (uint32_t value = 0U; value < 256U; ++value) {
    uint32_t crc = value;
    for (uint32_t bit = 0U; bit < 8U; ++bit) {
      uint32_t mask = (uint32_t)(0U - (crc & 1U));
      crc = (crc >> 1U) ^ (UINT32_C(0x82f63b78) & mask);
    }
    table[value] = crc;
  }
}

static uint32_t m3_crc32c(const unsigned char *data, size_t length) {
  uint32_t table[256];
  m3_crc32c_table(table);
  uint32_t crc = UINT32_MAX;
  for (size_t index = 0U; index < length; ++index) {
    uint32_t lookup = (crc ^ data[index]) & UINT32_C(0xff);
    crc = table[lookup] ^ (crc >> 8U);
  }
  return ~crc;
}

static uint64_t m3_now_ns(void) {
  struct timespec value;
  if (timespec_get(&value, TIME_UTC) != TIME_UTC) return UINT64_C(0);
  if (value.tv_sec < 0 || value.tv_nsec < 0) return UINT64_C(0);
  return (uint64_t)value.tv_sec * UINT64_C(1000000000) + (uint64_t)value.tv_nsec;
}

static int m3_status_sqlite(int rc) {
  /* ROW/DONE are handled at their statement call sites, not generic success codes. */
  if (rc == SQLITE_OK) return M3FS_OK;
  int primary = rc & 0xff;
  switch (primary) {
    case SQLITE_BUSY:
    case SQLITE_LOCKED:
      return M3FS_BUSY;
    case SQLITE_FULL:
      return M3FS_NOSPACE;
    case SQLITE_NOMEM:
      return M3FS_NOMEM;
    case SQLITE_READONLY:
      return M3FS_READ_ONLY;
    case SQLITE_CORRUPT:
    case SQLITE_NOTADB:
      return M3FS_CORRUPT;
    default:
      return M3FS_IO;
  }
}

static int m3_exec(sqlite3 *db, const char *sql) {
  char *message = NULL;
  int rc = sqlite3_exec(db, sql, NULL, NULL, &message);
  sqlite3_free(message);
  return m3_status_sqlite(rc);
}

/* Fixed per-handle query cache, protected by the existing filesystem mutex.
 * No row, namespace answer or caller pointer is cached between operations. */
static int m3_cached_statement(
    m3fs *fs, sqlite3_stmt **slot, const char *sql, sqlite3_stmt **out) {
  *out = NULL;
  if (*slot == NULL) {
    int rc = sqlite3_prepare_v2(fs->db, sql, -1, slot, NULL);
    if (rc != SQLITE_OK) {
      (void)sqlite3_finalize(*slot);
      *slot = NULL;
      return m3_status_sqlite(rc);
    }
  }
  *out = *slot;
  return M3FS_OK;
}

/* Reset alone retains bindings. Clear them even after failure, before borrowed data expires. */
static int m3_reset_statement(sqlite3_stmt *stmt, int status) {
  if (stmt == NULL) return status;
  int reset = sqlite3_reset(stmt);
  int cleared = sqlite3_clear_bindings(stmt);
  if (status == M3FS_OK && reset != SQLITE_OK) status = m3_status_sqlite(reset);
  if (status == M3FS_OK && cleared != SQLITE_OK) status = m3_status_sqlite(cleared);
  return status;
}

static int m3_integrity_insert_missing(
    m3fs *fs, sqlite3_int64 block_id, uint32_t crc32c) {
  sqlite3_stmt *stmt = NULL;
  int status = m3_cached_statement(
      fs, &fs->insert_integrity,
      "INSERT OR IGNORE INTO block_integrity(block_id,crc32c) VALUES(?1,?2)", &stmt);
  if (status != M3FS_OK) return status;
  int rc = sqlite3_bind_int64(stmt, 1, block_id);
  if (rc == SQLITE_OK) rc = sqlite3_bind_int64(stmt, 2, (sqlite3_int64)crc32c);
  if (rc == SQLITE_OK) rc = sqlite3_step(stmt);
  status = rc == SQLITE_DONE ? M3FS_OK : m3_status_sqlite(rc);
  return m3_reset_statement(stmt, status);
}

static int m3_integrity_read(
    m3fs *fs, sqlite3_int64 block_id, uint32_t *crc32c, int *present) {
  sqlite3_stmt *stmt = NULL;
  *crc32c = 0U;
  *present = 0;
  int status = m3_cached_statement(
      fs, &fs->read_integrity,
      "SELECT crc32c FROM block_integrity WHERE block_id=?1", &stmt);
  if (status != M3FS_OK) return status;
  int rc = sqlite3_bind_int64(stmt, 1, block_id);
  if (rc == SQLITE_OK) rc = sqlite3_step(stmt);
  if (rc == SQLITE_ROW) {
    sqlite3_int64 stored = sqlite3_column_int64(stmt, 0);
    if (stored < 0 || (uint64_t)stored > UINT32_MAX) status = M3FS_CORRUPT;
    else {
      *crc32c = (uint32_t)stored;
      *present = 1;
    }
  } else if (rc != SQLITE_DONE) {
    status = m3_status_sqlite(rc);
  }
  return m3_reset_statement(stmt, status);
}

static int m3_record_block_integrity(
    m3fs *fs, sqlite3_int64 block_id, const unsigned char *block) {
  uint32_t expected = m3_crc32c(block, M3FS_BLOCK_SIZE);
  int status = m3_integrity_insert_missing(fs, block_id, expected);
  if (status != M3FS_OK) return status;
  uint32_t stored = 0U;
  int present = 0;
  status = m3_integrity_read(fs, block_id, &stored, &present);
  if (status != M3FS_OK) return status;
  return present && stored == expected ? M3FS_OK : M3FS_CORRUPT;
}

static int m3_integrity_table_exists(sqlite3 *db, int *exists) {
  sqlite3_stmt *stmt = NULL;
  *exists = 0;
  int rc = sqlite3_prepare_v2(
      db,
      "SELECT 1 FROM sqlite_master WHERE type='table' AND name='block_integrity'",
      -1, &stmt, NULL);
  if (rc != SQLITE_OK) return m3_status_sqlite(rc);
  rc = sqlite3_step(stmt);
  int status = M3FS_OK;
  if (rc == SQLITE_ROW) *exists = 1;
  else if (rc != SQLITE_DONE) status = m3_status_sqlite(rc);
  int finished = sqlite3_finalize(stmt);
  if (status == M3FS_OK && finished != SQLITE_OK) status = m3_status_sqlite(finished);
  return status;
}

static int m3_block_scratch(m3fs *fs, unsigned char **out) {
  if (fs->block_scratch == NULL) {
    fs->block_scratch = (unsigned char *)malloc(M3FS_BLOCK_SIZE);
    if (fs->block_scratch == NULL) return M3FS_NOMEM;
  }
  *out = fs->block_scratch;
  return M3FS_OK;
}

static int m3_begin(m3fs *fs) {
  if (fs->read_only) return M3FS_READ_ONLY;
  return m3_exec(fs->db, "BEGIN IMMEDIATE");
}

/* A retained view must never silently restart after SQLite loses its transaction. */
static int m3_read_begin(m3fs *fs) {
  if (fs->read_snapshot) {
    return sqlite3_get_autocommit(fs->db) ? M3FS_IO : M3FS_OK;
  }
  return m3_exec(fs->db, "BEGIN");
}

static int m3_finish(m3fs *fs, int status) {
  if (fs->read_snapshot) {
    /* Rejected mutations and failed lookups must not release the caller's read view. */
    return sqlite3_get_autocommit(fs->db) ? M3FS_IO : status;
  }
  if (status == M3FS_OK) {
    int commit = m3_exec(fs->db, "COMMIT");
    if (commit == M3FS_OK) return M3FS_OK;
    (void)m3_exec(fs->db, "ROLLBACK");
    return commit;
  }
  (void)m3_exec(fs->db, "ROLLBACK");
  return status;
}

static int m3_utf8_valid(const unsigned char *s, uint32_t length) {
  uint32_t i = 0;
  while (i < length) {
    unsigned char c = s[i++];
    if (c <= 0x7fU) continue;
    if (c >= 0xc2U && c <= 0xdfU) {
      if (i >= length || (s[i] & 0xc0U) != 0x80U) return 0;
      ++i;
      continue;
    }
    if (c >= 0xe0U && c <= 0xefU) {
      if (i + 1U >= length) return 0;
      unsigned char c1 = s[i];
      unsigned char c2 = s[i + 1U];
      if ((c1 & 0xc0U) != 0x80U || (c2 & 0xc0U) != 0x80U) return 0;
      if (c == 0xe0U && c1 < 0xa0U) return 0;
      if (c == 0xedU && c1 >= 0xa0U) return 0;
      i += 2U;
      continue;
    }
    if (c >= 0xf0U && c <= 0xf4U) {
      if (i + 2U >= length) return 0;
      unsigned char c1 = s[i];
      unsigned char c2 = s[i + 1U];
      unsigned char c3 = s[i + 2U];
      if ((c1 & 0xc0U) != 0x80U
          || (c2 & 0xc0U) != 0x80U
          || (c3 & 0xc0U) != 0x80U) {
        return 0;
      }
      if (c == 0xf0U && c1 < 0x90U) return 0;
      if (c == 0xf4U && c1 >= 0x90U) return 0;
      i += 3U;
      continue;
    }
    return 0;
  }
  return 1;
}

static int m3_valid_path(const char *path) {
  if (path == NULL || path[0] != '/') return 0;
  size_t length = strlen(path);
  if (length == 0U || length > M3FS_MAX_PATH_BYTES) return 0;
  if (length == 1U) return 1;
  if (path[length - 1U] == '/') return 0;

  const unsigned char *bytes = (const unsigned char *)path;
  if (!m3_utf8_valid(bytes, (uint32_t)length)) return 0;

  size_t start = 1U;
  for (size_t i = 1U; i <= length; ++i) {
    if (i == length || path[i] == '/') {
      size_t component = i - start;
      if (component == 0U || component > M3FS_MAX_NAME_BYTES) return 0;
      if (component == 1U && path[start] == '.') return 0;
      if (component == 2U && path[start] == '.' && path[start + 1U] == '.') return 0;
      start = i + 1U;
    }
  }
  return 1;
}

static int m3_lookup_child(
    m3fs *fs,
    uint64_t parent,
    const char *name,
    uint32_t name_bytes,
    uint64_t *child) {
  sqlite3_stmt *stmt = NULL;
  int prepared = m3_cached_statement(fs, &fs->lookup_child,
      "SELECT child FROM dirents WHERE parent=?1 AND name=?2", &stmt);
  if (prepared != M3FS_OK) return prepared;
  int rc = sqlite3_bind_int64(stmt, 1, (sqlite3_int64)parent);
  if (rc == SQLITE_OK) rc = sqlite3_bind_blob(stmt, 2, name, (int)name_bytes, SQLITE_TRANSIENT);
  if (rc == SQLITE_OK) rc = sqlite3_step(stmt);
  int status;
  if (rc == SQLITE_ROW) {
    sqlite3_int64 value = sqlite3_column_int64(stmt, 0);
    if (value <= 0) status = M3FS_CORRUPT;
    else {
      *child = (uint64_t)value;
      status = M3FS_OK;
    }
  } else if (rc == SQLITE_DONE) {
    status = M3FS_NOT_FOUND;
  } else {
    status = m3_status_sqlite(rc);
  }
  return m3_reset_statement(stmt, status);
}

static int m3_lookup_path(m3fs *fs, const char *path, uint64_t *inode) {
  if (!m3_valid_path(path)) return M3FS_INVALID;
  if (path[1] == '\0') {
    *inode = M3_ROOT_INODE;
    return M3FS_OK;
  }

  uint64_t current = M3_ROOT_INODE;
  const char *start = path + 1;
  const char *cursor = start;
  for (;;) {
    if (*cursor == '/' || *cursor == '\0') {
      uint32_t bytes = (uint32_t)(cursor - start);
      uint64_t next = 0;
      int status = m3_lookup_child(fs, current, start, bytes, &next);
      if (status != M3FS_OK) return status;
      current = next;
      if (*cursor == '\0') break;
      start = cursor + 1;
    }
    ++cursor;
  }
  *inode = current;
  return M3FS_OK;
}

static int m3_lookup_parent(
    m3fs *fs,
    const char *path,
    uint64_t *parent,
    m3_name *leaf) {
  if (!m3_valid_path(path) || path[1] == '\0') return M3FS_INVALID;
  const char *slash = strrchr(path, '/');
  if (slash == NULL || slash[1] == '\0') return M3FS_INVALID;
  leaf->data = slash + 1;
  leaf->bytes = (uint32_t)strlen(leaf->data);
  if (slash == path) {
    *parent = M3_ROOT_INODE;
    return M3FS_OK;
  }
  char parent_path[M3FS_MAX_PATH_BYTES + 1U];
  size_t bytes = (size_t)(slash - path);
  if (bytes > M3FS_MAX_PATH_BYTES) return M3FS_INVALID;
  memcpy(parent_path, path, bytes);
  parent_path[bytes] = '\0';
  return m3_lookup_path(fs, parent_path, parent);
}

static int m3_inode_stat(m3fs *fs, uint64_t inode, m3fs_stat *out) {
  sqlite3_stmt *stmt = NULL;
  int prepared = m3_cached_statement(fs, &fs->stat_inode,
      "SELECT kind,mode,size,nlink,generation,mtime_ns FROM inodes WHERE id=?1", &stmt);
  if (prepared != M3FS_OK) return prepared;
  int rc = sqlite3_bind_int64(stmt, 1, (sqlite3_int64)inode);
  if (rc == SQLITE_OK) rc = sqlite3_step(stmt);
  int status;
  if (rc == SQLITE_ROW) {
    sqlite3_int64 size = sqlite3_column_int64(stmt, 2);
    sqlite3_int64 generation = sqlite3_column_int64(stmt, 4);
    sqlite3_int64 mtime = sqlite3_column_int64(stmt, 5);
    int kind = sqlite3_column_int(stmt, 0);
    int mode = sqlite3_column_int(stmt, 1);
    int nlink = sqlite3_column_int(stmt, 3);
    if (size < 0 || generation < 1 || mtime < 0
        || kind < M3FS_KIND_FILE || kind > M3FS_KIND_SYMLINK
        || mode < 0 || nlink < 0) {
      status = M3FS_CORRUPT;
    } else {
      out->inode = inode;
      out->size = (uint64_t)size;
      out->generation = (uint64_t)generation;
      out->mtime_ns = (uint64_t)mtime;
      out->kind = (uint32_t)kind;
      out->mode = (uint32_t)mode;
      out->nlink = (uint32_t)nlink;
      out->block_size = M3FS_BLOCK_SIZE;
      status = M3FS_OK;
    }
  } else if (rc == SQLITE_DONE) {
    status = M3FS_NOT_FOUND;
  } else {
    status = m3_status_sqlite(rc);
    /* Success without a row cannot certify initialized stat fields. */
    if (status == M3FS_OK) status = M3FS_CORRUPT;
  }
  return m3_reset_statement(stmt, status);
}

static int m3_require_directory(m3fs *fs, uint64_t inode) {
  m3fs_stat stat;
  int status = m3_inode_stat(fs, inode, &stat);
  if (status != M3FS_OK) return status;
  return stat.kind == M3FS_KIND_DIRECTORY
      ? M3FS_OK
      : M3FS_NOT_DIRECTORY;
}

/* Persisted high water prevents inode ABA after unlink, including legacy schema-1 images. */
static int m3_seed_inode_high_water(m3fs *fs) {
  int status = m3_begin(fs);
  if (status != M3FS_OK) return status;
  status = m3_exec(fs->db,
      "INSERT OR IGNORE INTO fs_meta(key,value)"
      " SELECT 'inode_high_water',MAX(1,COALESCE(MAX(id),1)) FROM inodes");
  sqlite3_stmt *stmt = NULL;
  if (status == M3FS_OK) {
    int rc = sqlite3_prepare_v2(fs->db,
        "SELECT value FROM fs_meta WHERE key='inode_high_water'", -1, &stmt, NULL);
    if (rc == SQLITE_OK) rc = sqlite3_step(stmt);
    if (rc != SQLITE_ROW || sqlite3_column_type(stmt, 0) != SQLITE_INTEGER
        || sqlite3_column_int64(stmt, 0) < 1) status = M3FS_CORRUPT;
    sqlite3_finalize(stmt);
  }
  if (status == M3FS_OK) {
    status = m3_exec(fs->db,
        "UPDATE fs_meta SET value=MAX(value,(SELECT MAX(id) FROM inodes))"
        " WHERE key='inode_high_water'");
  }
  return m3_finish(fs, status);
}

static int m3_insert_inode(
    m3fs *fs, uint32_t kind, uint32_t mode, uint64_t size, uint64_t *inode) {
  sqlite3_stmt *stmt = NULL;
  int rc = sqlite3_prepare_v2(fs->db,
      "SELECT value FROM fs_meta WHERE key='inode_high_water'", -1, &stmt, NULL);
  if (rc != SQLITE_OK) return m3_status_sqlite(rc);
  rc = sqlite3_step(stmt);
  sqlite3_int64 next = 0;
  int status = M3FS_CORRUPT;
  if (rc == SQLITE_ROW && sqlite3_column_type(stmt, 0) == SQLITE_INTEGER
      && sqlite3_column_int64(stmt, 0) >= 1) {
    sqlite3_int64 high = sqlite3_column_int64(stmt, 0);
    status = high == INT64_MAX ? M3FS_NOSPACE : M3FS_OK;
    if (status == M3FS_OK) next = high + 1;
  } else if (rc != SQLITE_ROW && rc != SQLITE_DONE) status = m3_status_sqlite(rc);
  sqlite3_finalize(stmt);
  if (status != M3FS_OK) return status;
  status = m3_exec(fs->db,
      "UPDATE fs_meta SET value=value+1 WHERE key='inode_high_water'");
  if (status != M3FS_OK) return status;
  rc = sqlite3_prepare_v2(fs->db,
      "INSERT INTO inodes(id,kind,mode,size,nlink,generation,mtime_ns)"
      " VALUES(?1,?2,?3,?4,1,1,?5)", -1, &stmt, NULL);
  if (rc == SQLITE_OK) rc = sqlite3_bind_int64(stmt, 1, next);
  if (rc == SQLITE_OK) rc = sqlite3_bind_int(stmt, 2, (int)kind);
  if (rc == SQLITE_OK) rc = sqlite3_bind_int(stmt, 3, (int)mode);
  if (rc == SQLITE_OK) rc = sqlite3_bind_int64(stmt, 4, (sqlite3_int64)size);
  if (rc == SQLITE_OK) rc = sqlite3_bind_int64(stmt, 5, (sqlite3_int64)m3_now_ns());
  if (rc == SQLITE_OK) rc = sqlite3_step(stmt);
  status = rc == SQLITE_DONE ? M3FS_OK : m3_status_sqlite(rc);
  sqlite3_finalize(stmt);
  if (status == M3FS_OK) *inode = (uint64_t)next;
  return status;
}

static int m3_insert_dentry(
    m3fs *fs, uint64_t parent, m3_name leaf, uint64_t child) {
  sqlite3_stmt *stmt = NULL;
  int rc = sqlite3_prepare_v2(
      fs->db,
      "INSERT INTO dirents(parent,name,child) VALUES(?1,?2,?3)",
      -1,
      &stmt,
      NULL);
  if (rc != SQLITE_OK) return m3_status_sqlite(rc);
  (void)sqlite3_bind_int64(stmt, 1, (sqlite3_int64)parent);
  (void)sqlite3_bind_blob(stmt, 2, leaf.data, (int)leaf.bytes, SQLITE_TRANSIENT);
  (void)sqlite3_bind_int64(stmt, 3, (sqlite3_int64)child);
  rc = sqlite3_step(stmt);
  int status;
  if (rc == SQLITE_DONE) status = M3FS_OK;
  else if ((rc & 0xff) == SQLITE_CONSTRAINT) status = M3FS_EXISTS;
  else status = m3_status_sqlite(rc);
  sqlite3_finalize(stmt);
  return status;
}

static int m3_touch_inode(m3fs *fs, uint64_t inode, uint64_t size) {
  sqlite3_stmt *stmt = NULL;
  int status = m3_cached_statement(fs, &fs->touch_inode,
      "UPDATE inodes SET size=?1,generation=generation+1,mtime_ns=?2 WHERE id=?3", &stmt);
  if (status != M3FS_OK) return status;
  int rc = sqlite3_bind_int64(stmt, 1, (sqlite3_int64)size);
  if (rc == SQLITE_OK) rc = sqlite3_bind_int64(stmt, 2, (sqlite3_int64)m3_now_ns());
  if (rc == SQLITE_OK) rc = sqlite3_bind_int64(stmt, 3, (sqlite3_int64)inode);
  if (rc == SQLITE_OK) rc = sqlite3_step(stmt);
  status = rc == SQLITE_DONE && sqlite3_changes(fs->db) == 1
      ? M3FS_OK
      : rc == SQLITE_DONE ? M3FS_NOT_FOUND : m3_status_sqlite(rc);
  return m3_reset_statement(stmt, status);
}

static int m3_create_node(
    m3fs *fs,
    const char *path,
    uint32_t kind,
    uint32_t mode,
    const char *symlink_target) {
  uint64_t parent = 0;
  m3_name leaf;
  int status = m3_lookup_parent(fs, path, &parent, &leaf);
  if (status != M3FS_OK) return status;
  status = m3_require_directory(fs, parent);
  if (status != M3FS_OK) return status;

  uint64_t existing = 0;
  status = m3_lookup_child(fs, parent, leaf.data, leaf.bytes, &existing);
  if (status == M3FS_OK) return M3FS_EXISTS;
  if (status != M3FS_NOT_FOUND) return status;

  uint64_t size = symlink_target == NULL ? 0U : (uint64_t)strlen(symlink_target);
  uint64_t inode = 0;
  status = m3_insert_inode(fs, kind, mode, size, &inode);
  if (status != M3FS_OK) return status;
  status = m3_insert_dentry(fs, parent, leaf, inode);
  if (status != M3FS_OK) return status;

  if (kind == M3FS_KIND_SYMLINK) {
    sqlite3_stmt *stmt = NULL;
    int rc = sqlite3_prepare_v2(
        fs->db,
        "INSERT INTO symlinks(inode,target) VALUES(?1,?2)",
        -1,
        &stmt,
        NULL);
    if (rc != SQLITE_OK) return m3_status_sqlite(rc);
    (void)sqlite3_bind_int64(stmt, 1, (sqlite3_int64)inode);
    (void)sqlite3_bind_blob(
        stmt, 2, symlink_target, (int)strlen(symlink_target), SQLITE_TRANSIENT);
    rc = sqlite3_step(stmt);
    status = rc == SQLITE_DONE ? M3FS_OK : m3_status_sqlite(rc);
    sqlite3_finalize(stmt);
  }
  return status;
}

/* Point reads are reserved for partial-block mutation. Missing references mean sparse zeroes. */
static int m3_load_block_present(
    m3fs *fs,
    uint64_t inode,
    uint64_t block_no,
    unsigned char *block,
    int *present) {
  sqlite3_stmt *stmt = NULL;
  if (present == NULL) return M3FS_INVALID;
  *present = 0;
  int status = m3_cached_statement(fs, &fs->read_block,
      "SELECT b.data FROM file_blocks f JOIN blocks b ON b.id=f.block_id"
      " WHERE f.inode=?1 AND f.block_no=?2", &stmt);
  if (status != M3FS_OK) return status;
  int rc = sqlite3_bind_int64(stmt, 1, (sqlite3_int64)inode);
  if (rc == SQLITE_OK) rc = sqlite3_bind_int64(stmt, 2, (sqlite3_int64)block_no);
  if (rc == SQLITE_OK) rc = sqlite3_step(stmt);
  if (rc == SQLITE_ROW) {
    const void *data = sqlite3_column_blob(stmt, 0);
    int bytes = sqlite3_column_bytes(stmt, 0);
    if (data == NULL || bytes != (int)M3FS_BLOCK_SIZE) {
      status = M3FS_CORRUPT;
    } else {
      memcpy(block, data, M3FS_BLOCK_SIZE);
      *present = 1;
    }
  } else if (rc == SQLITE_DONE) {
    memset(block, 0, M3FS_BLOCK_SIZE);
  } else {
    status = m3_status_sqlite(rc);
  }
  return m3_reset_statement(stmt, status);
}

static int m3_load_block(
    m3fs *fs, uint64_t inode, uint64_t block_no, unsigned char *block) {
  int present = 0;
  return m3_load_block_present(fs, inode, block_no, block, &present);
}

/* The UNIQUE(data) index remains the exact equality authority, not a digest approximation. */
static int m3_find_block(m3fs *fs, const unsigned char *block, sqlite3_int64 *id) {
  sqlite3_stmt *stmt = NULL;
  int status = m3_cached_statement(fs, &fs->find_block,
      "SELECT id FROM blocks WHERE data=?1", &stmt);
  if (status != M3FS_OK) return status;
  int rc = sqlite3_bind_blob(stmt, 1, block, (int)M3FS_BLOCK_SIZE, SQLITE_STATIC);
  if (rc == SQLITE_OK) rc = sqlite3_step(stmt);
  if (rc == SQLITE_ROW) {
    *id = sqlite3_column_int64(stmt, 0);
    if (*id <= 0) status = M3FS_CORRUPT;
  } else {
    status = rc == SQLITE_DONE ? M3FS_CORRUPT : m3_status_sqlite(rc);
  }
  return m3_reset_statement(stmt, status);
}

static int m3_admit_block(m3fs *fs, const unsigned char *block, sqlite3_int64 *id) {
  sqlite3_stmt *stmt = NULL;
  *id = 0;
  int status = m3_cached_statement(fs, &fs->insert_block,
      "INSERT OR IGNORE INTO blocks(data) VALUES(?1)", &stmt);
  if (status != M3FS_OK) return status;
  /* Borrow only until reset+clear below. No native or Java caller buffer is retained. */
  int rc = sqlite3_bind_blob(stmt, 1, block, (int)M3FS_BLOCK_SIZE, SQLITE_STATIC);
  if (rc == SQLITE_OK) rc = sqlite3_step(stmt);
  status = rc == SQLITE_DONE ? M3FS_OK : m3_status_sqlite(rc);
  if (status == M3FS_OK && sqlite3_changes(fs->db) == 1) {
    *id = sqlite3_last_insert_rowid(fs->db);
    if (*id <= 0) status = M3FS_CORRUPT;
  }
  status = m3_reset_statement(stmt, status);
  if (status != M3FS_OK || *id != 0) return status;
  return m3_find_block(fs, block, id);
}

static int m3_store_block(
    m3fs *fs, uint64_t inode, uint64_t block_no, const unsigned char *block) {
  sqlite3_int64 id = 0;
  int status = m3_admit_block(fs, block, &id);
  if (status != M3FS_OK) return status;
  status = m3_record_block_integrity(fs, id, block);
  if (status != M3FS_OK) return status;
  sqlite3_stmt *stmt = NULL;
  status = m3_cached_statement(fs, &fs->link_block,
      "INSERT INTO file_blocks(inode,block_no,block_id) VALUES(?1,?2,?3)"
      " ON CONFLICT(inode,block_no) DO UPDATE SET block_id=excluded.block_id", &stmt);
  if (status != M3FS_OK) return status;
  int rc = sqlite3_bind_int64(stmt, 1, (sqlite3_int64)inode);
  if (rc == SQLITE_OK) rc = sqlite3_bind_int64(stmt, 2, (sqlite3_int64)block_no);
  if (rc == SQLITE_OK) rc = sqlite3_bind_int64(stmt, 3, id);
  if (rc == SQLITE_OK) rc = sqlite3_step(stmt);
  status = rc == SQLITE_DONE ? M3FS_OK : m3_status_sqlite(rc);
  return m3_reset_statement(stmt, status);
}

/* Copy just the requested row intersection; zero gaps, never zero then overwrite real data. */
static int m3_copy_range_row(
    sqlite3_stmt *stmt, uint64_t offset, uint32_t wanted,
    unsigned char *output, uint32_t *produced) {
  sqlite3_int64 number = sqlite3_column_int64(stmt, 0);
  uint64_t end = offset + (uint64_t)wanted;
  if (number < 0 || (uint64_t)number < offset / M3FS_BLOCK_SIZE
      || (uint64_t)number > (end - 1U) / M3FS_BLOCK_SIZE) {
    return M3FS_CORRUPT;
  }
  uint64_t base = (uint64_t)number * M3FS_BLOCK_SIZE;
  uint64_t first = base < offset ? offset : base;
  uint64_t last = base + M3FS_BLOCK_SIZE;
  if (last > end) last = end;
  if (first < offset + *produced) return M3FS_CORRUPT;
  const unsigned char *data = (const unsigned char *)sqlite3_column_blob(stmt, 1);
  if (data == NULL || sqlite3_column_bytes(stmt, 1) != (int)M3FS_BLOCK_SIZE) {
    return M3FS_CORRUPT;
  }
  uint32_t start = (uint32_t)(first - offset);
  if (start > *produced) memset(output + *produced, 0, start - *produced);
  memcpy(output + start, data + (size_t)(first - base), (size_t)(last - first));
  *produced = (uint32_t)(last - offset);
  return M3FS_OK;
}

/* One indexed cursor traverses allocated blocks, not every possible sparse block number. */
static int m3_read_range(
    m3fs *fs, uint64_t inode, uint64_t offset, unsigned char *output, uint32_t wanted) {
  if (wanted == 0U) return M3FS_OK;
  sqlite3_stmt *stmt = NULL;
  int status = m3_cached_statement(fs, &fs->read_range,
      "SELECT f.block_no,b.data FROM file_blocks f LEFT JOIN blocks b ON b.id=f.block_id"
      " WHERE f.inode=?1 AND f.block_no>=?2 AND f.block_no<=?3 ORDER BY f.block_no", &stmt);
  if (status != M3FS_OK) return status;
  int rc = sqlite3_bind_int64(stmt, 1, (sqlite3_int64)inode);
  if (rc == SQLITE_OK) rc = sqlite3_bind_int64(stmt, 2,
      (sqlite3_int64)(offset / M3FS_BLOCK_SIZE));
  if (rc == SQLITE_OK) rc = sqlite3_bind_int64(stmt, 3,
      (sqlite3_int64)((offset + wanted - 1U) / M3FS_BLOCK_SIZE));
  uint32_t produced = 0U;
  while (rc == SQLITE_OK && (rc = sqlite3_step(stmt)) == SQLITE_ROW) {
    status = m3_copy_range_row(stmt, offset, wanted, output, &produced);
    if (status != M3FS_OK) break;
    rc = SQLITE_OK;
  }
  if (status == M3FS_OK && rc != SQLITE_DONE) status = m3_status_sqlite(rc);
  if (status == M3FS_OK && produced < wanted) {
    memset(output + produced, 0, wanted - produced);
  }
  return m3_reset_statement(stmt, status);
}

static int m3_write_partial_block(
    m3fs *fs, uint64_t inode, uint64_t block_no,
    uint32_t within, const unsigned char *input, uint32_t length) {
  unsigned char *scratch = NULL;
  int status = m3_block_scratch(fs, &scratch);
  if (status == M3FS_OK) status = m3_load_block(fs, inode, block_no, scratch);
  if (status == M3FS_OK) {
    memcpy(scratch + within, input, length);
    status = m3_store_block(fs, inode, block_no, scratch);
  }
  return status;
}

static int m3_delete_dentry(
    m3fs *fs, uint64_t parent, m3_name leaf, uint64_t inode) {
  sqlite3_stmt *stmt = NULL;
  int rc = sqlite3_prepare_v2(
      fs->db,
      "DELETE FROM dirents WHERE parent=?1 AND name=?2 AND child=?3",
      -1,
      &stmt,
      NULL);
  if (rc != SQLITE_OK) return m3_status_sqlite(rc);
  (void)sqlite3_bind_int64(stmt, 1, (sqlite3_int64)parent);
  (void)sqlite3_bind_blob(stmt, 2, leaf.data, (int)leaf.bytes, SQLITE_TRANSIENT);
  (void)sqlite3_bind_int64(stmt, 3, (sqlite3_int64)inode);
  rc = sqlite3_step(stmt);
  int status = rc == SQLITE_DONE && sqlite3_changes(fs->db) == 1
      ? M3FS_OK
      : rc == SQLITE_DONE ? M3FS_NOT_FOUND : m3_status_sqlite(rc);
  sqlite3_finalize(stmt);
  return status;
}

static int m3_decrement_link(m3fs *fs, uint64_t inode) {
  sqlite3_stmt *stmt = NULL;
  int rc = sqlite3_prepare_v2(
      fs->db,
      "UPDATE inodes SET nlink=nlink-1,generation=generation+1,mtime_ns=?1"
      " WHERE id=?2 AND nlink>0",
      -1,
      &stmt,
      NULL);
  if (rc != SQLITE_OK) return m3_status_sqlite(rc);
  (void)sqlite3_bind_int64(stmt, 1, (sqlite3_int64)m3_now_ns());
  (void)sqlite3_bind_int64(stmt, 2, (sqlite3_int64)inode);
  rc = sqlite3_step(stmt);
  int status = rc == SQLITE_DONE && sqlite3_changes(fs->db) == 1
      ? M3FS_OK
      : rc == SQLITE_DONE ? M3FS_CORRUPT : m3_status_sqlite(rc);
  sqlite3_finalize(stmt);
  if (status != M3FS_OK) return status;

  rc = sqlite3_prepare_v2(
      fs->db,
      "DELETE FROM inodes WHERE id=?1 AND nlink=0",
      -1,
      &stmt,
      NULL);
  if (rc != SQLITE_OK) return m3_status_sqlite(rc);
  (void)sqlite3_bind_int64(stmt, 1, (sqlite3_int64)inode);
  rc = sqlite3_step(stmt);
  status = rc == SQLITE_DONE ? M3FS_OK : m3_status_sqlite(rc);
  sqlite3_finalize(stmt);
  return status;
}

static int m3_directory_empty(m3fs *fs, uint64_t inode, int *empty) {
  sqlite3_stmt *stmt = NULL;
  int rc = sqlite3_prepare_v2(
      fs->db,
      "SELECT 1 FROM dirents WHERE parent=?1 LIMIT 1",
      -1,
      &stmt,
      NULL);
  if (rc != SQLITE_OK) return m3_status_sqlite(rc);
  (void)sqlite3_bind_int64(stmt, 1, (sqlite3_int64)inode);
  rc = sqlite3_step(stmt);
  if (rc == SQLITE_DONE) {
    *empty = 1;
    sqlite3_finalize(stmt);
    return M3FS_OK;
  }
  if (rc == SQLITE_ROW) {
    *empty = 0;
    sqlite3_finalize(stmt);
    return M3FS_OK;
  }
  int status = m3_status_sqlite(rc);
  sqlite3_finalize(stmt);
  return status;
}

static int m3_parent_of_directory(
    m3fs *fs, uint64_t child, uint64_t *parent) {
  if (child == M3_ROOT_INODE) {
    *parent = M3_ROOT_INODE;
    return M3FS_OK;
  }
  sqlite3_stmt *stmt = NULL;
  int rc = sqlite3_prepare_v2(
      fs->db,
      "SELECT parent FROM dirents WHERE child=?1 LIMIT 2",
      -1,
      &stmt,
      NULL);
  if (rc != SQLITE_OK) return m3_status_sqlite(rc);
  (void)sqlite3_bind_int64(stmt, 1, (sqlite3_int64)child);
  rc = sqlite3_step(stmt);
  if (rc != SQLITE_ROW) {
    int status = rc == SQLITE_DONE ? M3FS_CORRUPT : m3_status_sqlite(rc);
    sqlite3_finalize(stmt);
    return status;
  }
  sqlite3_int64 value = sqlite3_column_int64(stmt, 0);
  rc = sqlite3_step(stmt);
  if (rc == SQLITE_ROW || value <= 0) {
    sqlite3_finalize(stmt);
    return M3FS_CORRUPT;
  }
  if (rc != SQLITE_DONE) {
    int status = m3_status_sqlite(rc);
    sqlite3_finalize(stmt);
    return status;
  }
  *parent = (uint64_t)value;
  sqlite3_finalize(stmt);
  return M3FS_OK;
}

static int m3_would_cycle(m3fs *fs, uint64_t source_dir, uint64_t new_parent) {
  uint64_t cursor = new_parent;
  for (uint32_t hops = 0; hops < M3FS_MAX_PATH_BYTES; ++hops) {
    if (cursor == source_dir) return 1;
    if (cursor == M3_ROOT_INODE) return 0;
    uint64_t parent = 0;
    int status = m3_parent_of_directory(fs, cursor, &parent);
    if (status != M3FS_OK) return -status;
    cursor = parent;
  }
  return -M3FS_CORRUPT;
}

static int m3_validate_existing_schema(m3fs *fs) {
  sqlite3_stmt *stmt = NULL;
  int rc = sqlite3_prepare_v2(
      fs->db,
      "SELECT CAST(value AS INTEGER) FROM fs_meta WHERE key='schema_version'",
      -1,
      &stmt,
      NULL);
  if (rc != SQLITE_OK) return M3FS_CORRUPT;
  rc = sqlite3_step(stmt);
  int status = M3FS_CORRUPT;
  if (rc == SQLITE_ROW && sqlite3_column_int(stmt, 0) == M3_SCHEMA_VERSION) {
    status = M3FS_OK;
  }
  sqlite3_finalize(stmt);
  return status;
}

int m3fs_abi_version(void) {
  return M3FS_ABI_VERSION;
}

uint32_t m3fs_capabilities(void) {
  return M3FS_CAP_CLONE_FILE | M3FS_CAP_LIST_PAGE
      | M3FS_CAP_READ_SNAPSHOT | M3FS_CAP_WRITEV
      | M3FS_CAP_NATIVE_MINDEX | M3FS_CAP_NATIVE_REGEX
      | M3FS_CAP_READV | M3FS_CAP_BLOCK_SCRUB
      | M3FS_CAP_PUNCH_HOLE | M3FS_CAP_ALLOCATED_RANGES
      | M3FS_CAP_SET_METADATA | M3FS_CAP_ATOMIC_REPLACE
      | M3FS_CAP_CHECKED_INODE_IO | M3FS_CAP_RENAME_NOREPLACE;
}

uint64_t m3fs_feature_bits(void) {
  return M3FS_FEATURE_EXACT_BLOCK_DEDUP
      | M3FS_FEATURE_SPARSE_FILES
      | M3FS_FEATURE_BLOCK_CLONE_COW
      | M3FS_FEATURE_HARD_LINKS
      | M3FS_FEATURE_SYMLINKS
      | M3FS_FEATURE_RETAINED_READ_SNAPSHOT
      | M3FS_FEATURE_DIRECTORY_PAGING
      | M3FS_FEATURE_SCATTER_GATHER_WRITE
      | M3FS_FEATURE_WAL_CHECKPOINT
      | M3FS_FEATURE_INTEGRITY_VERIFY
      | M3FS_FEATURE_ORPHAN_BLOCK_GC
      | M3FS_FEATURE_HOLE_PUNCH
      | M3FS_FEATURE_ALLOCATED_RANGE_QUERY
      | M3FS_FEATURE_SCATTER_GATHER_READ | M3FS_FEATURE_NATIVE_MINDEX
      | M3FS_FEATURE_NATIVE_REGEX | M3FS_FEATURE_BLOCK_SCRUB
      | M3FS_FEATURE_SET_METADATA | M3FS_FEATURE_ATOMIC_REPLACE
      | M3FS_FEATURE_CHECKED_INODE_IO | M3FS_FEATURE_RENAME_NOREPLACE;
}

const char *m3fs_status_string(int status) {
  switch (status) {
    case M3FS_OK: return "OK";
    case M3FS_NOT_FOUND: return "NOT_FOUND";
    case M3FS_EXISTS: return "EXISTS";
    case M3FS_NOT_DIRECTORY: return "NOT_DIRECTORY";
    case M3FS_IS_DIRECTORY: return "IS_DIRECTORY";
    case M3FS_NOT_EMPTY: return "NOT_EMPTY";
    case M3FS_INVALID: return "INVALID";
    case M3FS_READ_ONLY: return "READ_ONLY";
    case M3FS_BUSY: return "BUSY";
    case M3FS_CORRUPT: return "CORRUPT";
    case M3FS_NOSPACE: return "NOSPACE";
    case M3FS_IO: return "IO";
    case M3FS_NOMEM: return "NOMEM";
    case M3FS_STALE: return "STALE";
    default: return "UNKNOWN";
  }
}

int m3fs_open(
    const char *database_path,
    uint32_t flags,
    m3fs **out) {
  if (database_path == NULL || database_path[0] == '\0' || out == NULL
      || (flags & ~(uint32_t)(M3FS_OPEN_CREATE | M3FS_OPEN_READ_ONLY
          | M3FS_OPEN_READ_SNAPSHOT)) != 0U
      || ((flags & M3FS_OPEN_READ_SNAPSHOT) != 0U
          && (flags & M3FS_OPEN_READ_ONLY) == 0U)
      || ((flags & M3FS_OPEN_CREATE) != 0U
          && (flags & M3FS_OPEN_READ_ONLY) != 0U)) {
    return M3FS_INVALID;
  }
  *out = NULL;
  m3fs *fs = (m3fs *)calloc(1U, sizeof(*fs));
  if (fs == NULL) return M3FS_NOMEM;
  if (m3_mutex_init(&fs->mutex) != 0) {
    free(fs);
    return M3FS_IO;
  }

  fs->read_snapshot = (flags & M3FS_OPEN_READ_SNAPSHOT) != 0U;
  int open_flags = SQLITE_OPEN_FULLMUTEX;
  if ((flags & M3FS_OPEN_READ_ONLY) != 0U) {
    open_flags |= SQLITE_OPEN_READONLY;
    fs->read_only = 1;
  } else {
    open_flags |= SQLITE_OPEN_READWRITE;
    if ((flags & M3FS_OPEN_CREATE) != 0U) open_flags |= SQLITE_OPEN_CREATE;
  }

  int rc = sqlite3_open_v2(database_path, &fs->db, open_flags, NULL);
  if (rc != SQLITE_OK) {
    int status = m3_status_sqlite(rc);
    sqlite3_close(fs->db);
    m3_mutex_destroy(&fs->mutex);
    free(fs);
    return status;
  }
  (void)sqlite3_extended_result_codes(fs->db, 1);
  (void)sqlite3_busy_timeout(fs->db, 5000);
  int status = m3_exec(fs->db, "PRAGMA foreign_keys=ON");
  if (status == M3FS_OK && !fs->read_only) {
    status = m3_exec(fs->db, "PRAGMA journal_mode=WAL;PRAGMA synchronous=FULL");
  }
  if (status == M3FS_OK && fs->read_snapshot) {
    status = m3_exec(fs->db, "BEGIN");
  }
  /* Reading fs_meta establishes the retained snapshot before open returns. */
  if (status == M3FS_OK) {
    if ((flags & M3FS_OPEN_CREATE) != 0U) {
      status = m3_exec(fs->db, M3_SCHEMA_SQL);
      if (status == M3FS_OK) status = m3_validate_existing_schema(fs);
    } else {
      status = m3_validate_existing_schema(fs);
    }
  }
  if (status == M3FS_OK && !fs->read_only) {
    status = m3_exec(fs->db, M3_INTEGRITY_SCHEMA_SQL);
    if (status == M3FS_OK) status = m3_seed_inode_high_water(fs);
  }
  if (status != M3FS_OK) {
    sqlite3_close(fs->db);
    m3_mutex_destroy(&fs->mutex);
    free(fs);
    return status;
  }
  *out = fs;
  return M3FS_OK;
}

void m3fs_close(m3fs *fs) {
  if (fs == NULL) return;
  m3_mutex_lock(&fs->mutex);
  sqlite3 *db = fs->db;
  fs->db = NULL;
  if (db != NULL) {
    sqlite3_stmt *statements[] = {fs->read_block, fs->read_range, fs->lookup_child,
        fs->stat_inode, fs->insert_block, fs->find_block, fs->link_block,
        fs->touch_inode, fs->directory_page, fs->insert_integrity, fs->read_integrity};
    for (size_t i = 0U; i < sizeof(statements) / sizeof(statements[0]); ++i) {
      (void)sqlite3_finalize(statements[i]);
    }
    if (fs->read_snapshot) (void)m3_exec(db, "ROLLBACK");
    (void)sqlite3_close_v2(db);
  }
  m3_mutex_unlock(&fs->mutex);
  m3_mutex_destroy(&fs->mutex);
  free(fs->block_scratch);
  free(fs);
}

int m3fs_mkdir(m3fs *fs, const char *path, uint32_t mode) {
  if (fs == NULL || !m3_valid_path(path) || path[1] == '\0' || mode > UINT32_C(07777)) {
    return M3FS_INVALID;
  }
  m3_mutex_lock(&fs->mutex);
  int status = m3_begin(fs);
  if (status == M3FS_OK) {
    status = m3_create_node(fs, path, M3FS_KIND_DIRECTORY, mode, NULL);
    status = m3_finish(fs, status);
  }
  m3_mutex_unlock(&fs->mutex);
  return status;
}

int m3fs_create(m3fs *fs, const char *path, uint32_t mode) {
  if (fs == NULL || !m3_valid_path(path) || path[1] == '\0' || mode > UINT32_C(07777)) {
    return M3FS_INVALID;
  }
  m3_mutex_lock(&fs->mutex);
  int status = m3_begin(fs);
  if (status == M3FS_OK) {
    status = m3_create_node(fs, path, M3FS_KIND_FILE, mode, NULL);
    status = m3_finish(fs, status);
  }
  m3_mutex_unlock(&fs->mutex);
  return status;
}

static int m3_resize_blocks(m3fs *fs, uint64_t inode, uint64_t old_size, uint64_t size) {
  int status = M3FS_OK;
  if (status == M3FS_OK && size < old_size) {
    uint64_t keep_blocks =
        size == 0U ? 0U : (size + M3FS_BLOCK_SIZE - 1U) / M3FS_BLOCK_SIZE;
    sqlite3_stmt *stmt = NULL;
    int rc = sqlite3_prepare_v2(
        fs->db,
        "DELETE FROM file_blocks WHERE inode=?1 AND block_no>=?2",
        -1,
        &stmt,
        NULL);
    if (rc != SQLITE_OK) status = m3_status_sqlite(rc);
    if (status == M3FS_OK) {
      (void)sqlite3_bind_int64(stmt, 1, (sqlite3_int64)inode);
      (void)sqlite3_bind_int64(stmt, 2, (sqlite3_int64)keep_blocks);
      rc = sqlite3_step(stmt);
      if (rc != SQLITE_DONE) status = m3_status_sqlite(rc);
    }
    sqlite3_finalize(stmt);

    uint32_t tail = (uint32_t)(size % M3FS_BLOCK_SIZE);
    if (status == M3FS_OK && tail != 0U) {
      unsigned char *scratch = NULL;
      status = m3_block_scratch(fs, &scratch);
      if (status == M3FS_OK) {
        uint64_t block_no = size / M3FS_BLOCK_SIZE;
        status = m3_load_block(fs, inode, block_no, scratch);
        if (status == M3FS_OK) {
          memset(scratch + tail, 0, M3FS_BLOCK_SIZE - tail);
          status = m3_store_block(fs, inode, block_no, scratch);
        }
      }
    }
  }

  return status;
}

static int m3_writev_transaction(
    m3fs *fs,
    const char *path,
    uint64_t expected_inode,
    uint32_t flags,
    uint64_t offset,
    const m3fs_iovec *segments,
    uint32_t segment_count,
    uint64_t total_length,
    uint64_t *resulting_size_out) {
  uint64_t inode = 0U;
  m3fs_stat stat;
  memset(&stat, 0, sizeof(stat));
  int status = m3_lookup_path(fs, path, &inode);
  if (status == M3FS_OK && expected_inode != 0U && inode != expected_inode) {
    status = M3FS_STALE;
  }
  if (status == M3FS_OK) status = m3_inode_stat(fs, inode, &stat);
  if (status == M3FS_OK && stat.kind == M3FS_KIND_DIRECTORY) {
    status = M3FS_IS_DIRECTORY;
  } else if (status == M3FS_OK && stat.kind != M3FS_KIND_FILE) {
    status = M3FS_INVALID;
  }

  if (status == M3FS_OK && (flags & M3FS_WRITE_APPEND) != 0U) {
    offset = stat.size;
  }
  if (status == M3FS_OK
      && (offset > (uint64_t)INT64_MAX || total_length > (uint64_t)INT64_MAX - offset)) {
    status = M3FS_INVALID;
  }
  uint64_t base_size = (flags & M3FS_WRITE_TRUNCATE) != 0U ? offset : stat.size;
  uint64_t final_size = base_size;
  if (status == M3FS_OK && offset + total_length > final_size) final_size = offset + total_length;
  if (status == M3FS_OK && (total_length != 0U || final_size != stat.size)
      && stat.generation >= (uint64_t)INT64_MAX) status = M3FS_NOSPACE;
  if (status == M3FS_OK && (flags & M3FS_WRITE_TRUNCATE) != 0U) {
    status = m3_resize_blocks(fs, inode, stat.size, offset);
  }
  uint64_t written = 0U;
  for (uint32_t segment_index = 0U;
       status == M3FS_OK && segment_index < segment_count;
       ++segment_index) {
    const unsigned char *input = (const unsigned char *)segments[segment_index].data;
    uint32_t consumed = 0U;
    const uint32_t segment_length = segments[segment_index].length;
    while (status == M3FS_OK && consumed < segment_length) {
      uint64_t absolute = offset + written + (uint64_t)consumed;
      uint64_t block_no = absolute / M3FS_BLOCK_SIZE;
      uint32_t within = (uint32_t)(absolute % M3FS_BLOCK_SIZE);
      uint32_t chunk = M3FS_BLOCK_SIZE - within;
      if (chunk > segment_length - consumed) chunk = segment_length - consumed;
      if (within == 0U && chunk == M3FS_BLOCK_SIZE) {
        status = m3_store_block(fs, inode, block_no, input + consumed);
      } else {
        status = m3_write_partial_block(
            fs, inode, block_no, within, input + consumed, chunk);
      }
      consumed += chunk;
    }
    written += (uint64_t)segment_length;
  }

  uint64_t resulting_size = 0U;
  if (status == M3FS_OK) {
    resulting_size = base_size;
    uint64_t end = offset + total_length;
    if (end > resulting_size) resulting_size = end;
    if (total_length != 0U || resulting_size != stat.size) {
      status = m3_touch_inode(fs, inode, resulting_size);
    }
  }
  if (status == M3FS_OK && resulting_size_out != NULL) {
    *resulting_size_out = resulting_size;
  }
  return status;
}

int m3fs_writev(
    m3fs *fs,
    const char *path,
    uint64_t offset,
    const m3fs_iovec *segments,
    uint32_t segment_count,
    uint64_t *new_size) {
  if (fs == NULL || !m3_valid_path(path)
      || segment_count > M3FS_MAX_WRITE_SEGMENTS
      || (segment_count != 0U && segments == NULL)) {
    return M3FS_INVALID;
  }
  uint64_t total_length = 0U;
  for (uint32_t index = 0U; index < segment_count; ++index) {
    if (segments[index].length != 0U && segments[index].data == NULL) {
      return M3FS_INVALID;
    }
    if (total_length > UINT64_MAX - (uint64_t)segments[index].length) {
      return M3FS_INVALID;
    }
    total_length += (uint64_t)segments[index].length;
  }
  if (offset > UINT64_MAX - total_length) return M3FS_INVALID;

  m3_mutex_lock(&fs->mutex);
  int status = m3_begin(fs);
  uint64_t resulting_size = 0U;
  if (status == M3FS_OK) {
    status = m3_writev_transaction(
        fs, path, 0U, 0U, offset, segments, segment_count, total_length, &resulting_size);
  }
  status = m3_finish(fs, status);
  if (status == M3FS_OK && new_size != NULL) *new_size = resulting_size;
  m3_mutex_unlock(&fs->mutex);
  return status;
}

int m3fs_write(
    m3fs *fs,
    const char *path,
    uint64_t offset,
    const void *data,
    uint32_t length,
    uint64_t *new_size) {
  m3fs_iovec segment = {data, length};
  return m3fs_writev(fs, path, offset, &segment, 1U, new_size);
}

static int m3_write_checked(
    m3fs *fs, const char *path, uint64_t expected_inode, uint64_t offset,
    const void *data, uint32_t length, uint32_t flags, uint64_t *new_size) {
  if (fs == NULL || !m3_valid_path(path) || (length != 0U && data == NULL)
      || flags > M3FS_WRITE_TRUNCATE || offset > (uint64_t)INT64_MAX) {
    return M3FS_INVALID;
  }
  m3fs_iovec segment = {data, length};
  m3_mutex_lock(&fs->mutex);
  int status = m3_begin(fs);
  uint64_t resulting_size = 0U;
  if (status == M3FS_OK) {
    status = m3_writev_transaction(fs, path, expected_inode, flags, offset,
        &segment, 1U, length, &resulting_size);
  }
  status = m3_finish(fs, status);
  if (status == M3FS_OK && new_size != NULL) *new_size = resulting_size;
  m3_mutex_unlock(&fs->mutex);
  return status;
}

int m3fs_write_if_inode(
    m3fs *fs, const char *path, uint64_t expected_inode, uint64_t offset,
    const void *data, uint32_t length, uint32_t flags, uint64_t *new_size) {
  if (expected_inode == 0U || expected_inode > (uint64_t)INT64_MAX) return M3FS_INVALID;
  return m3_write_checked(fs, path, expected_inode, offset, data, length, flags, new_size);
}

int m3fs_replace(
    m3fs *fs, const char *path, const void *data, uint32_t length, uint64_t *new_size) {
  return m3_write_checked(fs, path, 0U, 0U, data, length, M3FS_WRITE_TRUNCATE, new_size);
}

static int m3_read_checked(
    m3fs *fs,
    const char *path,
    uint64_t expected_inode,
    uint64_t offset,
    void *output,
    uint32_t capacity,
    uint32_t *read_bytes) {
  if (fs == NULL || !m3_valid_path(path) || read_bytes == NULL
      || (capacity != 0U && output == NULL)) {
    return M3FS_INVALID;
  }
  *read_bytes = 0U;
  m3_mutex_lock(&fs->mutex);
  int snapshot = m3_read_begin(fs);
  if (snapshot != M3FS_OK) {
    m3_mutex_unlock(&fs->mutex);
    return snapshot;
  }
  uint64_t inode = 0;
  m3fs_stat stat;
  int status = m3_lookup_path(fs, path, &inode);
  if (status == M3FS_OK && expected_inode != 0U && inode != expected_inode) {
    status = M3FS_STALE;
  }
  if (status == M3FS_OK) status = m3_inode_stat(fs, inode, &stat);
  if (status == M3FS_OK && stat.kind == M3FS_KIND_DIRECTORY) {
    status = M3FS_IS_DIRECTORY;
  } else if (status == M3FS_OK && stat.kind != M3FS_KIND_FILE) {
    status = M3FS_INVALID;
  }

  uint64_t available = 0U;
  if (status == M3FS_OK && offset < stat.size) available = stat.size - offset;
  uint32_t wanted = capacity;
  if ((uint64_t)wanted > available) wanted = (uint32_t)available;
  if (status == M3FS_OK) {
    status = m3_read_range(fs, inode, offset, (unsigned char *)output, wanted);
  }
  status = m3_finish(fs, status);
  if (status == M3FS_OK) *read_bytes = wanted;
  m3_mutex_unlock(&fs->mutex);
  return status;
}

int m3fs_read(
    m3fs *fs, const char *path, uint64_t offset, void *output,
    uint32_t capacity, uint32_t *read_bytes) {
  return m3_read_checked(fs, path, 0U, offset, output, capacity, read_bytes);
}

int m3fs_read_if_inode(
    m3fs *fs, const char *path, uint64_t expected_inode, uint64_t offset,
    void *output, uint32_t capacity, uint32_t *read_bytes) {
  if (expected_inode == 0U || expected_inode > (uint64_t)INT64_MAX) return M3FS_INVALID;
  return m3_read_checked(fs, path, expected_inode, offset, output, capacity, read_bytes);
}

int m3fs_readv(
    m3fs *fs,
    const char *path,
    uint64_t offset,
    const m3fs_mut_iovec *segments,
    uint32_t segment_count,
    uint64_t *read_bytes) {
  if (fs == NULL || !m3_valid_path(path) || read_bytes == NULL) {
    return M3FS_INVALID;
  }
  *read_bytes = 0U;
  if (segment_count > M3FS_MAX_READ_SEGMENTS
      || (segment_count != 0U && segments == NULL)) {
    return M3FS_INVALID;
  }

  uint64_t total_capacity = 0U;
  for (uint32_t index = 0U; index < segment_count; ++index) {
    if (segments[index].length != 0U && segments[index].data == NULL) {
      return M3FS_INVALID;
    }
    if (total_capacity > UINT64_MAX - (uint64_t)segments[index].length) {
      return M3FS_INVALID;
    }
    total_capacity += (uint64_t)segments[index].length;
  }
  if (offset > UINT64_MAX - total_capacity) return M3FS_INVALID;

  m3_mutex_lock(&fs->mutex);
  int snapshot = m3_read_begin(fs);
  if (snapshot != M3FS_OK) {
    m3_mutex_unlock(&fs->mutex);
    return snapshot;
  }

  uint64_t inode = 0U;
  m3fs_stat stat;
  int status = m3_lookup_path(fs, path, &inode);
  if (status == M3FS_OK) status = m3_inode_stat(fs, inode, &stat);
  if (status == M3FS_OK && stat.kind == M3FS_KIND_DIRECTORY) {
    status = M3FS_IS_DIRECTORY;
  } else if (status == M3FS_OK && stat.kind != M3FS_KIND_FILE) {
    status = M3FS_INVALID;
  }

  uint64_t available = 0U;
  if (status == M3FS_OK && offset < stat.size) available = stat.size - offset;
  uint64_t wanted = total_capacity < available ? total_capacity : available;
  uint64_t completed = 0U;
  for (uint32_t index = 0U;
       status == M3FS_OK && index < segment_count && completed < wanted;
       ++index) {
    uint64_t remaining = wanted - completed;
    uint32_t count = segments[index].length;
    if ((uint64_t)count > remaining) count = (uint32_t)remaining;
    if (count != 0U) {
      status = m3_read_range(
          fs, inode, offset + completed,
          (unsigned char *)segments[index].data, count);
      completed += (uint64_t)count;
    }
  }

  status = m3_finish(fs, status);
  if (status == M3FS_OK) *read_bytes = completed;
  m3_mutex_unlock(&fs->mutex);
  return status;
}

int m3fs_read_owned(
    m3fs *fs,
    const char *path,
    unsigned char **out_bytes,
    uint32_t *out_length) {
  if (fs == NULL || !m3_valid_path(path) || out_bytes == NULL || out_length == NULL) {
    return M3FS_INVALID;
  }
  *out_bytes = NULL;
  *out_length = 0U;

  m3_mutex_lock(&fs->mutex);
  int snapshot = m3_read_begin(fs);
  if (snapshot != M3FS_OK) {
    m3_mutex_unlock(&fs->mutex);
    return snapshot;
  }

  uint64_t inode = 0U;
  m3fs_stat stat;
  int status = m3_lookup_path(fs, path, &inode);
  if (status == M3FS_OK) status = m3_inode_stat(fs, inode, &stat);
  if (status == M3FS_OK && stat.kind == M3FS_KIND_DIRECTORY) {
    status = M3FS_IS_DIRECTORY;
  } else if (status == M3FS_OK && stat.kind != M3FS_KIND_FILE) {
    status = M3FS_INVALID;
  }
  if (status == M3FS_OK
      && stat.size > (uint64_t)M3FS_MAX_SEMANTIC_IMAGE_BYTES) {
    status = M3FS_NOSPACE;
  }

  unsigned char *owned = NULL;
  uint32_t length = 0U;
  if (status == M3FS_OK) {
    length = (uint32_t)stat.size;
    if (length != 0U) {
      owned = (unsigned char *)malloc((size_t)length);
      if (owned == NULL) {
        status = M3FS_NOMEM;
      } else {
        status = m3_read_range(fs, inode, UINT64_C(0), owned, length);
      }
    }
  }

  status = m3_finish(fs, status);
  if (status == M3FS_OK) {
    *out_bytes = owned;
    *out_length = length;
  } else {
    free(owned);
  }
  m3_mutex_unlock(&fs->mutex);
  return status;
}

int m3fs_truncate(m3fs *fs, const char *path, uint64_t size) {
  return m3_write_checked(fs, path, 0U, size, NULL, 0U, M3FS_WRITE_TRUNCATE, NULL);
}

int m3fs_set_metadata(
    m3fs *fs, const char *path, uint32_t mode, uint64_t mtime_ns, uint32_t flags) {
  if (fs == NULL || !m3_valid_path(path) || flags > 3U
      || ((flags & M3FS_METADATA_MODE) != 0U && mode > UINT32_C(07777))
      || ((flags & M3FS_METADATA_MTIME) != 0U && mtime_ns > (uint64_t)INT64_MAX)) {
    return M3FS_INVALID;
  }
  m3_mutex_lock(&fs->mutex);
  int status = m3_begin(fs);
  uint64_t inode = 0U;
  m3fs_stat stat;
  if (status == M3FS_OK) status = m3_lookup_path(fs, path, &inode);
  if (status == M3FS_OK) status = m3_inode_stat(fs, inode, &stat);
  if (status == M3FS_OK) {
    uint32_t next_mode = (flags & M3FS_METADATA_MODE) != 0U ? mode : stat.mode;
    uint64_t next_time = (flags & M3FS_METADATA_MTIME) != 0U ? mtime_ns : stat.mtime_ns;
    if (next_mode != stat.mode || next_time != stat.mtime_ns) {
      if (stat.generation >= (uint64_t)INT64_MAX) status = M3FS_NOSPACE;
      else {
        sqlite3_stmt *stmt = NULL;
        int rc = sqlite3_prepare_v2(fs->db,
            "UPDATE inodes SET mode=?1,mtime_ns=?2,generation=generation+1 WHERE id=?3",
            -1, &stmt, NULL);
        if (rc == SQLITE_OK) rc = sqlite3_bind_int(stmt, 1, (int)next_mode);
        if (rc == SQLITE_OK) rc = sqlite3_bind_int64(stmt, 2, (sqlite3_int64)next_time);
        if (rc == SQLITE_OK) rc = sqlite3_bind_int64(stmt, 3, (sqlite3_int64)inode);
        if (rc == SQLITE_OK) rc = sqlite3_step(stmt);
        status = rc == SQLITE_DONE && sqlite3_changes(fs->db) == 1 ? M3FS_OK
            : rc == SQLITE_DONE ? M3FS_NOT_FOUND : m3_status_sqlite(rc);
        sqlite3_finalize(stmt);
      }
    }
  }
  status = m3_finish(fs, status);
  m3_mutex_unlock(&fs->mutex);
  return status;
}

static int m3_block_is_zero(const unsigned char *block) {
  for (uint32_t index = 0U; index < M3FS_BLOCK_SIZE; ++index) {
    if (block[index] != 0U) return 0;
  }
  return 1;
}

static int m3_delete_block_ref(
    m3fs *fs,
    uint64_t inode,
    uint64_t block_no,
    int *changed) {
  sqlite3_stmt *stmt = NULL;
  int rc = sqlite3_prepare_v2(
      fs->db,
      "DELETE FROM file_blocks WHERE inode=?1 AND block_no=?2",
      -1,
      &stmt,
      NULL);
  if (rc == SQLITE_OK) rc = sqlite3_bind_int64(stmt, 1, (sqlite3_int64)inode);
  if (rc == SQLITE_OK) rc = sqlite3_bind_int64(stmt, 2, (sqlite3_int64)block_no);
  if (rc == SQLITE_OK) rc = sqlite3_step(stmt);
  int status = rc == SQLITE_DONE ? M3FS_OK : m3_status_sqlite(rc);
  if (status == M3FS_OK && sqlite3_changes(fs->db) != 0) *changed = 1;
  int finalized = sqlite3_finalize(stmt);
  return status == M3FS_OK ? m3_status_sqlite(finalized) : status;
}

static int m3_zero_block_range(
    m3fs *fs,
    uint64_t inode,
    uint64_t block_no,
    uint32_t start,
    uint32_t length,
    int *changed) {
  if (start > M3FS_BLOCK_SIZE
      || length > M3FS_BLOCK_SIZE - start) {
    return M3FS_INVALID;
  }
  if (length == 0U) return M3FS_OK;

  unsigned char *scratch = NULL;
  int status = m3_block_scratch(fs, &scratch);
  int present = 0;
  if (status == M3FS_OK) {
    status = m3_load_block_present(fs, inode, block_no, scratch, &present);
  }
  if (status != M3FS_OK || !present) return status;

  int range_changed = 0;
  for (uint32_t index = 0U; index < length; ++index) {
    if (scratch[start + index] != 0U) {
      range_changed = 1;
      break;
    }
  }
  if (range_changed) memset(scratch + start, 0, length);

  if (m3_block_is_zero(scratch)) {
    return m3_delete_block_ref(fs, inode, block_no, changed);
  }
  if (range_changed) {
    status = m3_store_block(fs, inode, block_no, scratch);
    if (status == M3FS_OK) *changed = 1;
  }
  return status;
}

static int m3_delete_block_span(
    m3fs *fs,
    uint64_t inode,
    uint64_t first_block,
    uint64_t end_block,
    int *changed) {
  if (first_block >= end_block) return M3FS_OK;
  sqlite3_stmt *stmt = NULL;
  int rc = sqlite3_prepare_v2(
      fs->db,
      "DELETE FROM file_blocks WHERE inode=?1 AND block_no>=?2 AND block_no<?3",
      -1,
      &stmt,
      NULL);
  if (rc == SQLITE_OK) rc = sqlite3_bind_int64(stmt, 1, (sqlite3_int64)inode);
  if (rc == SQLITE_OK) rc = sqlite3_bind_int64(stmt, 2, (sqlite3_int64)first_block);
  if (rc == SQLITE_OK) rc = sqlite3_bind_int64(stmt, 3, (sqlite3_int64)end_block);
  if (rc == SQLITE_OK) rc = sqlite3_step(stmt);
  int status = rc == SQLITE_DONE ? M3FS_OK : m3_status_sqlite(rc);
  if (status == M3FS_OK && sqlite3_changes(fs->db) != 0) *changed = 1;
  int finalized = sqlite3_finalize(stmt);
  return status == M3FS_OK ? m3_status_sqlite(finalized) : status;
}

int m3fs_punch_hole(
    m3fs *fs,
    const char *path,
    uint64_t offset,
    uint64_t length) {
  if (fs == NULL || !m3_valid_path(path) || offset > UINT64_MAX - length) {
    return M3FS_INVALID;
  }
  if (length == 0U) return M3FS_OK;

  m3_mutex_lock(&fs->mutex);
  int status = m3_begin(fs);
  uint64_t inode = 0U;
  m3fs_stat stat;
  if (status == M3FS_OK) status = m3_lookup_path(fs, path, &inode);
  if (status == M3FS_OK) status = m3_inode_stat(fs, inode, &stat);
  if (status == M3FS_OK && stat.kind == M3FS_KIND_DIRECTORY) {
    status = M3FS_IS_DIRECTORY;
  } else if (status == M3FS_OK && stat.kind != M3FS_KIND_FILE) {
    status = M3FS_INVALID;
  }

  int changed = 0;
  if (status == M3FS_OK && offset < stat.size) {
    uint64_t requested_end = offset + length;
    uint64_t end = requested_end < stat.size ? requested_end : stat.size;
    if (offset < end) {
      uint64_t first_block = offset / M3FS_BLOCK_SIZE;
      uint64_t last_block = (end - 1U) / M3FS_BLOCK_SIZE;
      uint32_t first_offset = (uint32_t)(offset % M3FS_BLOCK_SIZE);
      uint32_t end_offset = (uint32_t)(end % M3FS_BLOCK_SIZE);

      if (first_block == last_block) {
        status = m3_zero_block_range(
            fs,
            inode,
            first_block,
            first_offset,
            (uint32_t)(end - offset),
            &changed);
      } else {
        uint64_t full_start = first_block;
        if (first_offset != 0U) {
          status = m3_zero_block_range(
              fs,
              inode,
              first_block,
              first_offset,
              M3FS_BLOCK_SIZE - first_offset,
              &changed);
          full_start = first_block + 1U;
        }

        uint64_t full_end = end / M3FS_BLOCK_SIZE;
        if (status == M3FS_OK && end_offset != 0U) {
          status = m3_zero_block_range(
              fs,
              inode,
              last_block,
              0U,
              end_offset,
              &changed);
          full_end = last_block;
        }
        if (status == M3FS_OK) {
          status = m3_delete_block_span(fs, inode, full_start, full_end, &changed);
        }
      }
    }
  }

  if (status == M3FS_OK && changed) {
    status = m3_touch_inode(fs, inode, stat.size);
  }
  status = m3_finish(fs, status);
  m3_mutex_unlock(&fs->mutex);
  return status;
}

static int m3_emit_allocated_range(
    m3fs_range_callback callback,
    void *context,
    uint64_t request_start,
    uint64_t request_end,
    uint64_t first_block,
    uint64_t end_block) {
  if (first_block >= end_block) return M3FS_OK;
  uint64_t start = first_block * (uint64_t)M3FS_BLOCK_SIZE;
  uint64_t end = end_block * (uint64_t)M3FS_BLOCK_SIZE;
  if (start < request_start) start = request_start;
  if (end > request_end) end = request_end;
  if (start >= end) return M3FS_OK;
  return callback(context, start, end - start);
}

int m3fs_allocated_ranges(
    m3fs *fs,
    const char *path,
    uint64_t offset,
    uint64_t length,
    m3fs_range_callback callback,
    void *context) {
  if (fs == NULL
      || !m3_valid_path(path)
      || callback == NULL
      || offset > UINT64_MAX - length) {
    return M3FS_INVALID;
  }
  if (length == 0U) return M3FS_OK;

  m3_mutex_lock(&fs->mutex);
  int snapshot = m3_read_begin(fs);
  if (snapshot != M3FS_OK) {
    m3_mutex_unlock(&fs->mutex);
    return snapshot;
  }

  uint64_t inode = 0U;
  m3fs_stat stat;
  int status = m3_lookup_path(fs, path, &inode);
  if (status == M3FS_OK) status = m3_inode_stat(fs, inode, &stat);
  if (status == M3FS_OK && stat.kind == M3FS_KIND_DIRECTORY) {
    status = M3FS_IS_DIRECTORY;
  } else if (status == M3FS_OK && stat.kind != M3FS_KIND_FILE) {
    status = M3FS_INVALID;
  }

  uint64_t end = 0U;
  if (status == M3FS_OK) {
    uint64_t requested_end = offset + length;
    end = requested_end < stat.size ? requested_end : stat.size;
  }

  if (status == M3FS_OK && offset < end) {
    uint64_t first = offset / M3FS_BLOCK_SIZE;
    uint64_t last = (end - 1U) / M3FS_BLOCK_SIZE;
    sqlite3_stmt *stmt = NULL;
    int rc = sqlite3_prepare_v2(
        fs->db,
        "SELECT block_no FROM file_blocks"
        " WHERE inode=?1 AND block_no>=?2 AND block_no<=?3 ORDER BY block_no",
        -1,
        &stmt,
        NULL);
    if (rc == SQLITE_OK) rc = sqlite3_bind_int64(stmt, 1, (sqlite3_int64)inode);
    if (rc == SQLITE_OK) rc = sqlite3_bind_int64(stmt, 2, (sqlite3_int64)first);
    if (rc == SQLITE_OK) rc = sqlite3_bind_int64(stmt, 3, (sqlite3_int64)last);

    int have_run = 0;
    uint64_t run_first = 0U;
    uint64_t run_end = 0U;
    while (status == M3FS_OK && (rc == SQLITE_OK || rc == SQLITE_ROW)
        && (rc = sqlite3_step(stmt)) == SQLITE_ROW) {
      sqlite3_int64 raw = sqlite3_column_int64(stmt, 0);
      if (raw < 0) {
        status = M3FS_CORRUPT;
        break;
      }
      uint64_t block = (uint64_t)raw;
      if (!have_run) {
        run_first = block;
        run_end = block + 1U;
        have_run = 1;
      } else if (block == run_end) {
        run_end = block + 1U;
      } else if (block > run_end) {
        status = m3_emit_allocated_range(
            callback, context, offset, end, run_first, run_end);
        if (status == M3FS_OK) {
          run_first = block;
          run_end = block + 1U;
        }
      } else {
        status = M3FS_CORRUPT;
      }
    }
    if (status == M3FS_OK && rc != SQLITE_DONE) {
      status = m3_status_sqlite(rc);
    }
    if (status == M3FS_OK && have_run) {
      status = m3_emit_allocated_range(
          callback, context, offset, end, run_first, run_end);
    }
    int finalized = sqlite3_finalize(stmt);
    if (status == M3FS_OK) status = m3_status_sqlite(finalized);
  }

  status = m3_finish(fs, status);
  m3_mutex_unlock(&fs->mutex);
  return status;
}

int m3fs_unlink(m3fs *fs, const char *path) {
  if (fs == NULL || !m3_valid_path(path) || path[1] == '\0') return M3FS_INVALID;
  m3_mutex_lock(&fs->mutex);
  int status = m3_begin(fs);
  uint64_t parent = 0;
  uint64_t inode = 0;
  m3_name leaf;
  m3fs_stat stat;
  if (status == M3FS_OK) status = m3_lookup_parent(fs, path, &parent, &leaf);
  if (status == M3FS_OK) status = m3_lookup_child(fs, parent, leaf.data, leaf.bytes, &inode);
  if (status == M3FS_OK) status = m3_inode_stat(fs, inode, &stat);
  if (status == M3FS_OK && stat.kind == M3FS_KIND_DIRECTORY) {
    status = M3FS_IS_DIRECTORY;
  }
  if (status == M3FS_OK) status = m3_delete_dentry(fs, parent, leaf, inode);
  if (status == M3FS_OK) status = m3_decrement_link(fs, inode);
  status = m3_finish(fs, status);
  m3_mutex_unlock(&fs->mutex);
  return status;
}

int m3fs_rmdir(m3fs *fs, const char *path) {
  if (fs == NULL || !m3_valid_path(path) || path[1] == '\0') return M3FS_INVALID;
  m3_mutex_lock(&fs->mutex);
  int status = m3_begin(fs);
  uint64_t parent = 0;
  uint64_t inode = 0;
  m3_name leaf;
  m3fs_stat stat;
  if (status == M3FS_OK) status = m3_lookup_parent(fs, path, &parent, &leaf);
  if (status == M3FS_OK) status = m3_lookup_child(fs, parent, leaf.data, leaf.bytes, &inode);
  if (status == M3FS_OK) status = m3_inode_stat(fs, inode, &stat);
  if (status == M3FS_OK && stat.kind != M3FS_KIND_DIRECTORY) {
    status = M3FS_NOT_DIRECTORY;
  }
  int empty = 0;
  if (status == M3FS_OK) status = m3_directory_empty(fs, inode, &empty);
  if (status == M3FS_OK && !empty) status = M3FS_NOT_EMPTY;
  if (status == M3FS_OK) status = m3_delete_dentry(fs, parent, leaf, inode);
  if (status == M3FS_OK) status = m3_decrement_link(fs, inode);
  status = m3_finish(fs, status);
  m3_mutex_unlock(&fs->mutex);
  return status;
}

int m3fs_link(m3fs *fs, const char *existing, const char *new_path) {
  if (fs == NULL || !m3_valid_path(existing) || !m3_valid_path(new_path)
      || new_path[1] == '\0') {
    return M3FS_INVALID;
  }
  m3_mutex_lock(&fs->mutex);
  int status = m3_begin(fs);
  uint64_t inode = 0;
  m3fs_stat stat;
  uint64_t parent = 0;
  m3_name leaf;
  if (status == M3FS_OK) status = m3_lookup_path(fs, existing, &inode);
  if (status == M3FS_OK) status = m3_inode_stat(fs, inode, &stat);
  if (status == M3FS_OK && stat.kind == M3FS_KIND_DIRECTORY) {
    status = M3FS_INVALID;
  }
  if (status == M3FS_OK) status = m3_lookup_parent(fs, new_path, &parent, &leaf);
  if (status == M3FS_OK) status = m3_require_directory(fs, parent);
  uint64_t found = 0;
  if (status == M3FS_OK) {
    int exists = m3_lookup_child(fs, parent, leaf.data, leaf.bytes, &found);
    if (exists == M3FS_OK) status = M3FS_EXISTS;
    else if (exists != M3FS_NOT_FOUND) status = exists;
  }
  if (status == M3FS_OK) status = m3_insert_dentry(fs, parent, leaf, inode);
  if (status == M3FS_OK) {
    sqlite3_stmt *stmt = NULL;
    int rc = sqlite3_prepare_v2(
        fs->db,
        "UPDATE inodes SET nlink=nlink+1,generation=generation+1,mtime_ns=?1"
        " WHERE id=?2 AND nlink<2147483647",
        -1,
        &stmt,
        NULL);
    if (rc != SQLITE_OK) status = m3_status_sqlite(rc);
    else {
      (void)sqlite3_bind_int64(stmt, 1, (sqlite3_int64)m3_now_ns());
      (void)sqlite3_bind_int64(stmt, 2, (sqlite3_int64)inode);
      rc = sqlite3_step(stmt);
      if (rc != SQLITE_DONE || sqlite3_changes(fs->db) != 1) {
        status = rc == SQLITE_DONE ? M3FS_CORRUPT : m3_status_sqlite(rc);
      }
    }
    sqlite3_finalize(stmt);
  }
  status = m3_finish(fs, status);
  m3_mutex_unlock(&fs->mutex);
  return status;
}

int m3fs_symlink(
    m3fs *fs, const char *target, const char *link_path) {
  if (fs == NULL || target == NULL || !m3_valid_path(link_path)
      || link_path[1] == '\0') {
    return M3FS_INVALID;
  }
  size_t target_bytes = strlen(target);
  if (target_bytes == 0U || target_bytes > M3FS_MAX_PATH_BYTES
      || !m3_utf8_valid((const unsigned char *)target, (uint32_t)target_bytes)) {
    return M3FS_INVALID;
  }
  m3_mutex_lock(&fs->mutex);
  int status = m3_begin(fs);
  if (status == M3FS_OK) {
    status = m3_create_node(
        fs, link_path, M3FS_KIND_SYMLINK, UINT32_C(511), target);
    status = m3_finish(fs, status);
  }
  m3_mutex_unlock(&fs->mutex);
  return status;
}

int m3fs_readlink(
    m3fs *fs,
    const char *path,
    char *output,
    uint32_t capacity,
    uint32_t *target_bytes) {
  if (fs == NULL || !m3_valid_path(path) || target_bytes == NULL
      || (capacity != 0U && output == NULL)) {
    return M3FS_INVALID;
  }
  *target_bytes = 0U;
  m3_mutex_lock(&fs->mutex);
  int snapshot = m3_read_begin(fs);
  if (snapshot != M3FS_OK) {
    m3_mutex_unlock(&fs->mutex);
    return snapshot;
  }
  uint64_t inode = 0;
  m3fs_stat stat;
  int status = m3_lookup_path(fs, path, &inode);
  if (status == M3FS_OK) status = m3_inode_stat(fs, inode, &stat);
  if (status == M3FS_OK && stat.kind != M3FS_KIND_SYMLINK) {
    status = M3FS_INVALID;
  }
  sqlite3_stmt *stmt = NULL;
  if (status == M3FS_OK) {
    int rc = sqlite3_prepare_v2(
        fs->db,
        "SELECT target FROM symlinks WHERE inode=?1",
        -1,
        &stmt,
        NULL);
    if (rc != SQLITE_OK) status = m3_status_sqlite(rc);
    else {
      (void)sqlite3_bind_int64(stmt, 1, (sqlite3_int64)inode);
      rc = sqlite3_step(stmt);
      if (rc == SQLITE_ROW) {
        int bytes = sqlite3_column_bytes(stmt, 0);
        const void *data = sqlite3_column_blob(stmt, 0);
        if (bytes < 0 || data == NULL || (uint32_t)bytes > capacity) {
          status = (uint32_t)(bytes < 0 ? 0 : bytes) > capacity
              ? M3FS_NOSPACE
              : M3FS_CORRUPT;
        } else {
          if (bytes != 0) memcpy(output, data, (size_t)bytes);
          *target_bytes = (uint32_t)bytes;
        }
      } else if (rc == SQLITE_DONE) status = M3FS_CORRUPT;
      else status = m3_status_sqlite(rc);
    }
  }
  sqlite3_finalize(stmt);
  status = m3_finish(fs, status);
  m3_mutex_unlock(&fs->mutex);
  return status;
}

static int m3_rename(m3fs *fs, const char *from, const char *to, int no_replace) {
  if (fs == NULL || !m3_valid_path(from) || !m3_valid_path(to)
      || from[1] == '\0' || to[1] == '\0') {
    return M3FS_INVALID;
  }
  if (strcmp(from, to) == 0) return M3FS_OK;
  m3_mutex_lock(&fs->mutex);
  int status = m3_begin(fs);

  uint64_t source_parent = 0, target_parent = 0, source_inode = 0;
  m3_name source_leaf, target_leaf;
  m3fs_stat source_stat;
  if (status == M3FS_OK) status = m3_lookup_parent(fs, from, &source_parent, &source_leaf);
  if (status == M3FS_OK) {
    status = m3_lookup_child(fs, source_parent, source_leaf.data, source_leaf.bytes, &source_inode);
  }
  if (status == M3FS_OK) status = m3_inode_stat(fs, source_inode, &source_stat);
  if (status == M3FS_OK) status = m3_lookup_parent(fs, to, &target_parent, &target_leaf);
  if (status == M3FS_OK) status = m3_require_directory(fs, target_parent);

  if (status == M3FS_OK && source_stat.kind == M3FS_KIND_DIRECTORY) {
    int cycle = m3_would_cycle(fs, source_inode, target_parent);
    if (cycle > 0) status = M3FS_INVALID;
    else if (cycle < 0) status = -cycle;
  }

  uint64_t target_inode = 0;
  int target_lookup = M3FS_NOT_FOUND;
  if (status == M3FS_OK) {
    target_lookup =
        m3_lookup_child(fs, target_parent, target_leaf.data, target_leaf.bytes, &target_inode);
    if (target_lookup != M3FS_OK && target_lookup != M3FS_NOT_FOUND) {
      status = target_lookup;
    }
  }

  if (status == M3FS_OK && target_lookup == M3FS_OK && no_replace) {
    status = M3FS_EXISTS;
  }

  if (status == M3FS_OK && target_lookup == M3FS_OK) {
    if (target_inode == source_inode) {
      /*
       * POSIX rename(old,new) is a successful no-op when both names already
       * refer to the same inode. Retain both directory entries and nlink.
       */
      status = m3_finish(fs, M3FS_OK);
      m3_mutex_unlock(&fs->mutex);
      return status;
    }

    m3fs_stat target_stat;
    status = m3_inode_stat(fs, target_inode, &target_stat);
    if (status == M3FS_OK
        && source_stat.kind == M3FS_KIND_DIRECTORY
        && target_stat.kind != M3FS_KIND_DIRECTORY) {
      status = M3FS_NOT_DIRECTORY;
    } else if (status == M3FS_OK
        && source_stat.kind != M3FS_KIND_DIRECTORY
        && target_stat.kind == M3FS_KIND_DIRECTORY) {
      status = M3FS_IS_DIRECTORY;
    }
    if (status == M3FS_OK && target_stat.kind == M3FS_KIND_DIRECTORY) {
      int empty = 0;
      status = m3_directory_empty(fs, target_inode, &empty);
      if (status == M3FS_OK && !empty) status = M3FS_NOT_EMPTY;
    }
    if (status == M3FS_OK) {
      status = m3_delete_dentry(fs, target_parent, target_leaf, target_inode);
    }
    if (status == M3FS_OK) status = m3_decrement_link(fs, target_inode);
  }

  if (status == M3FS_OK) {
    sqlite3_stmt *stmt = NULL;
    int rc = sqlite3_prepare_v2(
        fs->db,
        "UPDATE dirents SET parent=?1,name=?2"
        " WHERE parent=?3 AND name=?4 AND child=?5",
        -1,
        &stmt,
        NULL);
    if (rc != SQLITE_OK) status = m3_status_sqlite(rc);
    else {
      (void)sqlite3_bind_int64(stmt, 1, (sqlite3_int64)target_parent);
      (void)sqlite3_bind_blob(
          stmt, 2, target_leaf.data, (int)target_leaf.bytes, SQLITE_TRANSIENT);
      (void)sqlite3_bind_int64(stmt, 3, (sqlite3_int64)source_parent);
      (void)sqlite3_bind_blob(
          stmt, 4, source_leaf.data, (int)source_leaf.bytes, SQLITE_TRANSIENT);
      (void)sqlite3_bind_int64(stmt, 5, (sqlite3_int64)source_inode);
      rc = sqlite3_step(stmt);
      if (rc != SQLITE_DONE || sqlite3_changes(fs->db) != 1) {
        status = rc == SQLITE_DONE ? M3FS_CORRUPT : m3_status_sqlite(rc);
      }
    }
    sqlite3_finalize(stmt);
  }

  status = m3_finish(fs, status);
  m3_mutex_unlock(&fs->mutex);
  return status;
}

int m3fs_stat_path(
    m3fs *fs, const char *path, m3fs_stat *out) {
  if (fs == NULL || out == NULL || !m3_valid_path(path)) return M3FS_INVALID;
  m3_mutex_lock(&fs->mutex);
  int snapshot = m3_read_begin(fs);
  if (snapshot != M3FS_OK) {
    m3_mutex_unlock(&fs->mutex);
    return snapshot;
  }
  uint64_t inode = 0;
  int status = m3_lookup_path(fs, path, &inode);
  if (status == M3FS_OK) status = m3_inode_stat(fs, inode, out);
  status = m3_finish(fs, status);
  m3_mutex_unlock(&fs->mutex);
  return status;
}

/* Shared decoder for legacy full listing and bounded pages. Pointers are borrowed. */
static int m3_emit_directory_row(
    sqlite3_stmt *stmt, m3fs_dir_callback callback, void *context) {
  const void *name = sqlite3_column_blob(stmt, 0);
  int name_bytes = sqlite3_column_bytes(stmt, 0);
  sqlite3_int64 id = sqlite3_column_int64(stmt, 1);
  sqlite3_int64 size = sqlite3_column_int64(stmt, 4);
  sqlite3_int64 generation = sqlite3_column_int64(stmt, 6);
  sqlite3_int64 mtime = sqlite3_column_int64(stmt, 7);
  if (name == NULL || name_bytes <= 0 || id <= 0 || size < 0 || generation < 1 || mtime < 0) {
    return M3FS_CORRUPT;
  }
  m3fs_stat item;
  item.inode = (uint64_t)id;
  int kind = sqlite3_column_int(stmt, 2);
  int mode = sqlite3_column_int(stmt, 3);
  int nlink = sqlite3_column_int(stmt, 5);
  if (kind < M3FS_KIND_FILE || kind > M3FS_KIND_SYMLINK
      || mode < 0 || mode > 07777 || nlink < 0) {
    return M3FS_CORRUPT;
  }
  item.kind = (uint32_t)kind;
  item.mode = (uint32_t)mode;
  item.size = (uint64_t)size;
  item.nlink = (uint32_t)nlink;
  item.generation = (uint64_t)generation;
  item.mtime_ns = (uint64_t)mtime;
  item.block_size = M3FS_BLOCK_SIZE;
  return callback(context, (const char *)name, (uint32_t)name_bytes, &item);
}

int m3fs_list(
    m3fs *fs,
    const char *path,
    m3fs_dir_callback callback,
    void *context) {
  if (fs == NULL || callback == NULL || !m3_valid_path(path)) return M3FS_INVALID;
  m3_mutex_lock(&fs->mutex);
  int snapshot = m3_read_begin(fs);
  if (snapshot != M3FS_OK) {
    m3_mutex_unlock(&fs->mutex);
    return snapshot;
  }
  uint64_t inode = 0;
  int status = m3_lookup_path(fs, path, &inode);
  if (status == M3FS_OK) status = m3_require_directory(fs, inode);

  sqlite3_stmt *stmt = NULL;
  if (status == M3FS_OK) {
    int rc = sqlite3_prepare_v2(
        fs->db,
        "SELECT d.name,i.id,i.kind,i.mode,i.size,i.nlink,i.generation,i.mtime_ns"
        " FROM dirents d JOIN inodes i ON i.id=d.child"
        " WHERE d.parent=?1 ORDER BY d.name",
        -1,
        &stmt,
        NULL);
    if (rc != SQLITE_OK) status = m3_status_sqlite(rc);
    else (void)sqlite3_bind_int64(stmt, 1, (sqlite3_int64)inode);
  }
  while (status == M3FS_OK) {
    int rc = sqlite3_step(stmt);
    if (rc == SQLITE_DONE) break;
    if (rc != SQLITE_ROW) {
      status = m3_status_sqlite(rc);
      break;
    }
    status = m3_emit_directory_row(stmt, callback, context);
  }
  sqlite3_finalize(stmt);
  status = m3_finish(fs, status);
  m3_mutex_unlock(&fs->mutex);
  return status;
}

int m3fs_checkpoint(m3fs *fs) {
  if (fs == NULL) return M3FS_INVALID;
  if (fs->read_only) return M3FS_READ_ONLY;
  m3_mutex_lock(&fs->mutex);
  int log_frames = 0;
  int checkpointed = 0;
  int rc = sqlite3_wal_checkpoint_v2(
      fs->db, NULL, SQLITE_CHECKPOINT_FULL, &log_frames, &checkpointed);
  (void)log_frames;
  (void)checkpointed;
  int status = m3_status_sqlite(rc);
  m3_mutex_unlock(&fs->mutex);
  return status;
}

static int m3_scalar_u64(sqlite3 *db, const char *sql, uint64_t *out) {
  sqlite3_stmt *stmt = NULL;
  int rc = sqlite3_prepare_v2(db, sql, -1, &stmt, NULL);
  if (rc != SQLITE_OK) return m3_status_sqlite(rc);
  rc = sqlite3_step(stmt);
  int status;
  if (rc == SQLITE_ROW) {
    sqlite3_int64 value = sqlite3_column_int64(stmt, 0);
    if (value < 0) status = M3FS_CORRUPT;
    else {
      *out = (uint64_t)value;
      status = M3FS_OK;
    }
  } else {
    status = rc == SQLITE_DONE ? M3FS_CORRUPT : m3_status_sqlite(rc);
  }
  sqlite3_finalize(stmt);
  return status;
}

int m3fs_get_stats(m3fs *fs, m3fs_stats *out) {
  if (fs == NULL || out == NULL) return M3FS_INVALID;
  memset(out, 0, sizeof(*out));
  m3_mutex_lock(&fs->mutex);
  int snapshot = m3_read_begin(fs);
  if (snapshot != M3FS_OK) {
    m3_mutex_unlock(&fs->mutex);
    return snapshot;
  }
  int status = m3_scalar_u64(fs->db, "SELECT count(*) FROM inodes", &out->inode_count);
  if (status == M3FS_OK) {
    status = m3_scalar_u64(fs->db, "SELECT count(*) FROM dirents", &out->dirent_count);
  }
  if (status == M3FS_OK) {
    status = m3_scalar_u64(
        fs->db,
        "SELECT COALESCE(sum(size),0) FROM inodes WHERE kind=1",
        &out->logical_bytes);
  }
  if (status == M3FS_OK) {
    status = m3_scalar_u64(fs->db, "SELECT count(*) FROM blocks", &out->unique_blocks);
  }
  if (status == M3FS_OK) {
    status = m3_scalar_u64(
        fs->db, "SELECT count(*) FROM file_blocks", &out->block_references);
  }
  if (status == M3FS_OK) {
    status = m3_scalar_u64(
        fs->db, "SELECT COALESCE(sum(length(data)),0) FROM blocks",
        &out->physical_block_bytes);
  }
  status = m3_finish(fs, status);
  m3_mutex_unlock(&fs->mutex);
  return status;
}

static int m3_expect_zero(sqlite3 *db, const char *sql) {
  uint64_t value = 0;
  int status = m3_scalar_u64(db, sql, &value);
  if (status != M3FS_OK) return status;
  return value == 0U ? M3FS_OK : M3FS_CORRUPT;
}

int m3fs_scrub(
    m3fs *fs, uint32_t flags, m3fs_scrub_report *out) {
  if (fs == NULL || out == NULL
      || (flags & ~(uint32_t)M3FS_SCRUB_BACKFILL_MISSING) != 0U) {
    return M3FS_INVALID;
  }
  memset(out, 0, sizeof(*out));
  int backfill = (flags & M3FS_SCRUB_BACKFILL_MISSING) != 0U;
  if (backfill && fs->read_only) return M3FS_READ_ONLY;

  m3_mutex_lock(&fs->mutex);
  int status = backfill ? m3_begin(fs) : m3_read_begin(fs);
  int table_exists = 0;
  if (status == M3FS_OK) {
    status = m3_integrity_table_exists(fs->db, &table_exists);
  }
  if (status == M3FS_OK && backfill && !table_exists) {
    status = m3_exec(fs->db, M3_INTEGRITY_SCHEMA_SQL);
    if (status == M3FS_OK) table_exists = 1;
  }

  sqlite3_stmt *stmt = NULL;
  if (status == M3FS_OK) {
    const char *sql = table_exists
        ? "SELECT b.id,b.data,i.crc32c FROM blocks b"
          " LEFT JOIN block_integrity i ON i.block_id=b.id ORDER BY b.id"
        : "SELECT id,data,NULL FROM blocks ORDER BY id";
    int rc = sqlite3_prepare_v2(fs->db, sql, -1, &stmt, NULL);
    if (rc != SQLITE_OK) status = m3_status_sqlite(rc);
  }

  while (status == M3FS_OK) {
    int rc = sqlite3_step(stmt);
    if (rc == SQLITE_DONE) break;
    if (rc != SQLITE_ROW) {
      status = m3_status_sqlite(rc);
      break;
    }
    sqlite3_int64 block_id = sqlite3_column_int64(stmt, 0);
    const unsigned char *data = (const unsigned char *)sqlite3_column_blob(stmt, 1);
    int bytes = sqlite3_column_bytes(stmt, 1);
    if (block_id <= 0 || data == NULL || bytes != (int)M3FS_BLOCK_SIZE) {
      status = M3FS_CORRUPT;
      break;
    }
    out->blocks_scanned++;
    uint32_t actual = m3_crc32c(data, M3FS_BLOCK_SIZE);
    if (sqlite3_column_type(stmt, 2) == SQLITE_NULL) {
      out->checksums_missing++;
      if (backfill) {
        status = m3_integrity_insert_missing(fs, block_id, actual);
        if (status != M3FS_OK) break;
        out->checksums_backfilled++;
      }
      continue;
    }
    sqlite3_int64 stored = sqlite3_column_int64(stmt, 2);
    if (stored < 0 || (uint64_t)stored > UINT32_MAX) {
      status = M3FS_CORRUPT;
      break;
    }
    if ((uint32_t)stored == actual) out->checksums_verified++;
    else out->checksums_mismatched++;
  }

  int finalized = sqlite3_finalize(stmt);
  if (status == M3FS_OK && finalized != SQLITE_OK) status = m3_status_sqlite(finalized);
  status = m3_finish(fs, status);
  m3_mutex_unlock(&fs->mutex);
  return status;
}

int m3fs_verify(m3fs *fs) {
  if (fs == NULL) return M3FS_INVALID;
  m3_mutex_lock(&fs->mutex);
  int snapshot = m3_read_begin(fs);
  if (snapshot != M3FS_OK) {
    m3_mutex_unlock(&fs->mutex);
    return snapshot;
  }
  sqlite3_stmt *stmt = NULL;
  int rc = sqlite3_prepare_v2(fs->db, "PRAGMA quick_check", -1, &stmt, NULL);
  int status = rc == SQLITE_OK ? M3FS_OK : m3_status_sqlite(rc);
  if (status == M3FS_OK) {
    rc = sqlite3_step(stmt);
    if (rc != SQLITE_ROW) status = m3_status_sqlite(rc);
    else {
      const unsigned char *text = sqlite3_column_text(stmt, 0);
      if (text == NULL || strcmp((const char *)text, "ok") != 0) status = M3FS_CORRUPT;
    }
  }
  sqlite3_finalize(stmt);

  if (status == M3FS_OK) {
    rc = sqlite3_prepare_v2(fs->db, "PRAGMA foreign_key_check", -1, &stmt, NULL);
    if (rc != SQLITE_OK) status = m3_status_sqlite(rc);
    else {
      rc = sqlite3_step(stmt);
      if (rc == SQLITE_ROW) status = M3FS_CORRUPT;
      else if (rc != SQLITE_DONE) status = m3_status_sqlite(rc);
    }
    sqlite3_finalize(stmt);
  }
  if (status == M3FS_OK) {
    status = m3_expect_zero(
        fs->db,
        "SELECT count(*) FROM file_blocks f JOIN blocks b ON b.id=f.block_id"
        " WHERE length(b.data)<>65536");
  }
  if (status == M3FS_OK) {
    status = m3_expect_zero(
        fs->db,
        "SELECT count(*) FROM dirents d JOIN inodes p ON p.id=d.parent WHERE p.kind<>2");
  }
  if (status == M3FS_OK) {
    status = m3_expect_zero(
        fs->db,
        "SELECT count(*) FROM inodes i WHERE i.id<>1"
        " AND i.nlink<>(SELECT count(*) FROM dirents d WHERE d.child=i.id)");
  }
  if (status == M3FS_OK) {
    status = m3_expect_zero(
        fs->db,
        "SELECT count(*) FROM symlinks s JOIN inodes i ON i.id=s.inode WHERE i.kind<>3");
  }
  if (status == M3FS_OK) {
    status = m3_expect_zero(
        fs->db,
        "SELECT count(*) FROM blocks b"
        " WHERE NOT EXISTS(SELECT 1 FROM file_blocks f WHERE f.block_id=b.id)");
  }
  status = m3_finish(fs, status);
  m3_mutex_unlock(&fs->mutex);
  return status;
}


/* Only reference rows are copied. Existing COW writes own payload admission and GC. */
static int m3_clone_block_refs(m3fs *fs, uint64_t source, uint64_t destination) {
  sqlite3_stmt *stmt = NULL;
  int rc = sqlite3_prepare_v2(fs->db,
      "INSERT INTO file_blocks(inode,block_no,block_id)"
      " SELECT ?1,block_no,block_id FROM file_blocks WHERE inode=?2",
      -1, &stmt, NULL);
  if (rc == SQLITE_OK) rc = sqlite3_bind_int64(stmt, 1, (sqlite3_int64)destination);
  if (rc == SQLITE_OK) rc = sqlite3_bind_int64(stmt, 2, (sqlite3_int64)source);
  if (rc == SQLITE_OK) rc = sqlite3_step(stmt);
  int status = rc == SQLITE_DONE ? M3FS_OK : m3_status_sqlite(rc);
  int finished = sqlite3_finalize(stmt);
  return status == M3FS_OK ? m3_status_sqlite(finished) : status;
}

static int m3_clone_file_in_transaction(
    m3fs *fs, const char *from, const char *to, uint64_t *destination) {
  uint64_t source = 0U, parent = 0U, existing = 0U;
  m3fs_stat stat;
  m3_name leaf;
  int status = m3_lookup_path(fs, from, &source);
  if (status == M3FS_OK) status = m3_inode_stat(fs, source, &stat);
  if (status == M3FS_OK && stat.kind != M3FS_KIND_FILE) {
    return stat.kind == M3FS_KIND_DIRECTORY
        ? M3FS_IS_DIRECTORY : M3FS_INVALID;
  }
  if (status == M3FS_OK) status = m3_lookup_parent(fs, to, &parent, &leaf);
  if (status == M3FS_OK) status = m3_require_directory(fs, parent);
  if (status != M3FS_OK) return status;
  status = m3_lookup_child(fs, parent, leaf.data, leaf.bytes, &existing);
  if (status == M3FS_OK) return M3FS_EXISTS;
  if (status != M3FS_NOT_FOUND) return status;
  status = m3_insert_inode(fs, M3FS_KIND_FILE, stat.mode, stat.size, destination);
  if (status == M3FS_OK) status = m3_insert_dentry(fs, parent, leaf, *destination);
  if (status == M3FS_OK) status = m3_clone_block_refs(fs, source, *destination);
  return status;
}

int m3fs_clone_file(
    m3fs *fs, const char *from, const char *to, uint64_t *out_inode) {
  if (out_inode == NULL) return M3FS_INVALID;
  *out_inode = 0U;
  if (fs == NULL || !m3_valid_path(from) || !m3_valid_path(to) || to[1] == '\0') {
    return M3FS_INVALID;
  }
  m3_mutex_lock(&fs->mutex);
  uint64_t inode = 0U;
  int status = m3_begin(fs);
  if (status == M3FS_OK) {
    status = m3_clone_file_in_transaction(fs, from, to, &inode);
    status = m3_finish(fs, status);
  }
  if (status == M3FS_OK) *out_inode = inode;
  m3_mutex_unlock(&fs->mutex);
  return status;
}

static int m3_valid_page_cursor(const char *name) {
  if (name == NULL || name[0] == '\0') return 1;
  size_t bytes = strlen(name);
  return bytes <= M3FS_MAX_NAME_BYTES && strchr(name, '/') == NULL
      && strcmp(name, ".") != 0 && strcmp(name, "..") != 0
      && m3_utf8_valid((const unsigned char *)name, (uint32_t)bytes);
}

static int m3_prepare_directory_page(
    m3fs *fs, uint64_t inode, const char *after, uint32_t limit,
    sqlite3_stmt **stmt) {
  int status = m3_cached_statement(fs, &fs->directory_page,
      "SELECT d.name,i.id,i.kind,i.mode,i.size,i.nlink,i.generation,i.mtime_ns"
      " FROM dirents d JOIN inodes i ON i.id=d.child"
      " WHERE d.parent=?1 AND d.name>?2 ORDER BY d.name LIMIT ?3", stmt);
  if (status != M3FS_OK) return status;
  int rc = sqlite3_bind_int64(*stmt, 1, (sqlite3_int64)inode);
  if (rc == SQLITE_OK) {
    rc = sqlite3_bind_blob(*stmt, 2, after, (int)strlen(after), SQLITE_TRANSIENT);
  }
  if (rc == SQLITE_OK) rc = sqlite3_bind_int(*stmt, 3, (int)limit + 1);
  return m3_status_sqlite(rc);
}

static int m3_collect_directory_page(
    sqlite3_stmt *stmt, uint32_t limit, m3fs_dir_callback callback,
    void *context, uint32_t *count, uint32_t *more) {
  for (;;) {
    int rc = sqlite3_step(stmt);
    if (rc == SQLITE_DONE) return M3FS_OK;
    if (rc != SQLITE_ROW) return m3_status_sqlite(rc);
    if (*count == limit) {
      *more = 1U;
      return M3FS_OK;
    }
    /* Enforce the page allocation bound even for a database modified outside this API. */
    int bytes = sqlite3_column_bytes(stmt, 0);
    if (bytes < 1 || bytes > (int)M3FS_MAX_NAME_BYTES) return M3FS_CORRUPT;
    int status = m3_emit_directory_row(stmt, callback, context);
    if (status != M3FS_OK) return status;
    ++*count;
  }
}

int m3fs_list_page(
    m3fs *fs, const char *path, const char *after_name, uint32_t limit,
    m3fs_dir_callback callback, void *context,
    uint32_t *entry_count, uint32_t *has_more) {
  if (entry_count == NULL || has_more == NULL) return M3FS_INVALID;
  *entry_count = 0U;
  *has_more = 0U;
  if (fs == NULL || callback == NULL || !m3_valid_path(path)
      || !m3_valid_page_cursor(after_name) || limit == 0U
      || limit > M3FS_MAX_PAGE_ENTRIES) return M3FS_INVALID;
  const char *after = after_name == NULL ? "" : after_name;
  m3_mutex_lock(&fs->mutex);
  int status = m3_read_begin(fs);
  if (status == M3FS_OK) {
    uint64_t inode = 0U;
    uint32_t count = 0U, more = 0U;
    sqlite3_stmt *stmt = NULL;
    status = m3_lookup_path(fs, path, &inode);
    if (status == M3FS_OK) status = m3_require_directory(fs, inode);
    if (status == M3FS_OK) {
      status = m3_prepare_directory_page(fs, inode, after, limit, &stmt);
    }
    if (status == M3FS_OK) {
      status = m3_collect_directory_page(stmt, limit, callback, context, &count, &more);
    }
    status = m3_reset_statement(stmt, status);
    status = m3_finish(fs, status);
    if (status == M3FS_OK) {
      *entry_count = count;
      *has_more = more;
    }
  }
  m3_mutex_unlock(&fs->mutex);
  return status;
}

int m3fs_rename(m3fs *fs, const char *from, const char *to) {
  return m3_rename(fs, from, to, 0);
}

int m3fs_rename_noreplace(m3fs *fs, const char *from, const char *to) {
  return m3_rename(fs, from, to, 1);
}
