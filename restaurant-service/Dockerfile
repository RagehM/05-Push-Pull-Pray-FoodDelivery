FROM eclipse-temurin:25.0.2_10-jdk
WORKDIR /app
COPY restaurant-service/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]