FROM openjdk:17-jdk-alpine
COPY build/libs/spring-boot-testcontainers-1.0-SNAPSHOT.jar product-1.0-SNAPSHOT.jar
ENTRYPOINT ["java", "-Dspring.profiles.active=docker", "-jar", "/product-1.0-SNAPSHOT.jar"]
