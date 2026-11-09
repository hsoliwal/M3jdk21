/* ML70_DIFFERENTIAL v1: master vs leaf scanNative/scanBoundedNative, and leaf countBoundedNative vs a reference, under a
 * fake JNIEnv (array lengths/regions, IsSameObject, exception flag). Inputs follow the Java-side admission: valid
 * geometry, masks in 0..255 (overlapping and zero masks included). */
#include <jni.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
typedef struct { jsize n; void *data; int width; } FakeArr;
static int pending;
static jsize JNICALL fLen(JNIEnv *e, jarray a) { (void) e; return ((FakeArr *) a)->n; }
static void JNICALL fGetB(JNIEnv *e, jbyteArray a, jsize s, jsize n, jbyte *b) { (void) e; memcpy(b, (jbyte *) ((FakeArr *) a)->data + s, (size_t) n); }
static void JNICALL fGetI(JNIEnv *e, jintArray a, jsize s, jsize n, jint *b) { (void) e; memcpy(b, (jint *) ((FakeArr *) a)->data + s, (size_t) n * 4); }
static void JNICALL fSetI(JNIEnv *e, jintArray a, jsize s, jsize n, const jint *b) { (void) e; memcpy((jint *) ((FakeArr *) a)->data + s, b, (size_t) n * 4); }
static jboolean JNICALL fExc(JNIEnv *e) { (void) e; return pending ? JNI_TRUE : JNI_FALSE; }
static jboolean JNICALL fSame(JNIEnv *e, jobject a, jobject b) { (void) e; return a == b ? JNI_TRUE : JNI_FALSE; }
static jclass JNICALL fFind(JNIEnv *e, const char *n) { (void) e; (void) n; return (jclass) (void *) 1; }
static jint JNICALL fThrow(JNIEnv *e, jclass c, const char *m) { (void) e; (void) c; (void) m; pending = 1; return 0; }
static void JNICALL fDel(JNIEnv *e, jobject o) { (void) e; (void) o; }
#define SCAN_ARGS JNIEnv *, jclass, jintArray, jbyteArray, jint, jint, jint, jintArray, jint, jint
#define BOUNDED_ARGS JNIEnv *, jclass, jintArray, jbyteArray, jint, jint, jint, jint, jintArray, jint, jint
jlong JNICALL m_scan(SCAN_ARGS); jlong JNICALL l_scan(SCAN_ARGS);
jlong JNICALL m_bounded(BOUNDED_ARGS); jlong JNICALL l_bounded(BOUNDED_ARGS);
jlong JNICALL l_count(JNIEnv *, jclass, jbyteArray, jint, jint, jint, jint);
static uint64_t rs = 20261107u;
static uint32_t rnd(uint32_t n) { rs ^= rs << 13; rs ^= rs >> 7; rs ^= rs << 17; return (uint32_t) (rs % n); }
int main(int argc, char **argv) {
    struct JNINativeInterface_ fn; JNIEnv env = &fn; long cases = argc > 1 ? atol(argv[1]) : 500000L, bad = 0, cbad = 0;
    uint64_t digest = 5;
    memset(&fn, 0, sizeof fn);
    fn.GetArrayLength = fLen; fn.GetByteArrayRegion = fGetB; fn.GetIntArrayRegion = fGetI; fn.SetIntArrayRegion = fSetI;
    fn.ExceptionCheck = fExc; fn.IsSameObject = fSame; fn.FindClass = fFind; fn.ThrowNew = fThrow; fn.DeleteLocalRef = fDel;
    for (long c = 0; c < cases; c++) {
        jint rows[300], out1[64], out2[64]; jbyte st[300];
        int n = (int) rnd(rnd(4) == 0 ? 300 : 70), on = 1 + (int) rnd(64);
        for (int i = 0; i < n; i++) { rows[i] = (jint) rnd(5000); st[i] = (jbyte) rnd(256); }
        for (int i = 0; i < on; i++) out1[i] = out2[i] = -7;
        int req = rnd(4) == 0 ? 0 : (int) (rnd(256) & rnd(256)), exc = rnd(3) == 0 ? 0 : (int) (rnd(256) & rnd(256) & rnd(256));
        int first = (int) rnd((uint32_t) n + 1), end = first + (int) rnd((uint32_t) (n - first) + 1);
        int off = (int) rnd((uint32_t) on), limit = 1 + (int) rnd((uint32_t) (on - off));
        FakeArr R = {n, rows, 4}, S = {n, st, 1}, O1 = {on, out1, 4}, O2 = {on, out2, 4};
        jlong a, b; int pa, pb;
        pending = 0; a = m_scan(&env, NULL, (jintArray) &R, (jbyteArray) &S, first, req, exc, (jintArray) &O1, off, limit); pa = pending;
        pending = 0; b = l_scan(&env, NULL, (jintArray) &R, (jbyteArray) &S, first, req, exc, (jintArray) &O2, off, limit); pb = pending;
        if (a != b || pa != pb || memcmp(out1, out2, sizeof(jint) * (size_t) on)) { if (bad++ < 5) printf("SCAN MISMATCH case=%ld\n", c); }
        digest = digest * 31 + (uint64_t) a;
        pending = 0; a = m_bounded(&env, NULL, (jintArray) &R, (jbyteArray) &S, first, end, req, exc, (jintArray) &O1, off, limit); pa = pending;
        pending = 0; b = l_bounded(&env, NULL, (jintArray) &R, (jbyteArray) &S, first, end, req, exc, (jintArray) &O2, off, limit); pb = pending;
        if (a != b || pa != pb || memcmp(out1, out2, sizeof(jint) * (size_t) on)) { if (bad++ < 5) printf("BOUNDED MISMATCH case=%ld master=%lld leaf=%lld\n", c, (long long) a, (long long) b); }
        digest = digest * 31 + (uint64_t) a;
        pending = 0; a = l_count(&env, NULL, (jbyteArray) &S, first, end, req, exc);
        { int cnt = 0; for (int i = first; i < end; i++) { int s = st[i] & 0xff; if ((s & req) == req && (s & exc) == 0) cnt++; }
          jlong want = ((jlong) end << 32) | (jlong) (uint32_t) cnt;
          if (a != want || pending) { if (cbad++ < 5) printf("COUNT MISMATCH case=%ld got=%llx want=%llx pending=%d\n", c, (unsigned long long) a, (unsigned long long) want, pending); } }
    }
    printf("ML70_DIFFERENTIAL cases=%ld scan_bounded_mismatches=%ld count_mismatches=%ld digest=%016llx\n", cases, bad, cbad, (unsigned long long) digest);
    return (bad || cbad) != 0;
}
