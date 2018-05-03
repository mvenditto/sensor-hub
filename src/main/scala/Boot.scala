import api.services.ServicesManager

object Boot extends App {

  val tag =
    """
       ::: version 0.0.1 :::                          :::           :::
        __   ___  _ __   ___   ___   _ __  ___        | |__   _   _ | |__
      / __| / _ \| '_ \ / __| / _ \ |  __|/ __| _____ |  _ \ | | | ||  _ \
      \__ \|  __/| | | |\__ \| (_) || |   \__ \|_____|| | | || |_| || |_) |
   :::|___/ \___||_| |_||___/ \___/ |_|   |___/       |_| |_| \____||____/.:::::

    """

  org.apache.log4j.BasicConfigurator.configure()
  ServicesManager.runAllServices()
  println(tag)

}
