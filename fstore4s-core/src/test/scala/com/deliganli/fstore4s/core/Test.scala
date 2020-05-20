package com.deliganli.fstore4s.core

import java.net.URI

import cats.effect.{IO, Resource}
import fs2.Stream

import scala.io.Source

object Test {

  def download(uri: URI) =
    Resource
      .make(IO(Source.fromURL(uri.toURL)))(s => IO(s.close()))
      .map(_.getLines().mkString("\n").trim)

  def loadResource(name: String) =
    Resource
      .make(IO(Source.fromResource(name)))(s => IO(s.close()))
      .map(_.getLines.mkString("\n"))
}
