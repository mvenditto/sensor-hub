# sensor-hub :whale:

## TODOs List :ballot_box_with_check:
- [x] global *configuration* management
- [ ] enhance services bootstrap phase (threads?, load order?, service hearthbeat?) 
- [ ] rethink *feature of interest* attachment to *observations* (move out from drivers level)
- [ ] add more checks on argument while creating resources (eg. createDevice ecc...)
- [ ] introduction of *session* concept and session recovery (separated service?)
- [ ] permissions implementation
- [x] refactor logging/auditing

### Optimizations :fire:
- [x] fix Observation instanced 2 times (cause .copy call in DataStream)
- [x] change timestamps from java.time.instant to Long (millis since epoch)
- [x] fix performance issues with infinite Observable created from observation procedure
- [x] add a default backpressure strategyy to Observation streams from sensors
- [ ] investigate other possible issues with memory/cpu usage (rx threading ecc...)
