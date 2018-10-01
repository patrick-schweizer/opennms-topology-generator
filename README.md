# opennms-topology-generator
Allows to generate (large) toplogies that can be used for performance Tests

Build:
```mvn package```

Run:
```java -jar opennms-topology-generator-21.1.0-SNAPSHOT-jar-with-dependencies.jar --nodes 3 --delete```     

available parameters:

parameter name | description                                                              | default value
-------------- | -------------------------------------------------------------------------|--------------
nodes          | amount of nodes to create                                                | 10
elements       | amount of elements to create, must not be larger than amount of nodes and  no less than 2 | amount of nodes                                                          |
links          | amount of links                                                          | ((amount of elements)²-amount of elements)/2
delete         | delete existing topology                                                 | false
topology       | which type of topology to create, possible values: `random`, `ring`, `complete` | random

