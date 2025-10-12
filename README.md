When using Gradle, there is [gradle-conventions](https://github.com/Forsakringskassan/gradle-conventions).

When using Maven, there is [fk-maven](https://github.com/Forsakringskassan/fk-maven).

This repository is now archived.

---------


# FK Code Standard

Defines the code standard used within Swedish Social Insurance Agency (a.k.a. FK) for java-projects. The format that is applied is opinionated, it is intentionally not configurable.

It uses [Spotless](https://github.com/diffplug/spotless) and configures its Eclipse formatter with [the formatting](/code-standard-java-eclipse.xml).

There are, currently, no published releases of this project. The value here, for anyone outside of FK, is a template to use for defining your opinionated code standard within your organization.

This plugin is included if you use the [fk-maven](https://github.com/Forsakringskassan/fk-maven) parent pom.

Published to https://github.com/Forsakringskassan/repository

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

## Eclipse update site

Many organizations will probably block `download.eclipse.org`. Spotless has a feature that lets you rewrite those calls to something that is reachable internally in your organization. Search for `p2Mirror` in this repo and you will find what to change.

Still, it might be hard to setup an internal Eclipse update site and get these files. You can get the files by running Wiremock somewhere with full Internet access:

```sh
npx wiremock \
 --root-dir eclipsedownload \
 --record-mappings \
 --proxy-all https://download.eclipse.org/eclipse/ \
 --verbose
```

And changing `p2Mirror` like:

```sh
eclipse.withP2Mirrors(Map.of(
"https://download.eclipse.org/eclipse/updates/" + eclipseVersion + "/",
"http://localhost:8080/updates/"+eclipseVersion+"/"));
```

Run the `build-and-test.sh` script to make it invoke Wiremock.

After that you will have the Wiremock mappings in folder `eclipsedownload`. Bring that folder inside your organization. Run in there with:

```sh
npx wiremock --verbose
```

Now you have a mocked update site on `localhost` and you should be able to run it without access to `download.eclipse.org` by changing the `p2Mirror` like above. Not a permanent solution, but will help you try it out and collect the files you need to host internally.
