# SPDX-License-Identifier: Apache-2.0
# M3-DAG-ROOT: 4b51e61047b7455498c98592d80f71f5c743ab65e2d8d9724037b283d02d92af
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
    inventory = BashOperator(task_id="inventory", bash_command="python3 m3/backports/inventory.py --help")
    compatibility = BashOperator(task_id="compatibility-proof", bash_command="python3 m3/backports/compatibility_queue.py --help")
    dependency = BashOperator(task_id="dependency-closure", bash_command="echo dependency-closure")
    delta = BashOperator(task_id="file-delta", bash_command="python3 m3/backports/file_delta_inventory.py --help")
    baseline = BashOperator(task_id="baseline-convergence", bash_command="echo require SOURCE_CONVERGENCE fixed-point receipt")
    recipe = BashOperator(task_id="recipe-crate", bash_command="python3 m3/backports/generate_recipe_crates.py --help")
    recipe_junit = BashOperator(task_id="recipe-junit", bash_command="mvn -B -ntp -f m3/tooling/migration-recipes/pom.xml test")
    diff = BashOperator(task_id="diff", bash_command="git diff --check")
    lint = BashOperator(task_id="lint", bash_command="echo lint")
    compile_jdk = BashOperator(task_id="compile", bash_command="echo openjdk-build")
    jtreg = BashOperator(task_id="jtreg", bash_command="echo jtreg")
    runtime = BashOperator(task_id="runtime", bash_command="echo runtime-proof")
    promote = BashOperator(task_id="promote", bash_command="echo serial-promotion")

    inventory >> compatibility >> dependency >> delta >> baseline >> recipe >> recipe_junit
    recipe_junit >> diff >> lint >> compile_jdk >> jtreg >> runtime >> promote
