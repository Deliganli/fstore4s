package com.deliganli.fstore4s.s3

import java.io.InputStream
import java.net.{URI, URLEncoder}
import java.nio.charset.StandardCharsets
import java.time.Duration

import cats.Applicative
import cats.effect.concurrent.Ref
import cats.effect.{Concurrent, Sync, Timer}
import cats.implicits._
import com.deliganli.fstore4s.core.CoreFileStore
import com.deliganli.fstore4s.core.FileStore.FileCodec
import com.deliganli.fstore4s.core.JDKCollectionConvertersCompat.Converters._
import fs2.{Chunk, Pipe, Stream}
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model._
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest

class S3Core[F[_]: Concurrent: Timer, T: FileCodec](
  config: S3Config,
  storage: S3Client,
  signer: S3Presigner)
    extends CoreFileStore[F, T] {

  override def uri(address: CoreFileStore.Address[T]): F[URI] = {
    val request = GetObjectPresignRequest
      .builder()
      .getObjectRequest(S3Requests.getObject(config.bucket, FileCodec[T].encode(address)))
      .signatureDuration(Duration.ofNanos(config.linkExpiration.toNanos))
      .build

    Sync[F]
      .delay(signer.presignGetObject(request))
      .map(r => Option(r).collect { case r if r.isBrowserExecutable => r.url().toURI })
      .flatMap(o => Sync[F].fromOption(o, new RuntimeException("This URL is not browser executable")))
  }

  override def delete(address: CoreFileStore.Address[T]): F[Boolean] = {
    val request = DeleteObjectRequest
      .builder()
      .bucket(config.bucket)
      .key(FileCodec[T].encode(address))
      .build

    def unsafeDelete = {
      storage
        .deleteObject(request)
        .deleteMarker()
    }

    Sync[F].delay(unsafeDelete)
  }

  override def read(address: CoreFileStore.Address[T]): F[Option[InputStream]] = {
    def unsafeRead = {
      storage
        .getObjectAsBytes(S3Requests.getObject(config.bucket, FileCodec[T].encode(address)))
        .asInputStream()
    }

    Sync[F]
      .delay(unsafeRead)
      .attempt
      .map(_.toOption)
  }

  override def write(address: CoreFileStore.Address[T]): Pipe[F, Byte, Unit] = { content =>
    def handlePart(
      createResponse: CreateMultipartUploadResponse,
      completedParts: Ref[F, Vector[CompletedPart]],
      chunk: Chunk[Byte]
    )(
      count: Int
    ): F[UploadPartResponse] = {
      val uploadPartRequest =
        S3Requests.uploadPart(config.bucket, FileCodec[T].encode(address), createResponse.uploadId(), count)

      Sync[F]
        .delay(storage.uploadPart(uploadPartRequest, RequestBody.fromBytes(chunk.toArray)))
        .flatTap(response => completedParts.update(_ :+ S3Requests.completedPart(count, response.eTag())))
    }

    Stream
      .eval(Applicative[F].tuple2(Ref.of(Vector.empty[CompletedPart]), Ref.of(1)))
      .flatMap {
        case (completedParts, partNumbers) =>
          Stream
            .resource(S3Multipart.multipart(config, storage)(FileCodec[T].encode(address), completedParts))
            .flatMap { createResponse =>
              content
                .groupWithin(config.bufferSize, config.bufferTimeLimit)
                .evalMap { chunk =>
                  partNumbers
                    .modify(c => (c + 1, c))
                    .flatMap(handlePart(createResponse, completedParts, chunk))
                }
            }
      }
      .void
  }

  override def list(prefix: String): F[Iterator[String]] = {
    val request = ListObjectsV2Request
      .builder()
      .bucket(config.bucket)
      .prefix(prefix)
      .build()

    Sync[F].delay {
      storage
        .listObjectsV2Paginator(request)
        .contents()
        .iterator()
        .asScala
        .map(_.key())
    }
  }

  override def copy(from: CoreFileStore.Address[T], to: CoreFileStore.Address[T]): F[Unit] = {
    val source = URLEncoder.encode(config.bucket + "/" + FileCodec[T].encode(from), StandardCharsets.UTF_8.toString)

    val request = CopyObjectRequest
      .builder()
      .copySource(source)
      .destinationKey(FileCodec[T].encode(to))
      .destinationBucket(config.bucket)
      .build()

    def unsafeCopy = storage.copyObject(request)

    Sync[F].delay(unsafeCopy)
  }
}
