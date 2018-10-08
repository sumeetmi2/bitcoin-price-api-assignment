package com.bitcoin.price.model

import io.swagger.annotations.{ApiModel, ApiModelProperty}

@ApiModel(description = "Custom Date Range query")
final case class CustomDateRangeRequest
(
  @ApiModelProperty("Start Date")
  startDate: String,
  @ApiModelProperty("End Date")
  endDate: String
)
