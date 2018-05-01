package api.internal

trait DeviceConfigurator extends Configurator {

  private var jniLibPath: Option[String] = Option.empty[String]

  def getJniLibPath: Option[String] = jniLibPath

  def setJniLibPath(jniLibs: String): Unit = { jniLibPath = Some(jniLibs) }

}
