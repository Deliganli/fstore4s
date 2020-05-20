import Dependencies._

lazy val publishSettings = Publish.settings("fstore4s")

lazy val versionTag = "0.2.0" + "-" + "SNAPSHOT"

lazy val root = project
  .in(file("."))
  .aggregate(
    fstore,
    fstoreS3,
    fstoreGCS,
    fstoreLocal
  )
  .settings(
    Common.settings,
    publishSettings,
    name := "fstore4s",
    version := versionTag,
    skip in publish := true,
    crossScalaVersions := Nil
  )

lazy val fstore = sbt.Project
  .apply("fstore4s-core", file("fstore4s-core"))
  .settings(
    Common.settings,
    publishSettings,
    name := "fstore4s-core",
    version := versionTag,
    libraryDependencies ++= Seq() ++ fs2 ++ scalatest ++ mockito ++ cats
  )

lazy val fstoreS3 = module("fstore4s-s3")
  .dependsOn(fstore % "compile->compile;test->test")
  .settings(publishSettings, libraryDependencies ++= Seq(awsS3))

lazy val fstoreGCS = module("fstore4s-gcs")
  .dependsOn(fstore % "compile->compile;test->test")
  .settings(publishSettings, libraryDependencies ++= Seq(gcStorage))

lazy val fstoreLocal = module("fstore4s-local")
  .dependsOn(fstore % "compile->compile;test->test")
  .settings(publishSettings, libraryDependencies ++= Seq(osLib))

def module(projectName: String): Project = {
  sbt.Project
    .apply(projectName, file(projectName))
    .dependsOn(fstore % "compile->compile;test->test")
    .settings(
      Common.settings,
      name := projectName,
      version := versionTag
    )
}
