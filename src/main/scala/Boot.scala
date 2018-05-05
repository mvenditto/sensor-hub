import api.internal.DriversManager
import api.services.ServicesManager
import fi.oph.myscalaschema.extraction.ObjectExtractor

object Boot extends App {
  ObjectExtractor.overrideClassLoader(DriversManager.cl)
  val tag =
    """
       ::: version 0.0.2 :::                          :::           :::
        __   ___  _ __   ___   ___   _ __  ___        | |__   _   _ | |__
      / __| / _ \| '_ \ / __| / _ \ |  __|/ __| _____ |  _ \ | | | ||  _ \
      \__ \|  __/| | | |\__ \| (_) || |   \__ \|_____|| | | || |_| || |_) |
   :::|___/ \___||_| |_||___/ \___/ |_|   |___/       |_| |_| \____||____/.:::::

    """

  //org.apache.log4j.BasicConfigurator.configure()
  System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StdErrLog")
  System.setProperty("org.eclipse.jetty.LEVEL", "OFF")

  ServicesManager.runAllServices()

  println(tag)

}
