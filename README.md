# ITC-2021 Fix-and-optimize Solver

Written by **[George Fonseca](http://professor.ufop.br/george/perfil)** and **[Túlio Toffolo](http://www.toffolo.com.br)**.

(C) Copyright 2021 by GOAL (UFOP). All rights reserved.  
More information: http://goal.ufop.br

Please address all contributions, suggestions, and inquiries to the repository owner, [George Fonseca](http://professor.ufop.br/george/perfil).

## Getting Started

This repository contains an integer programming formulation and a fix-and-optimize method to address the Sports Timetabling Problem proposed during the International Timetabling Competition of 2021 (ITC2021)

### Compiling the code

This project now uses [gradle](http://gradle.org "Gradle").
It simplifies compiling the code with its dependencies. Make sure you have access to Gurobi's Java library and just run:

- ``gradle build``

The jar file ``itc2021.jar`` will be generated.

### Usage examples:

- ``java -jar itc2021.jar instance.xml solution.xml``
- ``java -jar itc2021.jar instance.xml solution.xml -validate``

### Requirements

- Java 16
- [Gurobi 9](https://www.gurobi.com) or newer 
- [Apache Commons Math](https://commons.apache.org/proper/commons-math/ "Apache Commons Math") library (included automatically by gradle).

## Questions?

If you have any questions, please feel free to contact us.
For additional information, we would like to direct you to http://goal.ufop.br

Thanks!
