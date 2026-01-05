# Étape 1 : Build
FROM eclipse-temurin:17-jdk AS build

# Copier et installer le module supabase-auth-module D'ABORD
# IMPORTANT: Le contexte de build doit être à la racine du projet
WORKDIR /tmp/supabase-auth-module
COPY supabase-auth-module/pom.xml ./pom.xml
COPY supabase-auth-module/mvnw ./mvnw
COPY supabase-auth-module/.mvn ./.mvn
COPY supabase-auth-module/src ./src

# Rendre mvnw exécutable et installer le module
RUN chmod +x mvnw 2>/dev/null || true
RUN ./mvnw clean install -DskipTests || mvn clean install -DskipTests

# Maintenant builder l'application applestore
WORKDIR /app

# Copier les fichiers Maven d'abord (pour le cache des layers)
COPY applestore/pom.xml ./pom.xml
COPY applestore/mvnw ./mvnw
COPY applestore/.mvn ./.mvn

# Rendre mvnw exécutable
RUN chmod +x mvnw

# Télécharger les dépendances (cache layer)
RUN ./mvnw dependency:go-offline -B || true

# Copier le code source
COPY applestore/src ./src

# Build l'application (le module supabase-auth-module est maintenant disponible)
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