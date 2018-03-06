# Java Runtime for Anthill Platform

This Runtime allows you to have all of the 
<a href="https://github.com/anthill-services/anthill">Anthill Platform</a> 
functionality inside of your Java-based game (for example, using <a href="https://github.com/libgdx/libgdx">libGDX</a>).

## Installation

#### Gradle

1. Add the JitPack repository to your `build.gradle` file

```
allprojects {
    repositories {
        ...
        maven { url "https://jitpack.io" }
    }
}
```

2. Add the dependency:

```
dependencies {
    compile 'com.github.anthill-services:anthill-runtime-java:0.1.4'
}
```

#### Maven

1. Add the JitPack repository to your `pom.xml` file

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

2. Add the dependency:

```xml
<dependency>
    <groupId>com.github.anthill-services</groupId>
    <artifactId>anthill-runtime-java</artifactId>
    <version>0.1.4</version>
</dependency>
```