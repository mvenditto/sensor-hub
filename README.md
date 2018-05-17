# sensor-hub [![codecov](https://codecov.io/gh/mvenditto/sensor-hub/branch/master/graph/badge.svg?token=Nj3INWZgrw)](https://codecov.io/gh/mvenditto/sensor-hub)

## TODOs List :ballot_box_with_check:
- [x] global *configuration* management
- [ ] enhance services bootstrap phase (threads?, load order?, service hearthbeat?) **[WIP]**
- [ ] rethink *feature of interest* attachment to *observations* (move out from drivers level) **[WIP]**
- [ ] add more checks on argument while creating resources (eg. createDevice ecc...) 
- [ ] introduction of *session* concept and session recovery (separated service?) **[WIP]**
- [ ] permissions implementation **[WIP]**
- [x] refactor logging/auditing

### Optimizations :fire:
- [x] fix Observation instanced 2 times (cause .copy call in DataStream)
- [x] change timestamps from java.time.instant to Long (millis since epoch)
- [x] fix performance issues with infinite Observable created from observation procedure
- [x] add a default backpressure strategyy to Observation streams from sensors
- [ ] investigate other possible issues with memory/cpu usage (rx threading ecc...)

# Coverage

[![codecov](https://codecov.io/gh/mvenditto/sensor-hub/branch/master/graphs/icicle.svg?token=Nj3INWZgrw)](https://codecov.io/gh/mvenditto/sensor-hub/branch/master/graphs/icicle.svg?token=Nj3INWZgrw)
