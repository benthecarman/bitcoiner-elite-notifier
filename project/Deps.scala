import sbt._

object Deps {

  lazy val arch: String = System.getProperty("os.arch")

  lazy val osName: String = System.getProperty("os.name") match {
    case n if n.startsWith("Linux") => "linux"
    case n if n.startsWith("Mac") =>
      if (arch == "aarch64") {
        //needed to accommodate the different chip
        //arch for M1. see: https://github.com/bitcoin-s/bitcoin-s/pull/3041
        s"mac-$arch"
      } else {
        "mac"
      }
    case n if n.startsWith("Windows") => "win"
    case x =>
      throw new Exception(s"Unknown platform $x!")
  }

  object V {
    val akkaV = "10.2.7"
    val akkaStreamV = "2.6.18"
    val akkaActorV: String = akkaStreamV

    val telegramV = "5.2.0"

    val okHttpV = "3.4.1"

    val scalaFxV = "16.0.0-R25"
    val javaFxV = "17-ea+8"

    val bitcoinsV = "1.8.0-164-d213e993-SNAPSHOT"

    val scoptV = "4.0.1"

    val sttpV = "1.7.2"

    val codehausV = "3.1.6"

    val microPickleV = "1.4.4"

    val grizzledSlf4jV = "1.3.4"
  }

  object Compile {

    val okHttp =
      "com.softwaremill.sttp.client3" %% "okhttp-backend" % V.okHttpV

    val akkaHttp =
      "com.typesafe.akka" %% "akka-http" % V.akkaV withSources () withJavadoc ()

    val akkaStream =
      "com.typesafe.akka" %% "akka-stream" % V.akkaStreamV withSources () withJavadoc ()

    val akkaActor =
      "com.typesafe.akka" %% "akka-actor" % V.akkaStreamV withSources () withJavadoc ()

    val akkaSlf4j =
      "com.typesafe.akka" %% "akka-slf4j" % V.akkaStreamV withSources () withJavadoc ()

    val grizzledSlf4j =
      "org.clapper" %% "grizzled-slf4j" % V.grizzledSlf4jV withSources () withJavadoc ()

    val bitcoinsLnd =
      "org.bitcoin-s" %% "bitcoin-s-lnd-rpc" % V.bitcoinsV withSources () withJavadoc ()

    val bitcoinsCLightning =
      "org.bitcoin-s" %% "bitcoin-s-clightning-rpc" % V.bitcoinsV withSources () withJavadoc ()

    val bitcoinsAppCommons =
      "org.bitcoin-s" %% "bitcoin-s-app-commons" % V.bitcoinsV withSources () withJavadoc ()

    val bitcoinsDbCommons =
      "org.bitcoin-s" %% "bitcoin-s-db-commons" % V.bitcoinsV withSources () withJavadoc ()

    val telegram =
      "com.bot4s" %% "telegram-akka" % V.telegramV
  }

  val base = List(Compile.bitcoinsLnd,
                  Compile.telegram,
                  Compile.okHttp,
                  Compile.akkaActor,
                  Compile.akkaHttp,
                  Compile.akkaStream,
                  Compile.akkaSlf4j,
                  Compile.grizzledSlf4j)
}
