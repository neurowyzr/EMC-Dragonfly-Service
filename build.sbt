import java.util.Properties

import com.typesafe.sbt.SbtNativePackager.autoImport.maintainer
import sbt.{IO, Resolver}
import sbt.Keys.{homepage, versionScheme}
import sbtsonar.SonarPlugin.autoImport.sonarUseExternalConfig

lazy val ghOwner     = "neurowyzr"
lazy val ghMavenRepo = "maven.pkg.github.com"
lazy val ghToken     = sys.env("GITHUB_TOKEN")

credentials ++= Seq(
  Credentials("GitHub Package Registry", ghMavenRepo, ghOwner, ghToken)
)

val projectConf = settingKey[Properties]("The project properties")

projectConf := {
  val prop = new Properties()
  IO.load(prop, new File("src/main/resources/project.conf"))
  prop
}

lazy val localEnvFile = "local.env"

lazy val disabledPlugins = {
  if (new File(localEnvFile).exists()) {
    Seq.empty
  } else {
    Seq(SbtDotenv)
  }
}

ThisBuild / envFileName := localEnvFile

lazy val baseSettings = Seq(
  resolvers ++= Resolver.sonatypeOssRepos("releases"),
  resolvers += "GitHub Packages".at(s"https://$ghMavenRepo/$ghOwner/nw-finatra-lib")
)

lazy val buildSettings = Seq(
  scalaVersion  := "2.13.14",
  organization  := "com.neurowyzr",
  maintainer    := "tech@neurowyzr.com",
  name          := projectConf.value.getProperty("app.name"),
  version       := projectConf.value.getProperty("app.version"),
  homepage      := Some(url("https://neurowyzr.com")),
  versionScheme := Some("semver-spec")
)

lazy val compileSettings = Seq(
  Compile / mainClass := Some("com.neurowyzr.nw.dragon.service.WebService")
)

lazy val SmokeTest = config("smoke").extend(Test)

lazy val root = (project in file("."))
  .enablePlugins(JavaServerAppPackaging)
  .enablePlugins(DockerPlugin)
  .disablePlugins(disabledPlugins: _*)
  .settings(baseSettings)
  .settings(buildSettings)
  .settings(compileSettings)
  .configs(IntegrationTest)
  .configs(SmokeTest)
  .settings(
    libraryDependencies ++= Dependencies.All,
    dependencyOverrides ++= Seq(
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.15.3" // required for logback-encoder
    ),
    Defaults.itSettings,
    inConfig(SmokeTest)(Defaults.testSettings)
  )

val testBasePath = settingKey[File]("Test base directory")
testBasePath := baseDirectory.value / "src" / "test" / "scala"

val testResourcesPath = settingKey[File]("Test resources directory")
testResourcesPath := baseDirectory.value / "src" / "test" / "resources"

Test / sourceDirectories += testBasePath.value / "unit"
Test / scalaSource       := testBasePath.value / "unit"
Test / resourceDirectory := testResourcesPath.value
Test / resourceDirectories += testResourcesPath.value
Test / unmanagedSourceDirectories += testBasePath.value / "shared"
Test / fork := true

IntegrationTest / sourceDirectories += testBasePath.value / "integration"
IntegrationTest / scalaSource       := testBasePath.value / "integration"
IntegrationTest / resourceDirectory := testResourcesPath.value
IntegrationTest / resourceDirectories += testResourcesPath.value
IntegrationTest / unmanagedSourceDirectories += testBasePath.value / "shared"
IntegrationTest / fork := true

SmokeTest / sourceDirectories += testBasePath.value / "smoke"
SmokeTest / scalaSource       := testBasePath.value / "smoke"
SmokeTest / resourceDirectory := testResourcesPath.value
SmokeTest / resourceDirectories += testResourcesPath.value
SmokeTest / unmanagedSourceDirectories += testBasePath.value / "shared"
SmokeTest / fork := true

scalacOptions ++= CompilerOptions.All

Compile / compile / wartremoverErrors ++= Seq(
  Wart.TripleQuestionMark,
  Wart.Var,
  Wart.Null,
  Wart.Return,
  Wart.Throw,
  Wart.IterableOps,
  Wart.Any,
  Wart.IsInstanceOf,
  Wart.AsInstanceOf,
  Wart.EitherProjectionPartial,
  Wart.DefaultArguments,
  Wart.Product,
  Wart.Serializable,
  Wart.StringPlusAny
)

ThisBuild / scapegoatVersion := "2.1.6"
scapegoatReports             := Seq("all")
Scapegoat / scalacOptions += "-P:scapegoat:overrideLevels:all=Warning"

sonarUseExternalConfig := true
