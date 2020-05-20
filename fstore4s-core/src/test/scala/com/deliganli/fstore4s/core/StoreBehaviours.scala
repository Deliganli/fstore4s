package com.deliganli.fstore4s.core

import java.nio.file.Path

import cats.effect.{Blocker, IO}
import com.deliganli.fstore4s.core.CoreFileStore.Address
import com.deliganli.fstore4s.core.Test.download

trait StoreBehaviours { this: IntegrationTest =>
  val testPath = "test"

  val testFileSource = Test.loadResource("test.txt").use(IO.pure).unsafeRunSync()

  val address1 = Address(testPath, SimpleDomainFile("V1", "test-category", "some-file.txt"))
  val address2 = Address(testPath, SimpleDomainFile("V2", "another-category", "some-other-file.txt"))

  def storeOperations(store: FileStore[IO, SimpleDomainFile]) {

    it should "put file" in {
      val filePath = Path.of(getClass.getResource("/test.txt").getPath.drop(1))

      Blocker[IO]
        .map(blocker => fs2.io.file.readAll[IO](filePath, blocker, 65536))
        .use(contentStream => store.put(address1, contentStream))
        .unsafeRunSync() shouldBe a[Right[_, _]]
    }

    it should "list" in {
      store
        .list(testPath)
        .compile
        .toVector
        .unsafeRunSync() should contain(Right(address1))
    }

    it should "generate URI" in {
      store
        .uri(address1)
        .flatMap(IO.fromEither)
        .flatMap(uri => download(uri).use(IO.pure).attempt)
        .unsafeRunSync() shouldBe Right(testFileSource)
    }

    it should "get" in {
      store.content(address1).unsafeRunSync() shouldBe Some(testFileSource)
    }

    it should "move" in {
      store.move(address1, address2).unsafeRunSync() shouldBe a[Right[_, _]]
      store.read(address2).unsafeRunSync() shouldBe a[Some[_]]
      store.read(address1).unsafeRunSync() shouldBe None
    }

    it should "delete" in {
      store.delete(address2).unsafeRunSync() shouldBe a[Right[_, _]]
      store.read(address2).unsafeRunSync() shouldBe None
    }
  }
}
