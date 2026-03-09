#!/bin/bash
set -euo pipefail

# Check a Gradle HTML test report for IndexOutOfBoundsException.
# Usage:
#   test_perf.sh [PATH_TO_REPORT]
# If PATH_TO_REPORT is not provided, a default Android unit test report path is used.

report_file="${1:-data/build/reports/tests/testDebugUnitTest/index.html}"
if [[ ! -r "$report_file" ]]; then
  echo "Report file not found or not readable: $report_file" >&2
  exit 1
fi

# Fail if IndexOutOfBoundsException is present in the report
if grep -qi "IndexOutOfBoundsException" "$report_file"; then
  echo "IndexOutOfBoundsException found in test report: $report_file" >&2
  exit 1
fi

exit 0
