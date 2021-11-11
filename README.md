# ITC-2021 Fix-and-optimize Solver

Written by **[George Fonseca](http://professor.ufop.br/george/perfil)** and **[Túlio Toffolo](http://www.toffolo.com.br)**.

(C) Copyright 2021 by GOAL (UFOP). All rights reserved.  
More information: http://goal.ufop.br

Please address all contributions, suggestions, and inquiries to the current project administrator.

## Getting Started

This repository contains an integer programming formulation and a fix-and-optimize method to address the Sports Timetabling Problem proposed during the International Timetabling Competition of 2021 (ITC2021)

### Compiling the code

This project now uses [gradle](http://gradle.org "Gradle").
It simplifies compiling the code with its dependencies. Just run:

- gradle build

The jar file (``itc2021.jar``) will be generated.

### Usage examples:

- ``java -jar upmsp.jar instance.txt solution.txt``  
- ``java -jar upmsp.jar instance.txt solution.txt -algorithm sa -alpha 0.98 -saMax 1000 -t0 100000``  
- ``java -jar upmsp.jar instance.txt solution.txt -algorithm ils -rnaMax 10000000 -itersP 700 -p0 10 -pMax 5``  
- ``java -jar upmsp.jar instance.txt solution.txt -algorithm lahc -listSize 100``  
- ``java -jar upmsp.jar instance.txt solution.txt -algorithm schc -stepSize 100``  

### Requirements

Java 16, Gurobi 9 and [Apache Commons Math](https://commons.apache.org/proper/commons-math/ "Apache Commons Math") library are required.

## Questions?

If you have any questions, please feel free to contact us.
For additional information, we would like to direct you to http://goal.ufop.br

Thanks!
