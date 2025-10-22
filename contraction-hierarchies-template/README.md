# Template Contraction Hierarchies Assignment

This repository presents a Java-based template to start with the contraction hierarchies assignment in Applied algorithms. 

## Build

The project uses gradle. Carry out the tests using `gradle test` and build the jar file using `gradle jar`. 

The graph is read from standard input and a test graph `test.graph` is provided in this repository. 

Basic file reading is already implemented in the Main class. An example run is 

```
gradle jar
java -jar app/build/libs/app.jar < test.graph
```

(Note that we simulate undirected edges by adding directed edges in both directions, so the count that is printed is twice as large as the number in the graph provided.)

## Code structure

We have implemented basic functionality such as graph reading, a graph data structure using HashMaps, and a basic Dijkstra implementation that keeps track of visited vertices using HashSets.  Basic unit tests for the Dijkstra implementation are available as well. 

To solve the assignment, you probably need to update the API by changing method signatures to incorporate additional functionality.




