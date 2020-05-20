package com.deliganli.fstore4s.core

import java.nio.file.Paths

import cats.Show
import cats.implicits._
import com.deliganli.fstore4s.core.CoreFileStore.Address
import com.deliganli.fstore4s.core.FileStore.FileCodec
import com.deliganli.fstore4s.core.FileStore.FileCodec.CodecError

case class SimpleDomainFile(version: String, category: String, name: String)

object SimpleDomainFile {
  val fileRegex = """^(.*)_(.*)_(.*)""".r

  implicit val show: Show[SimpleDomainFile] = Show.show(v => Array(v.version, v.category, v.name).mkString("_"))

  implicit val codec: FileCodec[SimpleDomainFile] = new FileCodec[SimpleDomainFile] {

    override def encode(data: Address[SimpleDomainFile]): String = {
      val filename = Array(data.file.version, data.file.category, data.file.name).mkString("_")

      Some(data.prefix)
        .filter(_.trim.nonEmpty)
        .map(_ + "/" + filename)
        .getOrElse(filename)
    }

    override def decode(key: String): Either[CodecError, Address[SimpleDomainFile]] = {
      val nioPath = Paths.get(key)

      fileRegex
        .findFirstMatchIn(nioPath.getFileName.toString)
        .map(m => SimpleDomainFile(m.group(1), m.group(2), m.group(3)))
        .toRight(new CodecError(key))
        .tupleLeft(Option(nioPath.getParent).fold("")(_.toString))
        .map(Function.tupled(Address[SimpleDomainFile]))
    }
  }
}
