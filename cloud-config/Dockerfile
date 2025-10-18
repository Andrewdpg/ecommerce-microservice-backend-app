
FROM maven:3.8.8-eclipse-temurin-11 AS build
ARG PROJECT_VERSION=0.1.0
WORKDIR /workspace
COPY . .
RUN mvn -B -DskipTests -pl cloud-config -am clean package

FROM eclipse-temurin:11-jre AS runtime
ARG PROJECT_VERSION=0.1.0
WORKDIR /home/app
COPY --from=build /workspace/cloud-config/target/cloud-config-v${PROJECT_VERSION}.jar app.jar
EXPOSE 9296
ENTRYPOINT ["java", "-jar", "app.jar"]


