package api.internal

import api.sensors.Sensors.DataStream

trait DeviceController {

  val configurator: DeviceConfigurator

  protected var _dataStreams = Seq.empty[DataStream]

  def dataStreams: Iterable[DataStream] = _dataStreams

  def init(): Unit

  def start(): Unit

  def stop(): Unit

}