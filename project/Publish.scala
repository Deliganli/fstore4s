import sbt.Keys._
import sbt._
import xerial.sbt.Sonatype.autoImport._

object Publish {

  val deliganli = Developer(
    "deliganli",
    "Sait Sami Kocatas",
    "saitkocatas@gmail.com",
    url("https://github.com/deliganli")
  )

  def scm(repo:String) = ScmInfo(
    url(s"https://github.com/deliganli/$repo"),
    s"git@github.com:Deliganli/$repo.git"
  )

  val apacheLicense = (
    "Apache-2.0",
    url("http://www.apache.org/licenses/LICENSE-2.0")
  )

  def settings(repo:String) = Seq(
    organization := "com.deliganli",
    homepage := Some(url(s"https://github.com/deliganli/$repo")),
    scmInfo := Some(scm(repo)),
    developers := List(deliganli),
    licenses += apacheLicense,
    publishMavenStyle := true,
    credentials += Credentials(Path.userHome / ".sbt" / "sonatype.credential"),
    publishTo := sonatypePublishToBundle.value
  )
}
