#!/usr/bin/env python3
"""Prepare a bounded canonical-owner proof; all original A3 methods stay unchanged."""
from pathlib import Path
import json,re,shutil,hashlib
HERE=Path(__file__).resolve().parent;ROOT=HERE.parents[1];OVER=HERE/'candidate/overlay';BASE=Path('m3/tooling/migration-recipes');TASK=BASE/'tasks/a3-code-string-repair-20261006';OLD=ROOT/'current-ci/a3-behavior-v01/workspace'
def put(path,text):
 path.parent.mkdir(parents=True,exist_ok=True)
 with path.open('x') as f:f.write(text)
def main():
 d=json.load(open(HERE/'candidate/CANDIDATES.json'));test=(ROOT/'current-ci/publication-v02/overlay'/BASE/'src/test/java/com/m3/rewrite/backport/JccCiRepairTest.java').read_text();test=test.replace('JccCiRepairTest','A3CodeStringRepairTest').replace('jcc-ci-repair-20261006','a3-code-string-repair-20261006').replace('com.m3.rewrite.backport.JccCiRepair','com.m3.rewrite.backport.A3CodeStringRepair').replace('m3.ci.materialized','m3.a3.repair.materialized').replace('Exact-current-input repair of two invalid Java literals and the foundation Maven environment.','Exact-current-input repair of two nested quotes and their bound Java-template hash.')
 start=test.index('    private static final List<Target> TARGETS = List.of(');end=test.index('\n\n    @Test',start)
 targets='    private static final List<Target> TARGETS = List.of(\n'+',\n'.join('            new Target('+json.dumps(r['repository_path'])+',\n                    '+json.dumps(r['before']['resource'])+', '+json.dumps(r['after']['resource'])+',\n                    '+json.dumps(r['before']['sha256'])+', '+json.dumps(r['after']['sha256'])+')' for r in d['rows'])+');'
 test=test[:start]+targets+test[end:]
 start=test.index('    @Test\n    void exactBrokenJavaInputsFailParsingAndOneByteRepairsPreserveAllOtherText()');end=test.index('    @Test',start+10)
 check=r'''    @Test
    void onlyNestedQuoteEscapesAndTheirManifestHashChange() throws IOException {
        String quote = Character.toString((char) 34);
        String slash = Character.toString((char) 92);
        String broken = quote + "Pattern.compile(" + quote + "a+b?" + quote + ")" + quote;
        String repaired = quote + "Pattern.compile(" + slash + quote + "a+b?" + slash + quote + ")" + quote;
        for (int i = 0; i < 2; i++) {
            Target target = TARGETS.get(i);
            String before = resource(target.before()), after = resource(target.after());
            assertEquals(1, occurrences(before, broken));
            assertEquals(before.replace(broken, repaired), after);
            assertEquals(before.getBytes(StandardCharsets.UTF_8).length + 2,
                    after.getBytes(StandardCharsets.UTF_8).length);
            assertTrue(parse("A3RegexMatrix.java", before).stream()
                    .anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR));
            assertTrue(parse("A3RegexMatrix.java", after).stream()
                    .noneMatch(d -> d.getKind() == Diagnostic.Kind.ERROR));
        }
        assertEquals(resource(TARGETS.get(0).after()), resource(TARGETS.get(1).after()));
        String beforeManifest = resource(TARGETS.get(2).before());
        assertEquals(1, occurrences(beforeManifest, TARGETS.get(0).beforeHash()));
        assertEquals(beforeManifest.replace(TARGETS.get(0).beforeHash(), TARGETS.get(0).afterHash()),
                resource(TARGETS.get(2).after()));
    }

    @Test
    void everyBeforeAfterCombinationConvergesToTheSameThreeBoundFiles() throws IOException {
        for (int mask = 0; mask < 8; mask++) {
            var sources = new ArrayList<SourceFile>();
            var observed = new TreeMap<String, String>();
            for (int i = 0; i < TARGETS.size(); i++) {
                Target target = TARGETS.get(i);
                String value = resource((mask & (1 << i)) == 0 ? target.before() : target.after());
                sources.add(text(target.path(), value));
                observed.put(target.path(), value);
            }
            var errors = new ArrayList<Throwable>();
            var run = recipe().run(new InMemoryLargeSourceSet(sources),
                    new InMemoryExecutionContext(errors::add), 1);
            assertTrue(errors.isEmpty(), errors.toString());
            assertEquals(3 - Integer.bitCount(mask), run.getChangeset().getAllResults().size());
            for (var result : run.getChangeset().getAllResults()) {
                SourceFile after = assertInstanceOf(PlainText.class, result.getAfter());
                observed.put(normalized(after), after.printAll());
            }
            assertEquals(expectedAfter(), observed);
        }
    }

'''
 test=test[:start]+check+test[end:];put(OVER/BASE/'src/test/java/com/m3/rewrite/backport/A3CodeStringRepairTest.java',test)
 pom=(ROOT/'current-ci/publication-v02/overlay'/BASE/'tasks/jcc-ci-repair-20261006/verification/pom.xml').read_text().replace('jcc-ci-repair','a3-code-string-repair').replace('m3-jcc-ci-repair','m3-a3-code-string-repair').replace('JccCiRepairTest','A3CodeStringRepairTest');put(OVER/TASK/'verification/pom.xml',pom)
 work=HERE/'proof-workspace';shutil.copytree(OVER,work)
 for row in json.load(open(ROOT/'current-ci/a3-behavior-v01/INPUTS.json'))['rows']:
  if row['origin']=='EXACT_CURRENT_GIT_BLOB' and row['repository_path'].endswith('.java'):
   dest=work/row['repository_path'];dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(row['path'],dest)
 frozen=[]
 for p in sorted(work.rglob('*')):
  if p.is_file():
   b=p.read_bytes();frozen.append({'path':str(p),'relative_path':str(p.relative_to(work)),'bytes':len(b),'sha256':hashlib.sha256(b).hexdigest()})
 put(HERE/'PROOF_INPUTS.json',json.dumps({'rows':frozen},indent=2)+'\n')
 print(json.dumps({'proof_inputs':len(frozen),'test_methods':test.count('@Test'),'source_files':len(list(work.rglob('*.java')))}))
if __name__=='__main__':main()
