package com.deliganli.fstore4s.core

import cats.effect.{ContextShift, IO, Timer}
import org.mockito.cats.IdiomaticMockitoCats
import org.mockito.scalatest.ResetMocksAfterEachTest
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{EitherValues, Inspectors, OptionValues}

import scala.concurrent.ExecutionContext

class UnitTest
    extends AnyFlatSpec
    with Matchers
    with OptionValues
    with EitherValues
    with Inspectors
    with MockitoSugar
    with ResetMocksAfterEachTest
    with IdiomaticMockitoCats
    with ArgumentMatchersSugar {
  implicit def CS: ContextShift[IO] = UnitTest.CS
  implicit def TM: Timer[IO]        = UnitTest.TM
}

object UnitTest {
  implicit val CS: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  implicit val TM: Timer[IO]        = IO.timer(ExecutionContext.global)
}
