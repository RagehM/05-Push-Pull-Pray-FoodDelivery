FROM maven:3-eclipse-temurin-25 AS build
WORKDIR /src
COPY pom.xml pom.xml
COPY api-gateway/pom.xml        api-gateway/pom.xml
COPY api-gateway/src            api-gateway/src
RUN mvn -B -q -f api-gateway/pom.xml package -DskipTests

FROM eclipse-temurin:25.0.2_10-jdk
WORKDIR /app
COPY --from=build /src/api-gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]