package com.deliganli.fstore4s.gcs

import java.io.FileInputStream

import cats.data.Kleisli
import cats.effect.{ContextShift, Sync}
import cats.implicits._
import com.deliganli.fstore4s.core.FileStore.FileCodec
import com.deliganli.fstore4s.core.{CoreFileStore, FileStore}
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.StorageOptions
import com.google.common.collect.Lists

class GCSStore[F[_]: Sync: ContextShift, T: FileCodec](val core: CoreFileStore[F, T]) extends FileStore[F, T]

object GCSStore {

  def create[F[_]: Sync: ContextShift, T: FileCodec]: Kleisli[F, GCSConfig, FileStore[F, T]] =
    Kleisli(create(_))

  def create[F[_]: Sync: ContextShift, T: FileCodec](config: GCSConfig): F[FileStore[F, T]] =
    for {
      credentials <- Sync[F].delay {
        GoogleCredentials
          .fromStream(new FileInputStream(config.credFile.toString))
          .createScoped(Lists.newArrayList("https://www.googleapis.com/auth/cloud-platform"))
      }
      store <- Sync[F].delay(
        StorageOptions
          .newBuilder()
          .setCredentials(credentials)
          .build()
          .getService
      )
    } yield {
      val core = new GCSCore[F, T](config, store)
      new GCSStore[F, T](core)
    }
}
