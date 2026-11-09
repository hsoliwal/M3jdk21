// SPDX-License-Identifier: Apache-2.0
/* White-box fault injection into the actual engine, not a replacement/stub implementation. */
#include "../src/m3fs.c"
#include <stdio.h>

static unsigned checks;
#define CHECK(c) do { ++checks; if (!(c)) { fprintf(stderr, "FAIL line %d: %s\n", __LINE__, #c); exit(1); } } while (0)
#define OK(c) CHECK((c) == M3FS_OK)
static int deny_blocks(void *ctx, int action, const char *a, const char *b, const char *db, const char *trigger) {
  (void)ctx; (void)b; (void)db; (void)trigger;
  return action == SQLITE_READ && a != NULL && strcmp(a, "blocks") == 0 ? SQLITE_DENY : SQLITE_OK;
}
static int sink(void *ctx, const char *name, uint32_t length, const m3fs_stat *stat) {
  (void)ctx; (void)name; (void)length; (void)stat; return M3FS_OK;
}
int main(int argc, char **argv) {
  CHECK(argc == 2);
  m3fs *writer = NULL, *view = NULL;
  OK(m3fs_open(argv[1], M3FS_OPEN_CREATE, &writer));
  OK(m3fs_create(writer, "/value", 0600U));
  OK(m3fs_write(writer, "/value", 0U, "old", 3U, NULL));
  OK(m3fs_open(argv[1], M3FS_OPEN_READ_ONLY | M3FS_OPEN_READ_SNAPSHOT, &view));
  CHECK(sqlite3_get_autocommit(view->db) == 0 && view->read_range == NULL);
  OK(m3fs_write(writer, "/value", 0U, "new", 3U, NULL));
  unsigned char out[3]; uint32_t got = 0U;
  OK(m3fs_read(view, "/value", 0U, out, sizeof(out), &got));
  sqlite3_stmt *compiled = view->read_range;
  CHECK(compiled != NULL && sqlite3_stmt_readonly(compiled) != 0);
  for (unsigned i = 0U; i < 200U; ++i) {
    OK(m3fs_read(view, "/value", 0U, out, sizeof(out), &got));
    CHECK(got == 3U && memcmp(out, "old", 3U) == 0);
    CHECK(view->read_range == compiled && sqlite3_stmt_busy(compiled) == 0);
    unsigned statements = 0U;
    for (sqlite3_stmt *item = sqlite3_next_stmt(view->db, NULL); item != NULL;
        item = sqlite3_next_stmt(view->db, item)) {
      CHECK(sqlite3_stmt_busy(item) == 0);
      ++statements;
    }
    CHECK(statements == 3U && view->lookup_child != NULL && view->stat_inode != NULL);
  }
  CHECK(sqlite3_stmt_status(compiled, SQLITE_STMTSTATUS_RUN, 0) >= 201);
  char *expanded = sqlite3_expanded_sql(compiled);
  CHECK(expanded != NULL && strstr(expanded, "f.inode=NULL") != NULL
      && strstr(expanded, "f.block_no>=NULL") != NULL
      && strstr(expanded, "f.block_no<=NULL") != NULL); sqlite3_free(expanded);
  /* Authorizer change expires prepared statements and injects a reprepare error. */
  CHECK(sqlite3_set_authorizer(view->db, deny_blocks, NULL) == SQLITE_OK);
  CHECK(m3fs_read(view, "/value", 0U, out, sizeof(out), &got) != M3FS_OK);
  CHECK(sqlite3_get_autocommit(view->db) == 0 && sqlite3_stmt_busy(compiled) == 0);
  CHECK(sqlite3_set_authorizer(view->db, NULL, NULL) == SQLITE_OK);
  OK(m3fs_read(view, "/value", 0U, out, sizeof(out), &got));
  CHECK(got == 3U && memcmp(out, "old", 3U) == 0);
  /* SQLite can abort a transaction after severe errors. Model that state directly. */
  CHECK(sqlite3_exec(view->db, "ROLLBACK", NULL, NULL, NULL) == SQLITE_OK);
  CHECK(sqlite3_get_autocommit(view->db) != 0);
  m3fs_stat stat; m3fs_stats stats; uint32_t count = 0U, more = 0U;
  CHECK(m3fs_read(view, "/value", 0U, out, sizeof(out), &got) == M3FS_IO);
  CHECK(m3fs_stat_path(view, "/value", &stat) == M3FS_IO);
  CHECK(m3fs_list(view, "/", sink, NULL) == M3FS_IO);
  CHECK(m3fs_list_page(view, "/", NULL, 1U, sink, NULL, &count, &more) == M3FS_IO);
  CHECK(m3fs_get_stats(view, &stats) == M3FS_IO);
  CHECK(m3fs_verify(view) == M3FS_IO);
  CHECK(m3fs_readlink(view, "/value", (char *)out, sizeof(out), &got) == M3FS_IO);
  CHECK(sqlite3_get_autocommit(view->db) != 0);
  m3fs_close(view); m3fs_close(writer);
  printf("PASS fault-native checks=%u statement-reused=YES reset=YES bindings-cleared=YES loss-rejected=YES\n", checks);
  return 0;
}
