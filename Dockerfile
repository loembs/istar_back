# Étape 1 : Build
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Copier les fichiers Maven d'abord (pour le cache des layers)
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
RUN chmod +x mvnw

# Copier et installer le module supabase-auth-module
# IMPORTANT: Ce Dockerfile nécessite que le contexte soit à la racine du projet
# Si le contexte est applestore/, utilisez le Dockerfile à la racine à la place
WORKDIR /tmp
COPY supabase-auth-module ./supabase-auth-module
WORKDIR /tmp/supabase-auth-module
# Installer le module dans le repository Maven local du conteneur
RUN chmod +x mvnw 2>/dev/null || true
RUN ./mvnw clean install -DskipTests || mvn clean install -DskipTests

# Retourner au répertoire de l'application
WORKDIR /app

# Copier le code source
COPY src ./src

# Build l'application (le module est maintenant disponible)
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
