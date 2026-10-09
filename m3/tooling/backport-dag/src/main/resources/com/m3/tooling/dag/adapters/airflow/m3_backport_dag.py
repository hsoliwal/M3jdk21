# SPDX-License-Identifier: Apache-2.0
# M3-DAG-ROOT: e5ab303248fa13f1e94317d7ce69bae54688d3a68e0d130ff8c192fa58208667
"""Airflow projection of the canonical M3JDK21 backport DAG."""

from airflow import DAG
from airflow.operators.bash import BashOperator
from datetime import datetime

with DAG(
    "m3_jdk21_backport",
    start_date=datetime(2026, 1, 1),
    schedule=None,
    catchup=False,
) as dag:
    review_code = BashOperator(task_id="review-code-signal", bash_command="echo code-signal-review")
    review_atom = BashOperator(task_id="review-atom-pattern", bash_command="echo atom-pattern-review")
    review_problem = BashOperator(task_id="review-problem-planner", bash_command="echo problem-planner-review")
    review_jni = BashOperator(task_id="review-jni-contract", bash_command="echo jni-contract-review")
    inventory = BashOperator(task_id="inventory", bash_command="python3 m3/backports/inventory.py --help")
    compatibility = BashOperator(task_id="compatibility-proof", bash_command="python3 m3/backports/compatibility_queue.py --help")
    dependency = BashOperator(task_id="dependency-closure", bash_command="echo dependency-closure")
    delta = BashOperator(task_id="file-delta", bash_command="python3 m3/backports/file_delta_inventory.py --help")
    a3 = BashOperator(task_id="a3-preparation", bash_command="echo run-M3A3BackportPreparation")
    a3_atom = BashOperator(task_id="a3-atomization-proof", bash_command="echo verify-A3-ATOMIZATION")
    a3_pattern = BashOperator(task_id="a3-pattern-iop-proof", bash_command="echo verify-A3-PATTERN_IOP")
    a3_docs = BashOperator(task_id="a3-documentation-proof", bash_command="echo verify-A3-DOCUMENTATION")
    a3_fixed = BashOperator(task_id="a3-fixed-point-proof", bash_command="echo verify-A3-FIXED_POINT")
    recipe = BashOperator(task_id="recipe-crate", bash_command="python3 m3/backports/generate_recipe_crates.py --help")
    recipe_junit = BashOperator(task_id="recipe-junit", bash_command="mvn -B -ntp -f m3/tooling/migration-recipes/pom.xml test")
    diff = BashOperator(task_id="diff", bash_command="git diff --check")
    lint = BashOperator(task_id="lint", bash_command="echo lint")
    compile_jdk = BashOperator(task_id="compile", bash_command="echo openjdk-build")
    jtreg = BashOperator(task_id="jtreg", bash_command="echo jtreg")
    runtime = BashOperator(task_id="runtime", bash_command="echo runtime-proof")
    promote = BashOperator(task_id="promote", bash_command="echo serial-promotion")

    review_code >> review_atom >> review_problem >> review_jni
    review_jni >> inventory >> compatibility >> dependency >> delta >> a3
    a3 >> a3_atom >> a3_pattern >> a3_docs >> a3_fixed >> recipe >> recipe_junit
    recipe_junit >> diff >> lint >> compile_jdk >> jtreg >> runtime >> promote
