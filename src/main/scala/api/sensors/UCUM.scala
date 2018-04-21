package api.sensors

import java.net.URI

import api.sensors.Sensors.UnitOfMeasurement

object UCUM {

  val DegreesCelsius =
    UnitOfMeasurement(
      name = "DegreesCelsius",
      symbol = "Cel",
      definition = new URI("http://download.hl7.de/documents/ucum/ucumdata.html")
    )

}
