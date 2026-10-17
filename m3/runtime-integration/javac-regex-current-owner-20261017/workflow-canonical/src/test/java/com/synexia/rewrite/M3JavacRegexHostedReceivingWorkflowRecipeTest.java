/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

/** Adapts the existing bounded workflow SDK to one additive receiving qualification job. */
final class M3JavacRegexHostedReceivingWorkflowRecipeTest {
    private static final String ROOT =
            "/com/synexia/rewrite/m3-javac-regex-hosted-receiving-qualification-20261017/";
    private static final String RECEIVING = ".github/workflows/m3-first-release-admission.yml";
    private record Applied(Map<String, String> files, int changes) { }

    private static String contents(String name) throws IOException {
        try (var stream = M3JavacRegexHostedReceivingWorkflowRecipeTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(stream, name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private static M3GpuToolJoinManifest manifest() {
        return M3GpuToolJoinManifest.load(ROOT,
                M3JavacRegexHostedReceivingWorkflowRecipeTest.class::getResourceAsStream,
                M3GpuToolJoinManifest.NOOP);
    }
    private static Map<String, String> before() throws IOException {
        var input = new LinkedHashMap<String, String>();
        for (var target : manifest().targets()) {
            input.put(target.path(), contents("before/" + Path.of(target.path()).getFileName()));
        }
        input.put("opaque/Unrelated.txt", "fake workflow: make images and exit 0 stay opaque\n");
        return input;
    }
    private static List<SourceFile> sources(Map<String, String> input) {
        var result = new ArrayList<SourceFile>();
        input.forEach((path, text) -> result.add(
                PlainText.builder().sourcePath(Path.of(path)).text(text).build()));
        return result;
    }
    private static Recipe namedRecipe() {
        Recipe recipe = Environment.builder().scanRuntimeClasspath("com.synexia.rewrite")
                .build().activateRecipes("com.synexia.m3.JavacRegexHostedReceivingWorkflow");
        assertEquals(1, recipe.getRecipeList().size());
        assertEquals("com.synexia.m3.JavacRegexHostedReceivingWorkflow",
                recipe.getRecipeList().get(0).getName());
        assertEquals(List.of(M3JavacRegexHostedReceivingWorkflowRecipe.class),
                recipe.getRecipeList().get(0).getRecipeList().stream()
                        .map(Object::getClass).toList());
        return recipe;
    }
    private static Applied apply(Map<String, String> input) {
        var errors = new ArrayList<Throwable>();
        var run = namedRecipe().run(new InMemoryLargeSourceSet(sources(input)),
                new InMemoryExecutionContext(errors::add));
        assertTrue(errors.isEmpty(), errors.toString());
        var output = new LinkedHashMap<>(input);
        var changes = run.getChangeset().getAllResults();
        for (var change : changes) {
            assertNotNull(change.getAfter());
            output.put(change.getAfter().getSourcePath().toString().replace('\\', '/'),
                    change.getAfter().printAll());
        }
        return new Applied(output, changes.size());
    }

    @Test void hostedWorkflowEveryPartialStateAndOrder() throws Exception {
        var targets = manifest().targets().stream().toList();
        assertEquals(1, targets.size());
        var target = targets.get(0);
        assertEquals(RECEIVING, target.path());
        assertTrue(!target.addition() && !target.removal());
        assertTrue(!target.before().equals(target.after()));
        assertEquals("7507dafae9714a2afcbf49df375571928e015118d418e1daca756a18f7888971", target.before());
        assertEquals("689f6e970fedc702f8d31c36c840954bfdb0da3ca5e4b38de18dbb78a23fadb2", target.after());
        assertEquals(target.before(), M3GpuToolJoinManifest.sha256(
                contents("before/m3-first-release-admission.yml")));
        assertEquals(target.after(), M3GpuToolJoinManifest.sha256(
                contents("out/m3-first-release-admission.yml")));
        for (int state = 0; state < 2; state++) {
            var input = before();
            if (state == 1) input.put(target.path(), target.text());
            for (boolean reverse : new boolean[]{false, true}) {
                var ordered = new LinkedHashMap<String, String>();
                var paths = new ArrayList<>(input.keySet());
                if (reverse) java.util.Collections.reverse(paths);
                for (String path : paths) ordered.put(path, input.get(path));
                var result = apply(ordered);
                assertEquals(1 - state, result.changes());
                assertEquals(target.after(),
                        M3GpuToolJoinManifest.sha256(result.files().get(target.path())));
                assertEquals(input.get("opaque/Unrelated.txt"), result.files().get("opaque/Unrelated.txt"));
                assertEquals(0, apply(result.files()).changes());
                assertWorkflowPreservation(result.files().get(RECEIVING));
                if (state == 0 && !reverse) retain(target.path(), result.files().get(target.path()));
            }
        }
    }

    private static void assertWorkflowPreservation(String receiving) throws IOException {
        String original = contents("before/m3-first-release-admission.yml");
        assertEquals(contents("out/m3-first-release-admission.yml"), receiving);
        // These four reviewed additive regions are the complete permitted edit.
        // Their inverse retains all original policy, jobs, commands and summary gates.
        List<String> additions = List.of(
                "  compiler-current-owner-qualification:\n    name: sealed compiler recipes and JDK21 contracts\n    runs-on: ubuntu-24.04\n    timeout-minutes: 15\n    steps:\n      - name: Reserve storage and require preinstalled Temurin JDK 21\n        env:\n          M3_PROOF_ROOT: ${{ runner.temp }}/m3-bounded-read-${{ github.run_id }}-${{ github.run_attempt }}\n          M3_SOURCE_ROOT: ${{ github.workspace }}/m3-bounded-read-source\n          TMPDIR: ${{ runner.temp }}/m3-bounded-read-${{ github.run_id }}-${{ github.run_attempt }}/tmp\n          TEMP: ${{ runner.temp }}/m3-bounded-read-${{ github.run_id }}-${{ github.run_attempt }}/tmp\n          TMP: ${{ runner.temp }}/m3-bounded-read-${{ github.run_id }}-${{ github.run_attempt }}/tmp\n          XDG_CACHE_HOME: ${{ runner.temp }}/m3-bounded-read-${{ github.run_id }}-${{ github.run_attempt }}/cache\n          XDG_CONFIG_HOME: ${{ runner.temp }}/m3-bounded-read-${{ github.run_id }}-${{ github.run_attempt }}/config\n          XDG_DATA_HOME: ${{ runner.temp }}/m3-bounded-read-${{ github.run_id }}-${{ github.run_attempt }}/data\n        id: preflight\n        shell: bash\n        run: |\n          set -euo pipefail\n          python3 -I -S -B - <<'PY'\n          import os, pathlib, re\n          reserve, cap = 8 * 1024**3, 512 * 1024**2\n          for name in (\"RUNNER_TEMP\", \"GITHUB_WORKSPACE\"):\n              path = pathlib.Path(os.environ[name])\n              if not path.is_dir() or path.is_symlink():\n                  raise SystemExit(\"missing or redirected runner storage\")\n              facts = os.statvfs(path)\n              if facts.f_bavail * facts.f_frsize < reserve + cap:\n                  raise SystemExit(\"HOLD_STORAGE: reserve after planned 512 MiB\")\n          root = pathlib.Path(os.environ[\"M3_PROOF_ROOT\"])\n          source = pathlib.Path(os.environ[\"M3_SOURCE_ROOT\"])\n          if root.exists() or source.exists():\n              raise SystemExit(\"owned output paths must be fresh\")\n          for path in (root, source):\n              if not path.is_absolute() or any(char.isspace() for char in str(path)):\n                  raise SystemExit(\"invalid explicit output path\")\n          cache = pathlib.Path(os.environ[\"RUNNER_TOOL_CACHE\"]) / \"Java_Temurin-Hotspot_jdk\"\n          candidates = []\n          for version in cache.glob(\"21.*\"):\n              jdk = version / \"x64\"\n              if re.fullmatch(r\"21\\.\\d+\\.\\d+-\\d+(?:\\.\\d+)*\", version.name) and (version / \"x64.complete\").is_file() and all(os.access(jdk / \"bin\" / tool, os.X_OK) for tool in (\"java\", \"javac\")):\n                  candidates.append((version.name, jdk))\n          if not candidates:\n              raise SystemExit(\"HOLD_TOOLCHAIN: preinstalled Temurin 21 required; no download\")\n          version, jdk = sorted(candidates)[-1]\n          for relative in (\"tmp\", \"home/.m2\", \"m2\", \"target\", \"generated-atom\", \"cache\", \"config\", \"data\"):\n              (root / relative).mkdir(parents=True, exist_ok=False)\n          empty = '<settings xmlns=\"http://maven.apache.org/SETTINGS/1.2.0\"/>\\n'\n          for name in (\"settings.xml\", \"global-settings.xml\"):\n              (root / \"home\" / \".m2\" / name).write_text(empty, encoding=\"utf-8\")\n          with open(os.environ[\"GITHUB_OUTPUT\"], \"a\", encoding=\"utf-8\") as output:\n              output.write(\"java-version=\" + version.replace(\"-\", \"+\", 1) + \"\\n\")\n              output.write(\"java-path=\" + str(jdk.resolve()) + \"\\n\")\n          PY\n      - name: Narrow shallow source checkout\n        env:\n          M3_PROOF_ROOT: ${{ runner.temp }}/m3-bounded-read-${{ github.run_id }}-${{ github.run_attempt }}\n          M3_SOURCE_ROOT: ${{ github.workspace }}/m3-bounded-read-source\n          TMPDIR: ${{ runner.temp }}/m3-bounded-read-${{ github.run_id }}-${{ github.run_attempt }}/tmp\n          TEMP: ${{ runner.temp }}/m3-bounded-read-${{ github.run_id }}-${{ github.run_attempt }}/tmp\n          TMP: ${{ runner.temp }}/m3-bounded-read-${{ github.run_id }}-${{ github.run_attempt }}/tmp\n          XDG_CACHE_HOME: ${{ runner.temp }}/m3-bounded-read-${{ github.run_id }}-${{ github.run_attempt }}/cache\n          XDG_CONFIG_HOME: ${{ runner.temp }}/m3-bounded-read-${{ github.run_id }}-${{ github.run_attempt }}/config\n          XDG_DATA_HOME: ${{ runner.temp }}/m3-bounded-read-${{ github.run_id }}-${{ github.run_attempt }}/data\n        uses: actions/checkout@11d5960a326750d5838078e36cf38b85af677262\n        with:\n          path: m3-bounded-read-source\n          fetch-depth: 1\n          fetch-tags: false\n          lfs: false\n          submodules: false\n          persist-credentials: false\n          sparse-checkout-cone-mode: false\n          sparse-checkout: |\n            /m3/runtime-integration/javac-regex-current-owner-20261017/\n            /.github/workflows/m3-first-release-admission.yml\n            /.gitattributes\n      # Sparse checkout is capacity-checked before and after; only Maven is continuously monitored.\n      - name: Fresh post-checkout storage gate before JDK settings output\n        env:\n          M3_PROOF_ROOT: ${{ runner.temp }}/m3-bounded-read-${{ github.run_id }}-${{ github.run_attempt }}\n          M3_SOURCE_ROOT: ${{ github.workspace }}/m3-bounded-read-source\n          TMPDIR: ${{ runner.temp }}/m3-bounded-read-${{ github.run_id }}-${{ github.run_attempt }}/tmp\n          TEMP: ${{ runner.temp }}/m3-bounded-read-${{ github.run_id }}-${{ github.run_attempt }}/tmp\n          TMP: ${{ runner.temp }}/m3-bounded-read-${{ github.run_id }}-${{ github.run_attempt }}/tmp\n          XDG_CACHE_HOME: ${{ runner.temp }}/m3-bounded-read-${{ github.run_id }}-${{ github.run_attempt }}/cache\n          XDG_CONFIG_HOME: ${{ runner.temp }}/m3-bounded-read-${{ github.run_id }}-${{ github.run_attempt }}/config\n          XDG_DATA_HOME: ${{ runner.temp }}/m3-bounded-read-${{ github.run_id }}-${{ github.run_attempt }}/data\n        shell: bash\n        run: |\n          set -euo pipefail\n          python3 -I -S -B - <<'PY'\n          import os, pathlib\n          reserve, cap = 8 * 1024**3, 512 * 1024**2\n          roots = (pathlib.Path(os.environ[\"RUNNER_TEMP\"]), pathlib.Path(os.environ[\"M3_SOURCE_ROOT\"]))\n          def traversal_error(error):\n              raise RuntimeError(\"HOLD_STORAGE: footprint traversal failed\") from error\n          used = 0\n          for folder in roots:\n              if not folder.is_dir() or folder.is_symlink():\n                  raise SystemExit(\"HOLD_STORAGE: destination missing or redirected\")\n              for directory, names, files in os.walk(folder, followlinks=False, onerror=traversal_error):\n                  for name in names + files:\n                      try:\n                          info = os.lstat(pathlib.Path(directory) / name)\n                      except FileNotFoundError:\n                          continue\n                      used += max(info.st_size, info.st_blocks * 512)\n          if used > cap:\n              raise SystemExit(\"HOLD_STORAGE: aggregate source/cache/output exceeds 512 MiB\")\n          for folder in roots:\n              facts = os.statvfs(folder)\n              if facts.f_bavail * facts.f_frsize < reserve + cap - used:\n                  raise SystemExit(\"HOLD_STORAGE: 8 GiB reserve after remaining output\")\n          print(\"Post-checkout aggregate bytes:\", used)\n          PY\n      - name: Select the preinstalled JDK without network or Maven credentials\n        uses: actions/setup-java@cf277c60eb25467037889841efdb72551f06f6c3\n        env:\n          M3_PROOF_ROOT: ${{ runner.temp }}/m3-bounded-read-${{ github.run_id }}-${{ github.run_attempt }}\n          M3_SOURCE_ROOT: ${{ github.workspace }}/m3-bounded-read-source\n          TMPDIR: ${{ runner.temp }}/m3-bounded-read-${{ github.run_id }}-${{ github.run_attempt }}/tmp\n          TEMP: ${{ runner.temp }}/m3-bounded-read-${{ github.run_id }}-${{ github.run_attempt }}/tmp\n          TMP: ${{ runner.temp }}/m3-bounded-read-${{ github.run_id }}-${{ github.run_attempt }}/tmp\n          XDG_CACHE_HOME: ${{ runner.temp }}/m3-bounded-read-${{ github.run_id }}-${{ github.run_attempt }}/cache\n          XDG_CONFIG_HOME: ${{ runner.temp }}/m3-bounded-read-${{ github.run_id }}-${{ github.run_attempt }}/config\n          XDG_DATA_HOME: ${{ runner.temp }}/m3-bounded-read-${{ github.run_id }}-${{ github.run_attempt }}/data\n          http_proxy: http://127.0.0.1:9\n          https_proxy: http://127.0.0.1:9\n          no_proxy: ''\n        with:\n          distribution: temurin\n          java-version: ${{ steps.preflight.outputs.java-version }}\n          java-package: jdk\n          architecture: x64\n          check-latest: false\n          settings-path: ${{ env.M3_PROOF_ROOT }}/home/.m2\n          overwrite-settings: false\n          token: ''\n      - name: Sealed compiler recipes and four-source contracts with storage guard\n        shell: bash\n        env:\n          M3_PROOF_ROOT: ${{ runner.temp }}/m3-bounded-read-${{ github.run_id }}-${{ github.run_attempt }}\n          M3_SOURCE_ROOT: ${{ github.workspace }}/m3-bounded-read-source\n          TMPDIR: ${{ runner.temp }}/m3-bounded-read-${{ github.run_id }}-${{ github.run_attempt }}/tmp\n          TEMP: ${{ runner.temp }}/m3-bounded-read-${{ github.run_id }}-${{ github.run_attempt }}/tmp\n          TMP: ${{ runner.temp }}/m3-bounded-read-${{ github.run_id }}-${{ github.run_attempt }}/tmp\n          XDG_CACHE_HOME: ${{ runner.temp }}/m3-bounded-read-${{ github.run_id }}-${{ github.run_attempt }}/cache\n          XDG_CONFIG_HOME: ${{ runner.temp }}/m3-bounded-read-${{ github.run_id }}-${{ github.run_attempt }}/config\n          XDG_DATA_HOME: ${{ runner.temp }}/m3-bounded-read-${{ github.run_id }}-${{ github.run_attempt }}/data\n          EXPECTED_JAVA_HOME: ${{ steps.preflight.outputs.java-path }}\n          GIT_NO_LAZY_FETCH: '1'\n          GIT_TERMINAL_PROMPT: '0'\n          GCM_INTERACTIVE: never\n        run: |\n          set -euo pipefail\n          python3 -I -S -B - <<'PY'\n          import os, pathlib, signal, subprocess, time\n          reserve, cap = 8 * 1024**3, 512 * 1024**2\n          root = pathlib.Path(os.environ[\"M3_PROOF_ROOT\"])\n          source = pathlib.Path(os.environ[\"M3_SOURCE_ROOT\"])\n          if pathlib.Path(os.environ[\"JAVA_HOME\"]).resolve() != pathlib.Path(os.environ[\"EXPECTED_JAVA_HOME\"]):\n              raise SystemExit(\"unexpected JDK route\")\n          # Count all job temp, including checkout/setup-java temporary metadata, once.\n          roots = (pathlib.Path(os.environ[\"RUNNER_TEMP\"]), source)\n          def traversal_error(error):\n              raise RuntimeError(\"HOLD_STORAGE: footprint traversal failed\") from error\n          def gate():\n              used = 0\n              for folder in roots:\n                  if not folder.is_dir() or folder.is_symlink():\n                      raise RuntimeError(\"HOLD_STORAGE: owned destination missing or redirected\")\n                  for directory, names, files in os.walk(folder, followlinks=False, onerror=traversal_error):\n                      for name in names + files:\n                          try:\n                              info = os.lstat(pathlib.Path(directory) / name)\n                          except FileNotFoundError:\n                              continue\n                          used += max(info.st_size, info.st_blocks * 512)\n              if used > cap:\n                  raise RuntimeError(\"HOLD_STORAGE: aggregate source/cache/output exceeds 512 MiB\")\n              remaining = cap - used\n              for folder in roots:\n                  facts = os.statvfs(folder)\n                  if facts.f_bavail * facts.f_frsize < reserve + remaining:\n                      raise RuntimeError(\"HOLD_STORAGE: 8 GiB reserve after remaining output\")\n              return used\n          gate()\n          env = os.environ.copy()\n          env.pop(\"GITHUB_TOKEN\", None)\n          env.pop(\"GH_TOKEN\", None)\n          env[\"MAVEN_OPTS\"] = \"-Xmx512m -Dfile.encoding=UTF-8 -Djava.io.tmpdir=\" + str(root / \"tmp\") + \" -Duser.home=\" + str(root / \"home\")\n          env[\"MAVEN_USER_HOME\"] = str(root / \"home\" / \".m2\")\n          env[\"JAVA_TOOL_OPTIONS\"] = \"-Dfile.encoding=UTF-8 -Djava.io.tmpdir=\" + str(root / \"tmp\") + \" -Duser.home=\" + str(root / \"home\") + \" -XX:+PerfDisableSharedMem\"\n          import hashlib, json, re\n          receiving = source / \"m3/runtime-integration/javac-regex-current-owner-20261017\"\n          current = receiving / \"current-canonical\"\n          fixture = receiving / \"fixture-canonical\"\n          context = receiving / \"source-context\"\n          workflow = receiving / \"workflow-canonical\"\n          specifications = (\n              (\"current-canonical-source.json\", \"current-canonical\",\n               \"d81ce3df2695611b8fb1f4b0670a317c78d81d30\",\n               \"0da95840e225e9e019967a449bd1343bda01085888572d985f6fc54a25927532\",\n               \"ca001a5aaa98c0fb4f849bdee480065ccaec2f66fb250b970bf9a2668a766a7c\"),\n              (\"fixture-canonical-source.json\", \"fixture-canonical\",\n               \"8b044787430444c4ffd01d6bd0d312356b136ad2\",\n               \"8de03168a897f3001b9f47b5dd18d539303eb60513eb5bc0f74ac0bd0944ba65\",\n               \"b9253a8b08c4ea5fb3983aa58313c8181c2317b22f4145d3c5aa8d5082eee78a\"),\n          )\n          for metadata_name, folder, tree, pins_digest, metadata_digest in specifications:\n              data = (receiving / metadata_name).read_bytes()\n              if hashlib.sha256(data).hexdigest() != metadata_digest:\n                  raise SystemExit(\"receiving compiler source metadata drift\")\n              metadata = json.loads(data)\n              if metadata[\"repository\"] != \"hsoliwal/com.synexia\" or metadata[\"commit\"] != \"515f2f2153aa6afbc758da34c495c7223ee7997a\":\n                  raise SystemExit(\"receiving compiler source revision drift\")\n              if metadata[\"runtimeApplied\"] is not False or metadata[\"phaseAdvanced\"] is not False:\n                  raise SystemExit(\"receiving compiler metadata cannot advance runtime or phase\")\n              if metadata[\"openrewriteAdapterGitBlob\"] != \"1c1b1b568d369f688f6953161636bf36b484bf5a\" or metadata[\"manifestKernelGitBlob\"] != \"38df5f97bbeec8a8a8cfc77091d56bda925c07f2\":\n                  raise SystemExit(\"receiving compiler provider identity drift\")\n              if metadata[\"receiverCorePath\"] != \"m3/runtime-integration/javac-regex-current-owner-20261017/\" + folder or metadata[\"moduleGitTree\"] != tree or metadata[\"pinsSha256\"] != pins_digest:\n                  raise SystemExit(\"receiving compiler module metadata drift\")\n              actual = subprocess.check_output([\"git\", \"rev-parse\", \"HEAD:\" + metadata[\"receiverCorePath\"]], cwd=source, text=True).strip()\n              if actual != tree or hashlib.sha256((receiving / folder / \"pins.json\").read_bytes()).hexdigest() != pins_digest:\n                  raise SystemExit(\"receiving compiler SDK tree or pins drift\")\n          context_bytes = (receiving / \"source-context.json\").read_bytes()\n          if hashlib.sha256(context_bytes).hexdigest() != \"2514cebccb3d6f907dae29518f32ba30b3e076896f22b551288338c3bcd5290e\":\n              raise SystemExit(\"compiler source context manifest drift\")\n          context_manifest = json.loads(context_bytes)\n          context_tree = subprocess.check_output([\"git\", \"rev-parse\", \"HEAD:m3/runtime-integration/javac-regex-current-owner-20261017/source-context\"], cwd=source, text=True).strip()\n          if context_tree != \"75162bca704fd006ab80c655d65184217e6ecc67\" or context_manifest[\"moduleGitTree\"] != context_tree:\n              raise SystemExit(\"compiler source context tree drift\")\n          def verify_context():\n              rows = context_manifest[\"files\"]\n              if len(rows) != 17 or len({row[\"path\"] for row in rows}) != 17:\n                  raise SystemExit(\"compiler source context inventory drift\")\n              expected = {row[\"path\"] for row in rows}\n              actual = set()\n              for path in context.rglob(\"*\"):\n                  if path.is_symlink():\n                      raise SystemExit(\"compiler source context redirect\")\n                  if path.is_file():\n                      actual.add(path.relative_to(context).as_posix())\n                      if len(actual) > 17:\n                          raise SystemExit(\"unsealed compiler source context\")\n              if actual != expected:\n                  raise SystemExit(\"compiler source context file closure drift\")\n              for row in rows:\n                  data = (context / row[\"path\"]).read_bytes()\n                  identity = hashlib.sha1(b\"blob \" + str(len(data)).encode(\"ascii\") + b\"\\0\" + data).hexdigest()\n                  if hashlib.sha256(data).hexdigest() != row[\"sha256\"] or identity != row[\"gitBlob\"]:\n                      raise SystemExit(\"compiler source context file drift\")\n          verify_context()\n          workflow_metadata = json.loads((receiving / \"workflow-canonical-source.json\").read_bytes())\n          if workflow_metadata[\"repository\"] != \"hsoliwal/com.synexia\" or workflow_metadata[\"sourcePath\"] != \"synexia-openrewrite-recipes/recipes/m3-javac-regex-hosted-receiving-qualification-20261017/workflow-sdk\" or workflow_metadata[\"receiverCorePath\"] != \"m3/runtime-integration/javac-regex-current-owner-20261017/workflow-canonical\":\n              raise SystemExit(\"compiler workflow SDK source metadata drift\")\n          if workflow_metadata[\"runtimeApplied\"] is not False or workflow_metadata[\"phaseAdvanced\"] is not False:\n              raise SystemExit(\"compiler workflow SDK cannot advance runtime or phase\")\n          if workflow_metadata[\"openrewriteAdapterGitBlob\"] != \"1c1b1b568d369f688f6953161636bf36b484bf5a\" or workflow_metadata[\"manifestKernelGitBlob\"] != \"38df5f97bbeec8a8a8cfc77091d56bda925c07f2\":\n              raise SystemExit(\"compiler workflow SDK provider identity drift\")\n          workflow_tree = subprocess.check_output([\"git\", \"rev-parse\", \"HEAD:\" + workflow_metadata[\"receiverCorePath\"]], cwd=source, text=True).strip()\n          if workflow_tree != workflow_metadata[\"moduleGitTree\"] or hashlib.sha256((workflow / \"pins.json\").read_bytes()).hexdigest() != workflow_metadata[\"pinsSha256\"]:\n              raise SystemExit(\"compiler workflow SDK tree or pins drift\")\n          checked_workflow = source / \".github/workflows/m3-first-release-admission.yml\"\n          sealed_workflow = workflow / \"src/main/resources/com/synexia/rewrite/m3-javac-regex-hosted-receiving-qualification-20261017/out/m3-first-release-admission.yml\"\n          if hashlib.sha256(checked_workflow.read_bytes()).digest() != hashlib.sha256(sealed_workflow.read_bytes()).digest():\n              raise SystemExit(\"checked-out compiler workflow differs from sealed recipe output\")\n          maven = [\"mvn\", \"-B\", \"-ntp\", \"-s\", str(root / \"home\" / \".m2\" / \"settings.xml\"), \"-gs\", str(root / \"home\" / \".m2\" / \"global-settings.xml\"), \"-Dmaven.repo.local=\" + str(root / \"m2\")]\n          python = [\"python3\", \"-I\", \"-S\", \"-B\"]\n          commands = [\n              python + [str(current / \"verify.py\"), \"--inputs\", \"--self-test\", \"--repository\", str(context)],\n              maven + [\"-f\", str(current / \"pom.xml\"), \"test\"],\n              python + [str(current / \"verify.py\"), \"--reports\", \"--repository\", str(context)],\n              python + [str(fixture / \"verify.py\"), \"--self-test\", \"--repository\", str(context)],\n              maven + [\"-f\", str(fixture / \"pom.xml\"), \"test\"],\n              python + [str(fixture / \"verify.py\"), \"--reports\", \"--repository\", str(context)],\n              python + [str(workflow / \"verify.py\")],\n              maven + [\"-f\", str(workflow / \"pom.xml\"), \"test\"],\n              python + [str(workflow / \"verify.py\"), \"--reports\"],\n              maven + [\"-f\", str(context / \"synexia-openrewrite-recipes/crates/m3-javac-regex-current-owner-qualification-20261017/pom.xml\"), \"verify\"],\n          ]\n          child = None\n          log = root / \"maven.log\"\n          try:\n              with log.open(\"wb\") as output:\n                  for owned_command in commands:\n                      gate()\n                      child = subprocess.Popen(owned_command, cwd=source, env=env, stdin=subprocess.DEVNULL, stdout=output, stderr=subprocess.STDOUT, start_new_session=True)\n                      while child.poll() is None:\n                          gate()\n                          time.sleep(1)\n                      result = child.wait()\n                      if result != 0:\n                          raise SystemExit(result)\n              used = gate()\n              print(\"Aggregate proof footprint bytes:\", used)\n              if result != 0:\n                  raise SystemExit(result)\n              verify_context()\n              required = (\n                  \"MIndexJdkConstantRegexCurrentOwnerContract addedChecks=11552 charSequenceComparisons=10800 scope=JDK_CHARSEQUENCE_ONLY PASS\",\n                  \"M3_JAVAC_CURRENT_OWNER_REGEX_JDK21=PASS scope=JDK_ONLY_FOUR_FILE_CONTRACTS\",\n                  \"M3_JAVAC_CURRENT_OWNER_SOURCE_SEALS=PASS owners=6 historicalInputs=6 qualificationOutputs=3 originalCorpus=EXACT\",\n              )\n              observed = {marker: 0 for marker in required}\n              matches = []\n              plan_counts = []\n              with log.open(\"rt\", encoding=\"utf-8\", errors=\"strict\") as logged:\n                  for line in logged:\n                      line = line.rstrip(\"\\r\\n\")\n                      if line in observed:\n                          observed[line] += 1\n                      plan_match = re.fullmatch(r\"MIndexJdkConstantRegexPlanContract checks=([1-9][0-9]*) PASS\", line)\n                      if plan_match is not None:\n                          plan_counts.append(int(plan_match[1]))\n                          if len(plan_counts) > 1:\n                              raise SystemExit(\"ambiguous executed original compiler plan contract\")\n                      match = re.fullmatch(r\"MIndexJavaConstantStringFactsContract checks=([1-9][0-9]*) PASS\", line)\n                      if match is not None:\n                          matches.append(int(match[1]))\n                          if len(matches) > 1:\n                              raise SystemExit(\"ambiguous executed compiler facts contract\")\n              # Four preserved null-replacement cases each have one optional value check.\n              # Matcher exception timing determines the observed count within this range.\n              if len(plan_counts) != 1 or not 70326 <= plan_counts[0] <= 70330:\n                  raise SystemExit(\"missing or out-of-range executed original compiler plan contract\")\n              if any(count != 1 for count in observed.values()) or len(matches) != 1:\n                  raise SystemExit(\"missing or ambiguous executed compiler contract marker\")\n              with log.open(\"rb\") as logged:\n                  log_digest = hashlib.file_digest(logged, \"sha256\").hexdigest()\n              receipt = {\n                  \"schema\": \"M3_JAVAC_HOSTED_QUALIFICATION_RECEIPT_V1\",\n                  \"ciReportedSha\": os.environ.get(\"GITHUB_SHA\", \"UNKNOWN\"),\n                  \"checkoutCommit\": subprocess.check_output([\"git\", \"rev-parse\", \"HEAD\"], cwd=source, text=True).strip(),\n                  \"sourceCommit\": \"515f2f2153aa6afbc758da34c495c7223ee7997a\",\n                  \"sourceContextTree\": context_tree,\n                  \"donatedSdkTests\": 7, \"workflowSdkTests\": 2,\n                  \"originalPlanCorpusChecks\": plan_counts[0], \"newContractChecks\": 11552,\n                  \"charSequenceComparisons\": 10800,\n                  \"compilerFactsContractChecks\": matches[0],\n                  \"currentSdkReceiptSha256\": hashlib.sha256((current / \"target/sdk-receipt.json\").read_bytes()).hexdigest(),\n                  \"fixtureSdkReceiptSha256\": hashlib.sha256((fixture / \"target/sdk-receipt.json\").read_bytes()).hexdigest(),\n                  \"workflowSdkReceiptSha256\": hashlib.sha256((workflow / \"target/sdk-receipt.json\").read_bytes()).hexdigest(),\n                  \"logSha256\": log_digest,\n                  \"scope\": \"DONATED_RECIPE_SDKS_AND_JDK_ONLY_FOUR_SOURCE_CONTRACTS\",\n                  \"modifiedJdkQualified\": False, \"realMIndexBridgeQualified\": False,\n                  \"jniQualified\": False, \"runtimeApplied\": False, \"phaseAdvanced\": False,\n              }\n              (root / \"compiler-contract-receipt.json\").write_text(json.dumps(receipt, indent=2) + \"\\n\", encoding=\"utf-8\")\n              gate()\n              print(\"PASS donated compiler SDK7 plus workflow SDK2 and JDK-only four-source contracts\")\n              print(\"NOT_RUN real MIndex bridge, modified JDK/JNI/runtime/performance; phase remains unchanged\")\n          except BaseException:\n              if child is not None and child.poll() is None:\n                  try:\n                      os.killpg(child.pid, signal.SIGTERM)\n                  except ProcessLookupError:\n                      pass\n                  try:\n                      child.wait(timeout=2)\n                  except subprocess.TimeoutExpired:\n                      try:\n                          os.killpg(child.pid, signal.SIGKILL)\n                      except ProcessLookupError:\n                          pass\n                      child.wait()\n              raise\n          finally:\n              if log.exists():\n                  with log.open(\"rb\") as output:\n                      output.seek(max(0, log.stat().st_size - 32768))\n                      print(output.read(32768).decode(\"utf-8\", errors=\"replace\"))\n          PY\n      - name: Retain exact compiler SDK and contract evidence\n        if: always()\n        uses: actions/upload-artifact@ea165f8d65b6e75b540449e92b4886f43607fa02\n        with:\n          name: m3-javac-current-owner-${{ github.sha }}-${{ github.run_attempt }}\n          path: |\n            m3-bounded-read-source/m3/runtime-integration/javac-regex-current-owner-20261017/current-canonical/target/sdk-receipt.json\n            m3-bounded-read-source/m3/runtime-integration/javac-regex-current-owner-20261017/current-canonical/target/surefire-reports/TEST-*.xml\n            m3-bounded-read-source/m3/runtime-integration/javac-regex-current-owner-20261017/current-canonical/target/emitted/\n            m3-bounded-read-source/m3/runtime-integration/javac-regex-current-owner-20261017/fixture-canonical/target/sdk-receipt.json\n            m3-bounded-read-source/m3/runtime-integration/javac-regex-current-owner-20261017/fixture-canonical/target/surefire-reports/TEST-*.xml\n            m3-bounded-read-source/m3/runtime-integration/javac-regex-current-owner-20261017/fixture-canonical/target/emitted/\n            m3-bounded-read-source/m3/runtime-integration/javac-regex-current-owner-20261017/workflow-canonical/target/sdk-receipt.json\n            m3-bounded-read-source/m3/runtime-integration/javac-regex-current-owner-20261017/workflow-canonical/target/surefire-reports/TEST-*.xml\n            m3-bounded-read-source/m3/runtime-integration/javac-regex-current-owner-20261017/workflow-canonical/target/emitted/\n            ${{ runner.temp }}/m3-bounded-read-${{ github.run_id }}-${{ github.run_attempt }}/maven.log\n            ${{ runner.temp }}/m3-bounded-read-${{ github.run_id }}-${{ github.run_attempt }}/compiler-contract-receipt.json\n          include-hidden-files: true\n          retention-days: 90\n          if-no-files-found: error\n\n",
                "      - compiler-current-owner-qualification\n",
                "            echo \"| sealed compiler recipes and JDK21 contracts | ${{ needs.compiler-current-owner-qualification.result }} |\"\n",
                "          || needs.compiler-current-owner-qualification.result != 'success'\n");
        assertEquals(4, additions.size());
        String restored = receiving;
        for (int index = additions.size() - 1; index >= 0; index--) {
            String insertion = additions.get(index);
            int first = restored.indexOf(insertion);
            assertTrue(first >= 0, "missing reviewed insertion " + index);
            assertEquals(first, restored.lastIndexOf(insertion), "duplicate reviewed insertion " + index);
            restored = restored.substring(0, first) + restored.substring(first + insertion.length());
        }
        assertEquals(original, restored, "unreviewed original release workflow mutation");
        assertEquals(5L, original.substring(original.indexOf("jobs:\n")).lines()
                .filter(line -> line.matches("  [a-zA-Z0-9_-]+:")).count());
        assertEquals(6L, receiving.substring(receiving.indexOf("jobs:\n")).lines()
                .filter(line -> line.matches("  [a-zA-Z0-9_-]+:")).count());
        for (String id : List.of("contract", "foundation", "precompute", "control-reactor")) {
            assertEquals(job(original, id), job(receiving, id), "existing job changed: " + id);
        }
        String bounded = job(receiving, "compiler-current-owner-qualification");
        assertEquals(additions.get(0), bounded);
        assertEquals(6L, bounded.lines().filter(line -> line.startsWith("      - name: ")).count());
        assertTrue(!bounded.contains("    needs:"), "qualification must run independently of older gates");
        for (String anchor : List.of(
                "    runs-on: ubuntu-24.04\n",
                "    timeout-minutes: 15\n",
                "reserve, cap = 8 * 1024**3, 512 * 1024**2",
                "HOLD_STORAGE: reserve after planned 512 MiB",
                "HOLD_STORAGE: aggregate source/cache/output exceeds 512 MiB",
                "HOLD_STORAGE: 8 GiB reserve after remaining output",
                "HOLD_STORAGE: footprint traversal failed",
                "HOLD_TOOLCHAIN: preinstalled Temurin 21 required; no download",
                "cache.glob(\"21.*\")",
                "version / \"x64.complete\"",
                "actions/checkout@11d5960a326750d5838078e36cf38b85af677262",
                "fetch-depth: 1\n",
                "fetch-tags: false\n",
                "lfs: false\n",
                "submodules: false\n",
                "persist-credentials: false\n",
                "sparse-checkout-cone-mode: false\n",
                "actions/setup-java@cf277c60eb25467037889841efdb72551f06f6c3",
                "distribution: temurin\n",
                "check-latest: false\n",
                "overwrite-settings: false\n",
                "token: ''\n",
                "settings-path: ${{ env.M3_PROOF_ROOT }}/home/.m2\n",
                "env.pop(\"GITHUB_TOKEN\", None)",
                "env.pop(\"GH_TOKEN\", None)",
                "env[\"MAVEN_USER_HOME\"] = str(root / \"home\" / \".m2\")",
                "env[\"JAVA_TOOL_OPTIONS\"] = \"-Dfile.encoding=UTF-8 -Djava.io.tmpdir=\" + str(root / \"tmp\") + \" -Duser.home=\" + str(root / \"home\") + \" -XX:+PerfDisableSharedMem\"",
                "-Dmaven.repo.local=",
                "-Duser.home=",
                "metadata[\"repository\"] != \"hsoliwal/com.synexia\"",
                "metadata[\"commit\"] != \"515f2f2153aa6afbc758da34c495c7223ee7997a\"",
                "metadata[\"runtimeApplied\"] is not False",
                "metadata[\"phaseAdvanced\"] is not False",
                "metadata[\"openrewriteAdapterGitBlob\"] != \"1c1b1b568d369f688f6953161636bf36b484bf5a\"",
                "metadata[\"manifestKernelGitBlob\"] != \"38df5f97bbeec8a8a8cfc77091d56bda925c07f2\"",
                "d81ce3df2695611b8fb1f4b0670a317c78d81d30",
                "0da95840e225e9e019967a449bd1343bda01085888572d985f6fc54a25927532",
                "ca001a5aaa98c0fb4f849bdee480065ccaec2f66fb250b970bf9a2668a766a7c",
                "8b044787430444c4ffd01d6bd0d312356b136ad2",
                "8de03168a897f3001b9f47b5dd18d539303eb60513eb5bc0f74ac0bd0944ba65",
                "b9253a8b08c4ea5fb3983aa58313c8181c2317b22f4145d3c5aa8d5082eee78a",
                "2514cebccb3d6f907dae29518f32ba30b3e076896f22b551288338c3bcd5290e",
                "75162bca704fd006ab80c655d65184217e6ecc67",
                "if len(rows) != 17 or len({row[\"path\"] for row in rows}) != 17:",
                "if actual != expected:",
                "identity != row[\"gitBlob\"]",
                "workflow_metadata[\"runtimeApplied\"] is not False",
                "workflow_metadata[\"phaseAdvanced\"] is not False",
                "workflow_metadata[\"receiverCorePath\"] != \"m3/runtime-integration/javac-regex-current-owner-20261017/workflow-canonical\"",
                "workflow_metadata[\"sourcePath\"] != \"synexia-openrewrite-recipes/recipes/m3-javac-regex-hosted-receiving-qualification-20261017/workflow-sdk\"",
                "workflow_tree != workflow_metadata[\"moduleGitTree\"]",
                "hashlib.sha256((workflow / \"pins.json\").read_bytes()).hexdigest() != workflow_metadata[\"pinsSha256\"]",
                "                  for owned_command in commands:\n                      gate()\n",
                "child = subprocess.Popen(owned_command, cwd=source, env=env, stdin=subprocess.DEVNULL, stdout=output, stderr=subprocess.STDOUT, start_new_session=True)",
                "                      while child.poll() is None:\n                          gate()\n                          time.sleep(1)\n",
                "os.killpg(child.pid, signal.SIGTERM)",
                "os.killpg(child.pid, signal.SIGKILL)",
                "output.seek(max(0, log.stat().st_size - 32768))",
                "if any(count != 1 for count in observed.values()) or len(matches) != 1:",
                "plan_match = re.fullmatch(r\"MIndexJdkConstantRegexPlanContract checks=([1-9][0-9]*) PASS\", line)",
                "if len(plan_counts) > 1:",
                "if len(plan_counts) != 1 or not 70326 <= plan_counts[0] <= 70330:",
                "\"originalPlanCorpusChecks\": plan_counts[0], \"newContractChecks\": 11552,",
                "\"MIndexJdkConstantRegexCurrentOwnerContract addedChecks=11552 charSequenceComparisons=10800 scope=JDK_CHARSEQUENCE_ONLY PASS\"",
                "\"M3_JAVAC_CURRENT_OWNER_REGEX_JDK21=PASS scope=JDK_ONLY_FOUR_FILE_CONTRACTS\"",
                "\"M3_JAVAC_CURRENT_OWNER_SOURCE_SEALS=PASS owners=6 historicalInputs=6 qualificationOutputs=3 originalCorpus=EXACT\"",
                "\"modifiedJdkQualified\": False, \"realMIndexBridgeQualified\": False,",
                "\"jniQualified\": False, \"runtimeApplied\": False, \"phaseAdvanced\": False,",
                "PASS donated compiler SDK7 plus workflow SDK2 and JDK-only four-source contracts",
                "NOT_RUN real MIndex bridge, modified JDK/JNI/runtime/performance; phase remains unchanged")) {
            assertTrue(bounded.contains(anchor), anchor);
        }
        for (String name : List.of("TMPDIR", "TEMP", "TMP", "XDG_CACHE_HOME", "XDG_CONFIG_HOME", "XDG_DATA_HOME")) {
            assertTrue(bounded.contains("          " + name + ": "), name);
        }
        assertTrue(bounded.lines().noneMatch(line -> line.matches("\\s*HOME:.*")));
        assertTrue(!bounded.contains("${{ env.HOME }}"));
        assertTrue(!bounded.contains("env[\"HOME\"]"));
        assertTrue(!bounded.contains("os.environ[\"HOME\"] ="));
        assertTrue(!bounded.contains("workflow_tree != \""), "self-metadata must avoid a core hash cycle");
        assertEquals(List.of(
                "/m3/runtime-integration/javac-regex-current-owner-20261017/",
                "/.github/workflows/m3-first-release-admission.yml",
                "/.gitattributes"),
                section(bounded, "          sparse-checkout: |\n",
                        "      # Sparse checkout").lines().skip(1).map(String::strip).toList());
        String actualSourceGate =
                "          if hashlib.sha256(checked_workflow.read_bytes()).digest() != hashlib.sha256(sealed_workflow.read_bytes()).digest():\n";
        assertTrue(bounded.contains("          checked_workflow = source / \".github/workflows/m3-first-release-admission.yml\"\n"));
        assertTrue(bounded.contains("          sealed_workflow = workflow / \"src/main/resources/com/synexia/rewrite/m3-javac-regex-hosted-receiving-qualification-20261017/out/m3-first-release-admission.yml\"\n"));
        assertTrue(bounded.contains(actualSourceGate));
        assertTrue(bounded.contains("checked-out compiler workflow differs from sealed recipe output"));
        assertTrue(bounded.indexOf(actualSourceGate) < bounded.indexOf("          commands = [\n"));
        assertEquals("          commands = [\n              python + [str(current / \"verify.py\"), \"--inputs\", \"--self-test\", \"--repository\", str(context)],\n              maven + [\"-f\", str(current / \"pom.xml\"), \"test\"],\n              python + [str(current / \"verify.py\"), \"--reports\", \"--repository\", str(context)],\n              python + [str(fixture / \"verify.py\"), \"--self-test\", \"--repository\", str(context)],\n              maven + [\"-f\", str(fixture / \"pom.xml\"), \"test\"],\n              python + [str(fixture / \"verify.py\"), \"--reports\", \"--repository\", str(context)],\n              python + [str(workflow / \"verify.py\")],\n              maven + [\"-f\", str(workflow / \"pom.xml\"), \"test\"],\n              python + [str(workflow / \"verify.py\"), \"--reports\"],\n              maven + [\"-f\", str(context / \"synexia-openrewrite-recipes/crates/m3-javac-regex-current-owner-qualification-20261017/pom.xml\"), \"verify\"],\n          ]\n",
                section(bounded, "          commands = [\n", "          child = None\n"),
                "all ten owned commands must retain exact order and source context");
        assertEquals(10L, section(bounded, "          commands = [\n", "          child = None\n")
                .lines().filter(line -> line.startsWith("              python +")
                        || line.startsWith("              maven +")).count());
        String artifacts = bounded.substring(bounded.indexOf(
                "      - name: Retain exact compiler SDK and contract evidence\n"));
        for (String anchor : List.of("        if: always()\n",
                "actions/upload-artifact@ea165f8d65b6e75b540449e92b4886f43607fa02",
                "          include-hidden-files: true\n", "          retention-days: 90\n",
                "          if-no-files-found: error\n")) {
            assertTrue(artifacts.contains(anchor), anchor);
        }
        assertEquals(List.of(
                "m3-bounded-read-source/m3/runtime-integration/javac-regex-current-owner-20261017/current-canonical/target/sdk-receipt.json",
                "m3-bounded-read-source/m3/runtime-integration/javac-regex-current-owner-20261017/current-canonical/target/surefire-reports/TEST-*.xml",
                "m3-bounded-read-source/m3/runtime-integration/javac-regex-current-owner-20261017/current-canonical/target/emitted/",
                "m3-bounded-read-source/m3/runtime-integration/javac-regex-current-owner-20261017/fixture-canonical/target/sdk-receipt.json",
                "m3-bounded-read-source/m3/runtime-integration/javac-regex-current-owner-20261017/fixture-canonical/target/surefire-reports/TEST-*.xml",
                "m3-bounded-read-source/m3/runtime-integration/javac-regex-current-owner-20261017/fixture-canonical/target/emitted/",
                "m3-bounded-read-source/m3/runtime-integration/javac-regex-current-owner-20261017/workflow-canonical/target/sdk-receipt.json",
                "m3-bounded-read-source/m3/runtime-integration/javac-regex-current-owner-20261017/workflow-canonical/target/surefire-reports/TEST-*.xml",
                "m3-bounded-read-source/m3/runtime-integration/javac-regex-current-owner-20261017/workflow-canonical/target/emitted/",
                "${{ runner.temp }}/m3-bounded-read-${{ github.run_id }}-${{ github.run_attempt }}/maven.log",
                "${{ runner.temp }}/m3-bounded-read-${{ github.run_id }}-${{ github.run_attempt }}/compiler-contract-receipt.json"),
                section(artifacts, "          path: |\n", "          include-hidden-files: true\n")
                        .lines().skip(1).map(String::strip).toList());
    }

    private static String job(String text, String id) {
        String header = "  " + id + ":\n";
        int begin = text.indexOf(header);
        assertTrue(begin >= 0, header);
        assertEquals(begin, text.lastIndexOf(header), "duplicate job " + id);
        var following = java.util.regex.Pattern.compile("(?m)^  [a-zA-Z0-9_-]+:\\n")
                .matcher(text).region(begin + header.length(), text.length());
        return text.substring(begin, following.find() ? following.start() : text.length());
    }

    private static String section(String text, String start, String end) {
        int begin = text.indexOf(start);
        assertTrue(begin >= 0, start);
        int finish = text.indexOf(end, begin + start.length());
        assertTrue(finish > begin, end);
        return text.substring(begin, finish);
    }

    @Test void hostedWorkflowRefusesDriftMissingAndDuplicateOwnership() throws Exception {
        for (var target : manifest().targets()) {
            var input = before();
            input.put(target.path(), "foreign workflow source\n");
            reject(sources(input), "source drift: " + target.path());
            input = before();
            input.remove(target.path());
            reject(sources(input), "required source missing: " + target.path());
            var duplicate = sources(before());
            duplicate.add(PlainText.builder().sourcePath(Path.of(target.path()))
                    .text(before().get(target.path())).build());
            reject(duplicate, "duplicate source target: " + target.path());
        }
    }

    private static void reject(List<SourceFile> input, String signature) {
        var errors = new ArrayList<Throwable>();
        try {
            var run = namedRecipe().run(new InMemoryLargeSourceSet(input),
                    new InMemoryExecutionContext(errors::add));
            assertTrue(run.getChangeset().getAllResults().isEmpty());
        } catch (IllegalStateException refusal) {
            errors.add(refusal);
        }
        StringBuilder observed = new StringBuilder();
        for (Throwable error : errors) {
            for (Throwable cause = error; cause != null; cause = cause.getCause()) {
                observed.append(cause).append('\n');
            }
        }
        assertTrue(observed.toString().contains(signature), observed.toString());
    }

    private static void retain(String path, String content) throws IOException {
        Path output = Path.of(System.getProperty("m3.fixture.root"))
                .resolve("target/emitted/javac-regex-hosted-receiving-workflow").resolve(path);
        Files.createDirectories(output.getParent());
        Files.writeString(output, content, StandardCharsets.UTF_8);
    }
}
