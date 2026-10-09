from pathlib import Path
import runpy

SCRIPT = Path(__file__).resolve().parent / "check-synexia-proper-noun-donor-pending.py"

def test_pending_proper_name_donor_is_not_a_receiver():
    result = runpy.run_path(str(SCRIPT), run_name="__main__")
    assert result["RECEIPT"].name.endswith("receipt.tsv")
