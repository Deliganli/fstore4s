package com.deliganli.fstore4s.core

import java.io.InputStream
import java.net.URI

import cats.data.OptionT
import cats.effect.{Blocker, ContextShift, Sync}
import cats.implicits._
import com.deliganli.fstore4s.core.CoreFileStore.Address
import com.deliganli.fstore4s.core.FileStore.FileCodec.CodecError
import com.deliganli.fstore4s.core.FileStore.{ContentStream, FileCodec, Result}
import fs2.{Pipe, Stream}

trait FileStore[F[_], T] {

  /**
    * Contains core operation implementations for a given provider
    * such as write, delete, list etc..
    */
  def core: CoreFileStore[F, T]

  /**
    * @return a URI pointing to a file
    */
  def uri(
    address: Address[T]
  )(
    implicit F: Sync[F]
  ): F[Result[URI]] = {
    core.uri(address).attempt
  }

  /**
    * Similar to [[write]] but returns a suspended result rather than
    * stream
    *
    * @param content byte stream of the content that will be transferred
    */
  def put(
    address: Address[T],
    content: ContentStream[F]
  )(
    implicit F: Sync[F]
  ): F[Result[URI]] = {
    def emptyStreamError = Left(new RuntimeException("Empty stream, how?"))

    content
      .through(write(address))
      .compile
      .toVector
      .map(_.headOption.getOrElse(emptyStreamError))
  }

  /**
    * Writes the given bytes through the designated address
    *
    * {{{
    * Stream
    *   .resource(Blocker[IO])
    *   .flatMap(b => io.file.readAll[IO](filePath, b, 65536))
    *   .through(store.write(address1))
    * }}}
    *
    * @param address target file
    */
  def write(
    address: Address[T]
  )(
    implicit F: Sync[F]
  ): Pipe[F, Byte, Result[URI]] = { content =>
    content
      .through(core.write(address))
      .evalMap(_ => core.uri(address).attempt)
  }

  /**
    * Opens a read stream if file exists
    *
    *{{{
    * OptionT(store.read(address1))
    * .semiflatMap { stream =>
    *   stream
    *     .through(fs2.text.utf8Decode)
    *     .through(fs2.text.lines)
    *     .intersperse("\n")
    *     .map(do something with the lines)
    * }
    *}}}
    *
    * @param bufferSize size of the each chunk that will be buffered
    */
  def read(
    address: Address[T],
    bufferSize: Int = 64000
  )(
    implicit F: Sync[F],
    CS: ContextShift[F]
  ): F[Option[ContentStream[F]]] = {
    def contentStream(stream: F[InputStream]): ContentStream[F] =
      Stream
        .resource(Blocker[F])
        .flatMap(blocker => fs2.io.readInputStream[F](stream, bufferSize, blocker))

    OptionT(core.read(address))
      .map(is => contentStream(Sync[F].pure(is)))
      .value
  }

  /**
    * Similar to [[read]] but bundles bytes by line in the address
    *
    * {{{
    * store
    *   .content(csvFileAddress)
    *   .map(line => decode csv line)
    * }}}
    */
  def readLines(
    address: Address[T],
    bufferSize: Int = 64000
  )(
    implicit F: Sync[F],
    CS: ContextShift[F]
  ): F[Option[Stream[F, String]]] = {
    def transformLine(stream: ContentStream[F]): Stream[F, String] = {
      stream
        .through(fs2.text.utf8Decode)
        .through(fs2.text.lines)
    }

    OptionT(read(address, bufferSize))
      .map(transformLine)
      .value
  }

  /**
    * Similar to [[read]] but exhausts the stream and emits the
    * whole content of the file
    *
    * {{{
    * store
    *   .content(jsonFileAddress)
    *   .map(string => decode json)
    * }}}
    */
  def content(
    address: Address[T],
    bufferSize: Int = 64000
  )(
    implicit F: Sync[F],
    CS: ContextShift[F]
  ): F[Option[String]] = {
    def consume(stream: Stream[F, String]): F[String] = {
      stream
        .intersperse("\n")
        .compile
        .toVector
        .map(_.mkString("").trim)
    }

    OptionT(readLines(address, bufferSize))
      .semiflatMap(consume)
      .value
  }

  /**
    * Returns a stream emitting files with given prefix.
    *
    * Right(Address[T]) returned emitted if decoder succeeds
    * Left(rawKey) otherwise
    *
    * @param prefix directory or common name for list of files
    */
  def list(
    prefix: String
  )(
    implicit F: Sync[F],
    T: FileCodec[T]
  ): Stream[F, Result[Address[T]]] = {
    Stream
      .eval(core.list(prefix))
      .flatMap(i => Stream.fromIterator(i))
      .map(FileCodec[T].decode)
  }

  def copy(
    from: Address[T],
    to: Address[T]
  )(
    implicit F: Sync[F]
  ): F[Result[Unit]] = {
    core.copy(from, to).attempt
  }

  def move(
    from: Address[T],
    to: Address[T]
  )(
    implicit F: Sync[F]
  ): F[Result[Unit]] = {
    def leakError = new RuntimeException("Previous file couldn't delete, possible leak")

    (for {
      _ <- core.copy(from, to)
      _ <- core.delete(from).ensure(leakError)(p => p)
    } yield ()).attempt.map(_.void)
  }

  def delete(
    address: Address[T]
  )(
    implicit F: Sync[F]
  ): F[Result[Unit]] = {
    core
      .delete(address)
      .ensure(new RuntimeException(s"File not found at: $address"))(deleted => deleted)
      .attempt
      .map(_.void)
  }
}

object FileStore {
  type ContentStream[F[_]] = Stream[F, Byte]
  type Result[T]           = Either[Throwable, T]

  trait FileCodec[T] {
    def encode(data: Address[T]): String
    def decode(filename: String): Either[CodecError, Address[T]]
  }

  object FileCodec {
    class CodecError(key: String) extends Exception(s"Couldn't decode key: $key")

    def apply[T](implicit ev: FileCodec[T]): FileCodec[T] = ev
  }
}
