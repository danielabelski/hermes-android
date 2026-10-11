"""Strengthen the existing workflow contracts against silently narrowed suites."""

import re
import unittest

from tools.tests.test_workflow_contracts import ci_job_blocks, read_workflow


def assert_unfiltered_command(test: unittest.TestCase, block: str, key: str, task: str) -> None:
    commands = re.findall(rf"^\s+{key}:\s*(.+)$", block, re.MULTILINE)
    gradle_commands = [command.strip() for command in commands if "./gradlew" in command]
    test.assertEqual(gradle_commands, [f"./gradlew --no-daemon {task}"])
    test.assertNotRegex(block, r"testInstrumentationRunnerArguments|--tests\b|excludeTests|filterTests")


class PrRegressionWorkflowTests(unittest.TestCase):
    def test_ci_unit_suite_cannot_be_narrowed(self) -> None:
        block = ci_job_blocks(read_workflow("0-ci-build-and-test.yml"))["unit-tests"]
        assert_unfiltered_command(self, block, "run", "testDebugUnitTest")

    def test_ci_emulator_suite_cannot_be_narrowed(self) -> None:
        block = ci_job_blocks(read_workflow("0-ci-build-and-test.yml"))["android-ui-smoke"]
        assert_unfiltered_command(self, block, "script", "connectedDebugAndroidTest")
        self.assertRegex(block, r"api-level:\s*\[35, 36\]")
        self.assertIn("api-level: ${{ matrix.api-level }}", block)
        changes = ci_job_blocks(read_workflow("0-ci-build-and-test.yml"))["changes"]
        self.assertIn("'app/src/**'", changes)

    def test_release_keeps_full_api36_suite(self) -> None:
        workflow = read_workflow("1-orchestration-release.yml")
        assert_unfiltered_command(self, workflow, "script", "connectedDebugAndroidTest")
        self.assertRegex(workflow, re.compile(r"^\s+api-level: 36\s*$", re.MULTILINE))

    def test_contract_rejects_filters_and_skipped_instrumentation(self) -> None:
        for narrowed in (
            "./gradlew --no-daemon testDebugUnitTest --tests '*Voice*'",
            "./gradlew --no-daemon testDebugUnitTest -x testDebugUnitTest",
            "./gradlew --no-daemon connectedDebugAndroidTest "
            "-Pandroid.testInstrumentationRunnerArguments.class=SelectedServerSettingsInstrumentationTest",
            "./gradlew --no-daemon connectedDebugAndroidTest "
            "-Pandroid.testInstrumentationRunnerArguments.annotation=androidx.test.filters.SmallTest",
        ):
            task = "connectedDebugAndroidTest" if "connectedDebugAndroidTest" in narrowed else "testDebugUnitTest"
            with self.subTest(command=narrowed), self.assertRaises(AssertionError):
                assert_unfiltered_command(self, f"        script: {narrowed}", "script", task)


if __name__ == "__main__":
    unittest.main()
