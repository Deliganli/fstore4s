package com.deliganli.fstore4s.s3

import software.amazon.awssdk.services.s3.model._

object S3Requests {

  def completedPart(count: Int, eTag: String): CompletedPart = {
    CompletedPart
      .builder()
      .partNumber(count)
      .eTag(eTag)
      .build()
  }

  def uploadPart(
    bucket: String,
    key: String,
    uploadId: String,
    count: Int
  ): UploadPartRequest = {
    UploadPartRequest
      .builder()
      .bucket(bucket)
      .key(key)
      .uploadId(uploadId)
      .partNumber(count)
      .build()
  }

  def getObject(bucket: String, key: String): GetObjectRequest = {
    GetObjectRequest
      .builder()
      .bucket(bucket)
      .key(key)
      .build
  }
}
