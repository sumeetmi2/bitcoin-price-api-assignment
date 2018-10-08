package com.bitcoin.price.swagger
import com.github.swagger.akka.model.Info
import io.swagger.models.Scheme

abstract class SwaggerDocService extends SwaggerUIService {
  override val info = Info(
    title = "Bitcoin Price API",
    description = "REST Interface for Bitcoin Price analysis",
    version = sys.props.getOrElse("version", "dev")
  )
  override val basePath = "/api"
  override val schemes: List[Scheme] = List(Scheme.HTTP)
}
