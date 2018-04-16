package driver_api.sensor

import java.net.URI

import st.api.SensorThings.UnitOfMeasurement

object UCUM {

  val DegreesCelsius =
    UnitOfMeasurement(
      name = "DegreesCelsius",
      symbol = "Cel",
      definition = new URI("http://download.hl7.de/documents/ucum/ucumdata.html")
    )

}
