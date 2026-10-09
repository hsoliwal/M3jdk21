from pathlib import Path
import runpy

SCRIPT = Path(__file__).resolve().parent / "check-synexia-rapidex-gap.py"

def test_rapidex_remains_fail_closed():
    result = runpy.run_path(str(SCRIPT), run_name="__main__")
    assert result["EXPECTED"]["status"] == "NO_CANONICAL_OWNER"
