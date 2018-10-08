package com.bitcoin.price.swagger

import java.util.Properties

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Route
import com.github.swagger.akka.SwaggerHttpService

import scala.io.Source

trait SwaggerUIService extends SwaggerHttpService {
  val swaggerUiVersion: String = {
    val props = new Properties()
    props.load(getClass.getResourceAsStream("/META-INF/maven/org.webjars/swagger-ui/pom.properties"))
    props.getProperty("version")
  }

  val swaggerResourcePath: String = s"META-INF/resources/webjars/swagger-ui/$swaggerUiVersion"

  def renderIndex: String = {
    val indexStream = getClass.getResourceAsStream(s"/$swaggerResourcePath/index.html")
    val indexFile = Source.fromInputStream(indexStream).mkString
    // Replace default URL with our URL and also disable validator (does not work for internal URL)
    indexFile.replaceFirst("url: .+,", s"url: '/$apiDocsPath/swagger.json', validatorUrl: null,")
  }

  // For Swagger UI
  override def routes: Route = concat(
    super.routes,
    pathPrefix("swagger") {
      concat(
        pathSingleSlash {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, renderIndex))
        },
        getFromResourceDirectory(swaggerResourcePath)
      )
    },
    (pathEndOrSingleSlash | path("swagger")) {
      redirect("/swagger/", StatusCodes.TemporaryRedirect)
    }
  )
}
