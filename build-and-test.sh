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
./mvnw versions:update-properties -DallowSnapshots=true
(./mvnw se.fk.codestandard:fk-code-standard-maven-plugin:spotlessCheck && exit 1) || echo "Should fail invalid format"
(./mvnw package -Pskip-automatic-fk-code-standard-apply=true && exit 1) || echo "Should fail invalid format"
./mvnw verify
./mvnw se.fk.codestandard:fk-code-standard-maven-plugin:spotlessApply
./mvnw se.fk.codestandard:fk-code-standard-maven-plugin:spotlessCheck
