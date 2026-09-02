# Multi-stage build: compile with Maven (Node/npm frontend build included via
# frontend-maven-plugin), then run on a lean JRE-only image -- no full JDK or
# Maven needed at runtime.

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml package.json package-lock.json tailwind.config.js postcss.config.js ./
COPY src ./src
# -Prelease triggers `npm run build-prod` (minified assets) instead of the
# plain dev build `mvn package` alone would produce.
RUN mvn -B clean package -Prelease -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
# dashboard/pom.xml has no <finalName>, so the jar is version-suffixed
# (dashboard-<version>.jar) -- glob it rather than hardcoding the version.
COPY --from=build /build/target/dashboard-*.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]
