package spi.service

trait Service extends Disposable {

  def init(): Unit

  def restart(): Unit

}
