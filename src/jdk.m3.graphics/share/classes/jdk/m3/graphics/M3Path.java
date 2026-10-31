package jdk.m3.graphics;

import jdk.m3.graphics.impl.*;
import jdk.m3.graphics.*;

/**
 * <p>Path contain geometry. Path may be empty, or contain one or more verbs that
 * outline a figure. Path always starts with a move verb to a Cartesian coordinate,
 * and may be followed by additional verbs that add lines or curves.</p>
 *
 * <p>Adding a close verb makes the geometry into a continuous loop, a closed contour.
 * Path may contain any number of contours, each beginning with a move verb.</p>
 *
 * <p>Path contours may contain only a move verb, or may also contain lines,
 * quadratic beziers, conics, and cubic beziers. Path contours may be open or
 * closed.</p>
 *
 * <p>When used to draw a filled area, Path describes whether the fill is inside or
 * outside the geometry. Path also describes the winding rule used to fill
 * overlapping contours.</p>
 *
 * <p>Internally, Path lazily computes metrics likes bounds and convexity. Call
 * {@link #updateBoundsCache()} to make Path thread safe.</p>
 */
/* M3-ATOM kind=TYPE id=d5a2cb83290e3ad29d1bfad33f89e7b60a3d0c59134254dae74e4140a707ad61 contract=PUBLIC_API patterns=INHERITANCE,INTERFACE_IMPLEMENTATION */
public class M3Path extends M3Managed implements Iterable<M3PathSegment> {
    static { M3Library.staticLoad(); }

    /* M3-ATOM kind=TYPE id=8b4372ed3427a71d4d889b66ce1c0720647705ea80d3c29ff54aacd86e84e3cd contract=PUBLIC_API patterns=PLAIN_TYPE */
    public static class _FinalizerHolder {
        /* M3-LEAF kind=FIELD_DECLARATION id=1a0a48d8031c4af9cc59fe6e93249e8992e08ba2d5682206f335ab7d70bad93c contract=INTERNAL_OR_PACKAGE patterns=FIELD_DECLARATION */
        public static final long PTR = _nGetFinalizer();
    }

    /* M3-ATOM kind=METHOD id=6a353734c01a20a387f31a62fa24ba5c3fef2699b91e64f19007f5f7024ae60b contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public static M3Path makeRaw(M3Point[] points, M3PathVerb[] verbs, float[] conicWeights, M3PathFillMode fillMode) {
        /* M3-LEAF kind=RETURN id=25c8a712e340902a42380cd5cdeb79f8fac3c136e002e0021e92c30c0653a53f contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=INVOCATION id=de849fd50a658d256c974d8c29230efc10930cc31ed46e9c6635734829a46aaf contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 makeRaw(points, verbs, conicWeights, fillMode, false);
    }

    /* M3-ATOM kind=METHOD id=009947c1566927d0d977b5396e21547ee649335d1829fe1abf5ff1e6cb973caf contract=PUBLIC_API patterns=ITERATION */
    public static M3Path makeRaw(M3Point[] points, M3PathVerb[] verbs, float[] conicWeights, M3PathFillMode fillMode, boolean isVolatile) {
        /* M3-LEAF kind=INVOCATION id=792d1b163db18eea8d487865354fdf8f72db8f326c4e8e586062c464220e431c contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=8ac551d9792c69da76198103b6c7886bb933988055792e058f144d0e376e4070 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
        /* M3-LEAF kind=LOCAL_DECLARATION id=c6f6fc239465cc39a2f04b7dd314d62406742989d7efe65f88332134f60676fc contract=INTERNAL_OR_PACKAGE patterns=LOCAL_DECLARATION */
        int[] verbOrdinals = new int[/* M3-LEAF kind=FIELDACCESS id=17cdce5efc4ce175807d1638002648f257a46511af18897f5817a3e385696d8f contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
verbs.length];
        /* M3-LEAF kind=ITERATION id=149ea7c2c4222f890b6c4c4c6132feb1131fdcba43f8fddda5718bce7f5f0f0b contract=INTERNAL_OR_PACKAGE patterns=ITERATION */
        for (int i = 0; i < /* M3-LEAF kind=FIELDACCESS id=ed3a11cf5f51436da2c87df34f656c7972cb5774cb82894955db46f9d679c39b contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 verbs.length; i++) {
            /* M3-LEAF kind=MUTATION id=22a5a230adb4e8f6942ee9eab4f47f08769d9459bc9ed540aacb27669e4d8c99 contract=INTERNAL_OR_PACKAGE patterns=MUTATION */
            verbOrdinals[i] = /* M3-LEAF kind=INVOCATION id=ae5d5533fd111f3d1b7d6301352a606c4de42d5eee6952bcd47bfef22e74d6ad contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 verbs[i].ordinal(/* M3-LEAF kind=EMPTY id=6e9bee618d76351672bc5d8963d99d90dc5989166eb5eb93205801d18ca08765 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
        }
        /* M3-LEAF kind=LOCAL_DECLARATION id=b806ba2c5bc7ca2a86a4b03b31c4c9f88b7249a0b96ef39d7023db950bc76bb8 contract=INTERNAL_OR_PACKAGE patterns=LOCAL_DECLARATION */
        float[] coords = new float[/* M3-LEAF kind=FIELDACCESS id=a43b85e4801fffe9c2d49c3d2e37fcdc61dd11084decacfa3072b8014c983be4 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
points.length * 2];
        /* M3-LEAF kind=ITERATION id=aa6e1ba55efb689971bdc9a53a9d2325de44fab25fe58718e43e4b27107849e0 contract=INTERNAL_OR_PACKAGE patterns=ITERATION */
        for (int i = 0; i < /* M3-LEAF kind=FIELDACCESS id=2ed2f9b874fb334fff4c70b73a65fa61af3060c4c0a6c0825c32cc83fec65ae2 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 points.length; i++) {
            /* M3-LEAF kind=MUTATION id=105f419072fddb997d760b4a5308c65efc3c223a3ba92f0781356e0982488322 contract=INTERNAL_OR_PACKAGE patterns=MUTATION */
            coords[i * 2] = /* M3-LEAF kind=FIELDACCESS id=610bc237c9a5e969f358c524cda0154f982720e104741ee7cfec1dd6993e5e04 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 points[i]._x;
            /* M3-LEAF kind=MUTATION id=08baa4072fb5c89fb3c152e0667c7a149dd97e7177d38ef51e253141ae00bde6 contract=INTERNAL_OR_PACKAGE patterns=MUTATION */
            coords[i * 2 + 1] = /* M3-LEAF kind=FIELDACCESS id=05da78a496084c2379823e2277bc75d557c0490bef7d42db60c7bbf80262df81 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 points[i]._y;
        }
        /* M3-LEAF kind=RETURN id=4347fa76d580d9c47d162a52f2cae08d25288421a8309513dcdf25deb4a2b037 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=CONSTRUCTION id=a40f50a0a4fe6bc6221342d9e7818840418976d0dedf2afe7df3fb101ed368d8 contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3Path(/* M3-LEAF kind=INVOCATION id=ec0400a5e1aae9d9a614e13b1144b4d6ef0544c18b70fedd96cbd302ece10197 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
_nMakeRaw(coords, verbOrdinals, conicWeights, /* M3-LEAF kind=INVOCATION id=bd4a777f89269214e42ed1a3fc1b40edb8353374cb51383140b07699e23a1466 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 fillMode.ordinal(/* M3-LEAF kind=EMPTY id=164ca73042053d391cd332647ac35c509fb9d58ed04f03084e5bbd0ca27820f3 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
), isVolatile));
    }

    /* M3-ATOM kind=METHOD id=bd347072a34cf6a7df15c79f5a98f4570c0558e67ec26c58dfae71acd170426f contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public static M3Path makeRect(M3Rect rect) {
        /* M3-LEAF kind=RETURN id=b8371da6cbf8e526c1aca918e270c1cd8aeffda860f7f4d37ba821875daddffa contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=INVOCATION id=b68a02fdb73003abf4bf8b372d5c43cb6eabf8ce5893ca16dc31b04c214c276a contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 makeRect(rect, /* M3-LEAF kind=FIELDACCESS id=0e8c414224f80b9cbec7097dabe04157557573047a0d0ff7f82a7ff51625fd10 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 M3PathDirection.CLOCKWISE, 0);
    }

    /* M3-ATOM kind=METHOD id=73f1d0b4168336b74382d9cc18ee3858266329a99ad958da427c4c7e7d9d8251 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public static M3Path makeRect(M3Rect rect, M3PathDirection dir) {
        /* M3-LEAF kind=RETURN id=a7919ebd0cff8040bbd15b7b314574f68e7e92cd53f765f4bb8ed50587ed0068 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=INVOCATION id=e5468309d9cd796646449890cb64e05b0eec23bf8971b383bbeb807f5051e527 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 makeRect(rect, dir, 0);
    }

    /* M3-ATOM kind=METHOD id=f874b2113138ccbfe217ec759f572c02bd3f8dbe9d8d81e1640ebf363a113936 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public static M3Path makeRect(M3Rect rect, M3PathDirection dir, int startIndex) {
        /* M3-LEAF kind=INVOCATION id=9d6b9e7f1e5e127d0d32dceaccdc7b864cbe6a3a90a46b4a2b6739a913330e44 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=8c3f7338984d5357f418c29af219e09c752c6163f4ed546776f6d2645476d657 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
        /* M3-LEAF kind=RETURN id=9d4422aa0f0517b7be881d2611630f88fead9fdd7cf8291592737a1b38918c40 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=CONSTRUCTION id=924fd0d252104334d70e6dbb9759319792dce27425b8a19cf5b70649db1a396e contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3Path(/* M3-LEAF kind=INVOCATION id=02c640bd69ca87ac48a142a8c809bfbc52eb1b1e7ddde53fc85ab91ddb6bb5c0 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
_nMakeRect(/* M3-LEAF kind=FIELDACCESS id=daeb069532da6acae374984c618c678caeb5be97ce353556b8674487013b2923 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
rect._left, /* M3-LEAF kind=FIELDACCESS id=abfb9090d9b54d700ab6c959b3a7c06b18abd55a06e77c00e6719ac935a35a48 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rect._top, /* M3-LEAF kind=FIELDACCESS id=0fee010acf020239bf1d2cc3757fab38009f71c7bc1ac27c06b6941be00ab035 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rect._right, /* M3-LEAF kind=FIELDACCESS id=7f1fa558db87a2edce66bf173b822b09083069eb0d13dbe153244955ed579d88 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rect._bottom, /* M3-LEAF kind=INVOCATION id=8c089a1eb740331576c9734a58daa65dcd9ec88aece70dd7e722345a3b778fb7 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 dir.ordinal(/* M3-LEAF kind=EMPTY id=93e80494ed0ea76818ec5e8d44d9d69920ef1dcca70d87a1597c33d79d45bf6e contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
), startIndex));
    }

    /* M3-ATOM kind=METHOD id=9226cc27969ac4d9c3a791c82daa7b8a74084a6c9c5e360cef7def27bf6a9b66 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public static M3Path makeOval(M3Rect oval) {
        /* M3-LEAF kind=RETURN id=30658bffb0945b13f8740041de7881f3183fa4257d57fbf747efa2431abca888 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=INVOCATION id=2524ed94518cfb8c532580a5a5d9c03316b9409e7c04f65d5314d219877456a1 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 makeOval(oval, /* M3-LEAF kind=FIELDACCESS id=93b4a6708e5748f87011cd3f59209608271b90d3768cefff03bb25b0dacd78f6 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 M3PathDirection.CLOCKWISE);
    }

    /* M3-ATOM kind=METHOD id=50e91d130c0f88970ea419214b5cbb87e431cedf259ca635a16eba83911918dc contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public static M3Path makeOval(M3Rect oval, M3PathDirection dir) {
        /* M3-LEAF kind=INVOCATION id=b6699ec7a1e324383791a7b9c23979bd500214b34890be379db06e726093cdbd contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=77eb300c27112886726302a02ca9eb4c8fa6deb3fbd23cb91ef4491e2d2f67a1 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
        /* M3-LEAF kind=RETURN id=1ef015e21b8924f938cd3cb9f3f2a70da89e53e9d1a0c41a09f69c2ff65c664d contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=CONSTRUCTION id=a4027dd0089f828547a83631d36878e51116fea0949f2fca96513d9b15af7e05 contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3Path(/* M3-LEAF kind=INVOCATION id=bb4c084cf5f055b48a77e986534e79f3b9ab280a235fe40f9abe43506e6b4098 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
_nMakeOval(/* M3-LEAF kind=FIELDACCESS id=968924e59de1faf6048a671a953d6a3aa2a7e036a59192672c8805c650cb18ea contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
oval._left, /* M3-LEAF kind=FIELDACCESS id=e0da6a83257aa73e0e06e7fa3c8160ae8273ca8579de3e6cb554f998c7b9da81 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 oval._top, /* M3-LEAF kind=FIELDACCESS id=907506b6ae1b409e250fc8b954e7823b0d7138495e9003da1f725b6fe7e97b1a contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 oval._right, /* M3-LEAF kind=FIELDACCESS id=2ea362943e676ad77393e155e00287db9f3f265dc035243919434da2f4da07ba contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 oval._bottom, /* M3-LEAF kind=INVOCATION id=8ba17f96ddf8629eaf0b6ad67d674ace5861e15c8248efce0a15fe3158f4e018 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 dir.ordinal(/* M3-LEAF kind=EMPTY id=9c66e5b1a3c7c28a3f07dc5c181819f61915eb5632dba8da96212178d29c65aa contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
)));
    }

    /* M3-ATOM kind=METHOD id=53288bc271c7874d8be6b53e51096b71f36508ab7687c5f5e48e9aaa66d422a5 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public static M3Path makeCircle(float x, float y, float radius) {
        /* M3-LEAF kind=RETURN id=ddd4258f6a4b23545697195990f0f2919c1109bce4c73b2f29dc23f356850b01 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=INVOCATION id=b3e207c515ad3c589cd2753b71199db737af33baffd3171e294f766113caa023 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 makeCircle(x, y, radius, /* M3-LEAF kind=FIELDACCESS id=fb8f587f2d951ae8b9bc7c780d2d20fc12795a46cd4cac44da09418c0c754f50 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 M3PathDirection.CLOCKWISE);
    }

    /* M3-ATOM kind=METHOD id=4f7b143b653d8025380e9b8ba78557d9ae664160560c61e569b3323a6a1095aa contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public static M3Path makeCircle(float x, float y, float radius, M3PathDirection dir) {
        /* M3-LEAF kind=INVOCATION id=c753b68677b51ab4efb1ec3bd38a46071070d309bef03af62374447153e3d336 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=4a0b6221a441433c1bef075c0b0e449299c1a46db19aa1bc85abed24b48214c0 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
        /* M3-LEAF kind=RETURN id=659c28f7461d38b72d8b075c9a959fdd47fc073e008e725539f4fd12c218bff7 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=CONSTRUCTION id=5030c476a66a05e04c9ac9bec37ce200ee80cdbef32ea9d17a79541922762661 contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3Path(/* M3-LEAF kind=INVOCATION id=b0003cb505aeaef6f7e4065ad01f9b5b3b98e1d02c6919338d4ebb83869ea814 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
_nMakeCircle(x, y, radius, /* M3-LEAF kind=INVOCATION id=c73bfa373f0435d77fef70d6f97a63c4c9c401f32f000712515608c3d51794bb contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 dir.ordinal(/* M3-LEAF kind=EMPTY id=e69cb8441193680f743e865c6c319e41f5e7416187988cb23c5075aaef484aec contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
)));
    }

    /* M3-ATOM kind=METHOD id=1403cf78022c1338f2823393e9a770141247170980dcf001440ac1eda4052a2a contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public static M3Path makeRRect(M3RRect rrect) {
        /* M3-LEAF kind=RETURN id=51b465f87988533a3f6e08a2d90467611a9824859d9797097a6e539141fc4c17 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=INVOCATION id=2f3099a72db9c14eedb28e1ec8cbb218d00498017f19fe566eddcb7c5814cff7 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 makeRRect(rrect, /* M3-LEAF kind=FIELDACCESS id=4c1408ab24359df8865700d76c15a58e58509db920227824f0f6acecea73913e contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 M3PathDirection.CLOCKWISE, 0);
    }

    /* M3-ATOM kind=METHOD id=2e8b8f0210db557176f49db9b9b89953cc171f0e7041806d4909f99440bfb8b7 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public static M3Path makeRRect(M3RRect rrect, M3PathDirection dir) {
        /* M3-LEAF kind=RETURN id=604c04e6b7a064df2054c06bd0ccd75f2afcab97e3d66196f9aa8a18d9120ccd contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=INVOCATION id=6cf1761e6ae304f359dc59a02844bdc2d2663cb6980aa78c3b813b151a4daab1 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 makeRRect(rrect, dir, 0);
    }

    /* M3-ATOM kind=METHOD id=837616ee8d5c57a9a84b6cb9c78c25dddb6ae1028af77c64650910c2c68a1ca0 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public static M3Path makeRRect(M3RRect rrect, M3PathDirection dir, int startIndex) {
        /* M3-LEAF kind=TRY id=f75bebfded1baa77255b64347f2d870f5d8e072bd1e06bfa9c5eeade4b4082e6 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=d5a2ad8c0153baa5eae4039c68b859a977cc15509afe73693929bdc511b84119 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=610ca3af6ac64c38fedb83ab91d581fd8792cf02880249121f2497fa12fe6ba0 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=77e9c7949f5d06613a9fac11df3a5508120ae674740cc25fb3066efce36ec9d4 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=CONSTRUCTION id=90ecc57cd56f5b7b7dde1603ed3b8978a234a2504a322a651f180824a8c3e97c contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3Path(/* M3-LEAF kind=INVOCATION id=7f0996a2fe10a0135995240964721bb62bcd35d3f8d7e3ead082107921a5172d contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
_nMakeRRect(/* M3-LEAF kind=FIELDACCESS id=05b3fd8750023742e41e8ad78bb5063448c4ce806c97648da303fe78e80db426 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
rrect._left, /* M3-LEAF kind=FIELDACCESS id=51cf0bb0e716baa404dff74a3a656199d37758c9f12ee3d4988ad0f4e628620c contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rrect._top, /* M3-LEAF kind=FIELDACCESS id=26d704b6df14ce3dbf4abbedf945005c34be381dbd9ccbad1555eabec2f26fa4 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rrect._right, /* M3-LEAF kind=FIELDACCESS id=d7d5133760965df287ab9bd98d8fd81977227633052f8b0bfb5f5356075c8c89 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rrect._bottom,
                                        /* M3-LEAF kind=FIELDACCESS id=fb6dfd386f451c1f4fb71017f444438eb8f53fc72e5ada98d61022e5465c195f contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
                                        rrect._radii, /* M3-LEAF kind=INVOCATION id=b3fec3e404dada060013dd425447de4e7f0f19dc5a049f641b5f2a81ac0f2031 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 dir.ordinal(/* M3-LEAF kind=EMPTY id=7a27f41edb626ba5e969e588389e8710b1255bf872209b5e0827a2fdacacc50e contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
), startIndex));
        } finally {
            /* M3-LEAF kind=INVOCATION id=4cb97a33ba6796e55f7f7f9a2785d7fbc285897f036bbf53870cab9aaf1dcaa7 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(rrect);
        }
    }

    /* M3-ATOM kind=METHOD id=9ee736f34068b2f866279fb8292a26c192941d3a84669c71bfe5c26a8fdf2aab contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public static M3Path makePolygon(M3Point[] pts, boolean isClosed) {
        /* M3-LEAF kind=RETURN id=ab9b8845fce2092966b1558178726eb0c1153ba08ec1970e24deb0582e67a76b contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=INVOCATION id=4c1ac557d0177aba4df861b30f98f66f5d6f98a19a8333bb3300868174390690 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 makePolygon(pts, isClosed, /* M3-LEAF kind=FIELDACCESS id=f6a521054524533fcf448f4a3e8752319055f9f60863747739963c72935e145f contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 M3PathFillMode.WINDING, false);
    }

    /* M3-ATOM kind=METHOD id=fd44f973f0f75294539f3b1f3c31874a48450bf2ce202e6cb49257efd0150483 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public static M3Path makePolygon(M3Point[] pts, boolean isClosed, M3PathFillMode fillMode) {
        /* M3-LEAF kind=RETURN id=fdd0bb190390f70c0c4c6931dbc280aaf438b467ab5250f1c68adb81e88e6cc8 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=INVOCATION id=b5eb833d33c7fb423f5153c327ba11506d08b402ed5ecbeacdaa797a72432b9c contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 makePolygon(pts, isClosed, fillMode, false);
    }

    /* M3-ATOM kind=METHOD id=12346217f7dd6b23486109f8a19ebb9a1f57232094d6bf6a4cca3a277b131eb8 contract=PUBLIC_API patterns=ITERATION */
    public static M3Path makePolygon(M3Point[] pts, boolean isClosed, M3PathFillMode fillMode, boolean isVolatile) {
        /* M3-LEAF kind=INVOCATION id=f7e0e70f15a5c4c199b3c0b96ec8209b46aa55880581be216b46ba914889de4e contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=eb35c2c2591782674437e3b31c1a5bb276185ffd0ef436c9860e6a85a8f6c9a7 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
        /* M3-LEAF kind=LOCAL_DECLARATION id=7edae5f1e713246c877ea4294d19e1a887b3450498caa1d37815fd1bfdafbc8b contract=INTERNAL_OR_PACKAGE patterns=LOCAL_DECLARATION */
        float[] coords = new float[/* M3-LEAF kind=FIELDACCESS id=060283666dcfa971fb9d2aab6df55c5cd9ee534124a6ad6c82ccfdb9653100f0 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
pts.length * 2];
        /* M3-LEAF kind=ITERATION id=98d28ce8ad24ca2fc98da363aa04a1c3770f78a0b90c88fac75ac46312e0912a contract=INTERNAL_OR_PACKAGE patterns=ITERATION */
        for (int i = 0; i < /* M3-LEAF kind=FIELDACCESS id=793abb0b422bfeaa6378e7073e50cdf8df703c1307a2a438e50faa87519cc428 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 pts.length; i++) {
            /* M3-LEAF kind=MUTATION id=22b26b380270f7606a793123488d9e6ba68755bc13d4238f809ff002a05fd4a7 contract=INTERNAL_OR_PACKAGE patterns=MUTATION */
            coords[i * 2] = /* M3-LEAF kind=FIELDACCESS id=66a8c3f0ab8633746859236153211a8b0495c76a01ff99926bde09e5bbc39b6e contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 pts[i]._x;
            /* M3-LEAF kind=MUTATION id=5dfcba5a9d0cf37672e567b70ca0333e470b1070a64603ff09c398008872d429 contract=INTERNAL_OR_PACKAGE patterns=MUTATION */
            coords[i * 2 + 1] = /* M3-LEAF kind=FIELDACCESS id=82e869d41d959221abe4093a4952b6c9dacdffa5b878cdbc259583470c480884 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 pts[i]._y;
        }
        /* M3-LEAF kind=RETURN id=278658b6d26dbd968005cf93090bb024ba224da4676bc3a4a19764903fffcb8e contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=CONSTRUCTION id=2fd31e7542703ea2fb88adb14cee487a123dd8d749abe069071d3cd165689269 contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3Path(/* M3-LEAF kind=INVOCATION id=f853c7a0f138a76f6ef858d842b68f6f93fcb138ca75c4c4f512fd6c420336cf contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
_nMakePolygon(coords, isClosed, /* M3-LEAF kind=INVOCATION id=64b84911e48d713e70e80c5cad58e7dd2ab0d266288b61ee814360a3fd6c7c12 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 fillMode.ordinal(/* M3-LEAF kind=EMPTY id=e3516d895a2d1b41f9fcac3a7cdc203eb86b50d94198c638d8cc67520fd7c498 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
), isVolatile));
    }

    /* M3-ATOM kind=METHOD id=8f047850e33b12604d85082984067ab72e8d1659d7877bff29797539ebf72f48 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public static M3Path makeLine(M3Point a, M3Point b) {
        /* M3-LEAF kind=RETURN id=5739ca7a5183fb719bbe4251c9be9491968d4e92db2f0d8872764aadb419676f contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=INVOCATION id=8eb59d8720984844c656620d8c747c97a76def7a9a6b8ba519ef1c2a56f09b89 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 makeLine(/* M3-LEAF kind=FIELDACCESS id=ceee675b1b13b56ecfbee0988747bc96b6a717d8fe341bb06ace4e8627ceebc0 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
a._x, /* M3-LEAF kind=FIELDACCESS id=b198b8b438ad7b4d6496add74c16faec5fe01abd768a8ac68aa0dd7819bac48f contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 a._y, /* M3-LEAF kind=FIELDACCESS id=ff51d3d0090ff39c41737890c8d11b81a7404fdf952e890e2599388098ce22d2 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 b._x, /* M3-LEAF kind=FIELDACCESS id=619e9eeb7ed2e81f29c347a4008280c0720c4d4cde77465026182043e6be2ddf contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 b._y);
    }

    /* M3-ATOM kind=METHOD id=7f0782b9533c3e2e514cb9bf43e35ec5147b2e24113380dc98e1acb89a357e19 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public static M3Path makeLine(float x0, float y0, float x1, float y1) {
        /* M3-LEAF kind=INVOCATION id=8f52b08a761aeaae3b1cdb73bedebbad4369502f03148a101fd403f42266040c contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=bd6719e64648a1646f5591d1b1dc43f0d2a680ead41dc61e7baf7141298fbaa4 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
        /* M3-LEAF kind=RETURN id=c859521249f4a1db41b86fccd24a64f9c0d93d2983306787bed046c9b558c892 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=CONSTRUCTION id=12813c2e80fa66c95bbd8f12cd885a10c63e658d85ebcc638b5d9a154538625e contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3Path(/* M3-LEAF kind=INVOCATION id=a1469038170554edaf8b6bf5400a52ecef0bc510bc8c1db4a1da4ccc7c85dd18 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
_nMakeLine(x0, y0, x1, y1));
    }

    /* M3-ATOM kind=METHOD id=3f629c8e0bf83fe5a91bf3248d12f88e0eb8e29494f7cce22ad3a2fc58de4eb2 contract=PUBLIC_API patterns=BRANCHING,EXCEPTION_BOUNDARY */
    public static M3Path makeFromSVGString(String svg) {
        /* M3-LEAF kind=ASSERTION id=deac07015771bf8885252d4fabcc2cd9d1ad2a32a0d690183d12d5a7e8662de2 contract=INTERNAL_OR_PACKAGE patterns=ASSERTION */
        assert svg != null : "Can't makeFromSVGString with svg == null";
        /* M3-LEAF kind=INVOCATION id=6fd63609c0099bb914d0e6492c28b8e487492af3671338360b289d7ded6a8fcd contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=c324a0acf24affd477ead2b4690e2c59f4d8af90471b609aec799135d0a02aa2 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
        /* M3-LEAF kind=LOCAL_DECLARATION id=90e225713e141033f076c94524576ed8d7ab8ca8148404a3c8cd243e734a0c86 contract=INTERNAL_OR_PACKAGE patterns=LOCAL_DECLARATION */
        long res = /* M3-LEAF kind=INVOCATION id=f83c8038f47deecf1eca2a4d76b2b5f7057acead0322ad9325aa0104b1994d1c contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nMakeFromSVGString(svg);
        /* M3-LEAF kind=BRANCH id=a669361364572761e5e3a5cfa842ac3fd6dab3396d09de32ab1508a12900bf40 contract=INTERNAL_OR_PACKAGE patterns=BRANCHING */
        if (res == 0)
            /* M3-LEAF kind=THROW id=346534ec8da61fa90f8c57585eed42d9e825df91b716e8fb7b9061c0ace6dc9d contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
            throw /* M3-LEAF kind=CONSTRUCTION id=52afb2af85e8bf6669d7f8b0804907ed879995e2705e72f9de1ee5a9dabb90bf contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new IllegalArgumentException("Failed to parse SVG Path string: " + svg);
        else
            /* M3-LEAF kind=RETURN id=caf18573e7f81138ee467b8b0d19a67956db2777d4aeae5be860370c7114e7a1 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=CONSTRUCTION id=579537fe404ae272f5985926c032b1dc4d703a406789954d67f30168635abd06 contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3Path(res);
    }

    /**
     * <p>Returns Path that is the result of applying the Op to the first path and the second path.
     * <p>The resulting path will be constructed from non-overlapping contours.
     * <p>The curve order is reduced where possible so that cubics may be turned
     * into quadratics, and quadratics maybe turned into lines.
     *
     * @param one The first operand (for difference, the minuend)
     * @param two The second operand (for difference, the subtrahend)
     * @param op  The operator to apply.
     * @return    Path if operation was able to produce a result, null otherwise
     */
    /* M3-ATOM kind=METHOD id=05853c14002ef0ce68a18a6adf3de7d5923d1d64a35126c008e8c4dda5dfa789 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public static M3Path makeCombining(M3Path one, M3Path two, M3PathOp op) {
        /* M3-LEAF kind=TRY id=4eb0d9e8b4972f6d0a5daa9ae744853c0fb0e25f2c9007408af1dc73093d0b46 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=43a0528aad58643d0c57c0c0ee9cbd04f5546134c244aa8993bec901f123916e contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=73b013d9df9a3b69774599aaf7d6438d7fbaee8bfde69bd75351db822e75dd32 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=LOCAL_DECLARATION id=d872e6d5fdd7bc92ceb0dc1e9be804f39caea73405ae70bdf4eb516c2abbbb36 contract=INTERNAL_OR_PACKAGE patterns=LOCAL_DECLARATION */
            long ptr = /* M3-LEAF kind=INVOCATION id=2c5acefbb170b4e59ea361b78ebdf20fecfb1993ceafff38d4bf7f454f37190f contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nMakeCombining(/* M3-LEAF kind=INVOCATION id=9b2c17d7b112251dfc7db4b17ae6396b12cc58276377402d2a375f299c2f57ec contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
M3Native.getPtr(one), /* M3-LEAF kind=INVOCATION id=9513d423c251d11a8e5b354c5b4c8a642e3edc7dfbf600567b3e87a710df7243 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 M3Native.getPtr(two), /* M3-LEAF kind=INVOCATION id=dcc538adea03d24964a3a792aad2d03f773ade7077d90521c4ffcb78fbb3d19b contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 op.ordinal(/* M3-LEAF kind=EMPTY id=3c28ec4356650b5875e46c7de84af8e9a3a67398214084f9de9e9034644dbe26 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
));
            /* M3-LEAF kind=RETURN id=2bd8b924a4b817f5b51e6ec00afe5e2994748193f0117fd2269c8eaea363953b contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=TERNARY id=dae95436683d745557cca8a37f577eea933b749c86c128f3de84572c4ae53555 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 ptr == 0 ? null : /* M3-LEAF kind=CONSTRUCTION id=8806730ef17698219a58044ea7fab0895245a6388a59152986cf099c19033344 contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3Path(ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=36b6a5ff556ab9f11193b2b0c31b4464d9f76313158203f9d5b7a08b3f689cfb contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(one);
            /* M3-LEAF kind=INVOCATION id=f1b98f18f007994de5d8ae89249d1b2131642ed90604c484b844191bd8616494 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(two);
        }
    }

    /**
     * Constructs an empty Path. By default, Path has no verbs, no {@link Point}, and no weights.
     * FillMode is set to {@link PathFillMode#WINDING}.
     *
     * @see <a href="https://fiddle.skia.org/c/@Path_empty_constructor">https://fiddle.skia.org/c/@Path_empty_constructor</a>
     */
    /* M3-ATOM kind=METHOD id=918f10c428df2bec2c694a723f8131c1f86d127ee8576a44efd4e2cc93f3371a contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public M3Path() {
        /* M3-LEAF kind=INVOCATION id=88d1564dc5932775abfb1f6503978397c9053d1f032b10e37124fb006210324d contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        this(/* M3-LEAF kind=INVOCATION id=de3d78c90956a2dcaa2bc13fe62599e8d23ca97281115294ccc0c7a2d9ecdd5e contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
_nMake(/* M3-LEAF kind=INVOCATION id=fdbfea27fb613b5750cc98687d97b6d9bf53be9381f79765390b3ccab3cad4c2 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
/* M3-LEAF kind=FIELDACCESS id=e582a0c9847fa2c70e6d3c5189a5cd02bc79e27a09162cc6c492a05e530f808a contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
M3PathFillMode.WINDING.ordinal(/* M3-LEAF kind=EMPTY id=a8968271838cac8c86c4949c1f73ff899f424c7d164f2f9654cdc87b1f873ee3 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
)));
        /* M3-LEAF kind=INVOCATION id=a63d4d581d5a3a277d94ce747807f6f8c7ec2288798ff821676d01172fd83ec1 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=daa789a6ebe0211d416f0cfcf64aae2eff717dd505bc1b412655b374c64965b8 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
    }

    /**
     * Constructs an empty {@link Path} with specified {@link PathFillMode}.
     * By default, {@link Path} has no verbs, no {@link Point}, and no weights.
     */
    /* M3-ATOM kind=METHOD id=7658c2305776d9837fa65450ae8861e5509f48df1bd2da1bfe9672126c0e6256 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public M3Path(M3PathFillMode fillMode) {
        /* M3-LEAF kind=INVOCATION id=fcaf7d34fa84e9d396af51137f97e9f21e1bec53a47319ddfc8b6e03f4b0ab8a contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        this(/* M3-LEAF kind=INVOCATION id=8fd8fa96a19bacb8aa0390d0ba2d1f8407f4977af02861687b838998912da5f8 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
_nMake(/* M3-LEAF kind=INVOCATION id=474084a74a0aa99f66c3a0847b9b3c0e8f505313347e9f7ce91c21cf0d3991c2 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
fillMode.ordinal(/* M3-LEAF kind=EMPTY id=bc434c2b52b91355abce31f5d38fb3a4c5c83b7b97c8628a75a243b7424cf659 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
)));
        /* M3-LEAF kind=INVOCATION id=9823967ab82cce11043cb962de36888509f97bb0a300e26357f27fda3f1a528b contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=0d1272365dd354d3156cedb044b92fc4a1b441f1ff76f5acbe526d1a921d6da5 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
    }

    /**
     * <p>Constructs a copy of an existing path. Copy constructor makes two
     * paths identical by value. Internally, path and the returned result share
     * pointer values. The underlying verb array, {@link Point} array and
     * weights are copied when modified.</p>
     *
     * <p>Creating a {@link Path} copy is very efficient and never allocates
     * memory. {@link Path} are always copied by value from the interface; the
     * underlying shared pointers are not exposed.</p>
     *
     * @param path  Path to copy by value
     * @see <a href="https://fiddle.skia.org/c/@Path_copy_const_SkPath">https://fiddle.skia.org/c/@Path_copy_const_SkPath</a>
     */
    /* M3-ATOM kind=METHOD id=3a4ae53be7881528ee6b325b1aebf468aab2411e71853cf4f93d75f77ef772d1 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public M3Path(M3Path path) {
        /* M3-LEAF kind=INVOCATION id=6bff7a1d13bd0b16b17a70a7689001568b57cc0be2613ed0990593440f6d93c3 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        this(/* M3-LEAF kind=INVOCATION id=8833c5d1e8eef7f095618894706ef2cc528553ef3d5399a7e3d14c5f66561cdc contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
_nMakeCopy(/* M3-LEAF kind=INVOCATION id=b288d07ec00a07d493eab83c3434162b87e3aca0b32e77cfa459b3806934dae0 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
M3Native.getPtr(path)));
        /* M3-LEAF kind=INVOCATION id=5b6330d178873d006a396c2ef92691e5177437b781a458c133ff244de8373384 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        M3ReferenceUtil.reachabilityFence(path);
        /* M3-LEAF kind=INVOCATION id=f5865af1152b1bd63ac05b10c4079ed34f4305c9d7db067fb3a3516243323a5f contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=45cd9a2e7d2c3f743e5676f3feef312dbce5d64edb028db2f64a5f32a3fdb714 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
    }

    /**
     * Compares this path and o; Returns true if {@link PathFillMode}, verb array, Point array, and weights
     * are equivalent.
     *
     * @param other  Path to compare
     * @return   true if this and Path are equivalent
    */
    /* M3-ATOM kind=METHOD id=3d233d78758e6975003cb9031994216844af32871cacc0fdc455d8024ee581e6 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    @Override
    public boolean _nativeEquals(M3Native other) {
        /* M3-LEAF kind=TRY id=991559e187adef002aa695b667376967298c978d2e073713bc91ba93091c4764 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=RETURN id=3673bf5ff4cf4b892f310e74ad00fcdb67f0e8b5b2c128497671b2bb9339ecea contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=9827820fe015df41eba1a1d41c9f43a42bc5caf7555a2aaf8ef65dbc25f735c6 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nEquals(_ptr, /* M3-LEAF kind=INVOCATION id=9571b785adf27b4bb9e55202b6167b2f520d4e5f69a160fab80635dcb1b329fa contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 M3Native.getPtr(other));
        } finally {
            /* M3-LEAF kind=INVOCATION id=bbce570ed15b41af8148d7898dd371fd2c199696650720582dfa189316af5644 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
            /* M3-LEAF kind=INVOCATION id=a928c4631da917e2723a7f3998a9e321cf55f3dc80d387abba31abc7198add21 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(other);
        }
    }

    /**
     * <p>Returns true if Path contain equal verbs and equal weights.
     * If Path contain one or more conics, the weights must match.</p>
     *
     * <p>{@link PathBuilder#conicTo(float, float, float, float, float)} may add different verbs
     * depending on conic weight, so it is not trivial to interpolate a pair of Path
     * containing conics with different conic weight values.</p>
     *
     * @param compare  Path to compare
     * @return         true if Path verb array and weights are equivalent
     *
     * @see <a href="https://fiddle.skia.org/c/@Path_isInterpolatable">https://fiddle.skia.org/c/@Path_isInterpolatable</a>
     */
    /* M3-ATOM kind=METHOD id=a693de4b7ca4229337bd57340334ab3f3b9d2987729f3bafdff0c457a58ee720 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public boolean isInterpolatable(M3Path compare) {
        /* M3-LEAF kind=TRY id=d051bea2bed00e23091ced4f5eab5c2088ec6eca024ed179f1b0cd6ef6580e0c contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=6176739b024d6618cc5f37275ca4a32e56d37e5b8f9334654467296febb04043 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=3bd670d66530f5318b01574c042873c20311abd9c0d06cd53a0cfe578f295e20 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=77e5cda133d5f78aec3b81ebeb0939258b170ddf211abaa1aa57ed5b6546d797 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=ee93b25e680f1cc74787a35bba2ebdc4d77b95afc43b673c1525e4839a44e34c contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nIsInterpolatable(_ptr, /* M3-LEAF kind=INVOCATION id=15b7c09db34772e508cc9181ee81dcfa6bda7c10311077a2c0da6feea68274c0 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 M3Native.getPtr(compare));
        } finally {
            /* M3-LEAF kind=INVOCATION id=7e22092685499998ffbce1901882390c1c525840169247ab6be4b44c3d77e007 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
            /* M3-LEAF kind=INVOCATION id=fe9215ccfe34fedd5a8855ef47dcc9a3ab5554e3c7249244e09a762e0cb2cd29 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(compare);
        }
    }

    /**
     * <p>Interpolates between Path with {@link Point} array of equal size.
     * Copy verb array and weights to out, and set out Point array to a weighted
     * average of this Point array and ending Point array, using the formula:
     *
     * <p>{@code (Path Point * weight) + ending Point * (1 - weight)}
     *
     * <p>weight is most useful when between zero (ending Point array) and
     * one (this Point_Array); will work with values outside of this
     * range.</p>
     *
     * <p>interpolate() returns null if Point array is not
     * the same size as ending Point array. Call {@link #isInterpolatable(Path)} to check Path
     * compatibility prior to calling interpolate().</p>
     *
     * @param ending  Point array averaged with this Point array
     * @param weight  contribution of this Point array, and
     *                one minus contribution of ending Point array
     * @return        interpolated Path if Path contain same number of Point, null otherwise
     *
     * @see <a href="https://fiddle.skia.org/c/@Path_interpolate">https://fiddle.skia.org/c/@Path_interpolate</a>
     */
    /* M3-ATOM kind=METHOD id=17c99574964046e8ef33ee1ebdbb0115abac5f888303edfffd5424d00009d3d4 contract=PUBLIC_API patterns=BRANCHING,EXCEPTION_BOUNDARY */
    public M3Path makeInterpolate(M3Path ending, float weight) {
        /* M3-LEAF kind=TRY id=06df656b220e21707c8e0dd4866d85c4ed596ad707d8ac9764514b29af7e5a9d contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=edd13cf85834410338dee5303848f9f868a43466e7a8108709fba7b2b7d364e1 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=e651cae74e0d30994a859e78292172c8b12d73c27852215a60d3513520392f77 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=LOCAL_DECLARATION id=fb0e1eeced2e5874b769ced5961490edf5ddeebe4ac439e39e4ed7e33ceab11e contract=INTERNAL_OR_PACKAGE patterns=LOCAL_DECLARATION */
            long ptr = /* M3-LEAF kind=INVOCATION id=01fa596d9ef4c0437cc5ec8a5c9eb668e10b5d698d26e02b517f5ebcc68fa294 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nMakeInterpolate(_ptr, /* M3-LEAF kind=INVOCATION id=e1a8e25085d32601f05b0fb715c8cb7fccb72d28e90b05844f43fcf60ce3964a contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 M3Native.getPtr(ending), weight);
            /* M3-LEAF kind=BRANCH id=9c19090b4c243e3cfabcc5efedf336631399f5c22d674f62c9b5f11435b6ba7b contract=INTERNAL_OR_PACKAGE patterns=BRANCHING */
            if (ptr == 0)
                /* M3-LEAF kind=THROW id=05b2260f0d6f539fb4317ced019d4a40cf36720ba85482c408a460c18c3b623a contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
                throw /* M3-LEAF kind=CONSTRUCTION id=0d261c3a7c76284eb96bb803c5b1a5f5cfa75e8d5bae8a5b6417be21507b652d contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new IllegalArgumentException("Point array is not the same size as ending Point array");
            /* M3-LEAF kind=RETURN id=3838d4cd17a94bced6ba7cb79ba3b181e7ae16a7bb1fae9bc917009673818a5c contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=CONSTRUCTION id=60f2d52a2ffab6fd024c1e06f8bfaff8cffbcd4a3fd7619a03fda8c91510c35a contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3Path(ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=43604bdeee5fdeb12b1991accd4b649c1453967180355cde52565fdfedab7d45 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
            /* M3-LEAF kind=INVOCATION id=77a1295c7542725adaa30256a318ca6989d40611d1e5c269d8a5b264c01d0d2d contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(ending);
        }
    }

    /**
     * Returns {@link PathFillMode}, the rule used to fill {@link Path}.
     *
     * @return  current {@link PathFillMode} setting
     */
    /* M3-ATOM kind=METHOD id=9538f01d407d7d8fcb8deff0a8fd23c3672dd3fd063869f9938461527289e968 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public M3PathFillMode getFillMode() {
        /* M3-LEAF kind=TRY id=2a7dd7191111dd56adfde3a2555973e3e9f3684e0c2958ba7e4e6ffb1fd84d99 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=3cb95772d7c3e052be2da6b3ad8fd25fc2054bdb2e575cea772465b12b53f61f contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=bcede6c5d3af0603fab1348086e986f22c0d806cf384db48919454e7078fa724 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=b8949ee47235801531354bc96271078f6fe8177a8a3a8a98feb4586dedb0a9e2 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=FIELDACCESS id=3994577396fab1eaa2d230293b3597a38e4051248b6474fe4aba53e3287108f5 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 M3PathFillMode._values[/* M3-LEAF kind=INVOCATION id=533543a885d47f6d1b3701d949deb6f84f02f49ece6a015421e36c7a94e144a4 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
_nGetFillMode(_ptr)];
        } finally {
            /* M3-LEAF kind=INVOCATION id=cd1d7f8b7867884b2e88e3d4b4c57d1693d7de14a166f8d953e74153f72d97de contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Creates an {@link Path} with the same properties and data, and with
     * {@link PathFillMode} set to fillMode.
     *
     * @return  new {@link Path}
     */
    /* M3-ATOM kind=METHOD id=2f3da9dbde0d80d5b9be1582a82186753ef711371fafc5619bfb53173ae77a38 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public M3Path makeWithFillMode(M3PathFillMode fillMode) {
        /* M3-LEAF kind=TRY id=736b4fb661b77b121046b230f6fa3e404f911bc171c7b6b51074d3c2c48a9820 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=34681ec4f15f6b4a8248671e551f70373ce835196d136d4644394ed56ef18eae contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=afa10344441c0c057890dbcd7e955540ac8da62baaf0e6cc4d026b376933d9d6 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=cc95a645b66c14d32202430b600a4b4c8579047e4c1e95c572d65ec11796cc3d contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=CONSTRUCTION id=5d2e987555ab4c645e8b45bdf9d5d75d907be1f33bad3ac853b318a34f7f0dd8 contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3Path(/* M3-LEAF kind=INVOCATION id=d7819d187f302ab476d05a30117d98853c688a12cebd62d8e855137764b246cf contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
_nMakeWithFillMode(_ptr, /* M3-LEAF kind=INVOCATION id=182871e96280dc783710410e0724b6389968ecfe03dfd9b0bc8e16bf1a159fef contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 fillMode.ordinal(/* M3-LEAF kind=EMPTY id=ae74888f3fc062348981aa15a01c6a59831518955015024a0b9a287835227a96 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
)));
        } finally {
            /* M3-LEAF kind=INVOCATION id=0bf94cba707b05bc3d7a9e51019ed78c6977b69d145ecb445cf69cdb124cd60a contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

   /**
    * Returns if {@link PathFillMode} describes area outside {@link Path}
    * geometry. The inverse fill area extends indefinitely.
    *
    * @return  true if {@link PathFillMode} is {@link PathFillMode#INVERSE_WINDING}
    *          or {@link PathFillMode#INVERSE_EVEN_ODD}
    */
    /* M3-ATOM kind=METHOD id=4c3f4a20ab223d24c703220e849bd043a804425be85f0d602e1a3f5ce4bd6f04 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
   public boolean isInverseFillType() {
        /* M3-LEAF kind=TRY id=fef494358d8630a640cdda337472b3a3a6728882b5d63907f763a575fe5f64e7 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=9ccb6519b80a16d9237ef51a0db4a708c5d918f3ae89e77f6177388f2eab552b contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=f262014352e5fdf772276a6e03930793c2b1bb913769db84d9c8d1854c37b4bb contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=a80a97da3af77542cd71ce5f6288be15bab0b881e958ad238683d38fc87b18c9 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=f3a070f710ec8dffa439fd35532f920702259a51c831fc33b0ae41c359e766ae contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nIsInverseFillType(_ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=a7e2f2c85c8eab60fb6835f4beaf4ca599fe56242dce92eb2dcad2cfc6e3c69a contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Creates an {@link Path} with the same properties and data, and with
     * {@link PathFillMode} replaced with its inverse. The inverse of
     * {@link PathFillMode} describes the area unmodified by the original
     * {@link PathFillMode}.
     *
     * @return  new {@link Path}
     */
    /* M3-ATOM kind=METHOD id=ae94961c6fdb3803e1ddc00cab1af35ad3689488cf9f4f9b93a27dcb8f8134b1 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public M3Path makeToggleInverseFillType() {
        /* M3-LEAF kind=TRY id=713458fbe7c429813599e46fa5b592428499346476cc818df4d4f45fc3f78001 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=c3e3a701a9fbcc4c00c81b7047f999346b1b2e5a351714c20eda779677b9557b contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=c4dbbb80dce3148691a59264ccbee5620cb5fcdfe8eaa7443045c51bd6bade81 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=8a03f2e24562b9037c6b6a3d8e2825d0ee35fc61aa016722dbbe2be3cc8b0d3e contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=CONSTRUCTION id=a017c652bd381134987677f36a8083461d9e7d5cab90e146d9de2c47ee6b17ef contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3Path(/* M3-LEAF kind=INVOCATION id=d5b53dad49b0103ae70ab6513fea7eb2bfa514f381ac4287fb7680f94c415e8f contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
_nMakeToggleInverseFillType(_ptr));
        } finally {
            /* M3-LEAF kind=INVOCATION id=c98d2688dd62b6068038047f8f7563a4f0285f92837596d1364f36080fa5999c contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Returns true if the path is convex. If necessary, it will first compute the convexity.
     *
     * @return  true or false
     */
    /* M3-ATOM kind=METHOD id=aa1bc47304d692b51fe6e54861cf8a00e359f75a2f46a7dc763aaddf292b7528 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public boolean isConvex() {
        /* M3-LEAF kind=TRY id=6a67cd89b49745c572221373fd1bf434240dbab9de607edbfa773976e7c7dec0 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=f3b569801ddca9bd6eae457b8191d7a0655078a98c8d283b6bd674b554b51c64 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=c6f1b19400bfc387ea90ceb99a6921b655eb75fce4f3c5f748feb0b5839e4614 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=5a96de112c08ea0761f42a2a014bce66710216f7131da7e22f25dd5c77fefbf4 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=dd963ea7473a5921c853740a1d8a21336c40b51d78eda1e3c75b7062456739c3 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nIsConvex(_ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=f71b50be71f91762098bda72762e3c6d914686fe376d64f41d6428b39a7328cc contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Returns oval bounds if this path is recognized as an oval or circle.
     *
     * @return  bounds is recognized as an oval or circle, null otherwise
     *
     * @see <a href="https://fiddle.skia.org/c/@Path_isOval">https://fiddle.skia.org/c/@Path_isOval</a>
     */
    /* M3-ATOM kind=METHOD id=a992ce0ff51e052743a7bdd27fd30bc5513427dcffd05898362b67171ce6da3f contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public M3Rect isOval() {
        /* M3-LEAF kind=TRY id=879426061a53cb9d9e0357e80f94b18344e3bad5fd73dc1cb1e89ec6f999b9bd contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=ac03068c35bf14e4cee53d678206abd076e8b125c43fa22cfee256d06dee96d1 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=14c15a1ec76468cbdf4c94c8e6360f54690e12d40c2352a588b13c813a4161c3 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=68fabebe2f6a62faf076e64ccd3ec4470bd4cd0d99afe791fecc8d0cdcab16a6 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=eea696417d87eecafa6fa68a07ad94def60f46f8c1d61aa0c12d0912b1b19fcd contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nIsOval(_ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=886fa1d58e87bc884ba3678a9798bd353b38d849526019d58df7a79a6437e7f1 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Returns {@link RRect} if this path is recognized as an oval, circle or RRect.
     *
     * @return  bounds is recognized as an oval, circle or RRect, null otherwise
     *
     * @see <a href="https://fiddle.skia.org/c/@Path_isRRect">https://fiddle.skia.org/c/@Path_isRRect</a>
     */
    /* M3-ATOM kind=METHOD id=14d2d6653648c8a6942c3656e3382d5a69eb225d3de5f9de1541ca43592c4347 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public M3RRect isRRect() {
        /* M3-LEAF kind=TRY id=7851c965b359773843c8e4fc42d9cebd45b8e0aeb5f36f0314a0764f6250d565 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=4b6e390093f84969a9e20273ffc943bfd6f56bbd1a4826f9ab5d96f9bfaf4f0b contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=874675c6855a278d362d1172375c21bb01ebef12535daa6213cfeb95f6ddcd88 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=866df4c2732fee44a60819009a2c65d0c64fc73e054653a0e909d553a9702752 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=beb3a284642848f85f447e01e03778da758111fc57243cd8fec8a754154ecf94 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nIsRRect(_ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=10532590ac655e2e39459bb21d0a3a079a9fb6778ec4c842d1072af51232ea04 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }


    /**
     * <p>Returns if Path is empty.</p>
     *
     * <p>Empty Path may have FillMode but has no {@link Point}, {@link PathVerb}, or conic weight.
     * {@link Path()} constructs empty Path; {@link #reset()} make Path empty.</p>
     *
     * @return  true if the path contains no Verb array
     */
    /* M3-ATOM kind=METHOD id=dbdf9817d1fffbb30d170b325d1f903ce04db5b70f26a8ba04f0e6cfbfd284f8 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public boolean isEmpty() {
        /* M3-LEAF kind=TRY id=44127de256e2cab11a11e4fde21e685645d5cb7e35c5ff5b9968bce51cfa5556 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=8aea51867074408158a5061c8a988c7c549a86d007734cad928c431e26ff717f contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=4418e8d876b3636b16e19f57b57aeaf39cc321ad53f3edd782552ddf64f2a264 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=19f837068baba16889798c08afacdf974621ce4f3809096dda788e3fb89ef4d8 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=04e637c26e291a12b063cc3fc47cf58a6f5163b27b2aede5e86ecd04a105876c contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nIsEmpty(_ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=abdbb7c7971a53ca1526f038c72fb4e14c2935065b3e55ccf24cecbb89877258 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Returns if contour is closed.</p>
     *
     * <p>Contour is closed if Path Verb array was last modified by {@link PathBuilder#closePath()}. When stroked,
     * closed contour draws {@link PaintStrokeJoin} instead of {@link PaintStrokeCap} at first and last Point.</p>
     *
     * @return  true if the last contour ends with a {@link PathVerb#CLOSE}
     *
     * @see <a href="https://fiddle.skia.org/c/@Path_isLastContourClosed">https://fiddle.skia.org/c/@Path_isLastContourClosed</a>
     */
    /* M3-ATOM kind=METHOD id=84004fa8a0edd1da381abf1b13f4c0aea4780f54ecc980bdd8ed65d01dd2cc1e contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public boolean isLastContourClosed() {
        /* M3-LEAF kind=TRY id=a7413470bf406ca7d7db9decba1d9d0ad8724718bab83f68f83d7afc2541c9d7 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=b3242397f689f2ad46465069791d50dab300741ed30481bb8f423079b1c3e7b7 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=908ca48cd0a061322f913c1631230aa233d11e7cb1c0be584f01d8ed8185ec45 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=1ef94e2c084321a289d53b89080a84f6808914a3d3a9cf7411e35c303f759473 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=b3839895f56289585781f58733e2dc9bf20961ec846a32d0ca1783ecd846f877 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nIsLastContourClosed(_ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=e9dc8a9b33d75862e768a34bbe80bc56f91ce8098c7e06ed057474a2e022c871 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Returns true for finite Point array values between negative Float.MIN_VALUE and
     * positive Float.MAX_VALUE. Returns false for any Point array value of
     * Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, or Float.NaN.
     *
     * @return  true if all Point values are finite
     */
    /* M3-ATOM kind=METHOD id=865ac3c55449191c2a7232848508dfb06c173e7bba70cc635225228cd6e6ae52 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public boolean isFinite() {
        /* M3-LEAF kind=TRY id=5b085693d3288b9b3cf896c912986210ec82659aa31674325e005cfc10fb883c contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=44da543262c805f7041394ae556b81240b03b65dce0f6b4f466ee283b2e08589 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=cabc60a7e5f9fb62ee609b59a5d00cd6e1e815fe9fdb7c2967ddd329e1898f3a contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=37bdc7ec38351a1e2e3f09443a40976eceebf5a5c25a2efa410c9cd4c8cdcf4a contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=c427c8d4553847c85bf15dff2fb2bd66d4b5a022057e0112ee9c983c10f8c24f contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nIsFinite(_ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=b7dc42e9045abbb94a5aabc63724242ef52507e836a0d7efbc12f44fc8cafb86 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Returns true if the path is volatile; it will not be altered or discarded
     * by the caller after it is drawn. Path by default have volatile set false, allowing
     * {@link Surface} to attach a cache of data which speeds repeated drawing. If true, {@link Surface}
     * may not speed repeated drawing.
     *
     * @return  true if caller will alter Path after drawing
     */
    /* M3-ATOM kind=METHOD id=64a24f360528f8a2dc8700080157875adf7c9ebfcc834a768665c58bde14c946 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public boolean isVolatile() {
        /* M3-LEAF kind=TRY id=3a7f25a34ee3336c44d24aadfcb0fd6ca89b60a2cd1c4b3de7d17ce83483988b contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=8daccbe955e3f212e5b3a1e593e17b6f76b2220401fbbf2c29cec4886f56ddf7 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=14f29bf7245452ce1e467a220b0a4d2b96c7cc593a84a6eba7880670facef5e4 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=e063f8498e6ed16d0e6dcbcc7fd1df1bd140b37a919b05f36f512ff7480c6ffd contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=1f0f49cc4a101bc775d735ce161aec3ea918692c0338c45fee11d5558b3d3831 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nIsVolatile(_ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=ae9bd8eb5569f31e3f8a7b4765ff087da71b340a8c9cf188677a2ea4a610e233 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Return a copy of {@link Path} with isVolatile indicating whether it
     * will be altered or discarded by the caller after it is drawn. {@link Path}
     * by default have volatile set false, allowing Skia to attach a cache of
     * data which speeds repeated drawing.</p>
     *
     * <p>Mark temporary paths, discarded or modified after use, as volatile
     * to inform Skia that the path need not be cached.</p>
     *
     * <p>Mark animating {@link Path} volatile to improve performance. Mark
     * unchanging {@link Path} non-volatile to improve repeated rendering.</p>
     *
     * <p>raster surface {@link Path} draws are affected by volatile for some
     * shadows. GPU surface {@link Path} draws are affected by volatile for some
     * shadows and concave geometries.</p>
     *
     * @param isVolatile  true if caller will alter {@link Path} after drawing
     * @return            new {@link Path}
     */
    /* M3-ATOM kind=METHOD id=af715ed2d7bf4ed1ba7621c999d6688ebb94f92975ebde2eecb536fbf2cb2d47 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public M3Path makeWithVolatile(boolean isVolatile) {
        /* M3-LEAF kind=TRY id=23efa3d150c22487ddd39903c61a04eabd82efaaf10476b0f71209f001ea3be9 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=463a1f393f542e4907153851f4cc2989aedb7257eb05644c92ea09de9707d0c8 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=06b87a97d36849b0e17e695bd2fbff428eb4d5018d910edf481b9ad0db072b2f contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=100d41817f1242885da65c5cacc3b1db1a52a08e622784407d7817be8359cc27 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=CONSTRUCTION id=3099331903726837342c0fa3af4cda0e1c2a16f7cc67f2f9720034609ded51a2 contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3Path(/* M3-LEAF kind=INVOCATION id=f69b56a839a12c777a00e05f11bef7ce8bec11b00ba2175ffa56fc51184f7ca0 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
_nMakeWithVolatile(_ptr, isVolatile));
        } finally {
            /* M3-LEAF kind=INVOCATION id=ea370caf786bd887029dc2cf6adb9eb0e11c16eb7de1ec65530e00e551ac1f5f contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Tests if line between Point pair is degenerate.</p>
     *
     * <p>Line with no length or that moves a very short distance is degenerate; it is
     * treated as a point.</p>
     *
     * <p>exact changes the equality test. If true, returns true only if p1 equals p2.
     * If false, returns true if p1 equals or nearly equals p2.</p>
     *
     * @param p1     line start point
     * @param p2     line end point
     * @param exact  if false, allow nearly equals
     * @return       true if line is degenerate; its length is effectively zero
     *
     * @see <a href="https://fiddle.skia.org/c/@Path_IsLineDegenerate">https://fiddle.skia.org/c/@Path_IsLineDegenerate</a>
     */
    /* M3-ATOM kind=METHOD id=5bbaa3c35337b32feec06a2a483b92d177d4a1a74143b0098b2957e2d419818a contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public static boolean isLineDegenerate(M3Point p1, M3Point p2, boolean exact) {
        /* M3-LEAF kind=INVOCATION id=820a41ed439ddf81e8822e10a9840224d64614ee7eff741cbc90e9908d7d2b73 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=36d987c1a594bfc60caf2c218309875c9c7344b4bbb465abe7de09396c1bddf8 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
        /* M3-LEAF kind=RETURN id=d8ff9d67b742e4879a96869856cdcdd475d364d058e54558b5369e24ba5b0a06 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=INVOCATION id=ded73d216acb73c78602ff028558ce16253d7c93aaf6ca43660684d68b29ab64 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nIsLineDegenerate(/* M3-LEAF kind=FIELDACCESS id=ae05ec1b08df9ea7f7c7df495b3b42a0c73d5c2f6d30a0b8397f34f3d0cb7bdc contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
p1._x, /* M3-LEAF kind=FIELDACCESS id=c9ef69bd55a842f675a2b5946501db63d9b242870003c8178108f849c6125b93 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 p1._y, /* M3-LEAF kind=FIELDACCESS id=833f5ea700045f2f9e667ba7265417a5dfae2187035bfec9550e546cc90ec751 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 p2._x, /* M3-LEAF kind=FIELDACCESS id=76a3cb7cb5b215e176be7ab377a06cd3fdaf5566bb207e47306cb7103b68e9e4 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 p2._y, exact);
    }

    /**
     * <p>Tests if quad is degenerate.</p>
     *
     * <p>Quad with no length or that moves a very short distance is degenerate; it is
     * treated as a point.</p>
     *
     * @param p1     quad start point
     * @param p2     quad control point
     * @param p3     quad end point
     * @param exact  if true, returns true only if p1, p2, and p3 are equal;
     *               if false, returns true if p1, p2, and p3 are equal or nearly equal
     * @return       true if quad is degenerate; its length is effectively zero
     */
    /* M3-ATOM kind=METHOD id=448595e17c72305c5534c57a30f604f9f8b1e210283245f3f2e673815bbf822d contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public static boolean isQuadDegenerate(M3Point p1, M3Point p2, M3Point p3, boolean exact) {
        /* M3-LEAF kind=INVOCATION id=267bc28cfeaaec662e4d9c9ec7351d4f1c747fd65d3910f5d6adafc292ae3d98 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=78e22445f15ed6a6131d9b300e0fe092e9d7bdbfaeafabd6916b1c655a3e597c contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
        /* M3-LEAF kind=RETURN id=a84dd37ca2998e8f7d8c23145a26617aca38069c743d60e2bd507bee636977a9 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=INVOCATION id=c24a352f38a39ea014c229dff1b153fd853b15a45918379106d38788ac7bb546 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nIsQuadDegenerate(/* M3-LEAF kind=FIELDACCESS id=3dcdef87ed108ff35e40cfc7556aaf6c0d538872eae38db63008660796fe1295 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
p1._x, /* M3-LEAF kind=FIELDACCESS id=cb60590e7ec616f0966dbf7601cdc5dfaabcb4ae7cd0f265a5b43086193d71a3 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 p1._y, /* M3-LEAF kind=FIELDACCESS id=899ec8fb50506876d257eba6f82980089f7aee06a6d263eb630d421ed35ade5a contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 p2._x, /* M3-LEAF kind=FIELDACCESS id=ea2436c2a6a869f1e9fe53974304ebf2adee15526f9cd370a64e9ef84aea88a2 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 p2._y, /* M3-LEAF kind=FIELDACCESS id=e07ae69cf63390c4b19f52608380304f377433ade0c088597f558b86fa6557f5 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 p3._x, /* M3-LEAF kind=FIELDACCESS id=0c32253fb21eca9140cfb3d51bed73576948c2623833f4f16d8600965c041f30 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 p3._y, exact);
    }

    /**
     * <p>Tests if cubic is degenerate.</p>
     *
     * <p>Cubic with no length or that moves a very short distance is degenerate; it is
     * treated as a point.</p>
     *
     * @param p1     cubic start point
     * @param p2     cubic control point 1
     * @param p3     cubic control point 2
     * @param p4     cubic end point
     * @param exact  if true, returns true only if p1, p2, p3, and p4 are equal;
     *               if false, returns true if p1, p2, p3, and p4 are equal or nearly equal
     * @return       true if cubic is degenerate; its length is effectively zero
     */
    /* M3-ATOM kind=METHOD id=4c7f1b9171fb166ee04bccc5905e206cf218fb284faa3848df45735300609cc4 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public static boolean isCubicDegenerate(M3Point p1, M3Point p2, M3Point p3, M3Point p4, boolean exact) {
        /* M3-LEAF kind=INVOCATION id=69e3baee57f7f5c40af215af61a001cd4a0c0b296032dc8b7ae17f8a79ca2399 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=8693b5acb1aa9448e2f7eee0d4a009e1310ee4edb2d528453cfb358b7fb60e04 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
        /* M3-LEAF kind=RETURN id=bb0f9f43f94741393fb203550383dd5ef5a705c6014cef5e2f913d4d4152a073 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=INVOCATION id=260504d3d819bfe05d73458ae578cb18bb5c4aef92d994efb847971b3195e42d contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nIsCubicDegenerate(/* M3-LEAF kind=FIELDACCESS id=f9c4cabea445ecc118850e6045d1c841262ff243c65f0c2c7aba3839aa4aec74 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
p1._x, /* M3-LEAF kind=FIELDACCESS id=0232dfdff99b87c5c673f6ef9e176dc91b708fe4b3906ec9f14ea562ad161a3b contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 p1._y, /* M3-LEAF kind=FIELDACCESS id=380b5e54305b19da427636ba819288bda4688875a928860f6b63b5d574eeb486 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 p2._x, /* M3-LEAF kind=FIELDACCESS id=2d647ae30daa8a9f457db33ea207f9b9d14e6960f9ce17b6b6beea961e90a22b contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 p2._y, /* M3-LEAF kind=FIELDACCESS id=3ebb1657021a36d1e29b3d4ee72182ecffd3f0d1c28037b86b684c1c29cc1288 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 p3._x, /* M3-LEAF kind=FIELDACCESS id=aa81b03ef0118731ebd62f952c68e373975721f6583d6090e1fbf219f183178a contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 p3._y, /* M3-LEAF kind=FIELDACCESS id=cb4980143b2c6e79d445dbb517f41b4c2f6a0f9c4ae1ccf4a072eab09a153a4d contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 p4._x, /* M3-LEAF kind=FIELDACCESS id=cb9e787bb39289fb3647ab5ea824f6d2bf448898c55d1eb403ba64658d2030c5 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 p4._y, exact);
    }

    /**
     * Returns array of two points if Path contains only one line;
     * Verb array has two entries: {@link PathVerb#MOVE}, {@link PathVerb#LINE}.
     * Returns null if Path is not one line.
     *
     * @return  Point[2] if Path contains exactly one line, null otherwise
     *
     * @see <a href="https://fiddle.skia.org/c/@Path_isLine">https://fiddle.skia.org/c/@Path_isLine</a>
     */
    /* M3-ATOM kind=METHOD id=2d548dce0b27df3f6dd5c22e3955cb2e3bcb71e792311e25b79904563984a135 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public M3Point[] getAsLine() {
        /* M3-LEAF kind=TRY id=a243c2e6f8760fe0927dbac2bf30d60289620c1f8c0b9128c898bf10e439c1a8 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=218cf44332a47d71a87eeca3ae37ebebd599783525339627da35ea808f432360 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=e1f4ced352aa19e79a1f56d0ebd945c79c23819954d17d6416817c8eb6425e90 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=37115cf73d8ff7dcd069220a1fc631fa5e4899b588c02d99724ee9a0bdc24c80 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=3b9c8ce1a04675f6f9d2616775443c69c1b5fdeec93ec81056f8c697066bf2be contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nMaybeGetAsLine(_ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=70e7ab3be833abca1cec6bbd6aabeffa86850fe371d9d609e712dc83731c505b contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Return a read-only view into the path's points.
     *
     * @return  array of {@link Point}s
     */
    /* M3-ATOM kind=METHOD id=471188612321934a15772a0b607fa43efefee9954ccdd5b0deac42d4ac243468 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public M3Point[] getPoints() {
        /* M3-LEAF kind=TRY id=18583af65f3868494056117a3846882fc6a56105b7fff6d08934788655323f4a contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=562dbbd53652950c2acdd7af884c224feeb92d96e1edb2bcab10d8e5378bf303 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=ae1c081c61bf234a905869c1a6d6ac848abf5d9b552399eb55dbdd115e7ce40b contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=ad1a1b4b8425f31f161860dbb55d5e4fa65015fbf05ba4fb91c1fa5fb46e0c17 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=7a2ec99ad5904bb6acc2d0908d52e51752e48ca8824924dbe8080f5b0716c36b contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nGetPoints(_ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=1476b6f0ea74b9aeeb50f1532b80b6adba104c6f3ebb9aef93994d66bdb35df8 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Return a read-only view into the path's verbs.
     *
     * @return  array of {@link PathVerb}s
     */
    /* M3-ATOM kind=METHOD id=e02ecff1df65147287e3e5f0c64dbd3a4bf2c19c097bc84473f764572c99d19d contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY,ITERATION */
    public M3PathVerb[] getVerbs() {
        /* M3-LEAF kind=TRY id=52908fce7edefe48118416fb5651fbe587783ebcf4a792a498130734e71e1bb6 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=ebd04dcd5d8a9925b2f2d6c4ced6391bf3cb1bb2dee4d4a4a8d3bdb016976525 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=1a915278268d454ddbd580d691966411f8a9cf86ca0368e34e0dcccf0c917222 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=LOCAL_DECLARATION id=80653df9dfdb192d5d293a0d45e92977e03e8f16e67000993c8eb4a433c207d7 contract=INTERNAL_OR_PACKAGE patterns=LOCAL_DECLARATION */
            int[] verbOrdinals = /* M3-LEAF kind=INVOCATION id=6b26689dccebf6d5ee05993118e4eb38996e11d08587bbaba2e0b2e67a9fe1e9 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nGetVerbs(_ptr);
            /* M3-LEAF kind=LOCAL_DECLARATION id=a7df8cf1f4db68326c3989b2277fccf2b943233556e3b85383a2b2c5f802f57e contract=INTERNAL_OR_PACKAGE patterns=LOCAL_DECLARATION */
            M3PathVerb[] verbs = new M3PathVerb[/* M3-LEAF kind=FIELDACCESS id=ae067dcecbb5d1cfaf09ad3e61bfcdb477cef28162932020a75b71aa51f19f65 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
verbOrdinals.length];
            /* M3-LEAF kind=ITERATION id=2796110b16f6848a0e9818745deb23fc3c5350a3fb45b7af56be40d74c1510b8 contract=INTERNAL_OR_PACKAGE patterns=ITERATION */
            for (int i = 0; i < /* M3-LEAF kind=FIELDACCESS id=540fc701e769c147bda73ab4a1350f6407799a6854452639d9fdf833e00321bc contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 verbOrdinals.length; i++) {
                /* M3-LEAF kind=MUTATION id=735955f12e3de6e68593555779047d3965d7949614cbe17ba24fa71a95683afd contract=INTERNAL_OR_PACKAGE patterns=MUTATION */
                verbs[i] = /* M3-LEAF kind=FIELDACCESS id=5835897dbe4b192c2c6a4cbb17728f602c6051593eb465ae5abe2e2aebd32df2 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 M3PathVerb._values[verbOrdinals[i]];
            }
            /* M3-LEAF kind=RETURN id=e4064ec6abc57e1744917d642fa42b8a6970bfa81fe6c2b373922601a404f6ec contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return verbs;
        } finally {
            /* M3-LEAF kind=INVOCATION id=a3e2760e0ac57e7282cf9bdbf5fda53035f2c0d1766b32009cbe7f0d767b96a5 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Return a read-only view into the path's conic-weights.
     *
     * @return  array of conic weights.
     */
    /* M3-ATOM kind=METHOD id=25a4e8cf751899454c6158a13b7ff6c2b1415e1c0a9d102c7e98746b158f91c6 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public float[] getConicWeights() {
        /* M3-LEAF kind=TRY id=1c9c5962a3e59de565df5cead8766bfcf872b022751792420b4605c9cfdc0108 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=419a5fb82d25fd8cacec0d956f3a6ce9221e62ad3fcf575e2c2a489896215ab4 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=24a449cc0f189a76b3fc857964e7f343b4c33900bc451da77e26a09887f02ffa contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=cfed4d86c58d174142c4a1e6fcb7a0d229fee9e0b9beee97bad633bd3958a160 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=02afce23df52b563eb3529b65473f7b72fc5630f85922ddf3ceb2ee563a7f5e9 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nGetConicWeights(_ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=62f9dfee79390448fd56f78c1e756e759ceec7d1d98f394660098178bd17a6ca contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Returns the number of points in Path.
     * Point count is initially zero.
     *
     * @return  Path Point array length
     *
     * @see <a href="https://fiddle.skia.org/c/@Path_countPoints">https://fiddle.skia.org/c/@Path_countPoints</a>
     */
    /* M3-ATOM kind=METHOD id=3cda6f4d20e8d36342c3e49ef881e9c6461237d08ae1fdac498224802e064839 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public int getPointsCount() {
        /* M3-LEAF kind=TRY id=7eb378910e86558e8aba17637ea28799288f526a3123d5f2ce133a7ed0166c5f contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=bdf6410fc3cb86dc9b46c0179c3d2b61c7c8c5e4cdd43648507f68b3332e5e0b contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=f6dde381f638e4b74b46cb28a2de19fc3de336bb0d77f2c04b1900cdc19ac8c1 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=9d649ee270e8f77f28699b40396927fa95885cf6c679d81266daba5c8d5ee12b contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=4676da3c92f5b6adf4c6a6bcaa0073566d655757d6002a62cbd9a67f3c1c4aa6 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nGetPointsCount(_ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=48fb2138c40fd23a3a5d0ba496ae564a694afb58319f655152620cf7fd5648b0 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Returns the number of verbs: {@link PathVerb#MOVE}, {@link PathVerb#LINE}, {@link PathVerb#QUAD}, {@link PathVerb#CONIC},
     * {@link PathVerb#CUBIC}, and {@link PathVerb#CLOSE}; added to Path.
     *
     * @return  length of verb array
     *
     * @see <a href="https://fiddle.skia.org/c/@Path_countVerbs">https://fiddle.skia.org/c/@Path_countVerbs</a>
     */
    /* M3-ATOM kind=METHOD id=2be823cf71e06087a8e540513a9f7293cfe201ac23463953a772c27ff2b1f7ca contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public int getVerbsCount() {
        /* M3-LEAF kind=TRY id=8019434061470f3ce7b81cf0d55ce2e8a12b9c9573ed196f8667ee40a7be43a2 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=f4956212d9721b1c1d687997426c6863c1e12d5c9c0121c96ed4455e5124982c contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=09222157e9b1d0a94d21e1f8fa69504134ad2c32908aa5f120a721230a1ca87f contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=9f06e72ef196c0ef21dc1f95acda411ed889c3f3d2c820de53cc9f8ea7f2a7ef contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=0a317fa6495575a2c8e81b8719c985f4e3682e7e8e853ce8597469a329713755 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nCountVerbs(_ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=61584733894111a535dbda5a9593fb650d609e31708477f71d081fa6047b77d5 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Return the last point, or null.
     *
     * @return  The last if the path contains one or more {@link Point}, else
     *          returns null
     * @see     <a href="https://fiddle.skia.org/c/@Path_getLastPt">https://fiddle.skia.org/c/@Path_getLastPt</a>
     */
    /* M3-ATOM kind=METHOD id=cc4314760e3590da02b31b14beabb009fda59978aed951e4671a874f2e36cb53 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public M3Point getLastPt() {
        /* M3-LEAF kind=TRY id=2130a45b4c6e7b4639cfe8e985549794d783e80c9b8daafa3a3fc237b4a149f7 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=6185196721c5ea6c33981a804cabff1c4f4f78f3e98e46f88555c4a2be8c84b0 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=ae18eb5ce04e0460327d4000c75752c72b7992170ac0755bd0144adb88053643 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=f0f5998bf4e03cf9444f3a016fa7d00c02d29c803dfc0d2b9274a619573ec96a contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=65359f3e21f3027e9b404739360663359b3475074b2b0d5baf767e8fd964043a contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nGetLastPt(_ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=64eea9b00d26f4d4e101a4a169c56f1bc268a1c4e9848ad6458d3cf88098fbd5 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Returns the approximate byte size of the Path in memory.
     *
     * @return  approximate size
     */
    /* M3-ATOM kind=METHOD id=ce6ebb123ef0396a5525d3c5f4c3d72e05448efcbeed60fa836cd10c72347d76 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public long getApproximateBytesUsed() {
        /* M3-LEAF kind=TRY id=c805080910a993f468edd687da5d2fe8ede6a6d092588fd515097ce3e11cda36 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=2576c3e57e4fe34274434c821045f00ea64018071fce0e368f43423f8003ea50 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=4652a61c6ec1b8928733fb2aa98f68ac136893f4505a17ebf1b5a1ca345853ec contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=0e72aae4f9254cb7dd5053d24c45500b5a449a762f1a53683611e77637b326bd contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=bf0fd7cf7e9ed030a57e7d11c98115092dd7e500c42a011808440e72ed688b59 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nApproximateBytesUsed(_ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=5a2081cafb76f407e49a6fba413e7f1dcfd4140461a59c180494a5401099471f contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Returns minimum and maximum axes values of Point array.</p>
     *
     * <p>Returns (0, 0, 0, 0) if Path contains no points. Returned bounds width and height may
     * be larger or smaller than area affected when Path is drawn.</p>
     *
     * <p>Rect returned includes all Point added to Path, including Point associated with
     * {@link PathVerb#MOVE} that define empty contours.</p>
     *
     * @return  bounds of all Point in Point array
     */
    /* M3-ATOM kind=METHOD id=99971f4296ea2a6be78c56595626967f66f11a3fe5f41ef39fc164456a249bb3 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public M3Rect getBounds() {
        /* M3-LEAF kind=TRY id=c146f528348bc4c04f8e0ae8d6fcdf6a50bbc239d0d17c93cabe0c7e336cb82c contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=b49b2349e56623ecaaafafa8e208d1b17a1184179599ee64acb5b1b087744d3e contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=c379ec70028d5e3acabaab4b686f06e3a277689d1013d5f8450365f3fbbc6bd9 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=1c6a19bbfb1c0a19741f9417f3d4df66e56bb6e7cd554f695fbe9c395e4124fd contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=220d02e22a149aba2b8e78a38af427e895c65b180adec6d546199ea54b2777c2 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nGetBounds(_ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=bce51baa2ace76c49305287c24e585be3754ebf497ad9177e7cbf955cfed574e contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Updates internal bounds so that subsequent calls to {@link #getBounds()} are instantaneous.
     * Unaltered copies of Path may also access cached bounds through {@link #getBounds()}.</p>
     *
     * <p>For now, identical to calling {@link #getBounds()} and ignoring the returned value.</p>
     *
     * <p>Call to prepare Path subsequently drawn from multiple threads,
     * to avoid a race condition where each draw separately computes the bounds.</p>
     *
     * @return  this
     */
    /* M3-ATOM kind=METHOD id=b2f77c11b1ca975cc50d874711e8ef3640b3381278eb09f2a5486637edb8e5f1 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public M3Path updateBoundsCache() {
        /* M3-LEAF kind=INVOCATION id=952bff578cd5653ccd96089fbc952a2e01e583d939bcd31cf59e8d3b0475f1e9 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=882a7a98f96d0f4c9432a2046cb7503597b1036744ea084387268fca735daac1 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
        /* M3-LEAF kind=INVOCATION id=03d221ed915bf2475d11772f950f889e56e36769e26b762d433a693611007adc contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        _nUpdateBoundsCache(_ptr);
        /* M3-LEAF kind=RETURN id=84ecc57965ce875b0c49f78ce8a03ffdfd83aed71276e672fdd0a42d5a974c52 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return this;
    }

    /**
     * <p>Returns minimum and maximum axes values of the lines and curves in Path.
     * Returns (0, 0, 0, 0) if Path contains no points.
     * Returned bounds width and height may be larger or smaller than area affected
     * when Path is drawn.</p>
     *
     * <p>Includes Point associated with {@link PathVerb#MOVE} that define empty
     * contours.</p>
     *
     * Behaves identically to {@link #getBounds()} when Path contains
     * only lines. If Path contains curves, computed bounds includes
     * the maximum extent of the quad, conic, or cubic; is slower than {@link #getBounds()};
     * and unlike {@link #getBounds()}, does not cache the result.
     *
     * @return  tight bounds of curves in Path
     *
     * @see <a href="https://fiddle.skia.org/c/@Path_computeTightBounds">https://fiddle.skia.org/c/@Path_computeTightBounds</a>
     */
    /* M3-ATOM kind=METHOD id=aefd4bcdac6db18d7b2c8ea89a30d57bec2375d27a46560b029f6cb4f865ab88 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public M3Rect computeTightBounds() {
        /* M3-LEAF kind=TRY id=4832ce3affc9c519a36b813e1d8ca44170ba4cee7ffa3f87c75938deec23dac5 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=87e5d43b3537885e50a7a58270e2ba98e7521c8c5562edf7cc6a4686de407e9d contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=071faafa1ce5298dd04c7ca274e601dff100a4691962cc4acf629f32b0ca5c91 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=79051468f9442c31d3e465b685b7f76e0dca7b7e753aac0944a44a2cbaf26d0e contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=99e1c816a340454dd1b6d62883d8679ad17ffd95b2d57df84ae13175784630b6 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nComputeTightBounds(_ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=4803c5904921f4842d988083248b9c43edb3ff6d1dd66d0f7b186ea1142396f7 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Returns true if rect is contained by Path.
     * May return false when rect is contained by Path.</p>
     *
     * <p>For now, only returns true if Path has one contour and is convex.
     * rect may share points and edges with Path and be contained.
     * Returns true if rect is empty, that is, it has zero width or height; and
     * the Point or line described by rect is contained by Path.</p>
     *
     * @param rect  Rect, line, or Point checked for containment
     * @return      true if rect is contained
     *
     * @see <a href="https://fiddle.skia.org/c/@Path_conservativelyContainsRect">https://fiddle.skia.org/c/@Path_conservativelyContainsRect</a>
     */
    /* M3-ATOM kind=METHOD id=6d644718281db33d2136c953653a62ea5e6fe319ad669119f423691e4c3afee2 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public boolean conservativelyContainsRect(M3Rect rect) {
        /* M3-LEAF kind=TRY id=64e31f0652d62afc60b630c4b74bccb0ddea446263d6a15949008440fa84af50 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=16930808e726e3772ebb406937443c4666616e9a3ed1d6642754237e768daf79 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=58d0d06ca55f18e81acc5facfb0c10572d38c2df0369f9e1bd5f5752c4fa6beb contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=807cd94f7a88455c0c4459efc274e28208a04e468faf167139687fba3536408a contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=5ecce442104cc194a895d9b4bef86a5498597c0d0c0df6642dc38f46289e4b0d contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nConservativelyContainsRect(_ptr, /* M3-LEAF kind=FIELDACCESS id=f6c48bbdc6f306604e83243768b3eaa1547599093ad8ff37940ee8425b861f46 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rect._left, /* M3-LEAF kind=FIELDACCESS id=bd3ea882f0f0ad0dc2f1392ddf8da7f422d30b38ab9c8e37374ed623bceafef2 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rect._top, /* M3-LEAF kind=FIELDACCESS id=bc65c935b8593982435cb140d3d90c01d881857a0effe2b87e80d862eb76e50c contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rect._right, /* M3-LEAF kind=FIELDACCESS id=bb728a0c605ba5e47de58c3703512d54952cbf04f42e3cadf5203a6bd9142b81 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rect._bottom);
        } finally {
            /* M3-LEAF kind=INVOCATION id=465d5804497ba74e96e161e793101243292e2dbe8c2c43b64e2dff4fe01e4a76 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Approximates conic with quad array. Conic is constructed from start Point p0,
     * control Point p1, end Point p2, and weight w.</p>
     *
     * <p>Quad array is stored in pts; this storage is supplied by caller.</p>
     *
     * <p>Maximum quad count is 2 to the pow2.</p>
     *
     * <p>Every third point in array shares last Point of previous quad and first Point of
     * next quad. Maximum pts storage size is given by: {@code (1 + 2 * (1 << pow2)).</p>}</p>
     *
     * <p>Returns quad count used the approximation, which may be smaller
     * than the number requested.</p>
     *
     * <p>conic weight determines the amount of influence conic control point has on the curve.</p>
     *
     * <p>w less than one represents an elliptical section. w greater than one represents
     * a hyperbolic section. w equal to one represents a parabolic section.</p>
     *
     * <p>Two quad curves are sufficient to approximate an elliptical conic with a sweep
     * of up to 90 degrees; in this case, set pow2 to one.</p>
     *
     * @param p0    conic start Point
     * @param p1    conic control Point
     * @param p2    conic end Point
     * @param w     conic weight
     * @param pow2  quad count, as power of two, normally 0 to 5 (1 to 32 quad curves)
     * @return      number of quad curves written to pts
     */
    /* M3-ATOM kind=METHOD id=9621a25390fbf528f1205323295dc426d5ea78f4d87bbafaf026e040131a7e41 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public static M3Point[] convertConicToQuads(M3Point p0, M3Point p1, M3Point p2, float w, int pow2) {
        /* M3-LEAF kind=INVOCATION id=4d33a36334b5106d62d9644f0f39cd50cac89492e87e591e02cd2a2deeb7fa3c contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=bda3342a25a1ca90384640cd629a3548a1f7fee28578fc0341c502bcf6c4fe52 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
        /* M3-LEAF kind=RETURN id=4e39cfe94949caee35c5fc9303f7870d471e1503828f793248b17686f9b97cf1 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=INVOCATION id=7765b9f5452f57e239bbe31ef399ca995e904369c46b24f28dbf0b13af8e1035 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nConvertConicToQuads(/* M3-LEAF kind=FIELDACCESS id=eed8bd60b31466b57fd3839056b3fff2997c4cf8d4987769673d5b8114b895cc contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
p0._x, /* M3-LEAF kind=FIELDACCESS id=b0215b46c1dcd42e87b6a07cc045f7c1f3024403a66a50404f13188978a609c3 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 p0._y, /* M3-LEAF kind=FIELDACCESS id=ac520a908d1b51d176b8c1ad266ccefe92d0f12ff67e355d04c43fbb1db02ce8 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 p1._x, /* M3-LEAF kind=FIELDACCESS id=24d472149bfda6d57775cc4bedba93f293369519d17e9919871126ea64767d42 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 p1._y, /* M3-LEAF kind=FIELDACCESS id=290d4577dfabaefaa35155d1a5019b68772009cfc9d148aa76736e31a187ea32 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 p2._x, /* M3-LEAF kind=FIELDACCESS id=a90ae422ccbaf5899dd911ce29e7bf3c1a5b1b7b8199e65a22b08ed2960e9c45 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 p2._y, w, pow2);
    }

    /**
     * <p>Returns Rect if Path is equivalent to Rect when filled.</p>
     *
     * rect may be smaller than the Path bounds. Path bounds may include {@link PathVerb#MOVE} points
     * that do not alter the area drawn by the returned rect.
     *
     * @return  bounds if Path contains Rect, null otherwise
     *
     * @see <a href="https://fiddle.skia.org/c/@Path_isRect">https://fiddle.skia.org/c/@Path_isRect</a>
     */
    /* M3-ATOM kind=METHOD id=b34670929d183aeb9c0b81442d6c735867836e0513a6d6288bbb36e0cd1f433f contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public M3Rect isRect() {
        /* M3-LEAF kind=TRY id=0678d12ae34cd01a89cac6c48eec047dc9e9f13c649123b27eba5490fdf4cb35 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=cba0b21a9c02cb0dc96e8ce201ea37397736c69601d98d674cb0a212135560de contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=753026c18668241b6c39ecc39b5de7fdb3d0678c556a0d619731b0a2ba7c1053 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=beb139dc6068e6ec230cae03622c6a20847056c9112b6c1173b1313278918e4e contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=74ed900a609dea187f15902f7564034dd18f6c961e080c261b121ce00ce49f35 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nIsRect(_ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=2e25a0558286cccac0c69897b42de9061af46a62f6dff8ddfce1ca97e7d15231 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Return a copy of {@link Path} with verb array, {@link Point} array,
     * and weight transformed by matrix. {@link #makeTransform(Matrix33)} may
     * change verbs and increase their number.</p>
     *
     * <p>If the resulting path has any non-finite values, returns null.</p>
     *
     * @param matrix  {@link Matrix33} to apply to {@link Path}
     * @return        {@link Path} if finite, or null
     */
    /* M3-ATOM kind=METHOD id=ed0a0cdf9690e1ae461b0862c99b0690fbaf6fe3a477bb7e4d72e3d03ff515db contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public M3Path makeTransform(M3Matrix33 matrix) {
        /* M3-LEAF kind=TRY id=6dae128721eacf217fdc61488cb28b811c37cd506aa546bc527bd095ee787479 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=664a37f8d55d4fc77ddf07cb3279648e992c339c67e89e579ac05d7b7189c05e contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=baebaa90db1cd93c55309617eacb64490c418d7ec9716f66dfe06ecbdb53363c contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=LOCAL_DECLARATION id=45e42520ed3a88611fa1fd444b3ff3c7faf9ea87777a83585dee5e42e82c8b1c contract=INTERNAL_OR_PACKAGE patterns=LOCAL_DECLARATION */
            long ptr = /* M3-LEAF kind=INVOCATION id=5f610895a5cfebe20ceba62fa428d5adddbf3b51fe890830bdff97541b64ad14 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nMakeTransform(_ptr, /* M3-LEAF kind=FIELDACCESS id=348a8981add62ff405f76ec4b8130b50551814382eaf80794b9bf7ffc2641d92 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 matrix._mat);
            /* M3-LEAF kind=RETURN id=c4a9d0122debe17ea299af2366d28fccd09d12574d4a345b9b90ec4c7bfb3004 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=TERNARY id=cd3146b2f7e35032083cbe478f66097aed06962e9afa061cf6e7d1d894b5a533 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 ptr == 0 ? null : /* M3-LEAF kind=CONSTRUCTION id=10de0aa1e06fd1fa14ca86a36c34a76f6d80cd5d05f035acabed73dd18f44a48 contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3Path(ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=4e7787f82bc5f7a6596f965f7915d2169102820d90eb579f57a28625b07ebc84 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Returns {@link Path} with {@link Point} array offset by (dx, dy).
     *
     * @param dx  offset added to {@link Point} array x-axis coordinates
     * @param dy  offset added to {@link Point} array y-axis coordinates
     * @return    new {@link Path}
     */
    /* M3-ATOM kind=METHOD id=cb52303627b4e7931718a64a200359a3f6673b3fe1407054af8854eb8258f470 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public M3Path makeOffset(float dx, float dy) {
        /* M3-LEAF kind=TRY id=86f640cac6185aa0902615974b33009bba815410386511c14b067d1265309dad contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=d9d24b35261e8abbbafafcd938582d59e12f55d94a7b81726e63be57766a1741 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=e2723fbcf7be478d9da55b5adc794d710f99bb91631232fd4b39963881cc840d contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=LOCAL_DECLARATION id=87951f0d251d2bbed68a297154467988a81533bfb00cbc851cbec513f55cdebe contract=INTERNAL_OR_PACKAGE patterns=LOCAL_DECLARATION */
            long ptr = /* M3-LEAF kind=INVOCATION id=ab44e34883d20f1c3f9cc8c9d632d7516c0cafb01ec5f5848aa2f5ffc139848d contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nMakeOffset(_ptr, dx, dy);
            /* M3-LEAF kind=RETURN id=062d009d7129bef23e1267fadd5c2c3f6c2592a328ed5bfdc471c42ce63f78c2 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=TERNARY id=6d915cf0058e14741aa4d7d09609434584d09c45057fc18fb1291becb402e869 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 ptr == 0 ? null : /* M3-LEAF kind=CONSTRUCTION id=10aaa49feab16e479f6b2291f2a56ea2466a4c26ea335550b38c52a9f61aa525 contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3Path(ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=4cfdcb2318e10eb4ee5be89339393fef3f90d2257ef797099f985b19bd52a4b2 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Returns {@link Path} with {@link Point} array scaled by s.
     *
     * @param s  scale applied to {@link Point} array coordinates
     * @return   new {@link Path}
     */
    /* M3-ATOM kind=METHOD id=43e5a3e2daa94a06845660379ca411fd8c73fa24e46639a685dd1517a242e62e contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public M3Path makeScale(float s) {
        /* M3-LEAF kind=RETURN id=c8635fbeb39b5301aec9b0069602eb1e1b867131f5698e93b6abbabfd704867a contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=INVOCATION id=f0e787aec43c1a65016af6cf3f9d149bcdcc06b33aec2b0171b0e1942b1ff991 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 makeScale(s, s);
    }

    /**
     * Returns {@link Path} with {@link Point} array scaled by (sx, sy).
     *
     * @param sx  scale applied to {@link Point} array x-axis coordinates
     * @param sy  scale applied to {@link Point} array y-axis coordinates
     * @return    new {@link Path}
     */
    /* M3-ATOM kind=METHOD id=607f2b669291d2130223fc7487275ad7730b83195611e9f8833d6b40970282c2 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public M3Path makeScale(float sx, float sy) {
        /* M3-LEAF kind=TRY id=31f4032b7999cd099b5c448dae530bd4244412d708801d0babb1aa470e153154 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=a9027fe09e4cf8357a7d333dc8afbcd973485206bc168de939a496e8fd829fb9 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=f657bde6892e4d245cc998fc4de57b73a2ca61e76203a19cb243c001caf30c6f contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=LOCAL_DECLARATION id=db268152805d77811daccb0c99dffbabf42a028edb4d659fc5093548b2473998 contract=INTERNAL_OR_PACKAGE patterns=LOCAL_DECLARATION */
            long ptr = /* M3-LEAF kind=INVOCATION id=9b58b74371a88d6159054e95445974fbe7e34ccdbef58572ed298793704378d4 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nMakeScale(_ptr, sx, sy);
            /* M3-LEAF kind=RETURN id=ab9d37e59564721da2a977fe010b4c7da780898448dec28bcc6df1fe958d56ed contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=TERNARY id=7a6b8b34a8241c1e6d3dab4fe316fe0d54473038595c7ed0ff9cf7d92c51abcf contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 ptr == 0 ? null : /* M3-LEAF kind=CONSTRUCTION id=a2e5bb5b933c370d88732e9054d14fa3129873c61eb755b072fd91e4b4057e0f contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3Path(ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=3a4e451055935c80de122899fe20bcea39383d37d78387f10943fee7c82fd3fc contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Returns array of {@link PathSegmentType} this {@link Path} contains.</p>
     *
     * <p>Returns empty array if Path contains no lines, or curves: quads, conics, or cubics.</p>
     *
     * <p>getSegmentTypes() returns a cached result; it is very fast.</p>
     *
     * @return  array of {@link PathSegmentType} this {@link Path} contains
     */
    /* M3-ATOM kind=METHOD id=076ebda0902fb7fcde74cd85c35bed6d738d3fc5b454d8ba8ce96af27e59d112 contract=PUBLIC_API patterns=BRANCHING,EXCEPTION_BOUNDARY,ITERATION */
    public M3PathSegmentType[] getSegmentTypes() {
        /* M3-LEAF kind=TRY id=a3e5e7866e6457dc77047a84a4f88367fee718f912bd5905d37b256202284209 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=69b0c58aa52929a28b0e0b997fa146062daa6781e7f8c5dce8debee503df8199 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=23b3a6aecbf92744ab337a24e0ae00ff2382496422e25f89859b34422cba7b46 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=LOCAL_DECLARATION id=bc72f798f667cf9bbaba13c93bac434f7d25b4d4f67d54c625cfcf393df11110 contract=INTERNAL_OR_PACKAGE patterns=LOCAL_DECLARATION */
            int mask = /* M3-LEAF kind=INVOCATION id=dca91ade5a463fc85520591f23d3a066429322e337947ae5d820f6d3f4ee119a contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nGetSegmentMasks(_ptr);
            /* M3-LEAF kind=LOCAL_DECLARATION id=f847ef5ea4220f16a4ec3bbbfeed13a4f3a8be5ffbe7aa0969c638abc73f0fcc contract=INTERNAL_OR_PACKAGE patterns=LOCAL_DECLARATION */
            M3PathSegmentType[] result = new M3PathSegmentType[/* M3-LEAF kind=INVOCATION id=bb852f720bd2a7810f74453a0060ef9104461d93100d38fe264bed1c684929cd contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
Integer.bitCount(mask)];
            /* M3-LEAF kind=LOCAL_DECLARATION id=31e2106ff94cafac1e88f98db20e330e28cf44a42798e86d61af392f9fcc0e85 contract=INTERNAL_OR_PACKAGE patterns=LOCAL_DECLARATION */
            int idx = 0;
            /* M3-LEAF kind=ITERATION id=810cb94ec7bfb373c762f53f7e695f401990d8ddb3a460a9fb93e2f794d2bf56 contract=INTERNAL_OR_PACKAGE patterns=ITERATION */
            for (M3PathSegmentType type: M3PathSegmentType._values) {
                /* M3-LEAF kind=BRANCH id=b59b8f5e7cbead188f6527c0242788416e06b70be4d49c6f4b35ce168444a54f contract=INTERNAL_OR_PACKAGE patterns=BRANCHING */
                if ((mask & /* M3-LEAF kind=FIELDACCESS id=4c3afb8c00c07e14ad9ec12ba5757ffe6797a12677c1866779f51d1e8e992b5a contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 type._value) != 0) {
                    /* M3-LEAF kind=MUTATION id=397f25ac5b9a0e63930786580d7f236fd519c75a08aaa4bd326f01b5a443e8bd contract=INTERNAL_OR_PACKAGE patterns=MUTATION */
                    result[idx] = type;
                    /* M3-LEAF kind=UNARY id=e21cbc0a0ab36def2f1fef58057957268119ae367f9c67afe703735b52e45c92 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
                    ++idx;
                }
            }
            /* M3-LEAF kind=RETURN id=c3c4225efc543df5ae1670898f56256f496192b2bb83da49203468cfb71b4ca8 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return result;
        } finally {
            /* M3-LEAF kind=INVOCATION id=dd8a50c079e39a812753a62544a0f08c170864876eaffe5f566eaf3ea3acb09d contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Specifies whether Path is volatile; whether it will be altered or discarded
     * by the caller after it is drawn. Path by default have volatile set false, allowing
     * SkBaseDevice to attach a cache of data which speeds repeated drawing.</p>
     *
     * <p>Mark temporary paths, discarded or modified after use, as volatile
     * to inform SkBaseDevice that the path need not be cached.</p>
     *
     * <p>Mark animating Path volatile to improve performance.
     * Mark unchanging Path non-volatile to improve repeated rendering.</p>
     *
     * <p>raster surface Path draws are affected by volatile for some shadows.
     * GPU surface Path draws are affected by volatile for some shadows and concave geometries.</p>
     *
     * @param isVolatile  true if caller will alter Path after drawing
     * @return            this
     */
    /* M3-ATOM kind=METHOD id=67342eb928232f87f7a5f41a3e6ed010b27278cf4dee1f636e27f67c025cd8dc contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public M3Path setVolatile(boolean isVolatile) {
        /* M3-LEAF kind=INVOCATION id=e139c1e27c8c5387f902f7676f4c1fc0a460459d52959d50038413eba061da65 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=520d39ebf5131c1dd86c66a9726753654827cbc4916f46189f70ba1a8e774056 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
        /* M3-LEAF kind=INVOCATION id=0d94530f6e8a46cda7aa804f76617b5d2ab4c84e85c2ae9fae48ccd97b9292ff contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        _nSetVolatile(_ptr, isVolatile);
        /* M3-LEAF kind=RETURN id=8f7a795894663962a494c023dd2d8e2ae2b58e56cf247e5e46d546129e1caa8e contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return this;
    }

    /**
     * <p>Exchanges the verb array, Point array, weights, and FillMode with other.
     * Cached state is also exchanged. swap() internally exchanges pointers, so
     * it is lightweight and does not allocate memory.</p>
     *
     * @param   other  Path exchanged by value
     * @return  this
     *
     * @see <a href="https://fiddle.skia.org/c/@Path_swap">https://fiddle.skia.org/c/@Path_swap</a>
     */
    /* M3-ATOM kind=METHOD id=89189615a7692d701a540f8e1a4d97acb549870200924ea444ae3ac061c4c97f contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public M3Path swap(M3Path other) {
        /* M3-LEAF kind=TRY id=82e17f4a73d398ea45e98f1402a829c2e1351ab21973389bb399b7624b83cb41 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=cb30d9d3825c05a3c73283c4dc11b0827bb3cb8d8e08fe4c4998b52faf704d9b contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=43b1ea1e95d3141349aae0a73f7b128c1f7ca256acd952fe887d96f180f8137e contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=INVOCATION id=ba7072421c031a60296066b2324ad9fa1b4c86d38a4b49168f5476b856549814 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            _nSwap(_ptr, /* M3-LEAF kind=INVOCATION id=71447a05253e614e2102b800553cb2218823d7c4b1ecce7af26401c0fec0ad2b contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 M3Native.getPtr(other));
            /* M3-LEAF kind=RETURN id=35eb6ab024c675f7510810ed72e13f7a7b20e3b010147a086b9949949c5dfea8 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return this;
        } finally {
            /* M3-LEAF kind=INVOCATION id=6b49f297146e513bd228c57e79c3e062bc4c70368827a44c63786d17dd1da200 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(other);
        }
    }

    /**
     * Sets {@link PathFillMode}, the rule used to fill {@link Path}
     *
     * @return this
     */
    /* M3-ATOM kind=METHOD id=6692961e682feef0cf99c279c2a4918695f37fb6ac92e4ba4fb82aaa8d30efa7 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public M3Path setFillMode(M3PathFillMode fillMode) {
        /* M3-LEAF kind=INVOCATION id=2c40192198b5e8ec98a37e049bc1c903a36240d37f6df614d71cc9a6c9236a9e contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=16a092bb8aa8e80dddb783fcd84dde280f47a7b5efd4c239865cb9ded03426af contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
        /* M3-LEAF kind=INVOCATION id=0c02492619e5501637d42c4bcf4e6889b63ec58c2e1a48b3eb590e440dcd21f3 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        _nSetFillMode(_ptr, /* M3-LEAF kind=INVOCATION id=7863830760556f103a5def6fa6c651f4fb18f610e5c6a02bab7e5285ab4a95d6 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 fillMode.ordinal(/* M3-LEAF kind=EMPTY id=653e8137f7c317e2f84146da81d76ac080fd7c3ae07b09a8d576027cfbc235a8 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
));
        /* M3-LEAF kind=RETURN id=3a85c22c974834d7e827ecc05db09f7e8e9528beb1395a32d72281024e7684de contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return this;
    }

    /**
     * <p>Sets Path to its initial state.</p>
     *
     * <p>Removes verb array, Point array, and weights, and sets FillMode to {@link PathFillMode#WINDING}.
     * Internal storage associated with Path is released.</p>
     *
     * @return  this
     *
     * @see <a href="https://fiddle.skia.org/c/@Path_reset">https://fiddle.skia.org/c/@Path_reset</a>
     */
    /* M3-ATOM kind=METHOD id=4b9bb48e3ecd93b73b252d73d66f6206b82cb1e787b8d2f870d1f840717bcec1 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public M3Path reset() {
        /* M3-LEAF kind=INVOCATION id=ddbe15bb497eee175e24cdcdf7dc4f68c78dd427707c33138e1665e3f2de5441 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=fff8d50e6b049682b950418dd96124f4fc8d6c6d9077f90ea0932438fbad3ea8 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
        /* M3-LEAF kind=INVOCATION id=5220b7c797432f1973f31ff48ae116aa6daf1f1cf1fc528e6e47975b3a7c9b62 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        _nReset(_ptr);
        /* M3-LEAF kind=RETURN id=c4ee08560f1aa45c1a319ec2f4e1dc209077d81e7fecd9e3f8a52e238238f091 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return this;
    }

    /**
     * Iterates through verb array, and associated {@link Point} array and conic
     * weight.
     *
     * @return  {@link PathSegmentIterator}
     */
    /* M3-ATOM kind=METHOD id=b2dfc3fda0fc9bf8beffdeca9a29683e9c0beef76b714593ed3c482e21562703 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    @Override
    public M3PathSegmentIterator iterator() {
        /* M3-LEAF kind=RETURN id=8688c3a235eb14d0769781585fee7ff9671a32451ba692a986a35014b67e31a6 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=INVOCATION id=cbc6ad95ceae690585e808d1afd31387bcf1e527cf604dd9099f20457c37fe58 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 iterator(false);
    }

    /**
     * Iterates through verb array, and associated {@link Point} array and conic
     * weight. Provides options to treat open contours as closed, and to ignore
     * degenerate data.
     *
     * @param forceClose  true if open contours generate {@link PathVerb#CLOSE}
     * @return            {@link PathSegmentIterator}
     */
    /* M3-ATOM kind=METHOD id=fb5340e116b42c2ebc3f75000dcafdd21b04a4587f78955e4bb4f8cdb3b00c4f contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public M3PathSegmentIterator iterator(boolean forceClose) {
        /* M3-LEAF kind=RETURN id=da51f657c668ba8040267a547dd57e4ca5b678fdf67fcf99da9aeb1413459d1a contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=INVOCATION id=ce82e579ba4b1effb96adc8be565913a6193ac224721464b644a9d7567f4729e contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 M3PathSegmentIterator.make(this, forceClose);
    }

    /**
     * Returns true if the point (x, y) is contained by Path, taking into
     * account {@link PathFillMode}.
     *
     * @param x  x-axis value of containment test
     * @param y  y-axis value of containment test
     * @return   true if Point is in Path
     *
     * @see <a href="https://fiddle.skia.org/c/@Path_contains">https://fiddle.skia.org/c/@Path_contains</a>
     */
    /* M3-ATOM kind=METHOD id=7c10d554bdb09291262c7b8d97e5d68221420da99ccf7f41031901b8ebee003a contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public boolean contains(float x, float y) {
        /* M3-LEAF kind=TRY id=1bf200b8791efd203895b4473127a7ff3a3c6ba65d6eb2c844d679119c24c073 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=d7a6f235d620ea37e8e8cba772d2a0554b7ad03329d95913aa0f182a8e2f7633 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=62faaf35c195c704323313ef53b75907b93a35f6c2acc5bd6ef8b0dbf075931a contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=79d9ef7823323856b863126494dd861aae0ca2db4048b909099980a5179011be contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=37653371fdc494a5416a157b2d5d2eae4d5cfa5bc72172d581d6c124b4211767 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nContains(_ptr, x, y);
        } finally {
            /* M3-LEAF kind=INVOCATION id=6d5729fa78cbc6e8c7928e4df2dcc66e38dd61bd9262a18d54f0dd9906ca0a23 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Returns true if the point is contained by Path, taking into
     * account {@link PathFillMode}.
     *
     * @param p  point of containment test
     * @return   true if Point is in Path
     *
     * @see <a href="https://fiddle.skia.org/c/@Path_contains">https://fiddle.skia.org/c/@Path_contains</a>
     */
    /* M3-ATOM kind=METHOD id=a6d719ecd3eb07c8ef0e6e0119648a4c5507d88274b385763f53288f7e3230f6 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public boolean contains(M3Point p) {
        /* M3-LEAF kind=RETURN id=d8688161ee18e1453aed406ff951275b79667e1fb07acbba0ca02e4d2d317151 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=INVOCATION id=efb1a7b6004eceaf42844f5bb7c8b13f05e0bd69bcc3c9cc19c8ff3be03c38ec contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 contains(/* M3-LEAF kind=FIELDACCESS id=4424953c25d9c4c36dfbebb6e40ccd28c9d596b087e7b82a3473a43878ff1c1a contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
p._x, /* M3-LEAF kind=FIELDACCESS id=1270c0bf4aae47877a26c5175b0dbc34c0976f20c5c3faf6aef74f15b5e87168 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 p._y);
    }

    /**
     * Writes text representation of Path to standard output. The representation may be
     * directly compiled as C++ code. Floating point values are written
     * with limited precision; it may not be possible to reconstruct original Path
     * from output.
     *
     * @return  this
     *
     * @see <a href="https://fiddle.skia.org/c/@Path_dump_2">https://fiddle.skia.org/c/@Path_dump_2</a>
     */
    /* M3-ATOM kind=METHOD id=c1853d5640d7d2ab2536bfe7920174c73b2648162648c6effb0507af95ae9945 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public M3Path dump() {
        /* M3-LEAF kind=INVOCATION id=558bf72d00ff8fb865463f2a214551f32495af6c5bc36138be9e6919585cd836 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=c18eabf1c9316a8cab6fa8ce05fedb0cc05ea410a839019b1e0500a46470d199 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
        /* M3-LEAF kind=INVOCATION id=1a965759e4a5eb2b7dfa2f3ab81ebb3b58d973b4a6a9655cdbbc48b933194bdf contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        _nDump(_ptr);
        /* M3-LEAF kind=RETURN id=a0affd00275890ef005b81a49455d527b6840369a844ffec51dff84738efda90 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return this;
    }

    /**
     * <p>Writes text representation of Path to standard output. The representation may be
     * directly compiled as C++ code. Floating point values are written
     * in hexadecimal to preserve their exact bit pattern. The output reconstructs the
     * original Path.</p>
     *
     * <p>Use instead of {@link dump()} when submitting</p>
     *
     * @return  this
     *
     * @see <a href="https://fiddle.skia.org/c/@Path_dumpHex">https://fiddle.skia.org/c/@Path_dumpHex</a>
     */
    /* M3-ATOM kind=METHOD id=f3ff3a33d5e2bf3beb6e7efa9bb2bed1b3f45fef99c8edd32bff719f0d589fce contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public M3Path dumpHex() {
        /* M3-LEAF kind=INVOCATION id=16da9aa6dffd03770936d69465099f178a899864abc2096685b43a8fbfbeb1d4 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=209d36cbd70a38a528c54703e0d1284b594f309597dd4800818e20ffd1fdd13d contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
        /* M3-LEAF kind=INVOCATION id=d331b70510fbc0069316db563eb365ecfb3d5d61fc3c1b6b2bcf0997a8bd21c1 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        _nDumpHex(_ptr);
        /* M3-LEAF kind=RETURN id=89eb55a5934e01f74466a0e5e9c3ac688884177d627a08bac3676f7482a612ca contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return this;
    }

    /**
     * <p>Writes Path to byte buffer.</p>
     *
     * <p>Writes {@link PathFillMode}, verb array, Point array, conic weight, and
     * additionally writes computed information like path convexity and bounds.</p>
     *
     * <p>Use only be used in concert with {@link makeFromBytes(byte[])};
     * the format used for Path in memory is not guaranteed.</p>
     *
     * @return  serialized Path; length always a multiple of 4
     *
     * @see <a href="https://fiddle.skia.org/c/@Path_writeToMemory">https://fiddle.skia.org/c/@Path_writeToMemory</a>
     */
    /* M3-ATOM kind=METHOD id=426579f3912392307243e2a1cc7a0f483837f0c33087eb986bc10d2dd207cc0a contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public byte[] serializeToBytes() {
        /* M3-LEAF kind=TRY id=30301adbd326d7f15e606b96c933f3539e5b895ed48db6cf8f73e8355fa0fce4 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=420793bf386b1300b00014fe1619c3f532a3330fa10f45159b8871c15fb39c4c contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=07b8cbe98318c2e49f821cdcd3c4a114f3508fddc9f02324b99bfb55dc2f7a69 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=2605b9120b8d88aad1418656ab611d4837023ce27e5c06935b0981b51f072f70 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=76dfe052248168e4fda1e1496fb2f4243fd9bef55a9e59015ab6c845d815253f contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nSerializeToBytes(_ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=266271d48d1460a927cf041523971c01f0f5b6f451eaf72454e8a4214849534a contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Initializes Path from byte buffer. Returns null if the buffer is
     * data is inconsistent, or the length is too small.</p>
     *
     * <p>Reads {@link PathFillMode}, verb array, Point array, conic weight, and
     * additionally reads computed information like path convexity and bounds.</p>
     *
     * <p>Used only in concert with {@link serializeToBytes()};
     * the format used for Path in memory is not guaranteed.</p>
     *
     * @param data  storage for Path
     * @return      reconstructed Path, or null if data is invalid
     *
     * @see <a href="https://fiddle.skia.org/c/@Path_readFromMemory">https://fiddle.skia.org/c/@Path_readFromMemory</a>
     */
    /* M3-ATOM kind=METHOD id=ac6fe75055c453515e5b7d850a6ab96caceb70e9312951474a49a8485687a995 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public static M3Path makeFromBytes(byte[] data) {
        /* M3-LEAF kind=INVOCATION id=c77a07124515172c1937b8b7da38d5bde746a2d2351fe270581c193f21c2dcc7 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=70f8a9b3d604189487f148fb65fc852868821bc164f2216cc44337d7b391e2c6 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
        /* M3-LEAF kind=LOCAL_DECLARATION id=c4ef0a5aaac5f5c95df8703f7759608dc703166a62d6e92c156ce67905b7548a contract=INTERNAL_OR_PACKAGE patterns=LOCAL_DECLARATION */
        long ptr = /* M3-LEAF kind=INVOCATION id=ddca9673554b10a733dd0b9e1c0980fad98166e8b2036adb7bfef1bf9349d6b2 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nMakeFromBytes(data);
        /* M3-LEAF kind=RETURN id=fbff0d6365371be52937bc6b56a76bb74c5fc51fd21ca4ef8ef49662e6a2341a contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=TERNARY id=380b4507f3f01bc4bac1001c62fb15ef1835bf24089a62f1dd23fcaab204a325 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 ptr == 0 ? null : /* M3-LEAF kind=CONSTRUCTION id=ea76453b382ef0c33854c0c1897328a3f4750603cfbc3b3d7f44e5f4b8ade22a contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3Path(ptr);
    }

    /**
     * <p>Returns a non-zero, globally unique value. A different value is returned
     * if verb array, Point array, or conic weight changes.</p>
     *
     * <p>Setting {@link PathFillMode} does not change generation identifier.</p>
     *
     * <p>Each time the path is modified, a different generation identifier will be returned.
     * {@link PathFillMode} does affect generation identifier on Android framework.</p>
     *
     * @return  non-zero, globally unique value
     *
     * @see <a href="https://fiddle.skia.org/c/@Path_getGenerationID">https://fiddle.skia.org/c/@Path_getGenerationID</a>
     * @see <a href="https://bugs.chromium.org/p/skia/issues/detail?id=1762">https://bugs.chromium.org/p/skia/issues/detail?id=1762</a>
     */
    /* M3-ATOM kind=METHOD id=a9f5f895a28510b2c21c1958bf37aee4a86d52ce29a530b132f7fe8562c70653 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public int getGenerationId() {
        /* M3-LEAF kind=TRY id=153e334d3a12a023f18c53a5c668fb3bab535b652bccd37f02c8466769180d60 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=e13972ccad71878ce256620fb6ad22480a61e5eee252c83db14172e2d10b27ab contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=09735fdac44084795a410a4d4a64881eaffb3779f30353730f8c5f9dac78994a contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=a43b1abf79d6393e7f4a6b68ee561ffd6dcb6ecbeae81a892675b4c62e82861b contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=fe55ff80815cc4f8d8c656f8a3fa632add097bfa06427774b29f25f43cc797f8 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nGetGenerationId(_ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=f53fe22933b43f340c481f4a3217f87654f69e58364c3cb915b53b7e4d2a761a contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Returns if Path data is consistent. Corrupt Path data is detected if
     * internal values are out of range or internal storage does not match
     * array dimensions.
     *
     * @return  true if Path data is consistent
     */
    /* M3-ATOM kind=METHOD id=88256c046ce8103609a59037d30d797d170b25361366f1e48d84f23aa947985f contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public boolean isValid() {
        /* M3-LEAF kind=TRY id=6e7cfbf6bb1b33b2c97e51ccc3a5de7fef6d771a86beb7e8085d1f872bacbf1b contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=59279e9a7808c6cc51594171377d6f62883f4e5a0960b55fe01e58ce7dad2f97 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=379ef28380c46e20f7d302c4212e57d20b52b761887bfaf58af2a26fc38e6849 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=eb403285c5340b1f5233b9dbec2b83a7b9928b056976f45c744f7fba74b9a65d contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=2df4b0961f5e3ef4127f6bd9d28a3afeba82b2d4b5a66c798821112a312ca3ad contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nIsValid(_ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=86b9f7c4a150bb04b26a3a4e0c6afc6eb74b2a3b116b8489a7b4e6224b7255a6 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Returns the filled equivalent of the stroked path, filling the destination path.
     *
     * @param paint     Paint describing the stroke properties
     * @param dst       Path to fill with the result, may be the same as src, but may not be nullptr
     * @param cull      Optional limit passed to PathEffect
     * @param matrix    Matrix to apply to the path
     * @return          true if the dst path was updated, false if it was not (e.g. if the path represents hairline and cannot be filled)
     */
    /* M3-ATOM kind=METHOD id=9dd9ad80db46d82e2ab0ed68eaddda2fa4ff9f2f3e1a92da81bfb59bb4e35fe4 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public boolean fillWithPaint(M3Paint paint, M3PathBuilder dst, M3Rect cull, M3Matrix33 matrix) {
        /* M3-LEAF kind=TRY id=ba3f4366ec61be624f24267a87765412a0d7d9890efce373f3434aa69aa7abf6 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=ASSERTION id=5d955fc0b5ba03644cd885eabd9eeab258d0474ee7cd659d3a870dfa715be67c contract=INTERNAL_OR_PACKAGE patterns=ASSERTION */
            assert paint != null : "Path::fillWithPaint expected paint != null";
            /* M3-LEAF kind=ASSERTION id=d72340d0f4987b87cfee795481a0990ecfc0876b13ee64b122ed999e818963c9 contract=INTERNAL_OR_PACKAGE patterns=ASSERTION */
            assert dst != null : "Path::fillWithPaint expected dst != null";
            /* M3-LEAF kind=ASSERTION id=01524686b7cf6e05772f0318283c3587755c1470956800c85ff42482a3328968 contract=INTERNAL_OR_PACKAGE patterns=ASSERTION */
            assert matrix != null : "Path::fillWithPaint expected matrix != null";
            /* M3-LEAF kind=INVOCATION id=f6a90de00ea82b3b7147368d3295bbf95df1fc589f15bf02215bb7988f13cb1c contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=3413a687bbd019bd0c27b591468f2fb90fcc1173fff9f8c6874c56e55a098ef5 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=297e7a8a96305cca6ddb773ea10f34851496fb754cc5174c6daf3d0ed801e3bc contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=5f69d39d819e232e92617493a47d6b8cecd8926eea6990df9987cab2178b1adf contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nFillWithPaint(_ptr, /* M3-LEAF kind=INVOCATION id=84d7dad8c55b823a04fc83f44bdb665b846cdc3249928b0580872eec58ea6c7a contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 M3Native.getPtr(paint), /* M3-LEAF kind=INVOCATION id=5196ff2b8293fa3687ab9b2d102fe632ce442ef6fff3404bf5f39997c6940f37 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 M3Native.getPtr(dst), cull, /* M3-LEAF kind=FIELDACCESS id=db19a0e0336d7175ef30c060bd2a8f254965b388d2d1bdcb0263be0d693ee145 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 matrix._mat);
        } finally {
            /* M3-LEAF kind=INVOCATION id=0622881aacbb39d0ff0486dd3c855631d2deb9fcae3cad774ccd775d1cf85f22 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
            /* M3-LEAF kind=INVOCATION id=c63d2ea5a1719889920a607b28cf6ab933d6b3ba10e1e33ff2719ea8399b6f80 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(paint);
            /* M3-LEAF kind=INVOCATION id=38468cd3f5bc1a762cbea0f892614ce7b732034a78c76481af1f241ef338c9d3 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(dst);
        }
    }

    /* M3-ATOM kind=METHOD id=8fc158641ca877e760f385648f23005559a8a39e20862057b5b2e08fa7e1ae5f contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public boolean fillWithPaint(M3Paint paint, M3PathBuilder dst, M3Matrix33 matrix) {
        /* M3-LEAF kind=RETURN id=645d0192098937afd244b2bf75a88891d67fdac2d0fad5fd060f85f4bee349f4 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=INVOCATION id=1d253c49ddf3e5a9632adbfb5b40e4645066575f5efeeb3c4960cc1429f009c8 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 fillWithPaint(paint, dst, null, matrix);
    }

    /* M3-ATOM kind=METHOD id=3b4618c8ccf0f1d7dfa84d3cdf2a45301990e77463908d8c490bd2339356723b contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public boolean fillWithPaint(M3Paint paint, M3PathBuilder dst) {
        /* M3-LEAF kind=RETURN id=e166f3a936f85b5a8f2af0cc01a714fcc9119676117d9d7d4059d6a6327911ed contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=INVOCATION id=65b439fce94e0a3e2225e2e3508e2573b351b10a93a99c31771e24b63d92efc8 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 fillWithPaint(paint, dst, null, /* M3-LEAF kind=FIELDACCESS id=4daf9d55ba2d3fa1f8daabe1625c2b5fe48abd89814ce78a4f83f5e6df04021f contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 M3Matrix33.IDENTITY);
    }

    /* M3-ATOM kind=METHOD id=75a680dac7d433f855974be89501cbf18b11c173fbc8d9cb305595b10ea19221 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public boolean fillWithPaint(M3Paint paint, M3PathBuilder dst, float resScale) {
        /* M3-LEAF kind=RETURN id=484758f6d71ef8b8c2a437f8368a3dd3c9f5c602447d43464aa9da9762b1bcf6 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=INVOCATION id=43bbccdc1e8df1df8f9f8b2f8eaa7332209aa2a3d3d82faca724435fc905f1b6 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 fillWithPaint(paint, dst, null, /* M3-LEAF kind=INVOCATION id=f7baaa81a75282ba980a9d81033c7ed0f9e9a9db4c80dfb85b0cd4fe666841d9 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 M3Matrix33.makeScale(resScale));
    }

    /* M3-ATOM kind=METHOD id=462dfe8ed9c8de55e1bfd071514d7646711fa7d344509764e7712d0d8a9495d6 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public M3Path(long ptr) {
        /* M3-LEAF kind=INVOCATION id=203a2cb07bbc3b3349b5aa3353c0236d08d0bbc2d9cc381e81b892fecb166392 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        super(ptr, /* M3-LEAF kind=FIELDACCESS id=312301b3edcdeffc090c13d4252308c1c7b290682b66c7e69d8e4055e08ed6a9 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 _FinalizerHolder.PTR);
    }

    /* M3-ATOM kind=METHOD id=5d056c7b3eed066c80c59a83463f3d0005dde6cc5d1815fa35db7f5cdf36dd91 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native long    _nGetFinalizer();
    /* M3-ATOM kind=METHOD id=200b39885609af5c8adfd6accb683b8669f8a280e270d80473909f6a0a64fff2 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native long    _nMakeRaw(float[] coords, int[] verbs, float[] conicWeights, int fillMode, boolean isVolatile);
    /* M3-ATOM kind=METHOD id=8433ed373727c7d2f8863fd91a6990ce8878ea380637cfcf81bd8996a0c73800 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native long    _nMakeRect(float l, float t, float r, float b, int dir, int startIndex);
    /* M3-ATOM kind=METHOD id=f9ad604ede6670bcd70a5619c33f2d31f3d305d9661ea3108efe5e9f5a498a42 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native long    _nMakeOval(float l, float t, float r, float b, int dir);
    /* M3-ATOM kind=METHOD id=a4bd0606ba650afa0d12615bc58f844cc472576f4aee1b46f7474be2ee049258 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native long    _nMakeCircle(float x, float y, float radius, int dir);
    /* M3-ATOM kind=METHOD id=b14bef8dfd3a1d439b6cf71dc5152414c588de7da2e0310dc705b15d3df756e9 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native long    _nMakeRRect(float l, float t, float r, float b, float[] radii, int dir, int startIndex);
    /* M3-ATOM kind=METHOD id=6ea0eb89dd0d7cd05b786076a947ef00ae638dbdc4924f481c6bf1875d3cb446 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native long    _nMakePolygon(float[] coords, boolean isClosed, int fillMode, boolean isVolatile);
    /* M3-ATOM kind=METHOD id=4ee6144bd14b3e0b3a8daaa15f618be16b9f2ab5b268803713c1267708cc1f57 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native long    _nMakeLine(float x0, float y0, float x1, float y1);
    /* M3-ATOM kind=METHOD id=557fc4779124f2f2aa755ecd48d45420da3dae73e85106e5bf536d47325fed53 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native long    _nMakeFromSVGString(String s);
    /* M3-ATOM kind=METHOD id=95187edf05b086489c668636fa0d8e4c14701dc64d28956e34cfc3c0b6c709a6 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native long    _nMake(int fillMode);
    /* M3-ATOM kind=METHOD id=fcb5ae233eee5233d092dbcc0fcf8f7487a3205a5830d1bf7453b14b6b9006a1 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native long    _nMakeCopy(long ptr);
    /* M3-ATOM kind=METHOD id=1f6f6be8e076ce766028a15dae88e85e0a18e7a4b414d67dbbe5b65cbfc51294 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native long    _nMakeCombining(long onePtr, long twoPtr, int op);
    /* M3-ATOM kind=METHOD id=0b3627e0079fa1bd5790d636567af22e729bb0d4d380aba59d1ed9d68d802d23 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native boolean _nEquals(long aPtr, long bPtr);
    /* M3-ATOM kind=METHOD id=8d95f50cfb2baac46fea3259f006a1099866ddff91c80f83b174a993c965e471 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native boolean _nIsInterpolatable(long ptr, long comparePtr);
    /* M3-ATOM kind=METHOD id=c4138ec474dca1c76f6a0d1487cf1af7168e03a97a9b88281b1904b1c6dfb556 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native long    _nMakeInterpolate(long ptr, long endingPtr, float weight);
    /* M3-ATOM kind=METHOD id=dd397c5315db3bfacce6114a97f5aeda3c63cb1a59b01de8f9338d0b93e74a98 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native int     _nGetFillMode(long ptr);
    /* M3-ATOM kind=METHOD id=bb2f798ab112d97e516006685ab451acf9fb8b63d403fb7be1909a377dfc73e2 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native long    _nMakeWithFillMode(long ptr, int fillMode);
    /* M3-ATOM kind=METHOD id=b81f7e1fb3000aad590ba25840064884f4a7dbe08ab36ea3f67c6147d5049606 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native boolean _nIsInverseFillType(long ptr);
    /* M3-ATOM kind=METHOD id=3928b84d6ea759836a52a60403efedd039eaa719cbd39fe75b8a9b5bd73ceef8 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native long    _nMakeToggleInverseFillType(long ptr);
    /* M3-ATOM kind=METHOD id=037806c2062875dd9fd44711dc89708594eff7c718a1da18946cbc9ca5ef3ed0 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native boolean _nIsConvex(long ptr);
    /* M3-ATOM kind=METHOD id=1756c86fb3e500f6e4d8a0b2840e30f90b725ec6af7d3720c3627da6e7b02c60 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native M3Rect    _nIsOval(long ptr);
    /* M3-ATOM kind=METHOD id=107beb976f0a997efc94016ac5b45d2446253874953d2020bda047e935d782bb contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native M3RRect   _nIsRRect(long ptr);
    /* M3-ATOM kind=METHOD id=c1c261190bd80c4104bc395077d234f664c4a42f9bd35f7b26ee701d4b610e7b contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native boolean _nIsEmpty(long ptr);
    /* M3-ATOM kind=METHOD id=72b1ad9f810b7e0d9c12b3144ab1afd1577db59ec3e8ae1c7ed9851f5dd05ded contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native boolean _nIsLastContourClosed(long ptr);
    /* M3-ATOM kind=METHOD id=d2cc49863b69bd0ccdcefad9f09205560d360109abb35f9648a59e556a5514a6 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native boolean _nIsFinite(long ptr);
    /* M3-ATOM kind=METHOD id=bc739f502fb69d4498df3c80894664db7ed5704478590f0ce2e43b3a8ce6a992 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native boolean _nIsVolatile(long ptr);
    /* M3-ATOM kind=METHOD id=b4292a5431e90ddfc1ba13301e7c7366103bc505f3db60a8b30a4035fa46c719 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native long    _nMakeWithVolatile(long ptr, boolean isVolatile);
    /* M3-ATOM kind=METHOD id=2f3568eec210635a2409dfb40bb69b3991b70abe6bd96f4bf41e82646c1dbe0d contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native boolean _nIsLineDegenerate(float x0, float y0, float x1, float y1, boolean exact);
    /* M3-ATOM kind=METHOD id=79aec18edccab20adf3723402f97b419f322972f556096d9669a7129bc08fcad contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native boolean _nIsQuadDegenerate(float x0, float y0, float x1, float y1, float x2, float y2, boolean exact);
    /* M3-ATOM kind=METHOD id=ae3eb5d72c3621fc11cd1bbbd23864e31b4524c5b6830142922f8f3ccc749bc2 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native boolean _nIsCubicDegenerate(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3, boolean exact);
    /* M3-ATOM kind=METHOD id=4e6b15710e218c8a08300e5b8c6b04127934c1b0387ba97c9ca9633328d996ee contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native M3Point[] _nMaybeGetAsLine(long ptr);
    /* M3-ATOM kind=METHOD id=fc1c9961a913cabfc72af4fa3a6bb6e8bcf0d9c4962e1c165cb3dbd1e9e82109 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native M3Point[] _nGetPoints(long ptr);
    /* M3-ATOM kind=METHOD id=6a1a8cd3287c8b7c9aaec3fe2df19712ae5dd0061a154a7b6e4e50a6058197e2 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native int[]   _nGetVerbs(long ptr);
    /* M3-ATOM kind=METHOD id=b502b771d364856f8f03f0a952543eeaaf150b8d44ffcfeea8ad84065b2ab971 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native float[] _nGetConicWeights(long ptr);
    /* M3-ATOM kind=METHOD id=4fca3214c72c34c56e7a56fea74add7691c90ede7c662ae9ff0ea7ebec59561f contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native int     _nGetPointsCount(long ptr);
    /* M3-ATOM kind=METHOD id=1795bd05d78d4d4866ccce70274de1d50b6490d10eac696c6d13343b00c1f716 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native int     _nCountVerbs(long ptr);
    /* M3-ATOM kind=METHOD id=040990966c40e541d1355785c206914a128b9caf0b82b5c155b300abd95bd412 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native M3Point   _nGetLastPt(long ptr);
    /* M3-ATOM kind=METHOD id=08f624e6f926aebec85596a4bd19346c9b2d11620385e35ef96efa68ce4cf8ac contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native long    _nApproximateBytesUsed(long ptr);
    /* M3-ATOM kind=METHOD id=af41b3a30d9acfaabc5c0b68d4a8b7a3894661b99876a2ea2c7e778c7194bbf1 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native M3Rect    _nGetBounds(long ptr);
    /* M3-ATOM kind=METHOD id=6244364ec62f44456da6f84fa6d6431013f54d33f30197a4d5db74e8bd0505ce contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native void    _nUpdateBoundsCache(long ptr);
    /* M3-ATOM kind=METHOD id=3156815aebdc604f0f94791ccef2ec70f3723cf407280d89de0cc923816768c6 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native M3Rect    _nComputeTightBounds(long ptr);
    /* M3-ATOM kind=METHOD id=e84764f97161fcba03175fd2661ce495bb2d09c4fe3e861391450d1996e32a36 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native boolean _nConservativelyContainsRect(long ptr, float l, float t, float r, float b);
    /* M3-ATOM kind=METHOD id=0d7f59deb0474e65d261ad3c3d9381f02b5711ddf2030a9fae8359d1c34eff0b contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native M3Point[] _nConvertConicToQuads(float x0, float y0, float x1, float y1, float x2, float y2, float w, int pow2);
    /* M3-ATOM kind=METHOD id=071cef35f4e0d94b66aad1674f74d5188694054b402362148207fed4e3d13ff0 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native M3Rect    _nIsRect(long ptr);
    /* M3-ATOM kind=METHOD id=61d0e6b8532981ae2380617b12bdb2b2bc746a8510d0992eb7f768f51b31e8bd contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native long    _nMakeTransform(long ptr, float[] matrix);
    /* M3-ATOM kind=METHOD id=3fbf7a6d178e14bb16b9382bdb5f96e96dd3c121f22c4ad370ebcee484bf0d30 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native long    _nMakeOffset(long ptr, float dx, float dy);
    /* M3-ATOM kind=METHOD id=99f35b5f3ab366955926817051c5af45a9f22e5165a5c3d7a54c1eb2fab8d610 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native long    _nMakeScale(long ptr, float sx, float sy);
    /* M3-ATOM kind=METHOD id=fe9d9f7b06a1633d34ca73cf0e735e20e67722bd98c620a1e5945b7db93d43ce contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native int     _nGetSegmentMasks(long ptr);
    /* M3-ATOM kind=METHOD id=25d9672552070f66e5d9bae3631559997d68ba31cbd7bdf119efeded68b168e5 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native void    _nSetVolatile(long ptr, boolean isVolatile);
    /* M3-ATOM kind=METHOD id=3957e0cc1e362dfecb1e7634b6554ebce4756f2caf5e00af5d79d419f94a2fbc contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native void    _nSwap(long ptr, long otherPtr);
    /* M3-ATOM kind=METHOD id=68c003106a25a9e55a86362d4152e8043d5d2153c61a39fcd7ed9d3b7b7400c3 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native void    _nSetFillMode(long ptr, int fillMode);
    /* M3-ATOM kind=METHOD id=33235e2fb9795cd51b952975c48496723ae0d5a4e898b946fe21f7e050c6b62b contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native void    _nReset(long ptr);
    /* M3-ATOM kind=METHOD id=18b141c8db2b9a06533880f06ae97fbbd324153d66a30bf80867752b32142f69 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native boolean _nContains(long ptr, float x, float y);
    /* M3-ATOM kind=METHOD id=dde2ce5fe97df713b4b4cf87333d8c384836e367559e7dac928c0570d263a249 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native void    _nDump(long ptr);
    /* M3-ATOM kind=METHOD id=30981e2f9857c0315f08f7257df51308698b76edc0e83648a6f7a9d40b7e7556 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native void    _nDumpHex(long ptr);
    /* M3-ATOM kind=METHOD id=9abbb13c882ea6f7349d70a7b3a43aee458e993502ce4de69fd31c8a8e327448 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native byte[]  _nSerializeToBytes(long ptr);
    /* M3-ATOM kind=METHOD id=2e92fda654fee53812d5395a83f0f0cab206e7f9ca5ee03c72bc32ea4541b039 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native long    _nMakeFromBytes(byte[] data);
    /* M3-ATOM kind=METHOD id=1895eb65574bb0b51f8c8f47743a6210d299e17dc437ac4968ca7914e30e87e5 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native int     _nGetGenerationId(long ptr);
    /* M3-ATOM kind=METHOD id=c30647ad9cfda592e95db16c29bfe2c6041946060a5ac355b3fe4f00bda1aac2 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native boolean _nIsValid(long ptr);
    /* M3-ATOM kind=METHOD id=439afc959b8f611b729a9070220b532219f2fc20c47f5e7d893713b05f8a5899 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native boolean _nFillWithPaint(long ptr, long srcPath, long dstPathBuilder, M3Rect cull, float[] matrix);
}
