# opennms-topology-generator
Allows to generate (large) toplogies that can be used for performance Tests

Build:
```mvn package```

Run:
```java -jar opennms-topology-generator-21.1.0-SNAPSHOT-jar-with-dependencies.jar -n 10 -e 8 -l 12 -d```     
Parameters:
- ```-n 10``` create 10 OnmsNodes
- ```-e 8``` create 8 CdpElements
- ```-l 12``` create 12 CdpLinks
- ```-d delete``` existing topology (with id's >= 100)
