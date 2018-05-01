package spi.service

trait Service extends Disposable {

  def init(metadata: ServiceMetadata): Unit

  def restart(): Unit

}
