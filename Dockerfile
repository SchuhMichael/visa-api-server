


# Stage 1: Build the application
FROM openjdk:21-jdk as builder

ARG MAVEN_OPTS

RUN mkdir -p /usr/src/app

WORKDIR /usr/src/app

COPY . /usr/src/app

# RUN ./mvnw clean package -B -Dquarkus.package.type=uber-jar -DskipTests=true $MAVEN_OPTS
RUN ./mvnw clean package -B -Dquarkus.package.type=mutable-jar -DskipTests=true $MAVEN_OPTS

# Stage 2: Create the runtime image
FROM amazoncorretto:21-alpine

RUN mkdir -p /app

# Copy the Maven wrapper and application source code for dev mode
COPY --from=builder /usr/src/app /app

WORKDIR /app

# EXPOSE 8086 8087 
EXPOSE 5005 8081 8086 8087 

# Run the application 
#CMD ["./mvnw", "quarkus:dev", "-Dquarkus.http.host=0.0.0.0"]
CMD ["java", "-jar", "visa-app/target/quarkus-app/quarkus-run.jar"]
# CMD ["java", "-jar", "visa-app/target/visa-app.jar"]





