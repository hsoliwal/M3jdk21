typedef unsigned int uint32_t;
typedef unsigned long long uint64_t;
_Static_assert(sizeof(uint32_t)==4 && sizeof(uint64_t)==8,"width");
static inline uint32_t fastrange32(uint32_t word, uint32_t p) {
	return (uint32_t)(((uint64_t)word * (uint64_t)p) >> 32);
}
__attribute__((export_name("reference"))) uint32_t reference(uint32_t word,uint32_t p){return fastrange32(word,p);}
