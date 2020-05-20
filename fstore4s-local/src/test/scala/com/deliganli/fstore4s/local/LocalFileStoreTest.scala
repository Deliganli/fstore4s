package com.deliganli.fstore4s.local

import cats.effect.IO
import com.deliganli.fstore4s.core.{IntegrationTest, SimpleDomainFile, StoreBehaviours}

class LocalFileStoreTest extends IntegrationTest with StoreBehaviours {
  val config = LocalFileStoreConfig("fstore4s-test", 65536)
  val store  = LocalFileStore.create[IO, SimpleDomainFile](config).unsafeRunSync()

  "LocalFileStore" should behave like storeOperations(store)
}
