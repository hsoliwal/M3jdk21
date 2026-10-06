# JEP484 Class-File API second-pass path map

This packet implements the next mechanical step required by merged inventory PR #164. It does not
copy or mutate Class-File API source.

The mapper enumerates JDK21 internal classfile Java sources and JDK24 final public/internal classfile
sources directly from pinned Git refs. It performs one deliberately narrow normalization:
`Classfile` -> `ClassFile`. Direct descendants are emitted only when exactly one donor path shares
that normalized relative identity. Missing and ambiguous nodes remain explicit typed residue.

Every source and donor file is SHA-256 bound. A separate VERSION_SIGNALS output records post-21
version/release symbols and class-file-major literals as review hints only; these signals do not
authorize automatic semantic adaptation.

The exact-head workflow must reproduce the established denominator: 241 JDK21 internal files,
245 JDK24 combined files, 238 direct descendants, 3 unmapped sources, 0 ambiguous mappings and
7 additions. Only after that map is sealed should source-sealed opt-in API crate generation begin.
