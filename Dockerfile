# MegaBank Payment Gateway - Dockerfile
# Multi-stage build for legacy Java application

# Stage 1: Build
FROM --platform=linux/amd64 maven:3.6.3-jdk-8 AS builder

WORKDIR /build

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime
FROM --platform=linux/amd64 tomcat:8.5-jre8

# Remove default Tomcat applications
RUN rm -rf /usr/local/tomcat/webapps/*

# Create necessary directories
RUN mkdir -p /var/data/megabank/incoming \
             /var/data/megabank/processed \
             /var/data/megabank/errors \
             /var/data/megabank/archive \
             /var/log/megabank

# Copy built artifact from builder stage
COPY --from=builder /build/target/*.jar /usr/local/tomcat/webapps/payment-gateway.war

# Copy configuration files
COPY src/main/resources/application.properties /usr/local/tomcat/conf/payment-gateway/

# Environment variables (overridable at runtime)
ENV JAVA_OPTS="-Xms512m -Xmx1024m -Denv=PROD"
ENV CATALINA_OPTS="-Djava.security.egd=file:/dev/./urandom"

# Expose Tomcat port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/health || exit 1

# Labels
LABEL maintainer="MegaBank DevOps Team" \
      version="2.3.1" \
      description="MegaBank Payment Gateway - Legacy Payment Processing System" \
      security.warning="This image contains intentional vulnerabilities for demonstration purposes"

# Start Tomcat
CMD ["catalina.sh", "run"]
