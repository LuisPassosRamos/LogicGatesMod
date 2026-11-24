plugins {
    java
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

sourceSets {
    main {
        java.setSrcDirs(listOf("../../src/main/java"))
        java.include("sisso/logicsim/circuit/**")
    }
    test {
        java.setSrcDirs(listOf("../../src/test/java"))
        java.include("sisso/logicsim/circuit/**")
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
}

tasks.test {
    useJUnitPlatform()
}
