#!/bin/bash

set -e

sbt clean scalafmtCheckAll smoke:test coverage it:test test coverageReport scapegoat

rc=$?
[[ $rc != 0 ]] && exit $rc

sbt sonarScan || true

rm -rf "$HOME/.ivy2/local" || true
find $HOME/Library/Caches/Coursier/v1 -name "ivydata-*.properties" -delete || true
find $HOME/.ivy2/cache                -name "ivydata-*.properties" -delete || true
find $HOME/.cache/coursier/v1         -name "ivydata-*.properties" -delete || true
find $HOME/.sbt                       -name "ivydata-*.properties" -delete || true
