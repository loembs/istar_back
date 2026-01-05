FROM eclipse-temurin:17-jdk AS build

# Build supabase-auth-module d'abord
WORKDIR /tmp/supabase-auth-module
COPY supabase-auth-module/pom.xml ./pom.xml
COPY supabase-auth-module/mvnw ./mvnw
COPY supabase-auth-module/.mvn ./.mvn
COPY supabase-auth-module/src ./src

RUN chmod +x mvnw 2>/dev/null || true
RUN ./mvnw clean install -DskipTests

# Build applestore
WORKDIR /app
COPY pom.xml mvnw ./
COPY .mvn ./.mvn
COPY src ./src

RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

# Production
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/applestore-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081
ENV SPRING_PROFILES_ACTIVE=prod
ENV PORT=8081

ENTRYPOINT ["sh", "-c", "java --add-opens=java.base/java.lang=ALL-UNNAMED -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod} -Dserver.port=${PORT:-8081} -jar app.jar"]