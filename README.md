# Spring Boot Test Containers

A project that demonstrates integration-testing communications between a Spring Boot app and external services such as Postgres, Kafka, and Schema Registry using Testcontainers.

## 1) Quickstart

Make sure you have [Docker installed and Docker socket enabled](https://java.testcontainers.org/supported_docker_environment/). At the project directory, run:
```sh
gradle build
```

See the test report at `<projectDirectory>/build/reports/tests/test/index.html`

See the integration tests at the [package containers](https://github.com/emeraldhieu/spring-boot-testcontainers/tree/master/src/test/java/com/emeraldhieu/testcontainers/product/logic/containers).

## 2) Test containers

[Testcontainers](https://java.testcontainers.org) is library that provides disposable Docker containers of common databases or any other services on the fly. The greatest benefit is that you don't need to prepare any external service on your host machine such as running a Docker Compose.

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

The set-up is done! Now you can write your test code by any means like injecting services or using MockMvc.

Long story short, Testcontainers does all container configurations for you such as:
+ Pull the Docker image
+ Create a Docker container with default configuration properties
+ Map the container port to a random host port

### Start Kafka

Declare a container with a Docker image name
```java
@Container
private static KafkaContainer kafka =
    new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"))
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

### Start Schema Registry

Declare a container with a Docker image name
```java
@Container
private static GenericContainer schemaRegistry =
    new GenericContainer(DockerImageName.parse("confluentinc/cp-schema-registry:7.4.0"))
        .withNetwork(network)
        .withExposedPorts(8081)
        .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
        .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081")
        .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS",
            "PLAINTEXT://" + kafka.getNetworkAliases().get(0) + ":9092")
        .dependsOn(kafka);
```

Set Schema Registry URL for Kafka
```java
@DynamicPropertySource
static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.kafka.properties.schema.registry.url",
    () -> "http://" + schemaRegistry.getHost() + ":" + schemaRegistry.getFirstMappedPort());
}
```

## 3) Run a group of tests

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
