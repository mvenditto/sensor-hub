package utils

import java.io.File

import driver_api.annotation.DriverAnnotations.Metadata
import driver_api.internal.exception.DriverException.MetadataParsingException
import pureconfig.{CamelCase, ConfigFieldMapping, ProductHint, loadConfigFromFiles}

import scala.util.{Failure, Success, Try}

object DriverUtils {

  private[this] implicit def camelCaseHint[T]: ProductHint[T] = ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))

  def tryParseMetadata(metadata: File): Try[Metadata] = {
    loadConfigFromFiles[Metadata](Seq(metadata.toPath)) match {
      case Left(failure) => Failure(new MetadataParsingException(failure.head.description))
      case Right(config) => Success(config)
    }
  }

}
