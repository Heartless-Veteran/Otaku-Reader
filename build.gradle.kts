// Top-level build file — configuration common to all subprojects is in build-logic/convention plugins.
buildscript {
    repositories {
        maven("https://maven.google.com")
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:9.1.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.20")
        classpath("org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.3.20")
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.59.2")
        classpath("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.3.6")
        classpath("androidx.room:room-gradle-plugin:2.8.4")
    }
}

// Security: Force secure versions of transitive dependencies
allprojects {
    configurations.all {
        resolutionStrategy {
            // Netty - fixes HTTP/2 Rapid Reset, SSL crash, DoS (CVSS 7.5)
            force("io.netty:netty-common:4.1.104.Final")
            force("io.netty:netty-buffer:4.1.104.Final")
            force("io.netty:netty-transport:4.1.104.Final")
            force("io.netty:netty-codec:4.1.104.Final")
            force("io.netty:netty-codec-http:4.1.104.Final")
            force("io.netty:netty-codec-http2:4.1.104.Final")
            force("io.netty:netty-handler:4.1.104.Final")

            // JDOM2 - fixes XXE injection (CVSS 7.5)
            force("org.jdom:jdom2:2.0.6.1")

            // jose4j - fixes DoS via compressed JWE (CVSS 7.5)
            force("org.bitbucket.b_c:jose4j:0.9.3")

            // Apache Commons Lang - fixes uncontrolled recursion
            force("org.apache.commons:commons-lang3:3.14.0")

            // Apache HttpClient - fixes XSS
            force("org.apache.httpcomponents:httpclient:4.5.14")
        }
    }
}
