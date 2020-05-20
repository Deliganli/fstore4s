package com.deliganli.fstore4s.s3

import cats.effect.concurrent.Ref
import cats.effect.{Resource, Sync}
import cats.implicits._
import com.deliganli.fstore4s.core.JDKCollectionConvertersCompat.Converters._
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model._

import scala.util.control.NonFatal

object S3Multipart {

  def multipart[F[_]: Sync](
    config: S3Config,
    storage: S3Client
  )(
    key: String,
    parts: Ref[F, Vector[CompletedPart]]
  ): Resource[F, CreateMultipartUploadResponse] = {
    Resource
      .make {
        val createRequest = CreateMultipartUploadRequest
          .builder()
          .bucket(config.bucket)
          .key(key)
          .build()

        Sync[F].delay(storage.createMultipartUpload(createRequest))
      } { createResponse =>
        parts.get
          .map { parts =>
            CompletedMultipartUpload
              .builder()
              .parts(parts.asJava)
              .build()
          }
          .map { completedRequest =>
            CompleteMultipartUploadRequest
              .builder()
              .bucket(config.bucket)
              .key(key)
              .uploadId(createResponse.uploadId())
              .multipartUpload(completedRequest)
              .build()
          }
          .flatMap(request => Sync[F].delay(storage.completeMultipartUpload(request)))
          .onError {
            case NonFatal(err) =>
              val abortRequest = AbortMultipartUploadRequest
                .builder()
                .bucket(config.bucket)
                .key(key)
                .uploadId(createResponse.uploadId())
                .build()

              Sync[F].delay(storage.abortMultipartUpload(abortRequest))
          }
          .void

      }
  }
}
