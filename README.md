# Dynamic DTO Mapper/Parser

![Build](https://github.com/zaidathar/dynamic-parser/actions/workflows/build.yml/badge.svg)
![Tests](https://github.com/zaidathar/dynamic-parser/actions/workflows/test.yml/badge.svg)
![Docker](https://github.com/zaidathar/dynamic-parser/actions/workflows/docker.yml/badge.svg)
[![codecov](https://codecov.io/gh/zaidathar/dynamic-parser/branch/main/graph/badge.svg)](https://codecov.io/gh/zaidathar/dynamic-parser)


## Overview

A Spring Boot based dynamic DTO parser service that maps data from one DTO to another using YAML-driven configurations. Supports transformation logic using JavaScript expressions and registered Java transformation methods.

## Features

- Config-driven DTO to DTO mapping (YAML based)
- No redeployment needed for mapping changes
- JavaScript expression support
- Custom transformation functions using Java
- REST API for dynamic parsing
- Unit-tested with JUnit
- Dockerized for deployment
- GitHub Actions for CI/CD and SonarQube scan


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
  "tempInCelsius": 26.85,
  "ageInDays": 11680
}

```