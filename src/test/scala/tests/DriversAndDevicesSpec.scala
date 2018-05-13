package tests

import java.net.URI

import api.internal.DriversManager
import api.sensors.DevicesManager
import api.sensors.Sensors.Encodings
import org.scalatest.FlatSpec

class DriversAndDevicesSpec extends FlatSpec with SensorsHubInit {

  "the DriversManager" should "have some available drivers" in {
    assert(DriversManager.availableDrivers.nonEmpty)
  }

  it should "instance a valid driver among the available ones" in {
    //assert(DriversManager.instanceDriver("driver 1").isDefined)
  }

  "the DevicesManager" should "create a Device provided a valid driver" in {
    assert(DriversManager.instanceDriver("driver 1")
      .map(drv => {
        drv.controller.init()
        drv.controller.start()
        DevicesManager.createDevice("", "", Encodings.PDF, new URI(""), drv)
      }).isDefined)
  }

  it should "expose created devices" in {
    assert(DevicesManager.devices().nonEmpty)
  }

  it should "return an existing device given its id" in {
    assert(DevicesManager.getDevice(0).isDefined)
  }

  it should "delete an existing device given its id" in {
    DevicesManager.deleteDevice(0)
    assert(DevicesManager.devices().isEmpty)
  }

}
