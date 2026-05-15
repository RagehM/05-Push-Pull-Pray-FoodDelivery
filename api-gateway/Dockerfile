FROM maven:3-eclipse-temurin-25 AS build
WORKDIR /src
COPY pom.xml .
COPY gateway/pom.xml              gateway/
COPY order-service/pom.xml        order-service/
COPY inventory-service/pom.xml    inventory-service/
COPY notification-service/pom.xml notification-service/
COPY gateway/src                  gateway/src
RUN mvn -B -q -pl gateway -am package -DskipTests

FROM eclipse-temurin:25.0.2_10-jdk
WORKDIR /app
COPY --from=build /src/gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]