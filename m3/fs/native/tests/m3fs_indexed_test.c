// SPDX-License-Identifier: Apache-2.0
#include "m3fs.h"
#include <sqlite3.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static unsigned checks;
#define CHECK(c) do { ++checks; if (!(c)) { fprintf(stderr, "FAIL line %d: %s\n", __LINE__, #c); exit(1); } } while (0)
#define OK(c) CHECK((c) == M3FS_OK)

typedef struct page_state { uint32_t count; char last[256]; int abort_status; } page_state;
static int collect(void *ctx, const char *name, uint32_t bytes, const m3fs_stat *stat) {
  page_state *s = (page_state *)ctx;
  if (s->abort_status != 0) return s->abort_status;
  CHECK(bytes <= 255U && stat->inode > 0U);
  CHECK(s->count == 0U || strncmp(s->last, name, bytes) < 0);
  memcpy(s->last, name, bytes); s->last[bytes] = '\0'; ++s->count;
  return M3FS_OK;
}
static void sql(sqlite3 *db, const char *text) {
  char *error = NULL;
  int rc = sqlite3_exec(db, text, NULL, NULL, &error);
  if (rc != SQLITE_OK) fprintf(stderr, "SQL: %s\n", error == NULL ? "?" : error);
  sqlite3_free(error); CHECK(rc == SQLITE_OK);
}
static sqlite3_int64 scalar(sqlite3 *db, const char *text) {
  sqlite3_stmt *stmt = NULL;
  CHECK(sqlite3_prepare_v2(db, text, -1, &stmt, NULL) == SQLITE_OK);
  CHECK(sqlite3_step(stmt) == SQLITE_ROW);
  sqlite3_int64 value = sqlite3_column_int64(stmt, 0);
  CHECK(sqlite3_finalize(stmt) == SQLITE_OK);
  return value;
}
int main(int argc, char **argv) {
  CHECK(argc == 2);
  m3fs *fs = NULL;
  OK(m3fs_open(argv[1], M3FS_OPEN_CREATE, &fs));
  CHECK(m3fs_abi_version() == 1);
  CHECK((m3fs_capabilities() & 3U) == 3U);
  OK(m3fs_mkdir(fs, "/d", 0755U));
  OK(m3fs_create(fs, "/d/source", 0640U));
  unsigned char source[65536]; memset(source, 73, sizeof(source));
  OK(m3fs_write(fs, "/d/source", 0U, source, sizeof(source), NULL));
  /* Sparse second region: absent intermediate blocks stay absent in the clone. */
  OK(m3fs_write(fs, "/d/source", 4U * M3FS_BLOCK_SIZE + 7U, "xyz", 3U, NULL));
  m3fs_stats before, after;
  OK(m3fs_get_stats(fs, &before));
  sqlite3 *db = NULL;
  CHECK(sqlite3_open(argv[1], &db) == SQLITE_OK);
  sql(db, "CREATE TRIGGER no_payload_insert BEFORE INSERT ON blocks BEGIN SELECT RAISE(ABORT,'payload copy'); END");
  uint64_t clone = 0U;
  OK(m3fs_clone_file(fs, "/d/source", "/d/clone", &clone));
  CHECK(clone > 1U);
  OK(m3fs_get_stats(fs, &after));
  CHECK(after.unique_blocks == before.unique_blocks);
  CHECK(after.block_references == before.block_references * 2U);
  m3fs_stat a, b;
  OK(m3fs_stat_path(fs, "/d/source", &a));
  OK(m3fs_stat_path(fs, "/d/clone", &b));
  CHECK(a.inode != b.inode && clone == b.inode && b.size == a.size);
  CHECK(b.mode == a.mode && a.nlink == 1U && b.nlink == 1U && b.generation == 1U);
  sql(db, "DROP TRIGGER no_payload_insert");
  unsigned char out[16]; uint32_t got = 0U;
  OK(m3fs_read(fs, "/d/clone", 2U * M3FS_BLOCK_SIZE, out, sizeof(out), &got));
  CHECK(got == sizeof(out));
  for (size_t i = 0U; i < sizeof(out); ++i) CHECK(out[i] == 0U);
  OK(m3fs_write(fs, "/d/clone", 0U, "changed", 7U, NULL));
  OK(m3fs_read(fs, "/d/source", 0U, out, 7U, &got));
  CHECK(got == 7U && memcmp(out, source, 7U) == 0);
  OK(m3fs_truncate(fs, "/d/clone", 0U));
  OK(m3fs_stat_path(fs, "/d/source", &a)); CHECK(a.size == b.size);

  /* Fail after destination inode and directory entry exist; all must roll back. */
  sqlite3_int64 old_inodes = scalar(db, "SELECT count(*) FROM inodes");
  sql(db, "CREATE TRIGGER reject_clone BEFORE INSERT ON file_blocks BEGIN SELECT RAISE(ABORT,'injected'); END");
  clone = 999U;
  CHECK(m3fs_clone_file(fs, "/d/source", "/d/rejected", &clone) != M3FS_OK);
  CHECK(clone == 0U);
  CHECK(scalar(db, "SELECT count(*) FROM inodes") == old_inodes);
  CHECK(m3fs_stat_path(fs, "/d/rejected", &a) == M3FS_NOT_FOUND);
  sql(db, "DROP TRIGGER reject_clone");
  CHECK(m3fs_clone_file(fs, "/d/source", "/d/clone", &clone) == M3FS_EXISTS);
  CHECK(m3fs_clone_file(fs, "/d", "/bad", &clone) == M3FS_IS_DIRECTORY);
  CHECK(m3fs_clone_file(fs, "/d/source", "/bad", NULL) == M3FS_INVALID);
  for (unsigned i = 0U; i < 101U; ++i) {
    char path[32]; (void)snprintf(path, sizeof(path), "/d/n%03u", i);
    OK(m3fs_create(fs, path, 0600U));
  }
  page_state page = {0U, "", 0}; uint32_t count = 0U, more = 0U, total = 0U;
  char cursor[256] = "";
  do {
    page.count = 0U;
    OK(m3fs_list_page(fs, "/d", cursor, 7U, collect, &page, &count, &more));
    CHECK(count == page.count && count <= 7U && (!more || count == 7U));
    if (count != 0U) memcpy(cursor, page.last, sizeof(cursor));
    total += count;
  } while (more);
  CHECK(total == 103U);
  page.abort_status = M3FS_BUSY; count = 999U; more = 999U;
  CHECK(m3fs_list_page(fs, "/d", NULL, 2U, collect, &page, &count, &more) == M3FS_BUSY);
  CHECK(count == 0U && more == 0U);
  page.abort_status = 0;
  CHECK(m3fs_list_page(fs, "/d", "bad/name", 2U, collect, &page, &count, &more) == M3FS_INVALID);
  CHECK(m3fs_list_page(fs, "/d", NULL, 4097U, collect, &page, &count, &more) == M3FS_INVALID);
  CHECK(m3fs_list_page(fs, "/d", NULL, 0U, collect, &page, &count, &more) == M3FS_INVALID);
  CHECK(m3fs_list_page(fs, "/d", NULL, 2U, collect, &page, NULL, &more) == M3FS_INVALID);
  OK(m3fs_verify(fs));
  CHECK(sqlite3_close(db) == SQLITE_OK);
  m3fs_close(fs);
  OK(m3fs_open(argv[1], M3FS_OPEN_READ_ONLY, &fs));
  CHECK(m3fs_clone_file(fs, "/d/source", "/d/new", &clone) == M3FS_READ_ONLY);
  page.count = 0U;
  OK(m3fs_list_page(fs, "/d", NULL, 2U, collect, &page, &count, &more));
  CHECK(count == 2U && more == 1U);
  m3fs_close(fs);
  printf("PASS indexed-native checks=%u rollback-injection=PASS no-payload-insert=PASS\n", checks);
  return 0;
}
