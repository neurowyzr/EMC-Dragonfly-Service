bashScriptExtraDefines ++= IO.readLines(baseDirectory.value / "cicd" / "scripts" / "startup.sh")

Docker / organizationName := organization.value
Docker / version          := version.value

dockerBaseImage        := "public.ecr.aws/amazoncorretto/amazoncorretto:11.0.23-al2023-headless"
dockerExposedPorts     := Seq(8888)
dockerExposedVolumes   := Seq("/opt/docker/logs")
Docker / daemonUser    := "daemon"
Docker / daemonUserUid := Some("1000")
