package tests

import java.net.URI

import api.internal.{DeviceController, DriversManager, TaskingSupport}
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
    for(_ <- 0 until 2) {
      assert(DriversManager.instanceDriver("driver 1")
        .map(drv => {
          drv.controller.init()
          drv.controller.start()
          DevicesManager.createDevice("", "", Encodings.PDF, new URI(""), drv)
        }).isDefined)
    }
  }

  it should "expose created devices" in {
    assert(DevicesManager.devices().nonEmpty)
  }

  it should "return an existing device given its id" in {
    assert(DevicesManager.getDevice(0).isDefined)
  }

  it should "delete an existing device given its id" in {
    DevicesManager.deleteDevice(0)
    assert(DevicesManager.devices().size == 1)
  }

  "a Datastream" should "be subscriptable to process produced Observations" in {
    DevicesManager.getDevice(1)
      .flatMap(dev => dev.dataStreams.find(_.name == "temperature"))
      .map(ds => ds.observable) match {
      case Some(ds) =>
        ds.blockingFirst()
        true
      case _ =>
        false
    }
  }

  "a Devices" should "support 0 to many tasks, that may return a result" in {
    DevicesManager.getDevice(1).foreach(dev => {
      dev.driver.controller match {
        case ctrl: DeviceController with TaskingSupport =>
          assertResult("""{"echo":"ping"}""")(ctrl.send("dummy-task", """{"message":"ping"}""").blockingGet())
        case _ =>
          false
      }
    })
  }



}
