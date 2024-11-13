#!/bin/bash

sbt clean scalafmtCheckAll smoke:test coverage it:test test coverageReport
