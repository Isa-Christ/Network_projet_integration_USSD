# ========================================
# ÉTAPE 1 : BUILD (Compilation du projet)
# ========================================
FROM maven:3.9.6-eclipse-temurin-21 AS build

# Définir le répertoire de travail
WORKDIR /app

# Copier les fichiers de config Maven d'abord (pour le cache Docker)
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
COPY mvnw.cmd .

# Télécharger les dépendances (cette étape sera cachée si pom.xml ne change pas)
RUN mvn dependency:go-offline -B

# Copier tout le code source
COPY src ./src

# Compiler et packager l'application (skip tests pour build rapide)
RUN mvn clean package -DskipTests

# ========================================
# ÉTAPE 2 : RUNTIME (Image finale légère)
# ========================================
FROM eclipse-temurin:21-jre-jammy

# Créer un utilisateur non-root pour la sécurité
RUN groupadd -r spring && useradd -r -g spring spring

# Définir le répertoire de travail
WORKDIR /app

# Copier le JAR depuis l'étape de build
COPY --from=build /app/target/*.jar app.jar

# Changer le propriétaire du fichier
RUN chown spring:spring app.jar

# Basculer vers l'utilisateur non-root
USER spring

# Exposer le port de l'application
EXPOSE 8080

# Variables d'environnement par défaut (seront overridées en production)
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Healthcheck pour Docker
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Commande de démarrage
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
