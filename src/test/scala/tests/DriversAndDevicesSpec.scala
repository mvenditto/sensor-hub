package tests

import java.net.URI

import api.internal.{DeviceController, DriversManager, TaskingSupport}
import api.sensors.DevicesManager
import api.sensors.Sensors.Encodings
import api.services.ServicesManager
import org.scalatest.FlatSpec

import scala.concurrent.Await
import scala.concurrent.duration._

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

  "a Device" should "be configured providing a string" in {
    val cfg = """multinomial {
                	"0.75" = "20.0"
                	"0.20" = "30.0"
                	"0.05" = "50.0"
                }"""
    assert(DriversManager.instanceDriver("driver 1")
      .map(drv => {
        drv.config.configureRaw(cfg)
        drv.controller.init()
        drv.controller.start()
        DevicesManager.createDevice("", "", Encodings.PDF, new URI(""), drv)
      }).isDefined)
  }

  it should "be configured providing also by a .conf file" in {
    val cfg = "conf.conf"
    assert(DriversManager.instanceDriver("driver 1")
      .map(drv => {
        drv.config.configure(cfg)
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
    assert(DevicesManager.devices().size == 2)
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

  "a Device" should "support 0 to many tasks that return a result" in {
    DevicesManager.getDevice(1).foreach(dev => {
      dev.driver.controller match {
        case ctrl: DeviceController with TaskingSupport =>
          assertResult("""{"echo":"ping"}""")(ctrl.send("dummy-task-w-response", """{"message":"ping"}""").blockingGet())
        case _ =>
          false
      }
    })
  }

  it should "support 0 to many tasks that not return a result" in {
    DevicesManager.getDevice(1).foreach(dev => {
      dev.driver.controller match {
        case ctrl: DeviceController with TaskingSupport =>
          assert(ctrl.send("dummy-task-wo-response", """{"message":"ping"}""").blockingGet("").isEmpty)
        case _ =>
          false
      }
    })
  }

  it should "not fail the system if asked a non existing task or given a wrong input" in {
    DevicesManager.getDevice(1).foreach(dev => {
      dev.driver.controller match {
        case ctrl: DeviceController with TaskingSupport =>
          assertThrows[IllegalArgumentException](
            ctrl.send("non-existing-task", """{"message":"ping"}""").blockingGet())
        case _ =>
          false
      }
    })
  }

  "a Task" should "complete with onError if an exception occurred" in {
    DevicesManager.getDevice(1).foreach(dev => {
      dev.tasks.foreach(t => println(t.taskingParameters))
      dev.driver.controller match {
        case ctrl: DeviceController with TaskingSupport =>
          println(ctrl.send("dummy-task-always-error", """{"message":"ping"}""").onErrorComplete().blockingGet())
          true
        case _ =>
          false
      }
    })
  }

  "the ServicesManager" should "load discover all available services" in {
    assert(ServicesManager.services.nonEmpty)
  }

  it should "load init and start all available services" in {
    Await.ready(ServicesManager.runAllServices(), 5 seconds)
    true
  }

}
