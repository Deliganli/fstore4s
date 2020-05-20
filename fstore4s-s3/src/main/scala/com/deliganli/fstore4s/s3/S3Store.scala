package com.deliganli.fstore4s.s3

import cats.data.Kleisli
import cats.effect.{Concurrent, Sync, Timer}
import cats.implicits._
import com.deliganli.fstore4s.core.FileStore.FileCodec
import com.deliganli.fstore4s.core.{CoreFileStore, FileStore}
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner

class S3Store[F[_]: Concurrent: Timer, T: FileCodec](val core: CoreFileStore[F, T]) extends FileStore[F, T]

object S3Store {

  def create[F[_]: Concurrent: Timer, T: FileCodec]: Kleisli[F, (S3Config, AwsCredentialsProvider), FileStore[F, T]] =
    Kleisli(Function.tupled(create[F, T]))

  def create[F[_]: Concurrent: Timer, T: FileCodec](
    config: S3Config,
    credentialsProvider: AwsCredentialsProvider
  ): F[FileStore[F, T]] =
    for {
      storage <- Sync[F].delay(
        S3Client
          .builder()
          .credentialsProvider(credentialsProvider)
          .region(config.region)
          .build()
      )
      signer <- Sync[F].delay(
        S3Presigner
          .builder()
          .credentialsProvider(credentialsProvider)
          .region(config.region)
          .build()
      )
    } yield {
      val core = new S3Core[F, T](config, storage, signer)
      new S3Store[F, T](core)
    }
}
