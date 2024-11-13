#!/bin/bash

if test -e "sonar-project.properties"; then
  echo "Adjusting content of sonar-project.properties for this PR."

  echo "sonar.host.url=http://localhost:9000" >>sonar-project.properties
  echo "sonar.login=admin" >>sonar-project.properties
  echo "sonar.password=admin" >>sonar-project.properties

  echo "sonar.scala.pullrequest.provider=github" >>sonar-project.properties
  echo "sonar.scala.pullrequest.number=$PR_NUMBER" >>sonar-project.properties
  echo "sonar.scala.pullrequest.github.repository=$GITHUB_REPO" >>sonar-project.properties
  echo "sonar.scala.pullrequest.github.oauth=$GITHUB_TOKEN" >>sonar-project.properties

  echo "sonar.scala.scalastyle.disable=true" >>sonar-project.properties
else
  echo "Missing sonar-project.properties in project root path."
fi
