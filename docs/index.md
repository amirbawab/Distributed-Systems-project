# V4Vim
## Architecture
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

## Building project
Distributed systems require a lot of dependencies between components especially when using RMI technology.
This project uses Gradle build tool in order to facilitate  managing complex archtectures and dependencies.
The project is composed of several Gradle sub-projects each of which can be started with a Gradle task
(Order of starting the sub-projects is imported).

*Note: The following commands must be executed from the root directory of the repository.*  
*Note: Refer to [Architecture](#architecture) section to identify machines A and B*
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
./gradlew rmi:rm:build rmi:rm:run -DrmName=car
./gradlew rmi:rm:build rmi:rm:run -DrmName=flight
./gradlew rmi:rm:build rmi:rm:run -DrmName=room
```

### Start Middleware Server on machine A
```
./gradlew rmi:mid-server:build rmi:mid-server:run
```

### Start a Client on any machine
```
./gradlew rmi:client:build rmi:client:run
```

## Configure network settings
To configure the IP and Port values for machine A and B, edit the corresponding variables in:
`build.gradle` located at the root of the repository.
