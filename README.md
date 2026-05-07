# Microservices Demo App

Two independent Java Spring Boot microservices with plain JavaScript UIs.

- `auth-service`: login UI, PostgreSQL user store, BCrypt password checks, JWT creation, token validation API.
- `catalog-service`: catalog UI, demo product images, protected catalog API, service-to-service auth check against `auth-service`.

## Default Login

```text
username: admin
password: Password123!
```

The default user is created by `auth-service` on startup if it does not exist.

## Build JARs

```sh
mvn -f auth-service/pom.xml clean package
mvn -f catalog-service/pom.xml clean package
```

Artifacts:

```text
auth-service/target/auth-service-1.0.0.jar
catalog-service/target/catalog-service-1.0.0.jar
```

## Build Images

Replace the registry prefix with your Docker Hub, ECR, or private registry path.

```sh
docker build -t replace-with-registry/auth-service:latest auth-service
docker build -t replace-with-registry/catalog-service:latest catalog-service
```

## Local Browser URLs

When you decide how these services should be deployed, expose or port-forward them so the browser can reach:

```text
http://localhost:8081
```

After login, the UI redirects to:

```text
http://localhost:8082
```

## Service Communication

The browser talks to the catalog service with a JWT:

```text
Authorization: Bearer <token>
```

The catalog service validates that token by calling:

```text
http://auth-service.microapps.svc.cluster.local:8080/api/auth/validate
```

That makes the services independent, while still requiring service-to-service communication for authorization.

Deployment manifests are intentionally not included yet. Add them later using the deployment approach you choose.
