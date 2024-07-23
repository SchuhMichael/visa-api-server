# Stage 1: Build the application
FROM openjdk:21-jdk as builder

ARG MAVEN_OPTS

RUN mkdir -p /usr/src/app

WORKDIR /usr/src/app

COPY . /usr/src/app

RUN ./mvnw clean package -B -Dquarkus.package.type=uber-jar -DskipTests=true $MAVEN_OPTS

# Stage 2: Create the runtime image
FROM amazoncorretto:21-alpine

RUN mkdir -p /app

WORKDIR /app

# Copy the Maven wrapper and application source code for dev mode
COPY --from=builder /usr/src/app /app
RUN chmod o+rw -R /app
# Expose the necessary ports
EXPOSE 5005 8081 8086 8087 

# Run the application in Quarkus dev mode
CMD ["./mvnw", "quarkus:dev", "-Dquarkus.http.host=0.0.0.0", ]
