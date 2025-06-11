# Dynamic DTO Mapper/Parser

![Build](https://github.com/zaidathar/dynamic-parser/actions/workflows/build.yml/badge.svg)
![Tests](https://github.com/zaidathar/dynamic-parser/actions/workflows/test.yml/badge.svg)
![Docker](https://github.com/zaidathar/dynamic-parser/actions/workflows/docker.yml/badge.svg)
[![codecov](https://codecov.io/gh/zaidathar/dynamic-parser/branch/main/graph/badge.svg)](https://codecov.io/gh/zaidathar/dynamic-parser)


## 🚀 Overview

The `dynamic-parser` is a robust Spring Boot microservice designed to provide highly flexible and dynamic data transformation capabilities. It solves the common problem of rigid data mapping by externalizing transformation rules into YAML configurations. This allows for real-time adaptation to changing data structures and business logic without requiring application downtime or redeployment.

Leveraging a sophisticated hot-reloading mechanism, `dynamic-parser` can update its mapping rules on the fly by listening to changes in a remote GitHub repository via secure webhooks, enhancing operational efficiency and accelerating the iteration cycle for data integration tasks.

## ✨ Features

* **Config-Driven DTO Mapping (YAML-based):** Define complex data transformations using intuitive YAML syntax.
* **Zero-Downtime Config Hot Reload:**
    * Automatically refreshes mapping configurations from a remote Git repository (e.g., GitHub).
    * Triggered via secure webhook calls from GitHub Actions on config file changes.
    * Includes **webhook signature verification** for enhanced security.
    * Implements **IP-based Rate Limiting for GitHub Webhooks**: Ensures robust operation by limiting incoming webhook requests from GitHub's IPs to `1 request/minute`, `5 requests/hour`, and `20 requests/day`.
* **Advanced Transformation Logic:**
    * **JavaScript Expression Support:** Embed JavaScript expressions directly within YAML for dynamic data manipulation.
    * **Custom Java Transformation Functions:** Register custom Java methods to handle complex, reusable transformation logic.
* **REST API for Dynamic Parsing:** A simple and intuitive REST endpoint for submitting data for transformation.
* **Robustness & Quality:**
    * Comprehensive Unit Tests with JUnit.
    * Dockerized for seamless deployment and portability.
    * Integrated GitHub Actions for CI/CD, including automated builds, tests, and SonarQube scans.

---

## Running application 

### Locally 
```bash
./mvnw clean install
./mvnw spring-boot:run

```

### Docker 

```bash
docker build -t dynamic-parser .
docker run -p 8080:8080 dynamic-parser
  
```

## Test & Coverage

```bash
./mvnw test
```

## API Usage

### Endpoint 
```bash
POST /api/parse
```

### Request Body
```json
{
  "name": "John",
  "age": 32,
  "profile": "https://someurl.com/123.jpeg",
  "contact": "090910"
}

```

### Response

```json
{
  "fullName": "John",
  "userAge": 32,
  "profilePic": "https://someurl.com/123.jpeg",
  "contact": "090910",
  "ageInMonths": 11680
}

```
