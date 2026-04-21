FROM eclipse-temurin:11-jdk

WORKDIR /app

COPY backend/ backend/
COPY frontend/ frontend/
COPY lib/ lib/

RUN mkdir out && \
    javac -cp "lib/mysql-connector-j-9.6.0.jar" \
    -d out \
    backend/DBConnection.java \
    backend/BankService.java \
    backend/Server.java

EXPOSE 8080

CMD ["java", "-cp", "out:lib/mysql-connector-j-9.6.0.jar", "Server"]