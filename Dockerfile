FROM maven:3.9-eclipse-temurin-21-alpine as builder

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
ARG CACHE_BUST=1
RUN mvn clean package -q -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/target/enterprise-doc-analyzer-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Xmx1G", "-jar", "app.jar"]
