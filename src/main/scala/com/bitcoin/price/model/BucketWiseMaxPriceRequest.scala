package com.bitcoin.price.model

import io.swagger.annotations.{ApiModel, ApiModelProperty}

@ApiModel(description = "Bucket wise query request object")
case class BucketWiseMaxPriceRequest
(
  customDateRangeRequest: CustomDateRangeRequest,
  @ApiModelProperty("bucket size")
  bucket: Int)
