# Spring Boot Test Containers

A project that shows how to integration-test communications between Spring Boot apps and external services such as Postgres, Kafka, Schema Registry using Testcontainers.

## 1) Quickstart

Make sure you have [Docker installed and Docker socket enabled](https://java.testcontainers.org/supported_docker_environment/). At the project directory, run:
```sh
gradle build
```

See the test report at `<projectDirectory>/build/reports/tests/test/index.html`

See the integration tests at the [package containers](https://github.com/emeraldhieu/spring-boot-testcontainers/tree/master/src/test/java/com/emeraldhieu/testcontainers/product/logic/containers).

## 2) Test containers

[Testcontainers](https://java.testcontainers.org) is library that supports integration-testing your applications with external services by starting disposable Docker containers on the fly.

### Start Postgres

Annotate your test class with `@Testcontainers`
```java
@Testcontainers
public class PostgresAndKafkaIT {
}
```

Declare a container with a Docker image name
```java
@Container
private static PostgreSQLContainer<?> postgres =
    new PostgreSQLContainer<>(DockerImageName.parse("postgres:15.3-alpine"));
```

Set datasource properties of the Spring Boot app based on the properties of the container.
```java
@DynamicPropertySource
static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    }
```

The set-up is done! Now you can write your test code by any means such as injecting a service, using MockMvc.

Long story short, Testcontainers does all container configurations for you such as:
+ Pull the Docker image
+ Create a Docker container with default configuration properties
+ Map the container port to a random host port

### Start Kafka

Declare a container with a Docker image name
```java
@Container
private static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"))
    .withNetwork(network)
    .withKraft()
    .withClusterId(clusterId);
```

Set bootstrap servers for Spring Boot's Kafka client
```java
@DynamicPropertySource
static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
```

## 2) Run a group of tests

A fun thing about Gradle is you don't need any plugin such as Maven Surefire or Failsafe to run a group of tests.

You can run a group of tests using [test filtering](https://docs.gradle.org/current/userguide/java_testing.html#test_filtering).

### Run unit tests only

```sh
gradle test --tests "*Test"
```

### Run integration tests only

```sh
gradle test --tests "*IT"
```

## References

+ [Database containers - Postgres Module](https://java.testcontainers.org/modules/databases/postgres)
+ [Kafka Containers](https://java.testcontainers.org/modules/kafka)

