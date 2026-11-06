FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /build
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw \
    && ./mvnw -B -ntp -DskipTests package

FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /build
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw \
    && ./mvnw -B -ntp -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# 1. CRIA O GRUPO E O USUÁRIO APP ANTES DE COPIAR OS ARQUIVOS
RUN groupadd -r app && useradd -r -g app app

# 2. COPIA O JAR ATRIBUINDO A PERMISSÃO AO NOVO USUÁRIO
COPY --from=build --chown=app:app /build/target/imoveis-1.0.0.jar /app/app.jar

USER app
EXPOSE 8080
ENTRYPOINT ["java", "-Djava.awt.headless=true", "-jar", "/app/app.jar"]
