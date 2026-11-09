// SPDX-License-Identifier: Apache-2.0
#include "m3fs.h"
#include <sqlite3.h>
#include <stdio.h>
#define CHECK(x) do { ++checks; if (!(x)) { fprintf(stderr, "FAIL line %d: %s\n", __LINE__, #x); return 1; } } while (0)
#define OK(x) CHECK((x) == M3FS_OK)
int main(int argc, char **argv) {
  unsigned checks = 0U;
  CHECK(argc == 2);
  m3fs *fs = NULL, *other = NULL;
  m3fs_stat a, b;
  OK(m3fs_open(argv[1], M3FS_OPEN_CREATE, &fs));
  OK(m3fs_create(fs, "/highest", 0644U));
  OK(m3fs_stat_path(fs, "/highest", &a));
  OK(m3fs_unlink(fs, "/highest"));
  OK(m3fs_create(fs, "/replacement", 0644U));
  OK(m3fs_stat_path(fs, "/replacement", &b));
  CHECK(b.inode > a.inode);
  m3fs_close(fs);
  sqlite3 *db = NULL;
  CHECK(sqlite3_open(argv[1], &db) == SQLITE_OK);
  CHECK(sqlite3_exec(db, "DELETE FROM fs_meta WHERE key='inode_high_water'", NULL, NULL, NULL) == SQLITE_OK);
  OK(m3fs_open(argv[1], 0U, &fs));
  OK(m3fs_unlink(fs, "/replacement"));
  OK(m3fs_open(argv[1], 0U, &other));
  OK(m3fs_create(other, "/legacy", 0644U));
  OK(m3fs_stat_path(fs, "/legacy", &a));
  CHECK(a.inode > b.inode);
  CHECK(sqlite3_exec(db, "CREATE TRIGGER deny_inode BEFORE INSERT ON inodes BEGIN SELECT RAISE(ABORT,'test'); END", NULL, NULL, NULL) == SQLITE_OK);
  CHECK(m3fs_create(fs, "/failure", 0644U) != M3FS_OK);
  CHECK(sqlite3_exec(db, "DROP TRIGGER deny_inode", NULL, NULL, NULL) == SQLITE_OK);
  OK(m3fs_create(fs, "/after-failure", 0644U));
  OK(m3fs_stat_path(fs, "/after-failure", &b));
  CHECK(b.inode == a.inode + 1U);
  CHECK(sqlite3_exec(db, "UPDATE fs_meta SET value=9223372036854775807 WHERE key='inode_high_water'", NULL, NULL, NULL) == SQLITE_OK);
  CHECK(m3fs_create(other, "/exhausted", 0644U) == M3FS_NOSPACE);
  CHECK(sqlite3_close(db) == SQLITE_OK);
  OK(m3fs_verify(fs));
  m3fs_close(other); m3fs_close(fs);
  printf("PASS inode identity checks=%u\n", checks);
  return 0;
}
