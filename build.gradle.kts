plugins { java }

group = "dev.artisan"
version = "0.1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:${property("paperApi")}")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(property("javaVersion").toString().toInt()))
}

tasks.withType<JavaCompile> { options.encoding = "UTF-8" }
