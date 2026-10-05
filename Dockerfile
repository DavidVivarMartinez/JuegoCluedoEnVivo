# syntax=docker/dockerfile:1
# Imagen única: Spring Boot sirve la API y también el frontend compilado.
# Así solo hay que desplegar un servicio (Render, Koyeb, Fly, una VM...).

# 1) Frontend: compila React con Vite
FROM node:22-alpine AS frontend
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# 2) Backend: empaqueta el jar con el frontend dentro como recursos estáticos
FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /app/backend
COPY backend/pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY backend/src ./src
COPY --from=frontend /app/frontend/dist ./src/main/resources/static
RUN mvn -B -q -DskipTests package

# 3) Imagen final, pequeña: solo el JRE y el jar
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=backend /app/backend/target/*.jar app.jar
# Memoria ajustada a los planes gratuitos (512 MB). Se puede sobrescribir por entorno.
ENV JAVA_TOOL_OPTIONS="-Xmx256m -XX:MaxMetaspaceSize=128m -XX:+UseSerialGC -Xss512k -XX:TieredStopAtLevel=1"
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
