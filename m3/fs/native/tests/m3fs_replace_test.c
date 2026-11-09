// SPDX-License-Identifier: Apache-2.0
#include "m3fs.h"
#include <sqlite3.h>
#include <stdio.h>
#include <string.h>
#define CHECK(x) do { ++checks; if (!(x)) { fprintf(stderr, "FAIL line %d: %s\n", __LINE__, #x); return 1; } } while (0)
#define OK(x) CHECK((x) == M3FS_OK)
int main(int argc, char **argv) {
  unsigned checks = 0U;
  CHECK(argc == 2);
  m3fs *fs = NULL, *snapshot = NULL;
  OK(m3fs_open(argv[1], M3FS_OPEN_CREATE, &fs));
  OK(m3fs_create(fs, "/f", 0644U));
  OK(m3fs_write(fs, "/f", 0U, "old bytes", 9U, NULL));
  OK(m3fs_link(fs, "/f", "/link"));
  m3fs_stat a, b;
  OK(m3fs_stat_path(fs, "/f", &a));
  OK(m3fs_open(argv[1], M3FS_OPEN_READ_ONLY | M3FS_OPEN_READ_SNAPSHOT, &snapshot));
  uint64_t size = 0U; uint32_t read = 0U; char bytes[32] = {0};
  OK(m3fs_replace(fs, "/f", "NEW", 3U, &size));
  CHECK(size == 3U);
  OK(m3fs_stat_path(fs, "/link", &b));
  CHECK(b.inode == a.inode && b.size == 3U && b.generation == a.generation + 1U);
  OK(m3fs_read(fs, "/link", 0U, bytes, sizeof(bytes), &read));
  CHECK(read == 3U && memcmp(bytes, "NEW", 3U) == 0);
  OK(m3fs_read(snapshot, "/f", 0U, bytes, sizeof(bytes), &read));
  CHECK(read == 9U && memcmp(bytes, "old bytes", 9U) == 0);
  sqlite3 *db = NULL;
  CHECK(sqlite3_open(argv[1], &db) == SQLITE_OK);
  CHECK(sqlite3_exec(db, "CREATE TRIGGER deny_replace BEFORE INSERT ON blocks BEGIN SELECT RAISE(ABORT,'test'); END", NULL, NULL, NULL) == SQLITE_OK);
  CHECK(m3fs_replace(fs, "/f", "failure", 7U, NULL) != M3FS_OK);
  OK(m3fs_stat_path(fs, "/f", &b));
  CHECK(b.size == 3U && b.generation == a.generation + 1U);
  OK(m3fs_read(fs, "/f", 0U, bytes, sizeof(bytes), &read));
  CHECK(read == 3U && memcmp(bytes, "NEW", 3U) == 0);
  CHECK(sqlite3_exec(db, "DROP TRIGGER deny_replace", NULL, NULL, NULL) == SQLITE_OK);
  CHECK(sqlite3_close(db) == SQLITE_OK);
  OK(m3fs_replace(fs, "/f", NULL, 0U, &size));
  CHECK(size == 0U);
  OK(m3fs_stat_path(fs, "/link", &b));
  CHECK(b.inode == a.inode && b.size == 0U && b.generation == a.generation + 2U);
  OK(m3fs_replace(fs, "/f", NULL, 0U, NULL));
  OK(m3fs_stat_path(fs, "/f", &b));
  CHECK(b.generation == a.generation + 2U);
  CHECK(m3fs_replace(fs, "/missing", NULL, 0U, NULL) == M3FS_NOT_FOUND);
  CHECK(m3fs_replace(fs, "/", NULL, 0U, NULL) == M3FS_IS_DIRECTORY);
  CHECK(m3fs_replace(snapshot, "/f", NULL, 0U, NULL) == M3FS_READ_ONLY);
  OK(m3fs_verify(fs));
  m3fs_close(snapshot); m3fs_close(fs);
  printf("PASS atomic replace checks=%u\n", checks);
  return 0;
}
