// SPDX-License-Identifier: Apache-2.0
#include "m3fs.h"
#include <pthread.h>
#include <sqlite3.h>
#include <stdio.h>
#include <string.h>
#define CHECK(x) do { ++checks; if (!(x)) { fprintf(stderr, "FAIL line %d: %s\n", __LINE__, #x); return 1; } } while (0)
#define OK(x) CHECK((x) == M3FS_OK)
typedef struct append_work { m3fs *fs; uint64_t inode; unsigned char byte; int status; } append_work;
static void *append_many(void *context) {
  append_work *work = (append_work *)context;
  work->status = M3FS_OK;
  for (unsigned i = 0U; i < 256U && work->status == M3FS_OK; ++i) {
    work->status = m3fs_write_if_inode(work->fs, "/data", work->inode, 0U,
        &work->byte, 1U, M3FS_WRITE_APPEND, NULL);
  }
  return NULL;
}
int main(int argc, char **argv) {
  unsigned checks = 0U;
  CHECK(argc == 2);
  m3fs *fs = NULL, *other = NULL, *snapshot = NULL;
  OK(m3fs_open(argv[1], M3FS_OPEN_CREATE, &fs));
  OK(m3fs_create(fs, "/data", 0644U));
  OK(m3fs_write(fs, "/data", 0U, "old", 3U, NULL));
  m3fs_stat original, current, after;
  OK(m3fs_stat_path(fs, "/data", &original));
  OK(m3fs_open(argv[1], M3FS_OPEN_READ_ONLY | M3FS_OPEN_READ_SNAPSHOT, &snapshot));
  OK(m3fs_open(argv[1], 0U, &other));
  OK(m3fs_unlink(other, "/data"));
  OK(m3fs_create(other, "/data", 0644U));
  OK(m3fs_stat_path(fs, "/data", &current));
  CHECK(current.inode != original.inode);
  uint32_t read = 77U; unsigned char bytes[600] = {0};
  CHECK(m3fs_read_if_inode(fs, "/data", original.inode, 0U, NULL, 0U, &read) == M3FS_STALE);
  CHECK(read == 0U);
  for (uint32_t flags = 0U; flags <= 2U; ++flags) {
    CHECK(m3fs_write_if_inode(fs, "/data", original.inode, 0U, NULL, 0U, flags, NULL) == M3FS_STALE);
  }
  OK(m3fs_read_if_inode(snapshot, "/data", original.inode, 0U, bytes, sizeof(bytes), &read));
  CHECK(read == 3U && memcmp(bytes, "old", 3U) == 0);
  OK(m3fs_link(fs, "/data", "/alias"));
  OK(m3fs_stat_path(fs, "/data", &current));
  OK(m3fs_write_if_inode(fs, "/alias", current.inode, 5U, NULL, 0U, M3FS_WRITE_TRUNCATE, NULL));
  OK(m3fs_stat_path(fs, "/data", &after));
  CHECK(after.size == 5U && after.generation == current.generation + 1U);
  OK(m3fs_write_if_inode(fs, "/data", current.inode, 5U, NULL, 0U, M3FS_WRITE_TRUNCATE, NULL));
  OK(m3fs_stat_path(fs, "/data", &after));
  CHECK(after.generation == current.generation + 1U);
  OK(m3fs_write_if_inode(fs, "/data", current.inode, 0U, "base", 4U, M3FS_WRITE_TRUNCATE, NULL));
  OK(m3fs_stat_path(fs, "/data", &current));
  sqlite3 *db = NULL;
  CHECK(sqlite3_open(argv[1], &db) == SQLITE_OK);
  CHECK(sqlite3_exec(db, "CREATE TRIGGER deny_checked BEFORE INSERT ON blocks BEGIN SELECT RAISE(ABORT,'test'); END", NULL, NULL, NULL) == SQLITE_OK);
  CHECK(m3fs_write_if_inode(fs, "/data", current.inode, 0U, "failure", 7U, M3FS_WRITE_TRUNCATE, NULL) != M3FS_OK);
  OK(m3fs_stat_path(fs, "/data", &after));
  CHECK(after.size == 4U && after.generation == current.generation);
  OK(m3fs_read_if_inode(fs, "/data", current.inode, 0U, bytes, sizeof(bytes), &read));
  CHECK(read == 4U && memcmp(bytes, "base", 4U) == 0);
  CHECK(sqlite3_exec(db, "DROP TRIGGER deny_checked", NULL, NULL, NULL) == SQLITE_OK);
  CHECK(sqlite3_close(db) == SQLITE_OK);
  OK(m3fs_write_if_inode(fs, "/data", current.inode, 0U, NULL, 0U, M3FS_WRITE_TRUNCATE, NULL));
  append_work a = {fs, current.inode, 'A', M3FS_OK}, b = {other, current.inode, 'B', M3FS_OK};
  pthread_t first, second;
  CHECK(pthread_create(&first, NULL, append_many, &a) == 0);
  CHECK(pthread_create(&second, NULL, append_many, &b) == 0);
  CHECK(pthread_join(first, NULL) == 0 && pthread_join(second, NULL) == 0);
  CHECK(a.status == M3FS_OK && b.status == M3FS_OK);
  OK(m3fs_read_if_inode(fs, "/data", current.inode, 0U, bytes, sizeof(bytes), &read));
  CHECK(read == 512U);
  unsigned count_a = 0U, count_b = 0U;
  for (uint32_t i = 0U; i < read; ++i) { if (bytes[i] == 'A') ++count_a; if (bytes[i] == 'B') ++count_b; }
  CHECK(count_a == 256U && count_b == 256U);
  CHECK(m3fs_write_if_inode(fs, "/data", current.inode, 0U, NULL, 0U, 3U, NULL) == M3FS_INVALID);
  OK(m3fs_create(fs, "/destination", 0644U));
  OK(m3fs_write(fs, "/destination", 0U, "safe", 4U, NULL));
  m3fs_stat destination;
  OK(m3fs_stat_path(fs, "/destination", &destination));
  CHECK(m3fs_rename_noreplace(fs, "/data", "/destination") == M3FS_EXISTS);
  OK(m3fs_stat_path(fs, "/destination", &after));
  CHECK(after.inode == destination.inode && after.size == 4U);
  OK(m3fs_read(fs, "/destination", 0U, bytes, sizeof(bytes), &read));
  CHECK(read == 4U && memcmp(bytes, "safe", 4U) == 0);
  OK(m3fs_stat_path(fs, "/data", &after));
  CHECK(after.inode == current.inode);
  OK(m3fs_rename_noreplace(fs, "/destination", "/moved"));
  OK(m3fs_stat_path(fs, "/moved", &after));
  CHECK(after.inode == destination.inode);
  OK(m3fs_verify(fs));
  m3fs_close(snapshot); m3fs_close(other); m3fs_close(fs);
  printf("PASS checked I/O checks=%u atomic-appends=512\n", checks);
  return 0;
}
