from pathlib import Path
import json, hashlib, collections
root=Path(__file__).resolve().parents[2]
out=root/'publication/hosted-ci-audit'
head='bc06298218dd88f0164ed9b67e74fd8234ed70b2'
def body(p):
 return json.loads(json.loads(p.read_text())['response']['structuredContent']['content'])
pages=[root/'publication/SOURCE_QUALIFIED_WORKFLOW_RUNS_0610.json',out/'WORKFLOW_RUNS_PAGE2.json']
runs=[r for p in pages for r in body(p)['workflow_runs']]
assert len(runs)==176 and len({r['id'] for r in runs})==176 and all(r['head_sha']==head for r in runs)
suites=[s for p in ['CHECK_SUITES_AT_HEAD.json','CHECK_SUITES_AT_HEAD_PAGE2.json'] for s in body(out/p)['check_suites']]
sm={s['id']:s for s in suites}
assert len(sm)==179
failed=[r for r in runs if r['status']=='completed' and r['conclusion']=='failure']
assert len(failed)==169 and all(sm[r['check_suite_id']]['latest_check_runs_count']==0 for r in failed)
checks=body(out/'CHECK_RUNS_REFRESH.json')
assert checks['total_count']==len(checks['check_runs'])==20 and all(c['status']=='queued' for c in checks['check_runs'])
samples=[]
for rid in [37422112578,37422112682,37422112648]:
 r=body(out/f'RUN_{rid}.json'); jobs=body(out/f'RUN_{rid}_JOBS.json')
 assert jobs=={'total_count':0,'jobs':[]}
 samples.append({k:r[k] for k in ['id','name','html_url','path','event','head_sha','status','conclusion','created_at','updated_at','run_started_at','check_suite_id']}|{'jobs_count':0,'latest_check_runs_count':sm[r['check_suite_id']]['latest_check_runs_count']})
summary={
 'schema':'synexia-hosted-ci-read-only-audit-v1',
 'audited_at_utc':'2026-10-06T06:12:48Z',
 'repository':'hsoliwal/com.synexia','pull_request':9479,'head_sha':head,
 'tree_sha':'a72565e0e67f330f12c712dcc7a3fcbe1d80d24e',
 'workflow_census':{'total':176,'completed_failure':169,'queued':7,'success':0,'event_counts':dict(collections.Counter(r['event'] for r in runs)), 'all_failed_workflow_suites_have_zero_check_runs':True},
 'check_suites_census':{'total':179,'github_actions_completed_failure_zero_check_runs':170,'github_actions_queued':7,'other_apps_queued':2},
 'commit_check_runs':{'total':20,'queued':20,'annotations':0},
 'representative_runs':samples,
 'java21_sample_timing':body(out/'RUN_37422112578_TIMING.json'),
 'java21_sample_log_response_utf8_length':len(json.loads((out/'RUN_37422112578_LOGS.json').read_text())['response']['structuredContent']['content']),
 'classification':'pre-job failure observed; exact rejection reason unavailable from returned API fields',
 'exact_cause_established':False,
 'billing_cause_established':False,'workflow_validation_cause_established':False,'source_compiler_test_failure_established':False,
 'failure_annotations_observed':False,
 'hosted_ci_pass_established':False,
 'limitations':['Workflow enumeration and check-suite captures are successive snapshots, not an atomic transaction. The extra failing suite is not silently added to the earlier 176-run census.','GitHub run_started_at is populated even for queued runs and does not prove runner/job execution.','Direct check-suite and check-suite/check-runs URLs were rejected by the connector as unsupported endpoints (HTTP 400). Commit-level suite enumeration succeeded but provides no diagnostic text.','All 169 enumerated failed workflows have zero associated check runs; only the three named samples had their job endpoints individually inspected.','A bounded public-web open of all three already-known run URLs returned DisabledError for each; no annotation was obtained.','No browser or authenticated-browser fallback, remote mutation, cancellation, rerun, or workflow dispatch was attempted.']
}
(out/'SUMMARY.json').write_text(json.dumps(summary,indent=2)+'\n')
lines=['# Published source CI audit','',f"PR: https://github.com/hsoliwal/com.synexia/pull/9479",f"Head: `{head}`",'Snapshot audit: 2026-10-06 06:12:48 UTC.','',
'## Finding','',
'Hosted CI has not passed. The two-page exact-head workflow census contains **176 distinct runs: 169 completed with failure, 7 queued, and zero successes**. All 169 failed runs map to GitHub Actions check suites with **zero check runs**. The three representative job-list calls each returned `total_count: 0` and an empty jobs array. These responses establish failure before any job is recorded for those samples; they do not identify a source compilation or test failure.','',
'**The precise rejection reason remains unestablished.** The returned run and suite JSON contain no failure message. It would be unsupported to label these failures as billing, workflow validation, platform outage, or a production code regression. No such classification is made.','',
'## Representative runs','',
'| Run | Workflow | Created / updated (UTC) | Jobs | Check runs |','|---|---|---|---:|---:|']
for r in samples: lines.append(f"| [{r['id']}]({r['html_url']}) | {r['name']} | {r['created_at']} / {r['updated_at']} | 0 | 0 |")
lines+=['',
'The Java 21 sample also returns `billable: {}` from its timing endpoint and an empty decoded log response. There is no job ID from which to request compiler or test logs. `run_started_at` is present even on queued runs and must not be treated as evidence that a runner executed.','',
'## API boundaries and remaining checks','',
'The refreshed commit check-run collection contains 20 entries, all queued and all with zero annotations. None belongs to the three failed sample suites. Direct suite and suite/check-runs requests were rejected by the connector with HTTP 400 (`GitHub Fetch URL is not an allowed public GitHub repository or search endpoint`). The later commit-level two-page suite enumeration succeeds and contains 179 suites: 170 failed GitHub Actions suites with zero check runs, seven queued GitHub Actions suites, and two queued suites for other apps. All 176 previously enumerated workflow runs have a corresponding suite. The extra failed suite is recorded separately because these calls are successive snapshots.','',
'A bounded public-web open of the same three run URLs returned `DisabledError` for each page; no failure annotation was exposed. No authenticated browser was used.\n\nNo full-repository or JNI hosted acceptance is established. Existing finite local recipe/compiler/JUnit results remain separately scoped evidence. Diagnosing the exact host rejection requires a surfaced GitHub run failure annotation or another supported authorized read of that diagnostic. No workflow, source file, branch, PR, or account setting was changed during this audit.','',
'## Evidence','',
'`SUMMARY.json` provides machine-readable findings. `EVIDENCE_MANIFEST.json` hashes the request/response captures and report inputs, including the original first-page census and status captures from the root publication directory. Unsupported endpoint responses and the empty log response are retained.']
(out/'REPORT.md').write_text('\n'.join(lines)+'\n')
paths=sorted([p for p in out.iterdir() if p.is_file() and p.name!='EVIDENCE_MANIFEST.json']+[root/'publication/SOURCE_QUALIFIED_WORKFLOW_RUNS_0610.json',root/'publication/SOURCE_QUALIFIED_CHECK_RUNS_0610.json',root/'publication/SOURCE_QUALIFIED_STATUS_0610.json'])
manifest={'schema':'synexia-hosted-ci-evidence-v1','files':[{'path':str(p.relative_to(root)),'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()} for p in paths]}
(out/'EVIDENCE_MANIFEST.json').write_text(json.dumps(manifest,indent=2)+'\n')
for name in ['REPORT.md','SUMMARY.json','EVIDENCE_MANIFEST.json']:
 p=out/name;print(name,p.stat().st_size,hashlib.sha256(p.read_bytes()).hexdigest())
