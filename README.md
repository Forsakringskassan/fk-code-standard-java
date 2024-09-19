# FK Code Standard

Defines the code standard used within Swedish Social Insurance Agency (a.k.a. FK) for java-projects. The format that is applied is opinionated, it is intentionally not configurable.

It uses [Spotless](https://github.com/diffplug/spotless) and configures its Eclipse formatter with [the formatting](/code-standard-java-eclipse.xml).

There are, currently, no published releases of this project. The value here, for anyone outside of FK, is a template to use for defining your opinionated code standard within your organization.

## Gradle

There is a running example [in this repo](/fk-code-standard-gradle-plugin-example). The plugin can be applied like this:

```groovy
buildscript {
  dependencies {
      classpath 'se.fk.codestandard:fk-code-standard-gradle-plugin:X'
  }
}

apply plugin: "se.fk.codestandard.gradle.FKCodeStandard"
```

It will run during `./gradlew build` but can also be invoked like:

```sh
./gradlew spotlessApply
```

```sh
./gradlew spotlessCheck
```

In a CI environment, where you may want to verify the code standard, the automatic formatting can be turned off with:

```sh
./gradlew build -Pskip-automatic-fk-code-standard-apply=true
```

## Maven

There is a running example [in this repo](/fk-code-standard-maven-plugin-example). The plugin can be applied like this in `pom.xml`:

```xml
<build>
 <plugins>
  <plugin>
   <groupId>se.fk.codestandard</groupId>
   <artifactId>fk-code-standard-maven-plugin</artifactId>
   <version>${fk-code-standard.version}</version>
   <executions>
    <execution>
     <goals>
      <goal>spotlessCheck</goal>
      <goal>spotlessApply</goal>
     </goals>
    </execution>
   </executions>
  </plugin>
 </plugins>
</build>
```

It will run during `./mvnw package` but can also be invoked like:

```sh
./mvnw se.fk.codestandard:fk-code-standard-maven-plugin:spotlessApply
```

```sh
./mvnw se.fk.codestandard:fk-code-standard-maven-plugin:spotlessCheck
```

In a CI environment, where you may want to verify the code standard, the automatic formatting can be turned off with:

```sh
./mvnw verify -Pskip-automatic-fk-code-standard-apply=true
```
