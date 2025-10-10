#!/bin/bash
set -ex
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

#
# Build the project
#
cd $SCRIPT_DIR
#./mvnw clean
./mvnw install


#
# Test Maven plugin
#
cd $SCRIPT_DIR/fk-code-standard-maven-plugin-example
./mvnw -P run-tests-only versions:update-properties -DallowSnapshots=true
(./mvnw -P run-tests-only se.fk.codestandard:fk-code-standard-maven-plugin:spotlessCheck && exit 1) || echo "Should fail invalid format"
(./mvnw -P run-tests-only package -Pskip-automatic-fk-code-standard-apply=true && exit 1) || echo "Should fail invalid format"
./mvnw -P run-tests-only verify
./mvnw -P run-tests-only se.fk.codestandard:fk-code-standard-maven-plugin:spotlessApply
./mvnw -P run-tests-only se.fk.codestandard:fk-code-standard-maven-plugin:spotlessCheck
