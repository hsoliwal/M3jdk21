from pathlib import Path
import runpy

SCRIPT = Path(__file__).resolve().parent / "check-synexia-rapidex-gap.py"
ROOT = SCRIPT.parents[2]

def test_rapidex_remains_fail_closed():
    result = runpy.run_path(str(SCRIPT))
    failed, checks = result["check"](ROOT)
    assert not failed, failed
    assert len(checks) == 14
    assert result["EXPECTED"]["status"] == "NO_CANONICAL_OWNER"
