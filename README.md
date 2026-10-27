# SiMu

SiMu is a music streaming project I started while learning Spring Boot.

It was my first larger Spring application: I wrote the backend, an early simple UI, and the music upload/playback flow. Later I worked on the project together with a frontend developer, so the repository also contains a React frontend.

The project was never deployed as a public service, but it was useful for learning how a real application fits together beyond a basic CRUD API.

## Backend features

- user registration and authentication;
- JWT-based security;
- music upload and download/streaming;
- playlists;
- user and music management;
- S3-compatible object storage for audio files;
- MySQL persistence;
- Swagger/OpenAPI documentation.

## Repository structure

- `SiMu-backend/` — Spring Boot backend
- `Frontend/` — React frontend from the later stage of the project

## Stack

Backend: Java 21, Spring Boot, Spring Security, Spring Data JPA, MySQL, JWT, S3 API, MapStruct, Gradle and Docker.

Frontend: React, TypeScript and Vite.

## Running the backend

Create an environment file from the example:

```bash
cp .env.example .env
```

Fill in the database and S3 credentials, then run:

```bash
cd SiMu-backend
docker compose up --build
```

Swagger UI is available at:

```
http://localhost:8081/swagger-ui.html
```

## Project status

Archived. This is an older project and some implementation choices reflect the stage I was at when I built it. I keep it here because it was the project where I first worked with Spring Security, JWT, object storage and a larger backend structure.
