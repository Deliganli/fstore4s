package com.deliganli.fstore4s.core

import java.io.InputStream
import java.net.URI

import cats.Show
import cats.implicits._
import CoreFileStore.Address
import fs2.Pipe

trait CoreFileStore[F[_], T] {
  def uri(address: Address[T]): F[URI]
  def read(address: Address[T]): F[Option[InputStream]]
  def write(address: Address[T]): Pipe[F, Byte, Unit]
  def list(prefix: String): F[Iterator[String]]
  def copy(from: Address[T], to: Address[T]): F[Unit]
  def delete(address: Address[T]): F[Boolean]
}

object CoreFileStore {
  case class Address[T](prefix: String, file: T)

  object Address {
    implicit def show[T: Show]: Show[Address[T]] = Show.show(v => s"${v.prefix}/${v.file.show}")
  }
}
