FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY target/ventas-0.0.1-SNAPSHOT.jar app.jar
COPY OracleWallet /app/OracleWallet

EXPOSE 8086

ENTRYPOINT ["java","-jar","app.jar"]