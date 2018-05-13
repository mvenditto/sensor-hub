package tests

import api.internal.DriversManager
import fi.oph.myscalaschema.extraction.ObjectExtractor

trait SensorsHubInit {

  ObjectExtractor.overrideClassLoader(DriversManager.cl)

  println("sh init ok...")

  Thread.sleep(5000)
}
