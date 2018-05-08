package spi.service

trait Service extends Disposable {

  def init(metadata: ServiceMetadata): Unit

  def start(): Unit

  def restart(): Unit

}
