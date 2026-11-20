Distributed Systems Technologies
================================


## At a glance

| | |
|---|---|
| **What** | My solutions to the three assignments of Distributed Systems Technologies, TU Wien (2024). |
| **Covers** | Persistence with JPA, jOOQ, MongoDB and Redis.<br>A custom IoC container (Javassist) and aspect-oriented programming (AspectJ / Spring AOP).<br>REST (Jersey) and gRPC services with authentication.<br>Messaging with RabbitMQ, complex event processing with Apache Flink, and elastic scaling of Docker-based workers. |
| **Stack** | Java, Maven, Spring Boot, Docker; a Python worker |
| **Note** | The assignment template is by the TU Wien Distributed Systems Group; the implementations are mine. |

Please find details about the template in the assignment document.

Build
-----

Each Maven module has a corresponding profile that contains the build config for the respective module.
For example, to build the JPA task of assignment 1, run

    mvn clean install -Pass1-jpa

You can add the `-DskipTests` flag to skip JUnit test execution.

There is also a profile `all`, that includes all modules.
You should activate this profile in your IDE's Maven ([IDEA],[Eclipse]) configuration!

   [IDEA]: https://www.jetbrains.com/help/idea/maven-support.html
   [Eclipse]: http://www.eclipse.org/m2e/documentation/release-notes-15.html#new-maven-profile-management-ui
