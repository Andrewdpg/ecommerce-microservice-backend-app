
FROM maven:3.8.8-eclipse-temurin-11 AS build
ARG PROJECT_VERSION=0.1.0
WORKDIR /workspace
COPY . .
RUN mvn -B -DskipTests -pl product-service -am clean package

FROM eclipse-temurin:11-jre AS runtime
ARG PROJECT_VERSION=0.1.0
ENV SPRING_PROFILES_ACTIVE=dev
WORKDIR /home/app
COPY --from=build /workspace/product-service/target/product-service-v${PROJECT_VERSION}.jar app.jar
EXPOSE 8500
ENTRYPOINT ["java", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}", "-jar", "app.jar"]


