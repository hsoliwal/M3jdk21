// SPDX-License-Identifier: Apache-2.0
#include "m3fs.h"
#include <sqlite3.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static unsigned checks;
#define CHECK(c) do { ++checks; if (!(c)) { fprintf(stderr, "FAIL line %d: %s\n", __LINE__, #c); exit(1); } } while (0)
#define OK(c) CHECK((c) == M3FS_OK)
#define SNAP_FLAGS ((uint32_t)(M3FS_OPEN_READ_ONLY | M3FS_OPEN_READ_SNAPSHOT))

typedef struct entries { unsigned count; char last[256]; } entries;
static int collect(void *context, const char *name, uint32_t bytes, const m3fs_stat *stat) {
  entries *e = (entries *)context;
  CHECK(bytes < sizeof(e->last) && stat->inode > 0U);
  memcpy(e->last, name, bytes); e->last[bytes] = '\0'; ++e->count;
  return M3FS_OK;
}
static int abort_page(void *context, const char *name, uint32_t bytes, const m3fs_stat *stat) {
  (void)context; (void)name; (void)bytes; (void)stat;
  return M3FS_NOMEM;
}
static void original(m3fs *fs, const unsigned char *expected, uint32_t length) {
  unsigned char *data = (unsigned char *)malloc(length);
  CHECK(data != NULL);
  uint32_t count = 0U;
  OK(m3fs_read(fs, "/d/source", 0U, data, length, &count));
  CHECK(count == length && memcmp(data, expected, length) == 0);
  free(data);
}
static void rejected_writes(m3fs *fs) {
  uint64_t inode = 0U;
  CHECK(m3fs_create(fs, "/new", 0600U) == M3FS_READ_ONLY);
  CHECK(m3fs_mkdir(fs, "/new", 0700U) == M3FS_READ_ONLY);
  CHECK(m3fs_write(fs, "/d/source", 0U, "x", 1U, NULL) == M3FS_READ_ONLY);
  CHECK(m3fs_truncate(fs, "/d/source", 0U) == M3FS_READ_ONLY);
  CHECK(m3fs_unlink(fs, "/d/source") == M3FS_READ_ONLY);
  CHECK(m3fs_rmdir(fs, "/d") == M3FS_READ_ONLY);
  CHECK(m3fs_rename(fs, "/d/source", "/other") == M3FS_READ_ONLY);
  CHECK(m3fs_link(fs, "/d/source", "/other") == M3FS_READ_ONLY);
  CHECK(m3fs_symlink(fs, "/d/source", "/other") == M3FS_READ_ONLY);
  CHECK(m3fs_clone_file(fs, "/d/source", "/other", &inode) == M3FS_READ_ONLY);
  CHECK(inode == 0U);
  CHECK(m3fs_checkpoint(fs) == M3FS_READ_ONLY);
}
int main(int argc, char **argv) {
  CHECK(argc == 2);
  m3fs *writer = NULL, *view = NULL, *live = NULL, *second = NULL;
  OK(m3fs_open(argv[1], M3FS_OPEN_CREATE, &writer));
  CHECK((m3fs_capabilities() & 7U) == 7U);
  OK(m3fs_mkdir(writer, "/d", 0755U));
  OK(m3fs_create(writer, "/d/source", 0640U));
  const uint32_t length = M3FS_BLOCK_SIZE * 2U + 17U;
  unsigned char *source = (unsigned char *)malloc(length);
  CHECK(source != NULL);
  for (uint32_t i = 0U; i < length; ++i) source[i] = (unsigned char)((i * 17U + i / 65536U) & 255U);
  OK(m3fs_write(writer, "/d/source", 0U, source, length, NULL));
  OK(m3fs_symlink(writer, "/d/source", "/d/link"));
  for (unsigned i = 0U; i < 32U; ++i) {
    char path[32]; (void)snprintf(path, sizeof(path), "/d/n%03u", i);
    OK(m3fs_create(writer, path, 0600U));
  }
  m3fs_stats before, after;
  m3fs_stat old_stat, current;
  OK(m3fs_get_stats(writer, &before));
  OK(m3fs_stat_path(writer, "/d/source", &old_stat));
  OK(m3fs_open(argv[1], SNAP_FLAGS, &view));
  OK(m3fs_open(argv[1], M3FS_OPEN_READ_ONLY, &live));
  /* No read through view has happened yet: open itself must have pinned the view. */
  OK(m3fs_write(writer, "/d/source", 0U, "changed", 7U, NULL));
  original(view, source, length);
  unsigned char small[16]; uint32_t got = 0U;
  OK(m3fs_read(live, "/d/source", 0U, small, 7U, &got));
  CHECK(got == 7U && memcmp(small, "changed", 7U) == 0);
  OK(m3fs_open(argv[1], SNAP_FLAGS, &second));
  OK(m3fs_unlink(writer, "/d/source"));
  OK(m3fs_unlink(writer, "/d/link"));
  OK(m3fs_get_stats(writer, &after)); CHECK(after.unique_blocks == 0U);
  original(view, source, length); /* Old blocks still readable after logical garbage collection. */
  OK(m3fs_read(second, "/d/source", 0U, small, 7U, &got));
  CHECK(got == 7U && memcmp(small, "changed", 7U) == 0);
  CHECK(m3fs_stat_path(live, "/d/source", &current) == M3FS_NOT_FOUND);
  OK(m3fs_stat_path(view, "/d/source", &current));
  CHECK(current.inode == old_stat.inode && current.generation == old_stat.generation
      && current.size == old_stat.size && current.mtime_ns == old_stat.mtime_ns);
  OK(m3fs_readlink(view, "/d/link", (char *)small, sizeof(small), &got));
  CHECK(got == 9U && memcmp(small, "/d/source", 9U) == 0);
  OK(m3fs_get_stats(view, &after));
  CHECK(after.inode_count == before.inode_count && after.block_references == before.block_references
      && after.unique_blocks == before.unique_blocks && after.logical_bytes == before.logical_bytes);
  rejected_writes(view);
  CHECK(m3fs_stat_path(view, "/missing", &current) == M3FS_NOT_FOUND);
  CHECK(m3fs_read(view, "/missing", 0U, small, sizeof(small), &got) == M3FS_NOT_FOUND);
  uint32_t count = 99U, more = 99U;
  CHECK(m3fs_list_page(view, "/d", NULL, 2U, abort_page, NULL, &count, &more) == M3FS_NOMEM);
  CHECK(count == 0U && more == 0U);
  original(view, source, length);
  entries e = {0U, ""};
  OK(m3fs_list(view, "/d", collect, &e)); CHECK(e.count == 34U);
  e.count = 0U; char cursor[256] = "";
  do {
    OK(m3fs_list_page(view, "/d", cursor, 3U, collect, &e, &count, &more));
    CHECK(count <= 3U);
    if (count != 0U) memcpy(cursor, e.last, sizeof(cursor));
    if (e.count == 3U) {
      for (unsigned i = 0U; i < 32U; ++i) {
        char path[32]; (void)snprintf(path, sizeof(path), "/d/n%03u", i);
        OK(m3fs_unlink(writer, path));
      }
      OK(m3fs_rmdir(writer, "/d"));
    }
  } while (more != 0U);
  CHECK(e.count == 34U);
  OK(m3fs_verify(view)); OK(m3fs_verify(second)); OK(m3fs_verify(writer));
  m3fs_close(live); m3fs_close(second);
  sqlite3 *db = NULL; CHECK(sqlite3_open(argv[1], &db) == SQLITE_OK);
  CHECK(sqlite3_busy_timeout(db, 0) == SQLITE_OK);
  /* A connection that has not touched WAL may have no journal context: read metadata first. */
  CHECK(sqlite3_exec(db, "SELECT count(*) FROM inodes", NULL, NULL, NULL) == SQLITE_OK);
  CHECK((sqlite3_wal_checkpoint_v2(db, NULL, SQLITE_CHECKPOINT_TRUNCATE, NULL, NULL) & 255) == SQLITE_BUSY);
  m3fs_close(view);
  CHECK(sqlite3_wal_checkpoint_v2(db, NULL, SQLITE_CHECKPOINT_TRUNCATE, NULL, NULL) == SQLITE_OK);
  CHECK(sqlite3_close(db) == SQLITE_OK);
  for (unsigned i = 0U; i < 128U; ++i) {
    OK(m3fs_open(argv[1], SNAP_FLAGS, &view));
    OK(m3fs_verify(view)); m3fs_close(view);
  }
  view = NULL;
  CHECK(m3fs_open(argv[1], M3FS_OPEN_READ_SNAPSHOT, &view) == M3FS_INVALID);
  CHECK(m3fs_open(argv[1], SNAP_FLAGS | M3FS_OPEN_CREATE, &view) == M3FS_INVALID);
  CHECK(view == NULL);
  free(source); m3fs_close(writer);
  printf("PASS retained-native checks=%u eager=YES cross-page=YES gc=YES release=YES\n", checks);
  return 0;
}
