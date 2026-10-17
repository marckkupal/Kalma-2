#!/bin/sh
# Opens every page for Student, Admin and Guidance and prints OK / FAIL per page.
cd "$(dirname "$0")" && java -cp out SmokeTest
