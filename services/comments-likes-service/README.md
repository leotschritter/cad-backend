# Comments & Likes Service

This project uses Quarkus, the Supersonic Subatomic Java Framework, and integrates with Google Cloud Firestore and Firebase Authentication.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Prerequisites

- Java 21
- Maven 3.8+
- (Optional) Docker for running Firestore emulator

## Configuration

The application is configured to work out-of-the-box for local development with the following defaults:

- **Project ID**: `local-dev-project` (can be overridden with `PROJECT_ID` env var)
- **Firestore Emulator**: Enabled by default (can be disabled with `USE_FIRESTORE_EMULATOR=false`)
- **Authentication**: Disabled by default (can be enabled with `IDENTITY_PLATFORM_AUTH_ENABLED=true`)

### Environment Variables

You can customize the configuration by setting environment variables. See `.env.example` for all available options.

For local development, you can either:
1. Set environment variables in your shell
2. Create a `.env` file (copy from `.env.example`)
3. Use the default values (recommended for quick start)

### Running with Firestore Emulator (Recommended for Development)

If you want to use the Firestore emulator locally:

1. Install Firebase CLI: `npm install -g firebase-tools`
2. Start the emulator: `firebase emulators:start --only firestore`
3. The application is configured to use the emulator by default

### Running with Real Google Cloud Services

To connect to actual Google Cloud services:

1. Set up Google Cloud credentials:
   - Install Google Cloud SDK
   - Run `gcloud auth application-default login`
2. Set environment variables:
   ```bash
   set PROJECT_ID=your-actual-project-id
   set USE_FIRESTORE_EMULATOR=false
   set IDENTITY_PLATFORM_AUTH_ENABLED=true
   ```

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/code-with-quarkus-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Related Guides

- REST ([guide](https://quarkus.io/guides/rest)): A Jakarta REST implementation utilizing build time processing and Vert.x. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it.
- REST Jackson ([guide](https://quarkus.io/guides/rest#json-serialisation)): Jackson serialization support for Quarkus REST. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it
- Hibernate ORM with Panache ([guide](https://quarkus.io/guides/hibernate-orm-panache)): Simplify your persistence code for Hibernate ORM via the active record or the repository pattern
- JDBC Driver - PostgreSQL ([guide](https://quarkus.io/guides/datasource)): Connect to the PostgreSQL database via JDBC

## Provided Code

### Hibernate ORM

Create your first JPA entity

[Related guide section...](https://quarkus.io/guides/hibernate-orm)

[Related Hibernate with Panache section...](https://quarkus.io/guides/hibernate-orm-panache)


### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)
