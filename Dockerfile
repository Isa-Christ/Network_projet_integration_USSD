# ===============================
# BUILD STAGE (Java backend)
# ===============================
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Aller offline pour dependencies
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Copier le code Java uniquement
COPY src ./src

# Build sans tests (rapide)
RUN mvn -B clean package -DskipTests

# ===============================
# RUNTIME STAGE
# ===============================
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copier le jar compilé
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
