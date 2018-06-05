package tests

import api.config.Preferences
import api.config.Preferences.configure

trait SensorsHubInit {

  configure("src/test/assets/sh-prefs.conf")

  println(s"test configuration: ${Preferences.cfg}")

  println("sh init ok...")
}
