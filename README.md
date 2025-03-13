# Software Engineering 2025 Spring Coursework Group 95

## How to build

**Note: Although the source code is portable, the built artifacts are not. If you build in on Linux, your executable will not run on Windows, and vice versa. We may fix this in the future, but not early stages of development.**

Require JDK 21 or later. If you have multiple JDKs installed, you can set the `PATH` environment variable to the JDK you want to use. For example,

```bash
PATH=/usr/lib/jvm/java-21-openjdk/bin:$PATH java -version
```

Run the following command in the root directory of the project:

```bash
./gradlew build
```

The built artifacts will be in the `app/build` directory. If you want to distribute the application, you can use the `app/build/distributions/app.zip` directory. Unzipping it will give you two directory: `bin` and `lib`. Use the `app` script in the `bin` directory to run the application.

## How to run

```bash
./gradlew run
```

## How to test

```bash
./gradlew test
```