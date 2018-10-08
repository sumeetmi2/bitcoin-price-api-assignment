package com.bitcoin.price.model

import io.swagger.annotations.{ApiModel, ApiModelProperty}

@ApiModel(description = "Get next 15 days price predictoin")
final case class GetNext15DaysPriceRequest
(
  @ApiModelProperty("Default period is 10.")
  period: Int
)
