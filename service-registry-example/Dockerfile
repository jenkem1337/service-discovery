FROM maven:3.9.9-eclipse-temurin-23 AS build
# root pom'u kopyala
COPY pom.xml /build/pom.xml
WORKDIR /build
RUN mvn -N install

COPY service-registry /build/service-registry
WORKDIR /build/service-registry
RUN mvn clean install -DskipTests

COPY service-registry-example /build/service-registry-example
WORKDIR /build/service-registry-example
RUN mvn clean package -DskipTests

FROM eclipse-temurin:23-jre-alpine
WORKDIR /app
COPY --from=build /build/service-registry-example/target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]