package com.deliganli.fstore4s.s3

import software.amazon.awssdk.regions.Region

import scala.concurrent.duration.FiniteDuration

case class S3Config(
  region: Region,
  bucket: String,
  bufferSize: Int,
  bufferTimeLimit: FiniteDuration,
  linkExpiration: FiniteDuration)
