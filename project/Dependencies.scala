import sbt.*

object Dependencies {

  private final val Versions =
    new {
      val awsSdkS3 = "2.24.13"

      val bcrypt = "4.3.0"

      val chimney = "1.1.0"

      val flyway = "9.22.3" // Cannot use version 10.x.x because it will require Java 17

      val idGenerator = "1.4.0"

      val jansi = "2.4.1"

      val javaJwt = "4.4.0"

      val jsonPath = "2.9.0"

      val logback = "1.3.7"

      val logstashEncoder = "7.4"

      val mockitoScala = "1.17.31"

      val mysqlDriver = "8.2.0"

      val nwFinatraLib = "1.51.0"

      val nwFinatraKeycloakLib = "1.13.0"

      val nwFinatraRabbitMqLib = "1.10.0"

      val pprint = "0.9.0"

      val pureConfig = "0.17.7"

      val quickLens = "1.9.7"

      val quill = "4.8.4"

      val rabbitMq = "5.21.0"

      val scalaTest = "3.2.19"

      val twitterLibs = "23.11.0"

      val wireMock = "3.0.1"
    }

  final val All = Seq(
    // project-specific
    "com.mysql"           % "mysql-connector-j"       % Versions.mysqlDriver,
    "com.neurowyzr"      %% "nw-finatra-rabbitmq-lib" % Versions.nwFinatraRabbitMqLib,
    "com.rabbitmq"        % "amqp-client"             % Versions.rabbitMq,
    "io.getquill"        %% "quill-jdbc"              % Versions.quill,
    "com.github.t3hnar"  %% "scala-bcrypt"            % Versions.bcrypt,
    "com.auth0"           % "java-jwt"                % Versions.javaJwt,
    "com.jayway.jsonpath" % "json-path"               % Versions.jsonPath,
    ("org.flywaydb" % "flyway-core"  % Versions.flyway).exclude("com.fasterxml.jackson.core", "jackson-databind"),
    ("org.flywaydb" % "flyway-mysql" % Versions.flyway).exclude("com.fasterxml.jackson.core", "jackson-databind"),
    ("software.amazon.awssdk" % "s3" % Versions.awsSdkS3)
      .exclude("software.amazon.awssdk", "netty-nio-client")
      .exclude("software.amazon.awssdk", "url-connection-client"),
    "com.zaxxer"           %  "HikariCP"            % "5.0.1",

    // boilerplate
    "com.neurowyzr"              %% "nw-finatra-keycloak-lib"        % Versions.nwFinatraKeycloakLib,
    "com.neurowyzr"              %% "nw-finatra-lib"                 % Versions.nwFinatraLib,
    "com.twitter"                %% "finagle-stats"                  % Versions.twitterLibs,
    "com.twitter"                %% "finatra-jackson"                % Versions.twitterLibs,
    "com.twitter"                %% "finatra-http-client"            % Versions.twitterLibs,
    "com.twitter"                %% "finatra-http-server"            % Versions.twitterLibs,
    "com.twitter"                %% "util-core"                      % Versions.twitterLibs,
    "com.twitter"                %% "util-jackson"                   % Versions.twitterLibs,
    "com.twitter"                %% "util-slf4j-api"                 % Versions.twitterLibs,
    "com.twitter"                %% "twitter-server-logback-classic" % Versions.twitterLibs,
    "com.github.pureconfig"      %% "pureconfig"                     % Versions.pureConfig,
    "com.softwaremill.quicklens" %% "quicklens"                      % Versions.quickLens,
    "io.scalaland"               %% "chimney"                        % Versions.chimney,

    // logging
    "com.lihaoyi"         %% "pprint"                   % Versions.pprint,
    "ch.qos.logback"       % "logback-classic"          % Versions.logback,
    "net.logstash.logback" % "logstash-logback-encoder" % Versions.logstashEncoder,
    "org.fusesource.jansi" % "jansi"                    % Versions.jansi,

    // testing
    ("com.neurowyzr"          %% "nw-finatra-lib"          % Versions.nwFinatraLib % "test,it").classifier("tests"),
    "com.twitter"             %% "finatra-jackson"         % Versions.twitterLibs  % "test,it",
    ("com.twitter"            %% "finatra-http-client"     % Versions.twitterLibs  % "test,it").classifier("tests"),
    ("com.twitter"            %% "finatra-http-server"     % Versions.twitterLibs  % "test,it").classifier("tests"),
    ("com.twitter"            %% "inject-app"              % Versions.twitterLibs  % "test,it").classifier("tests"),
    ("com.twitter"            %% "inject-core"             % Versions.twitterLibs  % "test,it").classifier("tests"),
    ("com.twitter"            %% "inject-server"           % Versions.twitterLibs  % "test,it").classifier("tests"),
    ("com.twitter"            %% "inject-modules"          % Versions.twitterLibs  % "test,it").classifier("tests"),
    "com.twitter"             %% "util-mock"               % Versions.twitterLibs  % "test,it",
    "org.scalatest"           %% "scalatest"               % Versions.scalaTest    % "test,it",
    "org.mockito"             %% "mockito-scala"           % Versions.mockitoScala % Test,
    "org.mockito"             %% "mockito-scala-scalatest" % Versions.mockitoScala % Test,
    "com.softwaremill.common" %% "id-generator"            % Versions.idGenerator  % Test,
    "com.github.tomakehurst"   % "wiremock"                % Versions.wireMock     % "test,it"
  )

}
