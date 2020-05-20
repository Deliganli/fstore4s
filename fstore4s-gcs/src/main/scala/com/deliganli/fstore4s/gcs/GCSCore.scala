package com.deliganli.fstore4s.gcs

import java.io.InputStream
import java.net.{URI, URL}
import java.nio.channels.Channels

import cats.data.OptionT
import cats.effect.{Blocker, ContextShift, Sync}
import cats.implicits._
import com.deliganli.fstore4s.core.FileStore.FileCodec
import com.deliganli.fstore4s.core.CoreFileStore
import com.google.cloud.storage.Storage.{BlobListOption, CopyRequest}
import com.google.cloud.storage.{BlobInfo, Storage}
import fs2.{Pipe, Stream}

import com.deliganli.fstore4s.core.JDKCollectionConvertersCompat.Converters._

class GCSCore[F[_]: Sync: ContextShift, T: FileCodec](config: GCSConfig, storage: Storage) extends CoreFileStore[F, T] {

  override def copy(from: CoreFileStore.Address[T], to: CoreFileStore.Address[T]): F[Unit] = {
    val request = CopyRequest.of(config.bucket, FileCodec[T].encode(from), FileCodec[T].encode(to))
    Sync[F].delay(storage.copy(request).getResult)
  }

  override def delete(address: CoreFileStore.Address[T]): F[Boolean] = {
    Sync[F].delay(storage.delete(config.bucket, FileCodec[T].encode(address)))
  }

  override def read(address: CoreFileStore.Address[T]): F[Option[InputStream]] = {
    val file = FileCodec[T].encode(address)

    OptionT(Sync[F].delay(Option(storage.get(config.bucket, file))))
      .semiflatMap(blob => Sync[F].delay(Channels.newInputStream(blob.reader())))
      .value
  }

  override def write(address: CoreFileStore.Address[T]): Pipe[F, Byte, Unit] = { content =>
    val blobInfo = BlobInfo
      .newBuilder(config.bucket, FileCodec[T].encode(address))
      .build

    def outputStream = {
      Sync[F].delay {
        val writer = storage.writer(blobInfo)

        Channels.newOutputStream(writer)
      }
    }

    Stream
      .resource(Blocker[F])
      .flatMap(blocker => content.through(fs2.io.writeOutputStream(outputStream, blocker)))
  }

  override def list(prefix: String): F[Iterator[String]] =
    Sync[F].delay {
      storage
        .list(config.bucket, BlobListOption.prefix(prefix))
        .iterateAll()
        .iterator()
        .asScala
        .map(_.getName)
    }

  override def uri(address: CoreFileStore.Address[T]): F[URI] = {
    def unsafeSign: URL =
      storage.signUrl(
        BlobInfo.newBuilder(config.bucket, FileCodec[T].encode(address)).build,
        config.linkExpiration.length,
        config.linkExpiration.unit
      )

    Sync[F].delay(unsafeSign).map(_.toURI)
  }
}
