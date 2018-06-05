package api.devices

import api.devices.Devices.Device
import api.internal.DeviceDriverWrapper

object DriversHotSwap {

  private[devices] trait UpdatableDevice extends Device {

    def updateWith(newDriver: DeviceDriverWrapper = driver): Device with UpdatableDevice = {
      val checkName = newDriver.metadata.name equals this.driver.metadata.name
      val newDriverDataStreams = newDriver.controller.dataStreams
      val checkDataStreams = dataStreams.forall(ds => newDriverDataStreams.map(_.name).exists(_ equals ds.name))
      if (!(checkName && checkDataStreams)) throw new IllegalArgumentException("new driver not compatible with current one!")
      val newDataStreams = dataStreams.map(ds =>
        ds.updateWith(newProcedure = newDriverDataStreams.find(_.name equals ds.name).map(_.procedure).get))
      new Device(id, name, description, encodingType, metadata, newDriver, tasks, newDataStreams, customProps) with UpdatableDevice
    }

  }

}
