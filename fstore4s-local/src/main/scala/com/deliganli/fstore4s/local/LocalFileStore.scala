package com.deliganli.fstore4s.local

import cats.data.Kleisli
import cats.effect.{Concurrent, ContextShift, Sync}
import com.deliganli.fstore4s.core.FileStore.FileCodec
import com.deliganli.fstore4s.core.{CoreFileStore, FileStore}

class LocalFileStore[F[_]: Concurrent: ContextShift, T: FileCodec](val core: CoreFileStore[F, T])
    extends FileStore[F, T]

object LocalFileStore {

  def create[F[_]: Concurrent: ContextShift, T: FileCodec]: Kleisli[F, LocalFileStoreConfig, FileStore[F, T]] =
    Kleisli(create(_))

  def create[F[_]: Concurrent: ContextShift, T: FileCodec](config: LocalFileStoreConfig): F[FileStore[F, T]] =
    Sync[F].pure {
      val core = new LocalFileStoreCore[F, T](config)
      new LocalFileStore[F, T](core)
    }
}
