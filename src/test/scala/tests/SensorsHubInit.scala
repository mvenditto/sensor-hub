package tests

import api.config.Preferences
import api.config.Preferences.configure
import api.internal.DriversManager
import fi.oph.myscalaschema.extraction.ObjectExtractor

trait SensorsHubInit {

  configure("src/test/assets/sh-prefs.conf")

  println(s"test configuration: ${Preferences.cfg}")

  ObjectExtractor.overrideClassLoader(DriversManager.cl)

  println("sh init ok...")
}
