// Local plain-HTTP Maven mirror (this sandbox only):
// Gradle/JVM TLS-through-proxy is unreliable in this VM, so when
// USE_LOCAL_MIRROR=1 the build uses a loopback mirror (see
// ~/workspace/tools/maven-mirror.py) that fetches via curl and serves
// artifacts over plain HTTP. Default (no env var) = standard repos,
// so the project still builds normally in Android Studio.
pluginManagement {
    repositories {
        if (System.getenv("USE_LOCAL_MIRROR") == "1") {
            maven { url = uri("http://127.0.0.1:8891/google"); isAllowInsecureProtocol = true }
            maven { url = uri("http://127.0.0.1:8891/plugins"); isAllowInsecureProtocol = true }
            maven { url = uri("http://127.0.0.1:8891/central"); isAllowInsecureProtocol = true }
            maven { url = uri("http://127.0.0.1:8891/mozilla"); isAllowInsecureProtocol = true }
        } else {
            google()
            mavenCentral()
            gradlePluginPortal()
            maven("https://maven.mozilla.org/maven2/")
        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        if (System.getenv("USE_LOCAL_MIRROR") == "1") {
            maven { url = uri("http://127.0.0.1:8891/google"); isAllowInsecureProtocol = true }
            maven { url = uri("http://127.0.0.1:8891/central"); isAllowInsecureProtocol = true }
            maven { url = uri("http://127.0.0.1:8891/plugins"); isAllowInsecureProtocol = true }
            maven { url = uri("http://127.0.0.1:8891/mozilla"); isAllowInsecureProtocol = true }
        } else {
            google()
            mavenCentral()
            maven("https://maven.mozilla.org/maven2/")
        }
    }
}
rootProject.name = "PureBrowser"
include(":app")
