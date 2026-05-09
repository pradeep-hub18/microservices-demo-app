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
mvn -f auth-service/pom.xml clean verify
mvn -f catalog-service/pom.xml clean verify
```

Artifacts:

```text
auth-service/target/auth-service-1.0.0.jar
catalog-service/target/catalog-service-1.0.0.jar
```

JaCoCo coverage reports are generated at:

```text
auth-service/target/site/jacoco/index.html
catalog-service/target/site/jacoco/index.html
```

## Jenkins Pipeline

The root `Jenkinsfile` builds both services from this single repository.

Pipeline flow:

```text
SCM checkout
Maven unit tests and JaCoCo reports
80% line coverage gate
SonarQube scan and quality gate
Trivy source/filesystem scan
Docker image build
Trivy image scan
ECR login, tag, and push
Optional EKS deployment by updating existing Kubernetes Deployments
```

Jenkins agent requirements:

```text
Java 17+
Maven
Docker
AWS CLI
kubectl
Trivy
SonarQube Scanner tool configured in Jenkins
```

Recommended Jenkins plugins:

```text
Pipeline
Git
JUnit
Email Extension
SonarQube Scanner for Jenkins
AWS Credentials
Credentials Binding
Workspace Cleanup
```

Important Jenkins parameters:

```text
AWS_ACCOUNT_ID          Default: 152406482015
AWS_REGION              Default: ap-south-1
AUTH_ECR_REPOSITORY     Default: microapps/auth-service
CATALOG_ECR_REPOSITORY  Default: microapps/catalog-service
QUALITY_THRESHOLD       Default: 80
ALERT_EMAIL             Email for quality gate and failure alerts
AWS_CREDENTIALS_ID      Default: aws-ecr-eks-creds
DEPLOY_TO_EKS           Set true only when Kubernetes Deployments already exist
EKS_CLUSTER_NAME        EKS cluster used for kubectl rollout
K8S_NAMESPACE           Namespace containing both Deployments
```

The pipeline can be run with the defaults. Leave `ALERT_EMAIL` empty when SMTP is not configured.

For SonarQube, create a quality gate in SonarQube with at least 80% coverage, then set `SONARQUBE_SERVER` and `SONAR_SCANNER_TOOL` to the names configured in Jenkins.

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
