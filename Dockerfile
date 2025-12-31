# Étape 1 : Build
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Copier les fichiers Maven d'abord (pour le cache des layers)
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
RUN chmod +x mvnw

# Copier le code source
COPY src ./src

# Build l'application
RUN ./mvnw clean package -DskipTests

# Étape 2 : Image de production (plus légère avec JRE)
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copier le JAR depuis l'étape de build
COPY --from=build /app/target/applestore-0.0.1-SNAPSHOT.jar app.jar

# Port de l'application
EXPOSE 8081

# Variables d'environnement par défaut
ENV SPRING_PROFILES_ACTIVE=prod
ENV PORT=8081

# Lancer l'application
ENTRYPOINT ["sh", "-c", "java --add-opens=java.base/java.lang=ALL-UNNAMED -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod} -Dserver.port=${PORT:-8081} -jar app.jar"]
