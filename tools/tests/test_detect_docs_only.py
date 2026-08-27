import io
import sys
import unittest
from contextlib import redirect_stdout
from pathlib import Path
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools"))

from detect_docs_only import main  # noqa: E402


class DocsOnlyDetectorTests(unittest.TestCase):
    def _run(self, paths: str, **arguments: str) -> str:
        completed = type("Completed", (), {"returncode": 0, "stdout": paths})()
        output = io.StringIO()
        with patch("detect_docs_only.subprocess.run", return_value=completed), redirect_stdout(output):
            self.assertEqual(
                main(sum(([f"--{key}", value] for key, value in arguments.items()), [])),
                0,
            )
        return output.getvalue().strip()

    def test_markdown_paths_are_docs_only(self) -> None:
        self.assertEqual(self._run("README.md\nUPPER.MD\n", base="base", head="head"), "true")

    def test_mixed_paths_require_full_suite(self) -> None:
        self.assertEqual(self._run("README.md\napp/src/MainActivity.kt\n", base="base", head="head"), "false")

    def test_empty_diff_requires_full_suite(self) -> None:
        self.assertEqual(self._run("", base="base", head="head"), "false")

    def test_missing_revision_range_requires_full_suite(self) -> None:
        output = io.StringIO()
        with redirect_stdout(output):
            self.assertEqual(main([]), 0)
        self.assertEqual(output.getvalue().strip(), "false")

    def test_invalid_diff_requires_full_suite(self) -> None:
        completed = type("Completed", (), {"returncode": 128, "stdout": ""})()
        output = io.StringIO()
        with patch("detect_docs_only.subprocess.run", return_value=completed), redirect_stdout(output):
            self.assertEqual(main(["--base", "base", "--head", "head"]), 0)
        self.assertEqual(output.getvalue().strip(), "false")

    def test_unavailable_git_requires_full_suite(self) -> None:
        output = io.StringIO()
        with patch("detect_docs_only.subprocess.run", side_effect=OSError), redirect_stdout(output):
            self.assertEqual(main(["--base", "base", "--head", "head"]), 0)
        self.assertEqual(output.getvalue().strip(), "false")

    def test_push_event_revision_fallback_is_supported(self) -> None:
        self.assertEqual(self._run("README.md\n", before="before", sha="head"), "true")

    def test_all_zero_revision_requires_full_suite(self) -> None:
        zero = "0" * 40
        self.assertEqual(self._run("README.md\n", base=zero, head="head"), "false")

    def test_diff_disables_rename_detection(self) -> None:
        completed = type("Completed", (), {"returncode": 0, "stdout": "README.md\n"})()
        output = io.StringIO()
        with patch("detect_docs_only.subprocess.run", return_value=completed) as run, redirect_stdout(output):
            self.assertEqual(main(["--base", "base", "--head", "head"]), 0)
        self.assertEqual(
            run.call_args.args[0],
            ["git", "diff", "--name-only", "--no-renames", "base...head"],
        )


if __name__ == "__main__":
    unittest.main()