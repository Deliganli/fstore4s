package com.deliganli.fstore4s.gcs

import java.nio.file.Path

import cats.effect.IO
import com.deliganli.fstore4s.core.{IntegrationTest, SimpleDomainFile, StoreBehaviours}

import scala.concurrent.duration._

class GCSStoreTest extends IntegrationTest with StoreBehaviours {

  val config = GCSConfig(
    Path.of(System.getenv("FSTORE4S_GCS_CREDENTIALS")),
    "fstore4s-test",
    65536,
    1.hour
  )

  val store = GCSStore
    .create[IO, SimpleDomainFile](config)
    .unsafeRunSync()

  "GCSStore" should behave like storeOperations(store)

}
