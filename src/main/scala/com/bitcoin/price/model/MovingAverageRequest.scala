package com.bitcoin.price.model

import io.swagger.annotations.{ApiModel, ApiModelProperty}

@ApiModel(description = "Moving average request")
case class MovingAverageRequest
(
  @ApiModelProperty("date range yyyy-MM-dd")
  customDateRangeRequest: CustomDateRangeRequest,
  @ApiModelProperty("period for moving average")
  period: Int
)


