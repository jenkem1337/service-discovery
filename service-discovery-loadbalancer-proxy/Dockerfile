FROM maven:3.9.9-eclipse-temurin-23 AS build
# root pom'u kopyala
COPY pom.xml /build/pom.xml
WORKDIR /build
RUN mvn -N install

COPY service-discovery-loadbalancer-proxy /build/service-discovery-loadbalancer-proxy
WORKDIR /build/service-discovery-loadbalancer-proxy
RUN mvn clean package -DskipTests

FROM eclipse-temurin:23-jre-alpine
WORKDIR /app
COPY --from=build /build/service-discovery-loadbalancer-proxy/target/app.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]