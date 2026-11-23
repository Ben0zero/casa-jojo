# La Casa JoJo - casajojo-ms-catalogo (menú del día). Puerto 8082.
# Construcción en dos etapas: Maven compila dentro del contenedor (no hace falta
# tener target/ en el equipo) y la imagen final solo lleva el JRE y el .jar.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/target/*.jar app.jar
# El wallet de Oracle NO se copia a la imagen: docker-compose lo monta en /wallet
# como volumen de solo lectura (TNS_ADMIN=/wallet).
ENV TNS_ADMIN=/wallet
EXPOSE 8082
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
