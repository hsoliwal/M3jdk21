package jdk.m3.graphics;

import jdk.m3.graphics.impl.*;
import jdk.m3.graphics.*;

/* M3-ATOM kind=TYPE id=1fccc8aa2ed8af5be793b71afe0cfc2c0dab7f7d797c3dc7e18e78f18782b5db contract=PUBLIC_API patterns=INHERITANCE */
public class M3Region extends M3Managed {
    static { M3Library.staticLoad(); }

    /* M3-ATOM kind=METHOD id=ac475e2ca585d94495d7d9f0ecd89edf626c929cdfd4206291352a98581ec222 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public M3Region() {
        /* M3-LEAF kind=INVOCATION id=a5d06dbf73a8a59d208d3874e5c7f822107d503b57cfa4bc0cf0098dfa4be099 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        super(/* M3-LEAF kind=INVOCATION id=92312dfbe2b6e8e9fe0f51e5467c7cbb97ff04d5b2d828c480a3c39b06927a84 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
_nMake(/* M3-LEAF kind=EMPTY id=467d89860da27da34c0a9a99c8b97662bf69685502eb3f3b9e154ded24deba08 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
), /* M3-LEAF kind=FIELDACCESS id=a4c248f2c999d9184df86c4792581c7686e36aaf99fb88f7c5c66b9c21b630d5 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 _FinalizerHolder.PTR);
        /* M3-LEAF kind=INVOCATION id=cac78a4ace5da8d8083ecd6aad2351576056756b45541c531c8fb5553bec6af5 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=732a4ef4296eb7b4e91218cf841db41e64f0deb6c5c546df4964b1d2e8d7ef77 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
    }

    /* M3-ATOM kind=METHOD id=413a452e95d06f5b9d9fa3aff417fa887ecf3eb7a5cefac21411ea1f2b2aa6f0 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public boolean set(M3Region r) {
        /* M3-LEAF kind=TRY id=e12d1ee71bc03eef5905ed8293798515dc0d225b19fa33d7d8e32d394f2b02be contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=d9b6d99b6fac976f20c3362c320d610d1fab0b0f66d8aab7d961911a7f349151 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=535db078b288572df15238614664d309aeee83cdf56cf3f92dbfaed96888c2fb contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=23b84b2e7d42e757272576c1bd12eb31b6ae7eea4357e13fb651a8dc144fa0cc contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=a168bd20bef278d21430e5fea19350917c07fc246844f9d245c15c4861cdf786 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nSet(_ptr, /* M3-LEAF kind=INVOCATION id=23a6885d967346ab8269f73b1220056bba6ed97b10fdc6cbb2490e3943891914 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 M3Native.getPtr(r));
        } finally {
            /* M3-LEAF kind=INVOCATION id=6f44a91e2b9364d47f332b672bab5c29060d6491ff950debcdf0d04c651a2c6f contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
            /* M3-LEAF kind=INVOCATION id=bd7b9ca3694c1ce6a48475703a2c857be75d7e756c98a7d5bce2345600fdcc5d contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(r);
        }
    }

    /* M3-ATOM kind=METHOD id=10ee8bdbecfbf4dc0c65372ca43910979d08aa77f3dbdf8d2cf24d0dddd72ec5 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public boolean isEmpty() {
        /* M3-LEAF kind=TRY id=d470da24253f2e972663847bfc9ecdf4bd12499f78a01cb6dc55d8d430ddf459 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=2d716f3bd50db0e54bbcf232e26160d6038dfa3f735f374001c3946a0395744f contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=3f9c93a05a27c4e87526552f7c4990ee98529e9a90ffda6964f86e83b6d7ba93 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=1e63b70546c92c8777c2940d58da80bd4add80c03c53333a540527199357d3f8 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=1647d08a4d16202d1cdb3f5d5ee4603ecd663bb09d964bc863a52c7e72c1c036 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nIsEmpty(_ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=d5916a138f459c3d52f2899d991b4e3b089b05b64f42214ed464797c03eab185 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /* M3-ATOM kind=METHOD id=9c0554e95becf95241026fbdf869d6a986e57b5801231030dcb1898b7d3c1b55 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public boolean isRect() {
        /* M3-LEAF kind=TRY id=78bfa428c6fd9529f6b8fa9810f651ecc06eafd74528d61693f54a5675617758 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=d7701aa7dc86e692dfbbd147f6c4c4a59ec8de7056ef8d00bef6eb68be464b4d contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=281cf8db841dbfb3499f0fbd5d5ef73c5a97d502aa212e5444ceb1952982583a contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=a8c1b5b5edfa2b926d2d9b0be12503afc9bd6f13f7aa8775e03b5ae0d0dccb81 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=437d05456dccddfd5ece6ae2054e0d26e12dbd48bf38eb605731b8ce397f68e6 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nIsRect(_ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=b306ece658672c77d47b360b40b59d9ed0f6cf7c98d2bedd336865a84590f65b contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /* M3-ATOM kind=METHOD id=a2eb27db3525a97bedcd6adfedffe5080154de6b95abd4a87ee34f506791fba9 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public boolean isComplex() {
        /* M3-LEAF kind=TRY id=c88e072b0cfd5fc251a94eb677018be414131b4c99fc30624b876b6ad4a8744c contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=7bb4cb1a6fb5c5b7f571ad455d7b64292ecdc292cb3322150b9d29e555987fa3 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=ea0fa39e6dfbf51803e291474330686445887ef47147808084b1334f0c4792c4 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=ebe6bf313931ba9356de56a8067cb5182f1240780ffee3695ff6edc921f06c15 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=e7a63550bd9c242305b0863bb1c5db595ea951157445b331c39b22594ee1f5e8 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nIsComplex(_ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=3884bf822e9d447b857db271d7b5c6024e7e9568d3bb17513a70130ffb861769 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /* M3-ATOM kind=METHOD id=67222adb7a0872b65572b8c9a2a280e05b14db2b69ae6d8f81c1dc3e1021fd92 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public M3IRect getBounds() {
        /* M3-LEAF kind=TRY id=ff9520833e1b87db03d53e89ca5a7e7a4993d966172965bf316c6db8ef2d2fba contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=8260677e24fa2392314fd2997d9e01170ec2fb222cf3e72251c47d200587a3cf contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=0891b0010bf9099e0ec8e955c9ece5e3d7b856513201cd05bb72bd8ecd629e0f contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=fc10ded81b89996d11de86c9e46dcd220ea96d17a799c507f0c8be34ed5b5e31 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=15f0cf047da63e65653760baa215b577a27bf1751279f0753ec7639acadcfb43 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nGetBounds(_ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=48e18bf440ef3e6ff7c20254af8eba7bc70e45f47defd00c6175a5a09897a21b contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /* M3-ATOM kind=METHOD id=4590f377d1db75243183c5ff33c441d5a330cb98eeafb09b9c613ac4d3f5db9f contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public int computeRegionComplexity() {
        /* M3-LEAF kind=TRY id=40518ef346eb924fec8696c1a0bf346963c26d2a9b6ba77dd865d1dfbb191d11 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=5c775baceb45efd795095c3e1ae76eb0afef76772c3c12812b6378cf3d857738 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=1ca8cfce4d527d99a3b65767cf1c40df3d683abe6dbdcc0e8ebe196a81d71596 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=ea83ae127da1bf5078be5d5809b9a06a8bfa1d46a69d4a10a0df0472f23384ee contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=87a2ed3beacccf9ab32a2a47681f6675b3105935af38aadbc52707a59eb915f6 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nComputeRegionComplexity(_ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=b93ea9c57e686c0bc3de510ea160627c46cf51afb27f4122497a45293fcd5343 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /* M3-ATOM kind=METHOD id=4105608c1c6a178e591f9da160446d6673321d81b0b21e90a3159b5878395b3e contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public M3Path getBoundaryPath() {
        /* M3-LEAF kind=TRY id=37ff3eebfb9b83c9f1ac549e49d530b3b3413ce544d6e0c18ad14dd20e6e8903 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=cdd636454a6712ef22a441a084f1c0102e17bf932d0ecf6392591a270ce69b08 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=a77c276532a8401d1a36f105e347aedd58308c3b61e0e55f3d0aab9595a8c414 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=e0e16ceed83e651ee614cba84da579de4ffa52b7ae35af9f325d360dac911989 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=CONSTRUCTION id=413c199bc4939f832fd3dd2ea5220a59507d5643d879359be26e264bd9aaa286 contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3Path(/* M3-LEAF kind=INVOCATION id=d0b38cc9fbb1c6d7a359dc1c96e3573106e2cacc37750f1bad4e1d49955fba07 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
_nGetBoundaryPath(_ptr));
        } finally {
            /* M3-LEAF kind=INVOCATION id=04a96f4b9e0b82d852f5625f028a82791cdf62f9f3f19ca827b5084bc2c59e32 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /* M3-ATOM kind=METHOD id=484fb95eefb60350ed58eb3d285e906a183aa24b9610371d1238bc3f0a5044eb contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public boolean setEmpty() {
        /* M3-LEAF kind=TRY id=122bf967dbee07c15dcb369f1099e3a071b99bf40e6599d282d9bf524547fb2d contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=738a77cadbbeccd60e75006e9fbac78fb6bd28d31faa7505fa0b545052c5db48 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=dc4378d6b7c1e5c7d8b2cc8021f3a2317edd31e9b18462b6ffa78ddc63cd7cc4 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=a3e1c247efd8e78f46697672be4b58aa933198c1872d84250b3a84e6eeddc20f contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=ef82a4cb4227ee9cb9a0c9bb9b51abdf1e4eedac6110e75702520fbc018d0b48 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nSetEmpty(_ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=f49e3acbc220131a46227ef9d0bfba650a58025a29eaaadc30e7cece7e103d5d contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /* M3-ATOM kind=METHOD id=5858fb7458f3987e0e8615b5f458220a03fe61fbd5855b172cd6144a1fc135f8 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public boolean setRect(M3IRect rect) {
        /* M3-LEAF kind=TRY id=7aa1bc05c1d521a046c93ec90343f24180b0f51d5e98e7aae9dfa197e7ad74b5 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=73a926d8a1e326fff1821f88b7bb2f8cdbb071719b32c2fea770c4dee283d3cf contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=570ad51bc59f4c9e70f39b5edba4997a25989526e2ce1e311e9946edf3441def contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=6ed41f914732db56fcbb884ac959a858fa9ac9feb8892759d291c06fcd845d42 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=4042a6354c2de9691b41a8f9845ea0d3f585357634f3344fd3adebe74aa54a5a contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nSetRect(_ptr, /* M3-LEAF kind=FIELDACCESS id=34ba7a883e5d49a8f3e93299a09c554159bc50d285f262c3887203f66bee33a3 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rect._left, /* M3-LEAF kind=FIELDACCESS id=bd65e05afc1ac084724ef98ac7e086e8864d264fd38ba19a6d15dea34b571022 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rect._top, /* M3-LEAF kind=FIELDACCESS id=59f0d6bef421d4ee83f234f962da67a04e008247b6029fb5a0893d6d777fd6d2 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rect._right, /* M3-LEAF kind=FIELDACCESS id=8a3fb77b912ddc23d17a83e907b247f1407dfca04bb9f0975ac8a6302629e3fb contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rect._bottom);
        } finally {
            /* M3-LEAF kind=INVOCATION id=1f72ec50b18939f7e903ba903599b5df0721ef4e523e7c62541fcc3cb43c9c51 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /* M3-ATOM kind=METHOD id=5c06c0894d4fb799fa8b1139061974fb68d499b9e26c95aae5b7cb4d727f5053 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY,ITERATION */
    public boolean setRects(M3IRect[] rects) {
        /* M3-LEAF kind=TRY id=49652a66fefa75fb1184ca3b9f40442e11803f25fe56158d5ef3528f80e411c8 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=LOCAL_DECLARATION id=050c38e951d9531e18de6c1848f92416622917a8483d823af7f6be43ff228744 contract=INTERNAL_OR_PACKAGE patterns=LOCAL_DECLARATION */
            int[] arr = new int[/* M3-LEAF kind=FIELDACCESS id=18a90b5618f8532d5ee888d971bb2aabc930782428a85ee6e65e89a9bc236a29 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
rects.length * 4];
            /* M3-LEAF kind=ITERATION id=482e6e2c9d8e8503c025261ed5ba818225a73eab7e64f961d44ce323bb5767a5 contract=INTERNAL_OR_PACKAGE patterns=ITERATION */
            for (int i = 0; i < /* M3-LEAF kind=FIELDACCESS id=7f46491f4bc472030ba77e3ba2049a7265370e5d3f502191439b57f6a9f2b49e contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rects.length; ++i) {
                /* M3-LEAF kind=MUTATION id=0e25111ed9a609fb7370bcef75772b0049f8ceddde2405b5761fccae40ac5d75 contract=INTERNAL_OR_PACKAGE patterns=MUTATION */
                arr[i * 4]     = /* M3-LEAF kind=FIELDACCESS id=9a1d79936b5d26be4b44a3f3ef88abcad3ca503db5844c5e62d8f6c62908fb2a contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rects[i]._left;
                /* M3-LEAF kind=MUTATION id=5fc1756fa6126a3bc3075bce6cdd14896f43ce25900294fb641e94476b942a80 contract=INTERNAL_OR_PACKAGE patterns=MUTATION */
                arr[i * 4 + 1] = /* M3-LEAF kind=FIELDACCESS id=432ac0508e4aa460543a074a80dcde391c77aef96ba5beccff46d6719e7454c8 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rects[i]._top;
                /* M3-LEAF kind=MUTATION id=346aad4768f8ce3a100aba483aad703d813aefe3d02ec5c49521c2fc2ea34f4f contract=INTERNAL_OR_PACKAGE patterns=MUTATION */
                arr[i * 4 + 2] = /* M3-LEAF kind=FIELDACCESS id=de1a36713b9e2e28839e9673f31e6ecd2770e6da1cea6c127766f0a889e1c1cc contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rects[i]._right;
                /* M3-LEAF kind=MUTATION id=3e1c2234431e80e6cbb2b694115265ab62338b4306ccb71c16e487b94b4b1afd contract=INTERNAL_OR_PACKAGE patterns=MUTATION */
                arr[i * 4 + 3] = /* M3-LEAF kind=FIELDACCESS id=818eb5dd3d8af595d3fb66a630ac2c3d90383210ed2c4490eda42df7600438df contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rects[i]._bottom;
            }
            /* M3-LEAF kind=INVOCATION id=780adc6822f1e10549f9f3f535c53c101b8d813feab20471f0e5b95a63f7f356 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=6e033c2198a30198659d6965372ddd86fbcb7b9fa60be50b9f4d21a057509262 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=f1575ce3abce67fa890b4d94784de00cf8ad5e7841dde68f10ede478915ae236 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=ee7c91a0e283a06fb9cc2fd5b0916e4b8734b0712665de4af1065384f8fcd55a contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nSetRects(_ptr, arr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=60a33452ff758cf4ee3c86da8c7189b489fc2dd7c10a9b6da7cceb9f60b9a47b contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /* M3-ATOM kind=METHOD id=4d62da2069cb7e94bbc0cf8a55f942565ef712a588bcb991d3ecb8961f310d06 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public boolean setRegion(M3Region r) {
        /* M3-LEAF kind=TRY id=492302f28b0377b28f6d92178f64bf4e9e9d67385ab66af76751a54c03b7b19d contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=8af3b9bcfe961aa8af788db65792023c280b433f2834d1731cece2de5bd782b0 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=bde491d0776c4f62a86386229d083833a342c4a958dfee8c92cf6c50ce9dc0fb contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=e7087b65eef539a58d1f07f6e17cf10b87c947a0f4d00db1fd30da8709a04861 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=f13588d1c660dcc6ec8a16939e376b864fa2e07144a4827e1b61502cd4e6a4bd contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nSetRegion(_ptr, /* M3-LEAF kind=INVOCATION id=f02840ea96e0e4ab5e4a4e069ffb8fbbccf5a4f5da11c7683b17716f5fd28972 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 M3Native.getPtr(r));
        } finally {
            /* M3-LEAF kind=INVOCATION id=1fc1bc72dce45881a46cf357f7ff0bc6688248ed1ca4c3d1983943024f15bff6 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
            /* M3-LEAF kind=INVOCATION id=93800af43ef1a92a879e385091e53ab8ed43f30b93760c460a08277fbcfa74ef contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(r);
        }
    }

    /* M3-ATOM kind=METHOD id=974fcf54e31469c93d3359084f578ba04812a6b706a35e9e755471f7a7773c1d contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public boolean setPath(M3Path path, M3Region clip) {
        /* M3-LEAF kind=TRY id=2b4debb836ecbc5efd67af7b82d0060cf62fc452540d4240ce3952bd7dc65c28 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=c298e1287834b14bd95580d4d8e569fe86e3446400397c420f0f08b3069db754 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=4f1de67a9e2a44488e3d47b05a61f05f43fd6be9a303891d8e93a9de4877e6ab contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=4f1ef896cf99ca5508c47c2d7b4d49c024d5d4d2452963400cefebe110fee6fb contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=b402115326e5dbd0353dfdeb79872b1a4e86bc4f08230ccfa3ba80369d6b14a1 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nSetPath(_ptr, /* M3-LEAF kind=INVOCATION id=fc97f3e52886e9801cbbc24d047dd43820f229568bbdbf306801cccb9f4df962 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 M3Native.getPtr(path), /* M3-LEAF kind=INVOCATION id=68fed4bd00420b38ce61349dfad91c496b655a0630bfbe8c86205eab4894682e contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 M3Native.getPtr(clip));
        } finally {
            /* M3-LEAF kind=INVOCATION id=6407b95c1e59529d187abdac8d5ecfd0aa76ee765b015b704ad1a1ef47f4138c contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
            /* M3-LEAF kind=INVOCATION id=f99c91c5e4dc58ed8c7882752de9ca75f922c02355d5bacf4546b2849e7eefd4 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(path);
            /* M3-LEAF kind=INVOCATION id=c30a5c7f870893c2f04e0636a1438287fa1516e9a5b37f91c80e42d95711d7f6 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(clip);
        }
    }

    /* M3-ATOM kind=METHOD id=39d23d39cc108887427c6ee3056d60034e5afcc75d4c83a396c197827be3ba77 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public boolean intersects(M3IRect rect) {
        /* M3-LEAF kind=TRY id=ad7dc460f129c74423c1e10308099347861fb496c280a9f011416d0776e8fcbe contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=b593e84be391de15f6272c13c3353e6f740dfe35e9f0a5c37234385fcbf469de contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=01f1afeed42bb0918f25712ce3ec152164be7c5b16b00308fe32563701c51dbe contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=1ca972d7d210e36b61f8d84fac0a2bf56c16f2e86a1dbfba6d77cd7c952d3217 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=d98295541a0fdd087fffdd65a3d834efed67c3cd6af6d5e6d8aad7a66f2dd86b contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nIntersectsIRect(_ptr, /* M3-LEAF kind=FIELDACCESS id=a1c64804e2c478443d4014f51c5b5179112b9e93e6fec7f03c56ad4736404de9 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rect._left, /* M3-LEAF kind=FIELDACCESS id=b3e058e49b49a98c3202bddca4a5b835f93f3ba64616ea49d75be1b4d010abe0 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rect._top, /* M3-LEAF kind=FIELDACCESS id=75179dcf58ac6b1d61def4a447ae432869280fca9916f563c09c15f8d3d7936e contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rect._right, /* M3-LEAF kind=FIELDACCESS id=d5717a92b324889c8de3ab7d23283f6e6d17e2c8d61891f1647f671fc2b2ec3d contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rect._bottom);
        } finally {
            /* M3-LEAF kind=INVOCATION id=cd2c12bc56b629496ad533143a93a7f56538ba65d1d4f71b5c3bb11fcd82b8ef contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /* M3-ATOM kind=METHOD id=0e47da79c88d7c95ccfd8d356a0d6218c7bebcefcd209b6091acbd6daad22ba4 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public boolean intersects(M3Region r) {
        /* M3-LEAF kind=TRY id=e89e923cf381571c26c786da0a5f28e65d952520dbe8e40303484a3bd5c2daa0 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=3300ca3187ab85e7d4b26e15533751ee6d3f01343f55ffc118a75ab0b64f0b07 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=7a47383bb94ad91a09ed816a0c70afaa89ac5cdb88244fce3dd1a54be1ca3463 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=8ebbca27e2706c1675994986906f34a499b7e16c983b222957541ed3ea460fdc contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=04242f20f07c42c8b1a4dad43b5d6bd42339d608b2e8e4fe68cff58d2129caa7 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nIntersectsRegion(_ptr, /* M3-LEAF kind=INVOCATION id=44ab4cf44146795b65c79cb8a04a97356555753ecf522d04ec0db113cc2f47e0 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 M3Native.getPtr(r));
        } finally {
            /* M3-LEAF kind=INVOCATION id=1b9fbcb92928eb8c096b2a37c4c3f104164ccd2ea18d835702f9550a4ffd9226 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
            /* M3-LEAF kind=INVOCATION id=ff235519da1c98a5e9d2a82e9bd5262d1042f559b6f3b26d09f32160a95aefba contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(r);
        }
    }

    /* M3-ATOM kind=METHOD id=3c853ca70c36be5e530605b4866f89e06fec3ec1ac8d0d6cb5a9bc352ec91178 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public boolean contains(int x, int y) {
        /* M3-LEAF kind=TRY id=a6a137e6aed2feea20433f1c20612651efd226f6b075cc974eedb30fe1716865 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=679752dec1743dbcee0116dda91ebc17c32c534f21d7d4b5efd48d58a9263fcd contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=f1b93aafefe7c50b79b6a232b701fe9b8b3a0600a8107ab5fe07a0e4359c1ca7 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=eabdfdaf26b2b2a42c650d9158fae31f4e6225fbc2ea102c221b6a117f43b12a contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=0598815476bacd5e9a80038e4da5d59c100a083e581626c27ea4e2f88f1a5cab contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nContainsIPoint(_ptr, x, y);
        } finally {
            /* M3-LEAF kind=INVOCATION id=bb2e7028b825c3b1fec0215947e2b321e38ef39b5c66b75aaf21101143148128 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /* M3-ATOM kind=METHOD id=3950ca11587362d9935f8459192574a8cf2106e324ed766a75217ef74c3715ac contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public boolean contains(M3IRect rect) {
        /* M3-LEAF kind=TRY id=8ee23e7f121e4d5811adeae8696cefc61e458d2ad6fe309a81417bd6d03ffce7 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=ef38a035955c30691faf1787d2bc7fda7829cd4f06af2b1cb4c221691bca2b31 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=50f566e47beebffc7805a43571093d2fd5318374dba9d3e14a906cb9f6d450e6 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=7bb69bf5a5a691b36eb330ecacdeda6fdddcb0f05517ffbcaf139c27de8ff50f contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=0dc6ebc3be785ed459a61fc9176fc4b84f8caf77c62606fd8a0fc7dc86e4c438 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nContainsIRect(_ptr, /* M3-LEAF kind=FIELDACCESS id=711f3f966e25ee49e7aa2a15635c902a5003cf90f37e929008d49ddc56d0d8f7 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rect._left, /* M3-LEAF kind=FIELDACCESS id=7a4de6aa4373a28701130342432b6aca97d1eff6919c18620a150eaed3f4c4ef contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rect._top, /* M3-LEAF kind=FIELDACCESS id=e2ed999316b474f5383e79fca4a29bf2db2a7901685dd40fa860027b6300a181 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rect._right, /* M3-LEAF kind=FIELDACCESS id=b2e3932f74c82627350689fdc44d35b5ff52a1b6ff1b67df020f572eb593c8a8 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rect._bottom);
        } finally {
            /* M3-LEAF kind=INVOCATION id=fa6b84b80553028bc18284abeaafc1931403988474c70074cc23c4ddc502f25b contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /* M3-ATOM kind=METHOD id=651f753af423742eea49991913b5b4d4216236b123cf4e17f90ec27caaa30c60 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public boolean contains(M3Region r) {
        /* M3-LEAF kind=TRY id=4c505080e5ac2fa39780fb3cd798a4a363984c80f01fbfedd65aa80e883f82b7 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=c50538babea7ae1f6fe9f7862ac1647ebb64b7f80bdb4808daf91e4ac97554b7 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=b86d869383f3685bbd61bbd4d50a073f777f70224cd8c1d6f90dc20251b0fbf1 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=729f6e91d5a30cb5b993d30f970ce4d878f7f5cc646b02ff7782695543f4b46d contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=c5e46edeed8afdfe615f18c1296d31a03f3c18fc463b58c07b52a5c4bac65cd1 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nContainsRegion(_ptr, /* M3-LEAF kind=INVOCATION id=ba4cf1c8ed49bdda6c3adbb4ecbc36e42b1465f8883abb088ed61ec6f89503c2 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 M3Native.getPtr(r));
        } finally {
            /* M3-LEAF kind=INVOCATION id=abb2ffd04e57c73b69a915806a3b80d9da34e150f20fcca35d86d75c6ae65509 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
            /* M3-LEAF kind=INVOCATION id=6df4e34ae05db9e34641176dab8a83822fca71fed860045dfd09162bd6e96b4a contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(r);
        }
    }

    /* M3-ATOM kind=METHOD id=faba476d75a8fea44c95ce0c3f485d52502fd17c2cf0be1634f74e716e001a11 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public boolean quickContains(M3IRect rect) {
        /* M3-LEAF kind=TRY id=f5dffd5d01bbafb006efb150dc5c69f67b5ffd07cc3581e4b588fcd720f10320 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=2648f3045a45ac8614bcab8a143134ea604a9fbb9c395706b72dc3bf1024c464 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=1b4e91f6a70336e3683eaaab2271b02a132752ed85a8fd06901773b8f2431dab contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=2980aa4d00e12d97503023ef9e344d7dcedd3bac00aac933c17a4bc19fd586f5 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=154b31e37b4e5c3a667db7972d9d1ba3f17188eb97f1ae7bf8ee026353072fa0 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nQuickContains(_ptr, /* M3-LEAF kind=FIELDACCESS id=b0e2f83068a0b8aba6a8fc1e42cd705461374c617d5e03c4f108d8f1dd844821 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rect._left, /* M3-LEAF kind=FIELDACCESS id=f69c0f2e49a580e43759ee7d32276e3e994b5cb1cf60f89658dad7dce8531601 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rect._top, /* M3-LEAF kind=FIELDACCESS id=3328326257dfb1b1e4f265d38fbe339ba93b3a885f93fd125709dec33111b142 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rect._right, /* M3-LEAF kind=FIELDACCESS id=d0aef3026e6e3d0276dda890f95eb7f70ae427fc76d04e6c9fe29bda06d4be29 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rect._bottom);
        } finally {
            /* M3-LEAF kind=INVOCATION id=2fa5e78f1dab8473a25d9535721a64a04009a0e18e290f69957a23baf340676f contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /* M3-ATOM kind=METHOD id=3eb30329be39d811633927b63d232a9edbb746f1d01812ea5c1c8c9165893ab3 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public boolean quickReject(M3IRect rect) {
        /* M3-LEAF kind=TRY id=3890307472c1b72a0e95e554e1484058515b19b9febf3b515c9c38fd9446f732 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=cc3f5311e82ddf12390df45c0ecd67d678721d32a3b5f1a3095c92485b84a883 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=e59e1a3107c57aa795a27b674ff69df660e09e9b5dd9ffca595239fd85d09358 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=d4acc32b9f347fe568cd0f128ddf60488022928e2671076bf46fd508af74877e contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=d1ace406a7e1937c01faf07625151ce53cec740e099349745d3b32d868f5e18d contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nQuickRejectIRect(_ptr, /* M3-LEAF kind=FIELDACCESS id=ea738d4cd37353487f70811b61047c9935c5447f79164de3a3878892735fbf2d contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rect._left, /* M3-LEAF kind=FIELDACCESS id=086a3510869db791304d903af08bc87dafc3ccc5b3dc9e86bea7769d433808ec contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rect._top, /* M3-LEAF kind=FIELDACCESS id=e0c130c899b95db23292d7c4a87d493a576c9dc5d276c76f2840f307c2b0bf80 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rect._right, /* M3-LEAF kind=FIELDACCESS id=c6f49bb264bba20000cc9aace039fbc1590a3592a0bf7c73382b9bd28518e235 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rect._bottom);
        } finally {
            /* M3-LEAF kind=INVOCATION id=7ab1321feff05685301a57f0bb4320baa65748d412ca4c473d49807d38cead72 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /* M3-ATOM kind=METHOD id=18be9d4a9fd43eedd970d2602ec0fa806c93da32f44aa17079393149684f9983 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public boolean quickReject(M3Region r) {
        /* M3-LEAF kind=TRY id=59966b7b11a37ff93b815c2c51ea62dd964ff1204e962e781590aab48ea167f7 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=46134f54768f79f91588964b45890e3278101eaa588541e75b78af4d6cf3c133 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=2e9d26fc96f1d3b1fa26e36c7ef2ab7d183f1b74243dd596267af5411f5e8ab7 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=1cbb16dd671c0601b5459ea1bffd8812a271c512febfdf6ca4756f1ea48736db contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=408fd07e5b672f4b36170c036130d5cf54ca00fb6a50edd779d2d6163c346b29 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nQuickRejectRegion(_ptr, /* M3-LEAF kind=INVOCATION id=93191e0f39295aa00c3e30d236e1ee16aa754efec44096957f99ec89e14ce0c6 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 M3Native.getPtr(r));
        } finally {
            /* M3-LEAF kind=INVOCATION id=39901b31a83e14747e7f676e0169cd9cc83d316c4dff9d9533909db0ca171e47 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
            /* M3-LEAF kind=INVOCATION id=12754136bdf6308f5c7b6701af265277ed22131148bae93c8293a684efda961e contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(r);
        }
    }

    /* M3-ATOM kind=METHOD id=96a13885dabe0bcd3d539a6fa73e92f1b43b8e0ec2ef32a5a2e7a0e4aaea98dd contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public void translate(int dx, int dy) {
        /* M3-LEAF kind=TRY id=2531cac39cfc2de4ed2bc49bcdf0a54bfefc3d926669807ecd4b143ac325d221 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=58b9cc0e860ff3a80ff6ef499200c01f4d297896f8b5b986787be721f7b0adaf contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=5b9fecd451c2a655946af493c63d1b2ab8e6b7196fd628adea3e97908ff10714 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=INVOCATION id=3e5b9da11159c6a5a645ecbb33c4688b3e564689d61b68f0792430c141ae0a45 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            _nTranslate(_ptr, dx, dy);
        } finally {
            /* M3-LEAF kind=INVOCATION id=a2d1bb4cfade9228089b2a5f7167c5bc342b3ed47fcdf3e3427c27a432192bfb contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /* M3-ATOM kind=METHOD id=01b5981fadc354be825913cc2cd732994a35b2192f6248d9039518d0197c93ec contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public boolean op(M3IRect rect, M3RegionOp op) {
        /* M3-LEAF kind=TRY id=342e3ff0e60317898b5e854d793ffe6d5163ff5b1aa05b458965dfd28669708d contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=23219e2e4b565ddc8a8a4fda6002184b8467e4fab75fa0a2515862e3d34e177d contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=0b04ccf9ffa5b0c5cd1b8a5c640a4171fedabc0c6e3bb43cea42c6568cc0ee18 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=f9e56f4b3f015cf683b86eaef178f114ad47ec5494f71dbeded71c50811620d4 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=62c294dc38e9c350d8c08d38e5047cfc520461981cdb9432fdd570d49ce98537 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nOpIRect(_ptr, /* M3-LEAF kind=FIELDACCESS id=ae7854375cdf755f1820480c814aaf207bbd6d3a2e63683ed45669899f35f391 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rect._left, /* M3-LEAF kind=FIELDACCESS id=b8ec0af6f012c652ede1c564e4715d8d2bb9ca6c55f7056e3879a3c12e7d0523 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rect._top, /* M3-LEAF kind=FIELDACCESS id=b86f5bbd4cb45d16ddd9368ebc124c9272b17232b0e23e223c368c4f0a91434b contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rect._right, /* M3-LEAF kind=FIELDACCESS id=5ed062571ce41b113acc5bbd971bdc2a7718ce75da7c14b868d7400dcf42fcf0 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rect._bottom, /* M3-LEAF kind=INVOCATION id=ae7a9eaa9c876ca799ec740628a63ba06c69fb1755b6dafa652df73f8e211b0d contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 op.ordinal(/* M3-LEAF kind=EMPTY id=622cd40b1161cc78c8576eb7bde6767f02862a501eba2263a492b23994b9ae93 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
));
        } finally {
            /* M3-LEAF kind=INVOCATION id=e0f394a09a1ea0dbeab0511079f9e6ee98c2086e8bfff376bb05727bdd8ee2e8 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /* M3-ATOM kind=METHOD id=6d29e9679857081188d2db1261193359c78261efc11eec9834b966b6ecd68651 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public boolean op(M3Region r, M3RegionOp op) {
        /* M3-LEAF kind=TRY id=28a4fb6b3ea5bc4083070974f63d0617748a3abd06b768bc2295399feaf10a50 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=c6e21545324e35e3f65364e88d0594c9cf00d7f28520c84e3a854b64900aaccd contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=32149ac608e0e6df9306f3c05a4d07fb296d1429a91215ea25bff1cc93b375c4 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=ace189edc75c00302be461235aaed889365dde709fd37633a97e38fab822eead contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=2bf6d2820b9abcdce0a0f34b9041dbf3bb3d26b91b7b69de0b412d7b37320970 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nOpRegion(_ptr, /* M3-LEAF kind=INVOCATION id=12adfb1d07b87d34bab657d4e9c88a4ec8c27ced6f6475636a7e56f8576cba11 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 M3Native.getPtr(r), /* M3-LEAF kind=INVOCATION id=bf36672b72c6375f0ebf751c0dbb1b54c9b6c7c0208415288aede374c58ab2fa contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 op.ordinal(/* M3-LEAF kind=EMPTY id=c35603d60943cc686bb8c5793663fe4374bb187ce28067b902341fea03119b2f contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
));
        } finally {
            /* M3-LEAF kind=INVOCATION id=61f64139a37461af144b900a780a081e68de20858b24bc5e24c472e861d64d43 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
            /* M3-LEAF kind=INVOCATION id=79cfcf9113469af9c1ae69b8953d9c999170832042fc0a7d23471929f6f8f03d contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(r);
        }
    }

    /* M3-ATOM kind=METHOD id=4c6a5f1926b90372136349bdce925953e0d89b7efa9a515afe137c4af9cf94d5 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public boolean op(M3IRect rect, M3Region r, M3RegionOp op) {
        /* M3-LEAF kind=TRY id=2ca664aacfbf30422942672ec1499f2e1e37c640dcb22948b42f85af70b79dd1 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=c5d67c774827186fff0c16f880f8adbe93a661ecc366c07fd9032d32cc8f8ff2 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=aa5bba4ee9ed53dc5c29e48863f9c3162428fa4e45d028f812ac6cb8d73a8bb0 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=8c2e27f01aa4a7841495fcf04a11443c9b58286a8e4b7709ab75a1a8585b9a21 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=e7debf3fa50011373fe051462f7fffd8ae63b938eb608c737f4ef6a8e61ebbb3 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nOpIRectRegion(_ptr, /* M3-LEAF kind=FIELDACCESS id=e693926fcdf20b49b9ef796665168ca493c3dc6044afa215ca8bdf91619623d3 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rect._left, /* M3-LEAF kind=FIELDACCESS id=ec1f5f6aa5a0758d3c0739bdc791e556a22298169c16dac74d35f8cb0e8447c6 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rect._top, /* M3-LEAF kind=FIELDACCESS id=edd332b198a6517c6394092a7d4059bb0fd6e54763bcb8bee2480a5d01f8a02f contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rect._right, /* M3-LEAF kind=FIELDACCESS id=eb0d626ca7650fe9db0ab70f94b2b87b5a308b04d67fbe82a065f4248d5c18bf contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rect._bottom, /* M3-LEAF kind=INVOCATION id=f5d2041fd0874e1cffb378fe0f84666ea4415d7ddc5afaebe0f9e5f2c182d5a7 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 M3Native.getPtr(r), /* M3-LEAF kind=INVOCATION id=02aa0e5faa00a4f48b6f7ad07f2dc40d46f86fbb3647f3afb3f3b748a142bc54 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 op.ordinal(/* M3-LEAF kind=EMPTY id=db7066994d6a0835f72dded4088488b4330bb63dd01c6d32d7bc6c122ff06704 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
));
        } finally {
            /* M3-LEAF kind=INVOCATION id=daabede7dc725865884a41e1f67a32d1efdbaf1293262ccab00694e34e4d8920 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
            /* M3-LEAF kind=INVOCATION id=221051764d16d233c1ba7eea6cd695f8829c2fd2c410908d345181a44bd0a791 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(r);
        }
    }

    /* M3-ATOM kind=METHOD id=78af5da9d54bbdc245ede3ac8555bd642dae7dfbeea7dc3fac2a7914517d543a contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public boolean op(M3Region r, M3IRect rect, M3RegionOp op) {
        /* M3-LEAF kind=TRY id=90de341f207bc4e86b9b6dc93b0ccbb94fe87f7809d826f095451ea52c216b8c contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=b76e52d49086d0dc8df0e8b331865c1278a3834284bc82c82555cdf094c8d3bc contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=541f0100ccd955a3d171749cbd2bcf05278bef78fbfda9bde9556f898a6f2a79 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=2cc6a52e469dd2e5b969604c65779c679c8e992089e99123c9a110d1b0267d43 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=ce9082a4f365768c81de63f7c02a105b1dc8686fea255603f2bf6c1e0eb74b9b contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nOpRegionIRect(_ptr, /* M3-LEAF kind=INVOCATION id=ffcb62410967ae740efab1ace26c87a21b0c411353493139ac1d3b61a478e9ae contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 M3Native.getPtr(r), /* M3-LEAF kind=FIELDACCESS id=174414364f6b88c8b1c3dd12784c3c27ebe9c70bd1472ec05f694ae2119e979b contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rect._left, /* M3-LEAF kind=FIELDACCESS id=6cd5528993235cd47d0e12618480b2cc73a62a81ca4480e634c81ec7b7b6f9f6 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rect._top, /* M3-LEAF kind=FIELDACCESS id=2fdd5269385cdac93f72dcf815aad9a220cb5ee66f68b90096c419c69e35576c contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rect._right, /* M3-LEAF kind=FIELDACCESS id=558d14a86ede7f65d9f3525ce88c3aef7a5c47991771f33c0ae1ec17a401bd31 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 rect._bottom, /* M3-LEAF kind=INVOCATION id=e604954ad42498a974d8a290d8bd30171f4a84a7cee9c5b1c888f9cdacf33019 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 op.ordinal(/* M3-LEAF kind=EMPTY id=1e76e7db7a063bec2de4355d5e04f18d06a788281c7c595a589798c26291e32a contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
));
        } finally {
            /* M3-LEAF kind=INVOCATION id=138aad2757a65932c5c953b109246dffc4ec81fbd201d2c039aa8b5b9a2ddf8f contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
            /* M3-LEAF kind=INVOCATION id=64bd9409334149d617b93ea4bef8f5b7e35b5e7756ff6f0cf94021e9d7113a94 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(r);
        }
    }

    /* M3-ATOM kind=METHOD id=a6ab49dcc63e43c8c0e7701c86df8a000283fc796774550071c54a1cfa902932 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public boolean op(M3Region a, M3Region b, M3RegionOp op) {
        /* M3-LEAF kind=TRY id=938c57ee2e27530ba37f06715affe8a9b7d5762805b9a514372f4b234c6aefb8 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=1230762c39c05ce4f7f12e1c683eacd12e71eeb76536f0a505e6f5b1fb7d4e38 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=00121630fb7aca525515aa4fea2a88e75f6d9b4dee58d1deaf1077bf29817fec contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=4c6b3fdb95f5d7a9cd42e9334fecdf06a814ac5a3d7984edcb07778d44a60aff contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=d90911a8ee82ac76f5b52e26e3798acaedd051f5952f7ce26801282db44d3202 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nOpRegionRegion(_ptr, /* M3-LEAF kind=INVOCATION id=5a9729f7d0a2077dd60fc2362fe9baf153d21ab10e0c75e5019f60dd2c3e63a5 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 M3Native.getPtr(a), /* M3-LEAF kind=INVOCATION id=0eb083e3e9e96999513f92fdf83d7da2176a8ab2ea2ac7c4751f168a4a9d94e9 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 M3Native.getPtr(b), /* M3-LEAF kind=INVOCATION id=9aab36c9ee5aa572b7ce0e2f08c971615fa404cb535b6c31f08863d47c76646c contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 op.ordinal(/* M3-LEAF kind=EMPTY id=07e3ea099a2f0b80294926ce254a2bd4da9c494f116e1e597459ecb59f568063 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
));
        } finally {
            /* M3-LEAF kind=INVOCATION id=ff570d263c88234ae126ba7381e278c6cab85b8965ba6c1c57d99596ee12354f contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
            /* M3-LEAF kind=INVOCATION id=28f1a3a313ab620211403178096c305b94ba9483db4246c2347d9499936c31d1 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(a);
            /* M3-LEAF kind=INVOCATION id=1a4610361c78cd482c88fdf5b669ed472ed5730851ec773beb2b47767dc71d4f contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(b);
        }
    }

    /* M3-ATOM kind=TYPE id=ce2bd84489d82a7b88ec5ec6a80b17f8a83a2d6c6c052bc1a55f4ffaded86448 contract=PUBLIC_API patterns=PLAIN_TYPE */
    public static class _FinalizerHolder {
        /* M3-LEAF kind=FIELD_DECLARATION id=671f4419e8b7337a3c80571ca8afeb8b8b34c1319d1580947a5d00b3f8a05685 contract=INTERNAL_OR_PACKAGE patterns=FIELD_DECLARATION */
        public static final long PTR = _nGetFinalizer();
    }

    /* M3-ATOM kind=METHOD id=43c16c5df88e3e67dcccf7ee9a8add085e53f4bf2f4a277a0ba0dd8103276665 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native long    _nMake();
    /* M3-ATOM kind=METHOD id=608ab245c2325829380f8fd26924b243081e3b4ac618b06850ad8b6584a4c60a contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native long    _nGetFinalizer();
    /* M3-ATOM kind=METHOD id=693b477efa265ae4df78e8c15cb5d090495cf672c11e8f16bf9700a15491156e contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native boolean _nSet(long ptr, long regoinPtr);
    /* M3-ATOM kind=METHOD id=997f35cb5a12159de9d6468af489cb08eabae4af3fe77a4f92ff25e682f7bcf0 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native boolean _nIsEmpty(long ptr);
    /* M3-ATOM kind=METHOD id=f4e50600747d577afcd982c9cae98311146ad820b2956a306f288180f8742ad3 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native boolean _nIsRect(long ptr);
    /* M3-ATOM kind=METHOD id=aca3f479a36518265aa3d74d85127e6d177066581ec84494d13e91505b68d802 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native boolean _nIsComplex(long ptr);
    /* M3-ATOM kind=METHOD id=ff07b716dec732acffb1e709d224fceea9d0e1c36a603bab516db1c0d6220d5a contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native M3IRect   _nGetBounds(long ptr);
    /* M3-ATOM kind=METHOD id=5f6b2f50a3f40d2e8e53c8b93fd7dd4d2304ae3877436a380a7baa964ef019c9 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native int     _nComputeRegionComplexity(long ptr);
    /* M3-ATOM kind=METHOD id=0df1ba765409bb146245972de1d5153c2c1522846174b808fbcfc80f8cfd803a contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native long    _nGetBoundaryPath(long ptr);
    /* M3-ATOM kind=METHOD id=b3b9404603202439b88d2e69c944e926e5dbfa83f06ef22a80d896a3e2ebb88a contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native boolean _nSetEmpty(long ptr);
    /* M3-ATOM kind=METHOD id=870c83ca6b28ce170a5320c3263307eac5c6dc953b06d794ecfa72624b7417cb contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native boolean _nSetRect(long ptr, int left, int top, int right, int bottom);
    /* M3-ATOM kind=METHOD id=fab87de13a2c9ab85d1fec2640e6f3942323d5b681cc56d4bd05041be9359852 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native boolean _nSetRects(long ptr, int[] rects);
    /* M3-ATOM kind=METHOD id=df74cab90d8ab2b377375bfb723c57a078a8d59d1d7e8ee8971da103ebcc4dfd contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native boolean _nSetRegion(long ptr, long regionPtr);
    /* M3-ATOM kind=METHOD id=b46ebc1a0015b4435cd533517c897af4d7d8b6e53dbe408a69262a4f568df0d9 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native boolean _nSetPath(long ptr, long pathPtr, long regionPtr);
    /* M3-ATOM kind=METHOD id=4ac62f468d93b3ee3548c9a075ec3fdcddba85627d19d7206a6e5e4acef677fc contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native boolean _nIntersectsIRect(long ptr, int left, int top, int right, int bottom);
    /* M3-ATOM kind=METHOD id=680018ae8b00483019b193cf35ca2930ad05f921f202c86fbebf97876eef1c3c contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native boolean _nIntersectsRegion(long ptr, long regionPtr);
    /* M3-ATOM kind=METHOD id=49b8c0144dc1ea73ed06290d4753e69801ccae279186d9f00516f3ab2b414912 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native boolean _nContainsIPoint(long ptr, int x, int y);
    /* M3-ATOM kind=METHOD id=28241128fdf6abce025bd49d1294148963062dac9560f45d1a25f926d8c99994 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native boolean _nContainsIRect(long ptr, int left, int top, int right, int bottom);
    /* M3-ATOM kind=METHOD id=331a531dadf088b2ed1d734cde9832f838e275a2e25fab6c02d0942b27a9dd08 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native boolean _nContainsRegion(long ptr, long regionPtr);
    /* M3-ATOM kind=METHOD id=d2698bd97248ab7e01302735a3f45db4a3c05308e4401ce2060561cb5480d44a contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native boolean _nQuickContains(long ptr, int left, int top, int right, int bottom);
    /* M3-ATOM kind=METHOD id=66951c93550af0e167b70118e4b75c300095ab297814ea93248532ad08574547 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native boolean _nQuickRejectIRect(long ptr, int left, int top, int right, int bottom);
    /* M3-ATOM kind=METHOD id=57bc650660f0d3c74651edbcb5d9fbc8321158c2b84d8a2b2dc2f4942662df78 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native boolean _nQuickRejectRegion(long ptr, long regionPtr);
    /* M3-ATOM kind=METHOD id=509b413e9281e1432621a33408657bf75c75cd810d19f895656653b7ef277afe contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native void    _nTranslate(long ptr, int dx, int dy);
    /* M3-ATOM kind=METHOD id=67735e0a2880dd4aed22748f5010f2ae326d2213cfde5188735f9901d9310343 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native boolean _nOpIRect(long ptr, int left, int top, int right, int bottom, int op);
    /* M3-ATOM kind=METHOD id=a3fa842062f38817735657cbe903d24fc0742d37c275f07ac03449be442c87e2 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native boolean _nOpRegion(long ptr, long regionPtr, int op);
    /* M3-ATOM kind=METHOD id=2e5fc8c686caa85fcb41cd577a33186fca06db80a2e1af2df1b79d158b070417 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native boolean _nOpIRectRegion(long ptr, int left, int top, int right, int bottom, long regionPtr, int op);
    /* M3-ATOM kind=METHOD id=5c03967c40eb5f10a0818292646f850041ad0fb1b5cbe7f832b5f6500bab8f49 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native boolean _nOpRegionIRect(long ptr, long regionPtr, int left, int top, int right, int bottom, int op);
    /* M3-ATOM kind=METHOD id=253ef0e516a3e4d41513a2658f89e1053551c9b222c60be7c6c97793635650a3 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native boolean _nOpRegionRegion(long ptr, long regionPtrA, long regionPtrB, int op);
}