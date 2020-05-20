package com.deliganli.fstore4s.local

import java.io.InputStream
import java.net.URI

import cats.data.OptionT
import cats.effect.{Blocker, ContextShift, Sync}
import cats.implicits._
import com.deliganli.fstore4s.core.CoreFileStore
import com.deliganli.fstore4s.core.FileStore.FileCodec
import com.deliganli.fstore4s.local.LocalFileStoreCore._
import fs2.{Pipe, Stream}
import os.Path

class LocalFileStoreCore[F[_]: Sync: ContextShift, T: FileCodec](config: LocalFileStoreConfig)
    extends CoreFileStore[F, T] {

  override def uri(address: CoreFileStore.Address[T]): F[URI] = {
    Sync[F]
      .delay(find(address))
      .flatMap(r => Sync[F].fromOption(r, new RuntimeException(s"File not found at: $address")))
      .map(_.toNIO.toUri)
  }

  override def delete(address: CoreFileStore.Address[T]): F[Boolean] = {
    Sync[F]
      .delay(os.remove(path(config.bucket, address)))
      .flatMap(_ => Sync[F].delay(find(address).isEmpty))
  }

  override def read(address: CoreFileStore.Address[T]): F[Option[InputStream]] = {
    OptionT(Sync[F].delay(Some(path(config.bucket, address)).filter(_.toIO.exists())))
      .semiflatMap(path => Sync[F].delay(os.read.inputStream(path)))
      .value
  }

  override def write(address: CoreFileStore.Address[T]): Pipe[F, Byte, Unit] = { content =>
    val file = path(config.bucket, address)

    Stream
      .eval(Sync[F].delay(os.makeDir.all(file / os.up)))
      .flatMap(_ => Stream.resource(Blocker[F]))
      .flatMap(blocker => content.through(fs2.io.file.writeAll(file.toNIO, blocker)))
      .append(Stream.emit[F, Unit](()))
  }

  override def list(prefix: String): F[Iterator[String]] = {
    val bucketPath = toPath(config.bucket)

    Sync[F].delay(
      os.list(path(config.bucket, prefix))
        .map(_.relativeTo(bucketPath).toString())
        .iterator
    )
  }

  override def copy(from: CoreFileStore.Address[T], to: CoreFileStore.Address[T]): F[Unit] = {
    Sync[F].delay(os.copy(path(config.bucket, FileCodec[T].encode(from)), path(config.bucket, FileCodec[T].encode(to))))
  }

  private def find(address: CoreFileStore.Address[T]) = Some(path(config.bucket, address)).filter(_.toIO.exists())

  def path(bucket: String, address: CoreFileStore.Address[T]): Path = path(bucket, FileCodec[T].encode(address))
  def path(bucket: String, key: String): Path                       = toPath(bucket + "/" + key)
}

object LocalFileStoreCore {
  private val HomeRegex     = """^(?:~/)(\.?.*)""".r
  private val AbsoluteRegex = """^(?:(/|\w:\\).*)""".r

  def toPath(filePath: String): Path = {
    filePath match {
      case HomeRegex(paths) =>
        os.home / os.RelPath(paths)
      case paths @ AbsoluteRegex(_) =>
        os.Path(paths)
      case paths =>
        os.pwd / os.RelPath(paths)
    }
  }
}
