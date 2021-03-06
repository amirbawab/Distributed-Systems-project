# V4Vim
## RMI - Architecture
<pre>
Each client can       Hosted on machine A   Hosted on machine B
run on a separate     running registry on   running registry on
machine               port: 2000            port: 2001
V                     V                     V
+----------+                              +-------------+
| Client 1 | <-----> +------------+ <-----> RM - Flight |
+----------+         |            |       |             |
+----------+         | Middleware |       |             |
| Client 2 | <-----> |   Server   | <-----> RM - Car    |
+--------            |            |       |             |
+----------+         |            |       |             |
| Client N | <-----> +------------+ <-----> RM - Room   |
+----------+                              +-------------+
</pre>

## TCP - Architecture
<pre>
                                            Hosted on any machine
Each client can       Hosted on machine A   port: 2003
run on a separate     listening on          |  port: 2004
machine               port: 2002            |  |  port: 2005
V                     V                     |  |  V
+----------+                                |  |  +-------------+
| Client 1 | <-----> +------------+ <-------+--+->| RM - Flight |
+----------+         |            |         |  V  +-------------+
+----------+         | Middleware |         |  +-------------+
| Client 2 | <-----> |   Server   | <-------+->| RM - Car    |
+--------            |            |         V  +-------------+
+----------+         |            |         +-------------+
| Client N | <-----> +------------+ <------>| RM - Room   |
+----------+                                +-------------+
</pre>

## Building project
Distributed systems require a lot of dependencies between components especially when using RMI technology.
This project uses Gradle build tool in order to facilitate  managing complex archtectures and dependencies.
The project is composed of several Gradle sub-projects each of which can be started with a Gradle task
(Order of starting the sub-projects is imported).

*Note: The following commands must be executed from the root directory of the repository.*  
*Note: Refer to [Architecture](#architecture) section to identify machines A and B*

## Run using RMI
### Start Registry on machine A
```
./gradlew midServerRMIRegistry
```

### Start Registry on machine B
```
./gradlew rmRMIRegistry
```

### Start RMs on machine B
```
./gradlew rmi:rm:build rmi:rm:runCar
./gradlew rmi:rm:build rmi:rm:runFlight
./gradlew rmi:rm:build rmi:rm:runRoom
```

### Start Middleware Server on machine A
```
./gradlew rmi:midserver:build rmi:midserver:run
```

### Start a Client on any machine
```
./gradlew rmi:client:build rmi:client:run
```

## Run using TCP
### Start RMs
```
./gradlew tcp:rm:build tcp:rm:runCar
./gradlew tcp:rm:build tcp:rm:runFlight
./gradlew tcp:rm:build tcp:rm:runRoom
```

### Start Middleware Server
```
./gradlew tcp:midserver:build tcp:midserver:run
```

### Start Client
```
./gradlew tcp:client:build tcp:client:run
```

## Configure network settings
To configure the IP and Port values for machine A and B, edit the corresponding variables in:
`build.gradle` located at the root of the repository.
