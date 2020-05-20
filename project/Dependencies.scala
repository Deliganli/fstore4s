import sbt._

object Dependencies {

  object Versions {
    val scalatest = "3.1.0"
    val cats      = "2.0.0"
    val osLib     = "0.3.0"
    val fs2       = "2.1.0"
    val mockito   = "1.5.17"
    val awsS3     = "2.13.10"
    val gcStorage = "1.107.0"
  }

  val fs2 = Seq(
    "co.fs2" %% "fs2-core",
    "co.fs2" %% "fs2-io"
  ).map(_ % Versions.fs2)

  val scalatest = Seq(
    "org.scalactic" %% "scalactic" % Versions.scalatest,
    "org.scalatest" %% "scalatest" % Versions.scalatest % Test
  )

  val mockito = Seq(
    "org.mockito" %% "mockito-scala",
    "org.mockito" %% "mockito-scala-cats",
    "org.mockito" %% "mockito-scala-scalatest"
  ).map(_ % Versions.mockito % Test)

  val cats = Seq(
    "org.typelevel" %% "cats-core",
    "org.typelevel" %% "cats-effect"
  ).map(_ % Versions.cats)

  val gcStorage = "com.google.cloud" % "google-cloud-storage" % Versions.gcStorage

  val awsS3 = "software.amazon.awssdk" % "s3" % Versions.awsS3

  val osLib = "com.lihaoyi" %% "os-lib" % Versions.osLib
}
