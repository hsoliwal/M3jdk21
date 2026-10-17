/* SPDX-License-Identifier: Apache-2.0 */
#include <jni.h>
#include <sqlite3.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <limits.h>

#define JNI_FN(name) Java_com_synexia_mindex_swap_SqliteSwapStore_##name
#define SLOT_COUNT 256
#define APP_ID 1397576535
#define VERSION 1

typedef struct {
    jlong token;
    sqlite3 *db;
    sqlite3_stmt *read, *insert, *head;
    char *space;
    int max_record, max_batch, max_records;
} Store;
static Store stores[SLOT_COUNT];
static jlong next_token = 1;
/* https://sqlite.org/wal.html#walreset: upstream fix and documented maintained backports. */
static uint32_t read_be32(const unsigned char *p) {
    return ((uint32_t)p[0] << 24) | ((uint32_t)p[1] << 16) | ((uint32_t)p[2] << 8) | (uint32_t)p[3];
}
static int valid_frame_header(const unsigned char *p, int n, jlong id) {
    uint64_t key = ((uint64_t)read_be32(p + 4) << 32) | read_be32(p + 8);
    return read_be32(p) == UINT32_C(0x53585031) && key == (uint64_t)id
            && read_be32(p + 12) == (uint32_t)(n - 48);
}
static int wal_safe(void) {
    int v = sqlite3_libversion_number();
    return v >= 3051003 || (v >= 3050007 && v < 3051000) || (v >= 3044006 && v < 3045000);
}
/* Serialize handle lookup AND use/close; tokens are never raw native pointers and never reused. */
static sqlite3_mutex *enter(void) {
    sqlite3_mutex *m = sqlite3_mutex_alloc(SQLITE_MUTEX_STATIC_APP1);
    sqlite3_mutex_enter(m); return m;
}
static void fail(JNIEnv *env, const char *message) {
    if ((*env)->ExceptionCheck(env)) return;
    jclass cls = (*env)->FindClass(env, "java/io/IOException");
    if (cls) { (*env)->ThrowNew(env, cls, message); (*env)->DeleteLocalRef(env, cls); }
}
static void dbfail(JNIEnv *env, sqlite3 *db, const char *operation) {
    char msg[512];
    snprintf(msg, sizeof(msg), "%s: sqlite rc=%d (%s)", operation,
             sqlite3_extended_errcode(db), sqlite3_errmsg(db));
    fail(env, msg);
}
static Store *lookup(JNIEnv *env, jlong token) {
    for (int i = 0; i < SLOT_COUNT; i++) if (token > 0 && stores[i].token == token) return &stores[i];
    fail(env, "invalid or closed native handle"); return NULL;
}
static int run(JNIEnv *env, sqlite3 *db, const char *sql) {
    if (sqlite3_exec(db, sql, NULL, NULL, NULL) != SQLITE_OK) { dbfail(env, db, "execute"); return 0; }
    return 1;
}
static int prepare(JNIEnv *env, sqlite3 *db, const char *sql, sqlite3_stmt **stmt) {
    if (sqlite3_prepare_v2(db, sql, -1, stmt, NULL) != SQLITE_OK) { dbfail(env, db, "prepare"); return 0; }
    return 1;
}
static void reset(sqlite3_stmt *s) { if (s) { sqlite3_reset(s); sqlite3_clear_bindings(s); } }
static int scalar(JNIEnv *env, sqlite3 *db, const char *sql, sqlite3_int64 *value) {
    sqlite3_stmt *s = NULL; int ok = 0;
    if (prepare(env, db, sql, &s)) {
        if (sqlite3_step(s) == SQLITE_ROW) { *value = sqlite3_column_int64(s, 0); ok = 1; }
        else dbfail(env, db, "scalar");
    }
    sqlite3_finalize(s); return ok;
}
static char *text(JNIEnv *env, jbyteArray bytes, int max, int identifier) {
    if (!bytes) { fail(env, "null text"); return NULL; }
    jsize n = (*env)->GetArrayLength(env, bytes);
    if (n < 1 || n > max) { fail(env, "text length limit"); return NULL; }
    char *s = malloc((size_t)n + 1);
    if (!s) { fail(env, "native allocation failed"); return NULL; }
    (*env)->GetByteArrayRegion(env, bytes, 0, n, (jbyte *)s);
    if ((*env)->ExceptionCheck(env)) { free(s); return NULL; }
    s[n] = 0;
    for (jsize i = 0; i < n; i++) {
        unsigned char c = (unsigned char)s[i];
        if (!c || (identifier && !(c >= 'a' && c <= 'z') && !(c >= 'A' && c <= 'Z')
                && !(c >= '0' && c <= '9') && c != '.' && c != '_' && c != ':' && c != '/' && c != '-')) {
            free(s); fail(env, "invalid text"); return NULL;
        }
    }
    return s;
}
static void dispose(Store *s) {
    sqlite3_finalize(s->read); sqlite3_finalize(s->insert); sqlite3_finalize(s->head);
    if (s->db) sqlite3_close_v2(s->db);
    free(s->space); memset(s, 0, sizeof(*s));
}
static int bind_space(JNIEnv *env, Store *s, sqlite3_stmt *stmt) {
    if (sqlite3_bind_text(stmt, 1, s->space, -1, SQLITE_STATIC) != SQLITE_OK) {
        dbfail(env, s->db, "bind space"); return 0;
    }
    return 1;
}
static int row(JNIEnv *env, Store *s, jlong id) {
    reset(s->read);
    if (!bind_space(env, s, s->read) || sqlite3_bind_int64(s->read, 2, id) != SQLITE_OK) {
        dbfail(env, s->db, "bind id"); return SQLITE_ERROR;
    }
    int rc = sqlite3_step(s->read);
    if (rc != SQLITE_ROW && rc != SQLITE_DONE) dbfail(env, s->db, "read");
    return rc;
}
static int gethead(JNIEnv *env, Store *s, const char *name, jlong *revision, jlong *id) {
    reset(s->head);
    if (!bind_space(env, s, s->head) || sqlite3_bind_text(s->head, 2, name, -1, SQLITE_TRANSIENT) != SQLITE_OK) {
        dbfail(env, s->db, "bind head"); return 0;
    }
    int rc = sqlite3_step(s->head);
    if (rc == SQLITE_DONE) { *revision = 0; *id = -1; reset(s->head); return 1; }
    if (rc != SQLITE_ROW) { dbfail(env, s->db, "read head"); reset(s->head); return 0; }
    *revision = sqlite3_column_int64(s->head, 0); *id = sqlite3_column_int64(s->head, 1);
    int valid = sqlite3_column_type(s->head, 0) == SQLITE_INTEGER
            && sqlite3_column_type(s->head, 1) == SQLITE_INTEGER && *revision > 0 && *id >= 0;
    reset(s->head);
    if (!valid) fail(env, "corrupt head");
    return valid;
}
JNIEXPORT jlong JNICALL JNI_FN(nOpen)(JNIEnv *env, jclass cls, jbyteArray pathBytes,
        jbyteArray namespaceBytes, jbyteArray codecBytes, jint maxRecord, jint maxBatch,
        jint maxRecords, jint busyMillis, jint cacheKiB, jint journalPolicy) {
    (void)cls; jlong result = 0; Store local = {0}; sqlite3_stmt *st = NULL;
    char *path = NULL, *codec = NULL; sqlite3_mutex *mutex = NULL;
    if (!sqlite3_threadsafe() || sqlite3_initialize() != SQLITE_OK) { fail(env, "threadsafe SQLite required"); return 0; }
    if (maxRecord < 48 || maxRecord > 268435504 || maxRecords < 1 || maxRecords > 4096
            || maxBatch < maxRecord || maxBatch > 1073938432 || busyMillis < 0
            || busyMillis > 600000 || cacheKiB < 64 || cacheKiB > 1048576
            || journalPolicy < 0 || journalPolicy > 2) {
        fail(env, "invalid native limits"); return 0;
    }
    if (journalPolicy == 1 && !wal_safe()) {
        fail(env, "WAL requires SQLite 3.51.3+ or documented 3.44.6/3.50.7 backport; use SAFE_AUTO or DELETE"); return 0;
    }
    path = text(env, pathBytes, 32768, 0); if (!path) goto end;
    local.space = text(env, namespaceBytes, 128, 1); if (!local.space) goto end;
    codec = text(env, codecBytes, 128, 1); if (!codec) goto end;
    mutex = enter();
    int slot = -1;
    for (int i = 0; i < SLOT_COUNT; i++) if (!stores[i].token) { slot = i; break; }
    if (slot < 0 || next_token == INT64_MAX) { fail(env, "native handle capacity exhausted"); goto end; }
    if (sqlite3_open_v2(path, &local.db, SQLITE_OPEN_READWRITE | SQLITE_OPEN_CREATE
            | SQLITE_OPEN_FULLMUTEX | SQLITE_OPEN_PRIVATECACHE, NULL) != SQLITE_OK) {
        if (local.db) dbfail(env, local.db, "open"); else fail(env, "SQLite open failed"); goto end;
    }
    if (!sqlite3_db_mutex(local.db)) { fail(env, "serialized SQLite connection required"); goto end; }
    sqlite3_extended_result_codes(local.db, 1);
    if (sqlite3_busy_timeout(local.db, busyMillis) != SQLITE_OK) { dbfail(env, local.db, "busy timeout"); goto end; }
    if (!run(env, local.db, "PRAGMA trusted_schema=OFF; PRAGMA foreign_keys=ON; PRAGMA mmap_size=0; PRAGMA synchronous=FULL")) goto end;
    /* Reject foreign files before changing their persistent journal mode. Recheck under write lock below. */
    sqlite3_int64 app = 0, version = 0, tables = 0;
    if (!scalar(env, local.db, "PRAGMA application_id", &app)
            || !scalar(env, local.db, "PRAGMA user_version", &version)) goto end;
    if (app == 0 && version == 0) {
        if (!scalar(env, local.db, "SELECT count(*) FROM sqlite_schema WHERE name NOT LIKE 'sqlite_%'", &tables)) goto end;
        if (tables) { fail(env, "refusing unrelated SQLite schema"); goto end; }
    } else if (app != APP_ID || version != VERSION) { fail(env, "incompatible SQLite application/schema version"); goto end; }
    int use_wal = journalPolicy != 2 && wal_safe();
    if (!prepare(env, local.db, use_wal ? "PRAGMA journal_mode=WAL" : "PRAGMA journal_mode=DELETE", &st)) goto end;
    if (sqlite3_step(st) != SQLITE_ROW || !sqlite3_column_text(st, 0)
            || strcmp((const char *)sqlite3_column_text(st, 0), use_wal ? "wal" : "delete")) {
        fail(env, "requested safe journal mode unavailable"); goto end;
    }
    sqlite3_finalize(st); st = NULL;
    if (!run(env, local.db, "BEGIN IMMEDIATE")) goto end;
    if (!scalar(env, local.db, "PRAGMA application_id", &app)
            || !scalar(env, local.db, "PRAGMA user_version", &version)) goto end;
    if (app == 0 && version == 0) {
        if (!scalar(env, local.db, "SELECT count(*) FROM sqlite_schema WHERE name NOT LIKE 'sqlite_%'", &tables)) goto end;
        if (tables) { fail(env, "refusing unrelated SQLite schema"); goto end; }
        if (!run(env, local.db,
            "CREATE TABLE spaces(name TEXT PRIMARY KEY,codec TEXT NOT NULL) WITHOUT ROWID;"
            "CREATE TABLE records(space TEXT NOT NULL,id INTEGER NOT NULL CHECK(id>=0),body BLOB NOT NULL,"
            "PRIMARY KEY(space,id),FOREIGN KEY(space) REFERENCES spaces(name)) WITHOUT ROWID;"
            "CREATE TABLE heads(space TEXT NOT NULL,name TEXT NOT NULL,revision INTEGER NOT NULL CHECK(revision>0),"
            "record_id INTEGER NOT NULL CHECK(record_id>=0),PRIMARY KEY(space,name),"
            "FOREIGN KEY(space,record_id) REFERENCES records(space,id)) WITHOUT ROWID;"
            "PRAGMA application_id=1397576535; PRAGMA user_version=1")) goto end;
    } else if (app != APP_ID || version != VERSION) { fail(env, "incompatible SQLite application/schema version"); goto end; }
    if (!prepare(env, local.db, "INSERT INTO spaces(name,codec) VALUES(?1,?2) ON CONFLICT(name) DO NOTHING", &st)) goto end;
    if (sqlite3_bind_text(st, 1, local.space, -1, SQLITE_STATIC) != SQLITE_OK
            || sqlite3_bind_text(st, 2, codec, -1, SQLITE_STATIC) != SQLITE_OK
            || sqlite3_step(st) != SQLITE_DONE) { dbfail(env, local.db, "bind namespace"); goto end; }
    sqlite3_finalize(st); st = NULL;
    if (!prepare(env, local.db, "SELECT codec FROM spaces WHERE name=?1", &st)) goto end;
    if (sqlite3_bind_text(st, 1, local.space, -1, SQLITE_STATIC) != SQLITE_OK
            || sqlite3_step(st) != SQLITE_ROW) { dbfail(env, local.db, "read codec"); goto end; }
    const void *stored = sqlite3_column_text(st, 0);
    if (!stored || sqlite3_column_bytes(st, 0) != (int)strlen(codec)
            || memcmp(stored, codec, strlen(codec))) { fail(env, "namespace codec mismatch"); goto end; }
    sqlite3_finalize(st); st = NULL;
    if (!run(env, local.db, "COMMIT")) goto end;
    char pragmas[128];
    snprintf(pragmas, sizeof(pragmas), "PRAGMA synchronous=FULL; PRAGMA cache_size=-%d; PRAGMA wal_autocheckpoint=1000", cacheKiB);
    if (!run(env, local.db, pragmas)) goto end;
    local.max_record = maxRecord; local.max_batch = maxBatch; local.max_records = maxRecords;
    sqlite3_limit(local.db, SQLITE_LIMIT_LENGTH, maxRecord + 4096);
    if (!prepare(env, local.db, "SELECT body FROM records WHERE space=?1 AND id=?2", &local.read)
            || !prepare(env, local.db, "INSERT INTO records(space,id,body) VALUES(?1,?2,?3) ON CONFLICT(space,id) DO NOTHING", &local.insert)
            || !prepare(env, local.db, "SELECT revision,record_id FROM heads WHERE space=?1 AND name=?2", &local.head)) goto end;
    local.token = next_token++; stores[slot] = local; result = local.token; memset(&local, 0, sizeof(local));
end:
    sqlite3_finalize(st);
    if (local.db && !sqlite3_get_autocommit(local.db)) sqlite3_exec(local.db, "ROLLBACK", NULL, NULL, NULL);
    dispose(&local); free(path); free(codec); if (mutex) sqlite3_mutex_leave(mutex); return result;
}
JNIEXPORT jbyteArray JNICALL JNI_FN(nRead)(JNIEnv *env, jclass cls, jlong token, jlong id) {
    (void)cls; jbyteArray result = NULL; sqlite3_mutex *m = enter(); Store *s = lookup(env, token);
    if (!s) goto end;
    if (id < 0) { fail(env, "negative id"); goto end; }
    if (row(env, s, id) == SQLITE_ROW) {
        int n = sqlite3_column_bytes(s->read, 0);
        if (sqlite3_column_type(s->read, 0) != SQLITE_BLOB || n < 48 || n > s->max_record) { fail(env, "corrupt/oversize stored frame"); goto end; }
        const void *data = sqlite3_column_blob(s->read, 0);
        if (!data) { fail(env, "SQLite blob allocation failed"); goto end; }
        result = (*env)->NewByteArray(env, n);
        if (result) (*env)->SetByteArrayRegion(env, result, 0, n, (const jbyte *)data);
    }
end:
    if (s) reset(s->read);
    sqlite3_mutex_leave(m); return result;
}
JNIEXPORT jlong JNICALL JNI_FN(nWrite)(JNIEnv *env, jclass cls, jlong token, jlongArray idsArray,
        jobjectArray frames, jbyteArray headBytes, jlong expected, jlong recordId) {
    (void)cls; jlong result = 0; sqlite3_mutex *m = enter(); Store *s = lookup(env, token);
    jlong *ids = NULL; char *head = NULL; unsigned char *copy = NULL; sqlite3_stmt *st = NULL;
    jobject frame = NULL; int transaction = 0;
    if (!s) goto end;
    if (!idsArray || !frames) { fail(env, "null batch"); goto end; }
    jsize n = (*env)->GetArrayLength(env, idsArray);
    if (n > s->max_records || n != (*env)->GetArrayLength(env, frames)) { fail(env, "batch count limit"); goto end; }
    if (headBytes) {
        if (expected < 0 || expected == INT64_MAX || recordId < 0) { fail(env, "invalid publication"); goto end; }
        head = text(env, headBytes, 128, 1); if (!head) goto end;
    }
    if (n) {
        ids = malloc((size_t)n * sizeof(*ids)); if (!ids) { fail(env, "native allocation failed"); goto end; }
        (*env)->GetLongArrayRegion(env, idsArray, 0, n, ids);
        if ((*env)->ExceptionCheck(env)) goto end;
    }
    int64_t total = 0;
    for (jsize i = 0; i < n; i++) {
        if (ids[i] < 0 || (i && ids[i] <= ids[i-1])) { fail(env, "ids must be nonnegative and strictly sorted"); goto end; }
        frame = (*env)->GetObjectArrayElement(env, frames, i);
        if (!frame) { fail(env, "null frame"); goto end; }
        jsize len = (*env)->GetArrayLength(env, (jbyteArray)frame);
        (*env)->DeleteLocalRef(env, frame); frame = NULL;
        total += len;
        if (len < 48 || len > s->max_record || total > s->max_batch) { fail(env, "native frame/batch limit"); goto end; }
    }
    if (!run(env, s->db, "BEGIN IMMEDIATE")) goto end;
    transaction = 1;
    if (head) {
        jlong current, oldId;
        if (!gethead(env, s, head, &current, &oldId)) goto end;
        if (current != expected) { result = -1; goto end; }
    }
    total = 0;
    for (jsize i = 0; i < n; i++) {
        frame = (*env)->GetObjectArrayElement(env, frames, i);
        if (!frame) { fail(env, "null frame"); goto end; }
        jsize len = (*env)->GetArrayLength(env, (jbyteArray)frame);
        total += len;
        if (len < 48 || len > s->max_record || total > s->max_batch) { fail(env, "mutated native batch exceeds limits"); goto end; }
        copy = malloc((size_t)len); if (!copy) { fail(env, "native allocation failed"); goto end; }
        (*env)->GetByteArrayRegion(env, (jbyteArray)frame, 0, len, (jbyte *)copy);
        (*env)->DeleteLocalRef(env, frame); frame = NULL;
        if ((*env)->ExceptionCheck(env)) goto end;
        if (!valid_frame_header(copy, len, ids[i])) { fail(env, "invalid native frame header"); goto end; }
        reset(s->insert);
        if (!bind_space(env, s, s->insert) || sqlite3_bind_int64(s->insert, 2, ids[i]) != SQLITE_OK
                || sqlite3_bind_blob(s->insert, 3, copy, len, SQLITE_STATIC) != SQLITE_OK
                || sqlite3_step(s->insert) != SQLITE_DONE) { dbfail(env, s->db, "insert record"); goto end; }
        if (!sqlite3_changes(s->db)) {
            int rc = row(env, s, ids[i]);
            if (rc != SQLITE_ROW || sqlite3_column_type(s->read, 0) != SQLITE_BLOB
                    || sqlite3_column_bytes(s->read, 0) != len) { fail(env, "immutable record conflict"); goto end; }
            const void *prior = sqlite3_column_blob(s->read, 0);
            if (!prior || memcmp(prior, copy, (size_t)len)) { fail(env, "immutable record conflict"); goto end; }
            reset(s->read);
        }
        reset(s->insert); free(copy); copy = NULL;
    }
    if (head) {
        if (row(env, s, recordId) != SQLITE_ROW) { fail(env, "publication target missing"); goto end; }
        reset(s->read);
        const char *sql = expected == 0
                ? "INSERT INTO heads(space,name,revision,record_id) VALUES(?1,?2,?3,?4)"
                : "UPDATE heads SET revision=?3,record_id=?4 WHERE space=?1 AND name=?2";
        if (!prepare(env, s->db, sql, &st)) goto end;
        if (!bind_space(env, s, st) || sqlite3_bind_text(st, 2, head, -1, SQLITE_STATIC) != SQLITE_OK
                || sqlite3_bind_int64(st, 3, expected + 1) != SQLITE_OK
                || sqlite3_bind_int64(st, 4, recordId) != SQLITE_OK || sqlite3_step(st) != SQLITE_DONE) {
            dbfail(env, s->db, "publish head"); goto end;
        }
        sqlite3_finalize(st); st = NULL; result = expected + 1;
    }
    if (!run(env, s->db, "COMMIT")) goto end;
    transaction = 0;
end:
    if (s) { reset(s->read); reset(s->insert); reset(s->head); }
    sqlite3_finalize(st);
    if (transaction) sqlite3_exec(s->db, "ROLLBACK", NULL, NULL, NULL);
    if (frame) (*env)->DeleteLocalRef(env, frame);
    free(copy); free(ids); free(head); sqlite3_mutex_leave(m); return result;
}
JNIEXPORT jlongArray JNICALL JNI_FN(nHead)(JNIEnv *env, jclass cls, jlong token, jbyteArray nameBytes) {
    (void)cls; jlongArray result = NULL; sqlite3_mutex *m = enter(); Store *s = lookup(env, token);
    char *name = NULL; jlong values[2];
    if (s) {
        name = text(env, nameBytes, 128, 1);
        if (name && gethead(env, s, name, &values[0], &values[1])) {
            result = (*env)->NewLongArray(env, 2);
            if (result) (*env)->SetLongArrayRegion(env, result, 0, 2, values);
        }
    }
    free(name); sqlite3_mutex_leave(m); return result;
}
JNIEXPORT jlongArray JNICALL JNI_FN(nIds)(JNIEnv *env, jclass cls, jlong token, jlong after, jint limit) {
    (void)cls; jlongArray result = NULL; sqlite3_mutex *m = enter(); Store *s = lookup(env, token);
    sqlite3_stmt *st = NULL; jlong *ids = NULL; int count = 0;
    if (!s) goto end;
    if (after < -1 || limit < 1 || limit > s->max_records) { fail(env, "invalid keyset bounds"); goto end; }
    ids = malloc((size_t)limit * sizeof(*ids)); if (!ids) { fail(env, "native allocation failed"); goto end; }
    if (!prepare(env, s->db, "SELECT id FROM records WHERE space=?1 AND id>?2 ORDER BY id LIMIT ?3", &st)) goto end;
    if (!bind_space(env, s, st) || sqlite3_bind_int64(st, 2, after) != SQLITE_OK || sqlite3_bind_int(st, 3, limit) != SQLITE_OK) {
        dbfail(env, s->db, "bind keyset"); goto end;
    }
    int rc;
    while ((rc = sqlite3_step(st)) == SQLITE_ROW) {
        jlong id = sqlite3_column_int64(st, 0);
        if (sqlite3_column_type(st, 0) != SQLITE_INTEGER || count >= limit || id < 0 || (count && id <= ids[count-1])) { fail(env, "corrupt keyset"); goto end; }
        ids[count++] = id;
    }
    if (rc != SQLITE_DONE) { dbfail(env, s->db, "keyset"); goto end; }
    result = (*env)->NewLongArray(env, count);
    if (result && count) (*env)->SetLongArrayRegion(env, result, 0, count, ids);
end:
    free(ids); sqlite3_finalize(st); sqlite3_mutex_leave(m); return result;
}
JNIEXPORT jlong JNICALL JNI_FN(nCount)(JNIEnv *env, jclass cls, jlong token) {
    (void)cls; jlong result = 0; sqlite3_mutex *m = enter(); Store *s = lookup(env, token); sqlite3_stmt *st = NULL;
    if (s && prepare(env, s->db, "SELECT count(*) FROM records WHERE space=?1", &st) && bind_space(env, s, st)) {
        if (sqlite3_step(st) == SQLITE_ROW) result = sqlite3_column_int64(st, 0);
        else dbfail(env, s->db, "count");
    }
    sqlite3_finalize(st); sqlite3_mutex_leave(m); return result;
}
JNIEXPORT jint JNICALL JNI_FN(nPrune)(
        JNIEnv *env, jclass cls, jlong token, jlong before, jint max_deletes) {
    (void)cls; jint result = 0; sqlite3_mutex *m = enter(); Store *s = lookup(env, token);
    sqlite3_stmt *st = NULL; int transaction = 0;
    if (!s) goto end;
    if (before < 0 || max_deletes < 1 || max_deletes > s->max_records) {
        fail(env, "invalid prune bounds"); goto end;
    }
    if (!run(env, s->db, "BEGIN IMMEDIATE")) goto end;
    transaction = 1;
    /*
     * Do not rely on SQLITE_ENABLE_UPDATE_DELETE_LIMIT. The scalar-ID subquery selects a bounded
     * set, and the NOT EXISTS fence makes every publication target undeletable.
     */
    if (!prepare(env, s->db,
            "DELETE FROM records WHERE space=?1 AND id IN ("
            "SELECT r.id FROM records AS r WHERE r.space=?1 AND r.id<?2 "
            "AND NOT EXISTS (SELECT 1 FROM heads AS h "
            "WHERE h.space=r.space AND h.record_id=r.id) "
            "ORDER BY r.id LIMIT ?3)", &st)) goto end;
    if (sqlite3_bind_text(st, 1, s->space, -1, SQLITE_STATIC) != SQLITE_OK
            || sqlite3_bind_int64(st, 2, before) != SQLITE_OK
            || sqlite3_bind_int(st, 3, max_deletes) != SQLITE_OK
            || sqlite3_step(st) != SQLITE_DONE) {
        dbfail(env, s->db, "prune unreferenced records"); goto end;
    }
    result = sqlite3_changes(s->db);
    sqlite3_finalize(st); st = NULL;
    if (!run(env, s->db, "COMMIT")) goto end;
    transaction = 0;
end:
    sqlite3_finalize(st);
    if (transaction && s) sqlite3_exec(s->db, "ROLLBACK", NULL, NULL, NULL);
    sqlite3_mutex_leave(m); return result;
}
JNIEXPORT jintArray JNICALL JNI_FN(nCheckpoint)(JNIEnv *env, jclass cls, jlong token) {
    (void)cls; jintArray result = NULL; sqlite3_mutex *m = enter(); Store *s = lookup(env, token);
    if (s) {
        int log = 0, done = 0;
        int rc = sqlite3_wal_checkpoint_v2(s->db, "main", SQLITE_CHECKPOINT_PASSIVE, &log, &done);
        if (rc != SQLITE_OK && rc != SQLITE_BUSY) dbfail(env, s->db, "checkpoint");
        else {
            jint values[3] = {rc == SQLITE_BUSY, log, done}; result = (*env)->NewIntArray(env, 3);
            if (result) (*env)->SetIntArrayRegion(env, result, 0, 3, values);
        }
    }
    sqlite3_mutex_leave(m); return result;
}
JNIEXPORT void JNICALL JNI_FN(nClose)(JNIEnv *env, jclass cls, jlong token) {
    (void)cls; sqlite3_mutex *m = enter(); Store *s = lookup(env, token); if (s) dispose(s); sqlite3_mutex_leave(m);
}
JNIEXPORT jstring JNICALL JNI_FN(nVersion)(JNIEnv *env, jclass cls) {
    (void)cls; return (*env)->NewStringUTF(env, sqlite3_libversion());
}

JNIEXPORT jboolean JNICALL JNI_FN(nWalSafe)(JNIEnv *env, jclass cls) {
    (void)env; (void)cls; return wal_safe() ? JNI_TRUE : JNI_FALSE;
}
JNIEXPORT jstring JNICALL JNI_FN(nJournal)(JNIEnv *env, jclass cls, jlong token) {
    (void)cls; jstring result = NULL; sqlite3_mutex *m = enter(); Store *s = lookup(env, token); sqlite3_stmt *st = NULL;
    if (s && prepare(env, s->db, "PRAGMA journal_mode", &st)) {
        if (sqlite3_step(st) == SQLITE_ROW && sqlite3_column_text(st, 0))
            result = (*env)->NewStringUTF(env, (const char *)sqlite3_column_text(st, 0));
        else dbfail(env, s->db, "journal mode");
    }
    sqlite3_finalize(st); sqlite3_mutex_leave(m); return result;
}


/* Deferred-page bulk ABI: Java owns format/policy; native only executes primitive loops. */
#define PAGE_JNI_FN(name) Java_com_synexia_mindex_swap_JniSwapPageBinaryOps_##name
#define PAGE_ABI_VERSION 2

static int page_direct_region(
        JNIEnv *env, jobject buffer, jint position, jint length, unsigned char **region) {
    if (buffer == NULL || region == NULL || position < 0 || length < 0) return 0;
    jlong capacity = (*env)->GetDirectBufferCapacity(env, buffer);
    if (capacity < 0 || (jlong)position > capacity
            || (jlong)length > capacity - (jlong)position) return 0;
    void *base = (*env)->GetDirectBufferAddress(env, buffer);
    if (base == NULL && length != 0) return 0;
    *region = base == NULL ? NULL : (unsigned char *)base + (size_t)position;
    return 1;
}

/* Defense at the native boundary too, including reflective direct invocation of this private ABI. */
static int page_writable(JNIEnv *env, jobject target) {
    if (target == NULL) return -1;
    jclass type = (*env)->GetObjectClass(env, target);
    if (type == NULL) return -1;
    jmethodID method = (*env)->GetMethodID(env, type, "isReadOnly", "()Z");
    if (method == NULL) { (*env)->DeleteLocalRef(env, type); return -1; }
    jboolean read_only = (*env)->CallBooleanMethod(env, target, method);
    int failed = (*env)->ExceptionCheck(env);
    (*env)->DeleteLocalRef(env, type);
    return failed ? -1 : read_only ? -4 : 0;
}

/* Integer address differences avoid undefined relational comparisons of unrelated C pointers. */
static int page_overlaps(const void *a, size_t a_length, const void *b, size_t b_length) {
    if (a_length == 0 || b_length == 0) return 0;
    uintptr_t left = (uintptr_t)a, right = (uintptr_t)b;
    return left <= right ? right - left < a_length : left - right < b_length;
}

static uint64_t page_read_le64(const unsigned char *source) {
    uint64_t value = 0;
    for (unsigned int index = 0; index < 8U; index++) {
        value |= (uint64_t)source[index] << (index * 8U);
    }
    return value;
}

static void page_write_le64(unsigned char *target, uint64_t value) {
    for (unsigned int index = 0; index < 8U; index++) {
        target[index] = (unsigned char)((value >> (index * 8U)) & UINT64_C(0xff));
    }
}

/* A null target is a validation/size pass over the very same encoding atom. */
static int page_put_varuint(uint64_t value, unsigned char *target, size_t capacity, size_t *offset) {
    do {
        if (*offset >= capacity) return -3;
        unsigned char current = (unsigned char)(value & UINT64_C(0x7f));
        value >>= 7U;
        if (value != 0U) current = (unsigned char)(current | 0x80U);
        if (target != NULL) target[*offset] = current;
        (*offset)++;
    } while (value != 0U);
    return 0;
}

static int page_get_varuint(const unsigned char *source, size_t length, size_t *offset, uint64_t *value) {
    uint64_t result = 0;
    for (unsigned int byte_index = 0; byte_index < 9U; byte_index++) {
        if (*offset >= length) return -2;
        unsigned char current = source[*offset];
        (*offset)++;
        result |= ((uint64_t)(current & 0x7fU)) << (byte_index * 7U);
        if ((current & 0x80U) == 0U) { *value = result; return 0; }
    }
    return -2;
}

static int page_encode(const unsigned char *source, size_t length, unsigned char *target, size_t capacity) {
    size_t count = length / 8U, used = 0;
    int status = page_put_varuint((uint64_t)count, target, capacity, &used);
    if (status != 0) return status;
    uint64_t previous = 0;
    for (size_t index = 0; index < count; index++) {
        uint64_t value = page_read_le64(source + index * 8U);
        if (value > (uint64_t)INT64_MAX || value < previous) return -2;
        status = page_put_varuint(value - previous, target, capacity, &used);
        if (status != 0) return status;
        previous = value;
    }
    return used > (size_t)INT_MAX ? -3 : (int)used;
}

static int page_decode(const unsigned char *source, size_t length, unsigned char *target, size_t capacity) {
    size_t offset = 0;
    uint64_t count64 = 0;
    int status = page_get_varuint(source, length, &offset, &count64);
    if (status != 0 || count64 > (uint64_t)INT_MAX) return -2;
    if (count64 * UINT64_C(8) > (uint64_t)capacity) return -3;
    uint64_t previous = 0;
    for (size_t index = 0; index < (size_t)count64; index++) {
        uint64_t delta = 0;
        status = page_get_varuint(source, length, &offset, &delta);
        if (status != 0 || delta > (uint64_t)INT64_MAX - previous) return -2;
        previous += delta;
        if (target != NULL) page_write_le64(target + index * 8U, previous);
    }
    return offset == length ? (int)count64 : -2;
}

JNIEXPORT jint JNICALL PAGE_JNI_FN(nativePageAbiVersion)(JNIEnv *env, jclass cls) {
    (void)env; (void)cls; return PAGE_ABI_VERSION;
}

JNIEXPORT jint JNICALL PAGE_JNI_FN(nativeCopy)(
        JNIEnv *env, jclass cls, jobject source_buffer, jint source_position, jint source_length,
        jobject target_buffer, jint target_position, jint target_remaining) {
    (void)cls;
    int status = page_writable(env, target_buffer);
    if (status != 0) return status;
    unsigned char *source = NULL, *target = NULL;
    if (!page_direct_region(env, source_buffer, source_position, source_length, &source)
            || !page_direct_region(env, target_buffer, target_position, target_remaining, &target)) return -1;
    if (target_remaining < source_length) return -3;
    if (source_length > 0) memmove(target, source, (size_t)source_length);
    return source_length;
}

JNIEXPORT jint JNICALL PAGE_JNI_FN(nativeDeltaEncode)(
        JNIEnv *env, jclass cls, jobject source_buffer, jint source_position, jint source_length,
        jobject target_buffer, jint target_position, jint target_remaining) {
    (void)cls;
    int status = page_writable(env, target_buffer);
    if (status != 0) return status;
    if ((source_length & 7) != 0) return -2;
    unsigned char *source = NULL, *target = NULL;
    if (!page_direct_region(env, source_buffer, source_position, source_length, &source)
            || !page_direct_region(env, target_buffer, target_position, target_remaining, &target)) return -1;
    int required = page_encode(source, (size_t)source_length, NULL, SIZE_MAX);
    if (required < 0) return required;
    if (required > target_remaining) return -3;
    unsigned char *scratch = NULL;
    if (page_overlaps(source, (size_t)source_length, target, (size_t)required)) {
        scratch = malloc((size_t)required);
        if (scratch == NULL) return -5;
    }
    status = page_encode(source, (size_t)source_length, scratch != NULL ? scratch : target, (size_t)required);
    if (scratch != NULL && status >= 0) memmove(target, scratch, (size_t)status);
    free(scratch);
    return (jint)status;
}

JNIEXPORT jint JNICALL PAGE_JNI_FN(nativeDeltaDecode)(
        JNIEnv *env, jclass cls, jobject source_buffer, jint source_position, jint source_length,
        jobject target_buffer, jint target_position, jint target_remaining) {
    (void)cls;
    int status = page_writable(env, target_buffer);
    if (status != 0) return status;
    unsigned char *source = NULL, *target = NULL;
    if (!page_direct_region(env, source_buffer, source_position, source_length, &source)
            || !page_direct_region(env, target_buffer, target_position, target_remaining, &target)) return -1;
    int count = page_decode(source, (size_t)source_length, NULL, (size_t)target_remaining);
    if (count < 0) return count;
    size_t required = (size_t)count * 8U;
    unsigned char *scratch = NULL;
    if (page_overlaps(source, (size_t)source_length, target, required)) {
        scratch = malloc(required);
        if (scratch == NULL) return -5;
    }
    status = page_decode(source, (size_t)source_length, scratch != NULL ? scratch : target, required);
    if (scratch != NULL && status >= 0) memmove(target, scratch, required);
    free(scratch);
    return (jint)status;
}
