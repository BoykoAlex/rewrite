![Logo](https://github.com/openrewrite/rewrite/raw/master/doc/logo-oss.png)
# Semantic code search and transformation

[![Build Status](https://circleci.com/gh/openrewrite/rewrite.svg?style=shield)](https://circleci.com/gh/openrewrite/rewrite)
[![Apache 2.0](https://img.shields.io/github/license/openrewrite/rewrite.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/org.openrewrite/rewrite-java.svg)](https://mvnrepository.com/artifact/org.openrewrite/rewrite-java)

The Rewrite project is a mass refactoring ecosystem for Java and other source code, designed to eliminate technical debt across an engineering organization. It consists of a platform of prepackaged refactoring recipes for common framework migration and stylistic consistency tasks in Java, ready for you to apply in your build via Maven or Gradle plugins.

Read the full documentation at [docs.openrewrite.org](https://docs.openrewrite.org/).

Feel free to join us on [Slack](https://join.slack.com/t/rewriteoss/shared_invite/zt-i2ouq1qd-KdkTZKr5jmNtooqfc4ITAQ)!

## Building & Developing Openrewrite

We use [Gradle](https://gradle.org/) to build this project.
The gradle wrapper checked into this project defines the gradle version to use.  
When building from the command line invoke the wrapper with `./gradlew build` on unix-style terminals and `gradlew build` on windows-style terminals.
 
### CLI Environment Configuration:

* [JDK](https://adoptopenjdk.net/) version: 11
* JDK language & bytecode level: 1.8
* [Gradle](https://gradle.org/) version: Defined in wrapper
* [Kotlin](https://kotlinlang.org/) version: 1.4
* Kotlin language level: 1.4
* Kotlin JVM bytecode level: 1.8 

### IDE Configuration

We use [IntellJ IDEA](https://www.jetbrains.com/idea/) to develop this project. 
Other IDEs or versions of this IDE can be made to work. 
These are one set of versions we know works:

* IDEA version:  2020.2
* [Kotlin plugin](https://plugins.jetbrains.com/plugin/6954-kotlin): 1.4.10-release-IJ2020.2-1
* [Lombok plugin](https://plugins.jetbrains.com/plugin/6317-lombok): 0.32-2020.2
