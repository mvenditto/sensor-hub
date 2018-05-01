package utils

import org.slf4j.Logger

import scala.util.{Failure, Success, Try}

object LoggingUtils {

  def logTry[T](tryResult: Try[T])(err: Throwable => String, info: T => String)(implicit logger: Logger): Unit = tryResult match {
    case Failure(reason) =>
      logger.error(err(reason))
    case Success(x) =>
      logger.info(info(x))
  }



  def logEitherOpt[E, T](v: Either[E, T])(implicit logger: Logger): Option[T] = v match {
    case Left(err) =>
      logger.error(err.toString)
      None
    case Right(t) =>
      Option(t)
  }

}
