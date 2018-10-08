package com.bitcoin.price.service

import java.time.format.DateTimeFormatter

object DateTimeDefaults {

  val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
  val inputDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
}
