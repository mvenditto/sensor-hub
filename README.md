# sensor-hub
## Architecture

<img src="https://github.com/mvenditto/sensor-hub/blob/security/docs/images/sh_core_arch.png" width="70%">

## Execution
### Execute with a "Security Manager"
Enabling a SecurityManager permits a granular control over actions installed extensions (aka external .jar services) can do. To enable the security management you will need:

JVM parameter | Description
------------ | -------------
-Dsensors-hub.home={root of sensors-hub project} | set sensors-hub.home system property
-Didea.no.launcher=true | not sure it's needed 
-Djava.security.manager | enable security manager
-Djava.security.debug=access | optionally enable debugging
-Djava.security.policy=sensors-hub.policy | specify the security policy
  
eg. *-Dsensors-hub.home=/path/to/sensors-hub/ -Didea.no.launcher=true -Djava.security.manager -Djava.security.debug=access -Djava.security.policy=sensors-hub.policy*

**See also:**
* https://docs.oracle.com/javase/tutorial/essential/environment/security.html
* https://docs.oracle.com/javase/7/docs/technotes/guides/security/PolicyFiles.html

### Sensors-Hub Permissions
TODO