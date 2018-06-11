# sensor-hub [![codecov](https://codecov.io/gh/mvenditto/sensor-hub/branch/master/graph/badge.svg?token=Nj3INWZgrw)](https://codecov.io/gh/mvenditto/sensor-hub)


## Directory structure
```
\<sensor-hub home>
    \bin
      sensor-hub.jar
      sh-prefs.conf
    \ext
      \drivers
        driver-name.jar
        driver-name.json
      \services
        \<service-name>
          service-name.jar
```
  
## Running
 - *java -jar sensor-hub.jar*
 - running as a OS *daemon*: [see](https://github.com/mvenditto/sensor-hub/wiki/Running)
 - configuration: *sh-prefs.conf*
 - web interface (optional): @ *localhost:8081/sh-admin.html* (default)
    - [REST server](https://github.com/mvenditto/sensor-hub_rest-server)
    - [REST server front-end](https://github.com/mvenditto/sensor-hub_rest-service-frontend)

## TODOs List :ballot_box_with_check:
- [x] global *configuration* management
- [ ] enhance services bootstrap phase **[WIP]**
- [x] rethink *feature of interest* attachment to *observations* (move out from drivers level)
- [ ] add more checks on argument while creating resources (eg. createDevice ecc...) 
- [x] introduction of *session* concept and session recovery (separated service?) 
- [ ] permissions implementation **[WIP]**
- [x] refactor logging/auditing

### Optimizations :fire:
- [x] fix Observation instanced 2 times (cause .copy call in DataStream)
- [x] change timestamps from java.time.instant to Long (millis since epoch)
- [x] fix performance issues with infinite Observable created from observation procedure
- [x] add a default backpressure strategyy to Observation streams from sensors
- [x] investigate other possible issues with memory/cpu usage (rx threading ecc...)

