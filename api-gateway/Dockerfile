FROM maven:3-eclipse-temurin-25 AS build
WORKDIR /src
COPY pom.xml pom.xml
COPY contracts/pom.xml          contracts/pom.xml
COPY shared-modules/pom.xml     shared-modules/pom.xml
COPY user-service/pom.xml       user-service/pom.xml
COPY restaurant-service/pom.xml restaurant-service/pom.xml
COPY order-service/pom.xml      order-service/pom.xml
COPY delivery-service/pom.xml   delivery-service/pom.xml
COPY checkout-service/pom.xml   checkout-service/pom.xml
COPY api-gateway/pom.xml        api-gateway/pom.xml
COPY api-gateway/src            api-gateway/src
RUN mvn -B -q -pl api-gateway -am package -DskipTests

FROM eclipse-temurin:25.0.2_10-jdk
WORKDIR /app
COPY --from=build /src/api-gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]