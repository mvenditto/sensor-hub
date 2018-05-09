
import api.events.EventLogging
import api.internal.DriversManager
import api.services.ServicesManager
import fi.oph.myscalaschema.extraction.ObjectExtractor
import org.apache.log4j.{Level, LogManager}

import scala.concurrent.Await
import scala.concurrent.duration._

object Boot extends App {

  EventLogging.init()

  ObjectExtractor.overrideClassLoader(DriversManager.cl)

  val tag =
    """
       ::: version 0.1.0 :::                          :::           :::
        __   ___  _ __   ___   ___   _ __  ___        | |__   _   _ | |__
      / __| / _ \| '_ \ / __| / _ \ |  __|/ __| _____ |  _ \ | | | ||  _ \
      \__ \|  __/| | | |\__ \| (_) || |   \__ \|_____|| | | || |_| || |_) |
   :::|___/ \___||_| |_||___/ \___/ |_|   |___/       |_| |_| \____||____/.:::::

    """

  //org.apache.log4j.BasicConfigurator.configure()

  System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StdErrLog")
  System.setProperty("org.eclipse.jetty.LEVEL", "OFF")

  Await.ready(ServicesManager.runAllServices(), 5 seconds)

  import scala.collection.JavaConverters._
  LogManager.getCurrentLoggers.asScala foreach {
    case l: org.apache.log4j.Logger =>
      if(!l.getName.startsWith("sh.")) l.setLevel(Level.OFF)
  }

  println(tag)

}
