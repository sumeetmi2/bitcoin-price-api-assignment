package com.bitcoin.price.model

import io.swagger.annotations.{ApiModel, ApiModelProperty}

@ApiModel(description = "Bucket wise max price response")
case class BucketWiseMaxPriceResponse
(
  @ApiModelProperty("bucket no")
  bucketNo: Int,
  @ApiModelProperty("date for price")
  date: String,
  @ApiModelProperty("max price")
  price: Double)
