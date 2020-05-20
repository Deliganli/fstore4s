package com.deliganli.fstore4s.s3

import cats.effect.IO
import com.deliganli.fstore4s.core.{IntegrationTest, SimpleDomainFile, StoreBehaviours}
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.regions.Region

import scala.concurrent.duration._

class S3StoreTest extends IntegrationTest with StoreBehaviours {

  val credentialsProvider = StaticCredentialsProvider.create(
    AwsBasicCredentials.create(
      System.getenv("FSTORE4S_AWS_KEY"),
      System.getenv("FSTORE4S_AWS_SECRET")
    )
  )

  val config = S3Config(
    Region.EU_CENTRAL_1,
    "fstore4s-test",
    65536,
    10.seconds,
    1.hour
  )

  val store = S3Store
    .create[IO, SimpleDomainFile](config, credentialsProvider)
    .unsafeRunSync()

  "S3Store" should behave like storeOperations(store)
}
