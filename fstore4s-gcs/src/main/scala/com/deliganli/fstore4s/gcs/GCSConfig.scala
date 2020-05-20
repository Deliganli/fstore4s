package com.deliganli.fstore4s.gcs

import java.nio.file.Path

import scala.concurrent.duration.FiniteDuration

case class GCSConfig(
  credFile: Path,
  bucket: String,
  bufferSize: Int,
  linkExpiration: FiniteDuration)
