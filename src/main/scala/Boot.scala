
import api.config.Preferences
import api.config.Preferences.configure
import api.events.EventLogging
import api.internal.DriversManager
import api.services.ServicesManager
import fi.oph.myscalaschema.extraction.ObjectExtractor
import org.apache.commons.daemon.{Daemon, DaemonContext}
import org.apache.log4j.{Level, LogManager}

import scala.concurrent.Await
import scala.concurrent.duration._

class BootDaemon extends Daemon {

  val tag =
    """
       ::: version 0.2.6a :::                         :::           :::
        __   ___  _ __   ___   ___   _ __  ___        | |__   _   _ | |__
      / __| / _ \| '_ \ / __| / _ \ |  __|/ __| _____ |  _ \ | | | ||  _ \
      \__ \|  __/| | | |\__ \| (_) || |   \__ \|_____|| | | || |_| || |_) |
   :::|___/ \___||_| |_||___/ \___/ |_|   |___/       |_| |_| \____||____/.:::::

    """

  override def init(context: DaemonContext): Unit = {

    configure("sh-prefs.conf")

    if (Preferences.cfg.logEvents) EventLogging.init()

    ObjectExtractor.overrideClassLoader(DriversManager.cl)

    //org.apache.log4j.BasicConfigurator.configure()

    System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StdErrLog")
    System.setProperty("org.eclipse.jetty.LEVEL", "OFF")

  }

  override def start(): Unit = {
    Await.ready(ServicesManager.runAllServices(), 5 seconds)
    
    import scala.collection.JavaConverters._
    LogManager.getCurrentLoggers.asScala foreach {
      case l: org.apache.log4j.Logger =>
        if(!l.getName.startsWith("sh.")) l.setLevel(Level.OFF)
    }

    println(tag)
  }

  override def destroy(): Unit = {
    println("destroy")
  }

  override def stop(): Unit = {
    println("stop")
  }
}

object Boot extends App {

  val daemon = new BootDaemon()
  daemon.init(null)
  daemon.start()

}
