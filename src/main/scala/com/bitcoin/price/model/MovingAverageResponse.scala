package com.bitcoin.price.model

import io.swagger.annotations.{ApiModel, ApiModelProperty}

@ApiModel(description = "Moving average response")
final case class MovingAverageResponse
(
  @ApiModelProperty("Date")
  date: String,
  @ApiModelProperty("Moving Average")
  value: Double
)
