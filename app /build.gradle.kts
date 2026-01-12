import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Properties
import com.android.build.api.attributes.BuildTypeAttr
import org.gradle.api.attributes.Attribute


plugins {
    alias(libs.plugins.ksp)
    id("com.android.application")
    id("kotlin-android")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("android-app-dependencies")
    id("test-app-dependencies")
    id("jacoco-app-dependencies")
}

repositories {
    mavenCentral()
    google()
}

fun generateGitBuild(): String {
    try {
        val processBuilder = ProcessBuilder("git", "describe", "--always")
        val output = File.createTempFile("git-build", "")
        processBuilder.redirectOutput(output)
        val process = processBuilder.start()
        process.waitFor()
        return output.readText().trim()
    } catch (_: Exception) {
        return "NoGitSystemAvailable"
    }
}

fun generateGitRemote(): String {
    try {
        val processBuilder = ProcessBuilder("git", "remote", "get-url", "origin")
        val output = File.createTempFile("git-remote", "")
        processBuilder.redirectOutput(output)
        val process = processBuilder.start()
        process.waitFor()
        return output.readText().trim()
    } catch (_: Exception) {
        return "NoGitSystemAvailable"
    }
}

fun generateDate(): String {
    val stringBuilder: StringBuilder = StringBuilder()
    // showing only date prevents app to rebuild everytime
    stringBuilder.append(SimpleDateFormat("yyyy.MM.dd").format(Date()))
    return stringBuilder.toString()
}

fun isMaster(): Boolean = !Versions.appVersion.contains("-")

fun gitAvailable(): Boolean {
    try {
        val processBuilder = ProcessBuilder("git", "--version")
        val output = File.createTempFile("git-version", "")
        processBuilder.redirectOutput(output)
        val process = processBuilder.start()
        process.waitFor()
        return output.readText().isNotEmpty()
    } catch (_: Exception) {
        return false
    }
}

fun allCommitted(): Boolean {
    try {
        val processBuilder = ProcessBuilder("git", "status", "-s")
        val output = File.createTempFile("git-comited", "")
        processBuilder.redirectOutput(output)
        val process = processBuilder.start()
        process.waitFor()
        return output.readText().replace(Regex("""(?m)^\s*(M|A|D|\?\?)\s*.*?\.idea\/codeStyles\/.*?\s*$"""), "")
            // ignore all files added to project dir but not staged/known to GIT
            .replace(Regex("""(?m)^\s*(\?\?)\s*.*?\s*$"""), "").trim().isEmpty()
    } catch (_: Exception) {
        return false
    }
}

val keyProps = Properties()
val keyPropsFile: File = rootProject.file("keystore/keystore.properties")
keyProps.load(FileInputStream(keyPropsFile))
fun getStoreFile(): String {
    var storeFile = keyProps["storeFile"].toString()
    if (storeFile.isEmpty()) {
        storeFile = System.getenv("storeFile") ?: ""
    }
    return storeFile
}

fun getStorePassword(): String {
    var storePassword = keyProps["storePassword"].toString()
    if (storePassword.isEmpty()) {
        storePassword = System.getenv("storePassword") ?: ""
    }
    return storePassword
}

fun getKeyAlias(): String {
    var keyAlias = keyProps["keyAlias"].toString()
    if (keyAlias.isEmpty()) {
        keyAlias = System.getenv("keyAlias") ?: ""
    }
    return keyAlias
}

fun getKeyPassword(): String {
    var keyPassword = keyProps["keyPassword"].toString()
    if (keyPassword.isEmpty()) {
        keyPassword = System.getenv("keyPassword") ?: ""
    }
    return keyPassword
}


android {

    namespace = "app.aaps"
    ndkVersion = Versions.ndkVersion

    defaultConfig {
        minSdk = Versions.minSdk
        targetSdk = Versions.targetSdk

        missingDimensionStrategy("standard", "full") // 👈 强制未声明 flavor 的模块使用 "full"

        buildConfigField("String", "VERSION", "\"$version\"")
        buildConfigField("String", "BUILDVERSION", "\"${generateGitBuild()}-${generateDate()}\"")
        buildConfigField("String", "REMOTE", "\"${generateGitRemote()}\"")
        buildConfigField("String", "HEAD", "\"${generateGitBuild()}\"")
        buildConfigField("String", "COMMITTED", "\"${allCommitted()}\"")

        // For Dagger injected instrumentation tests in app module
        testInstrumentationRunner = "app.aaps.runners.InjectedTestRunner"
    }

    // 👇 添加这一段 👇
    buildTypes {
        debug { /* ... */ }
        release { /* ... */ }
    }

    flavorDimensions.add("standard")
    productFlavors {
        create("full") {
            isDefault = true
            applicationId = "info.nightscout.androidaps"
            dimension = "standard"
            resValue("string", "app_name", "AAPS")
            versionName = Versions.appVersion
            manifestPlaceholders["appIcon"] = "@mipmap/ic_launcher"
            manifestPlaceholders["appIconRound"] = "@mipmap/ic_launcher_round"
        }
        create("pumpcontrol") {
            applicationId = "info.nightscout.aapspumpcontrol"
            dimension = "standard"
            resValue("string", "app_name", "Pumpcontrol")
            versionName = Versions.appVersion + "-pumpcontrol"
            manifestPlaceholders["appIcon"] = "@mipmap/ic_pumpcontrol"
            manifestPlaceholders["appIconRound"] = "@null"
        }
        create("aapsclient") {
            applicationId = "info.nightscout.aapsclient"
            dimension = "standard"
            resValue("string", "app_name", "AAPSClient")
            versionName = Versions.appVersion + "-aapsclient"
            manifestPlaceholders["appIcon"] = "@mipmap/ic_yellowowl"
            manifestPlaceholders["appIconRound"] = "@mipmap/ic_yellowowl"
        }
        create("aapsclient2") {
            applicationId = "info.nightscout.aapsclient2"
            dimension = "standard"
            resValue("string", "app_name", "AAPSClient2")
            versionName = Versions.appVersion + "-aapsclient"
            manifestPlaceholders["appIcon"] = "@mipmap/ic_blueowl"
            manifestPlaceholders["appIconRound"] = "@mipmap/ic_blueowl"
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file(getStoreFile())
            storePassword = getStorePassword()
            keyAlias = getKeyAlias()
            keyPassword = getKeyPassword()
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
        }

    }

    useLibrary("org.apache.http.legacy")

    //Deleting it causes a binding error
    buildFeatures {
        dataBinding = true
        buildConfig = true
        aidl = true  // ⭐⭐⭐ 需要添加这个以支持 AIDL ⭐⭐⭐
    }

    

}

allprojects {
    repositories {
    }
}


// Fix variant ambiguity for KSP processor classpath
configurations.configureEach {
    if (name.contains("FullRelease") && name.contains("KotlinProcessorClasspath")) {
        attributes.attribute(
            BuildTypeAttr.ATTRIBUTE,
            objects.named(BuildTypeAttr::class.java, "release")
        )
        // 使用字符串定义 attribute，避免 internal API
        attributes.attribute(
            Attribute.of("artifactType", String::class.java),
            "jar"
        )
    }
}

dependencies {
    // in order to use internet"s versions you"d need to enable Jetifier again
    // https://github.com/nightscout/graphview.git
    // https://github.com/nightscout/iconify.git
    api(project(":shared:impl"))
    api(project(":core:data")) 
    api(project(":core:objects")) 
    api(project(":core:graph")) 
    api(project(":core:graphview"))
    api(project(":core:interfaces"))
    api(project(":core:keys"))
    api(project(":core:libraries"))
    api(project(":core:nssdk")) 
    api(project(":core:utils"))
    api(project(":core:ui"))
    api(project(":core:validators"))
    api(project(":ui"))
    implementation(project(":plugins:aps"))
    implementation(project(":plugins:automation"))
    implementation(project(":plugins:configuration"))
    implementation(project(":plugins:constraints"))
    implementation(project(":plugins:insulin"))
    implementation(project(":plugins:main"))
    implementation(project(":plugins:sensitivity"))
    implementation(project(":plugins:smoothing"))
    implementation(project(":plugins:source")) {
        attributes {
            attribute(Attribute.of("com.android.build.api.attributes.ProductFlavor", String::class.java), "full")
        }
    }
    
    implementation(project(":plugins:sync"))
    implementation(project(":implementation"))
    implementation(project(":database:impl"))
    implementation(project(":database:persistence"))
    implementation(project(":pump:combov2"))
    implementation(project(":pump:dana"))
    implementation(project(":pump:danars"))
    implementation(project(":pump:danar"))
    implementation(project(":pump:diaconn"))
    implementation(project(":pump:eopatch"))
    implementation(project(":pump:medtrum"))
    implementation(project(":pump:equil"))
    implementation(project(":pump:insight"))
    implementation(project(":pump:medtronic"))
    implementation(project(":pump:pump-common"))
    implementation(project(":pump:omnipod-common"))
    implementation(project(":pump:omnipod-eros"))
    implementation(project(":pump:omnipod-dash"))
    implementation(project(":pump:rileylink"))
    implementation(project(":pump:virtual"))
    implementation(project(":workflow"))

    testImplementation(project(":shared:tests"))
    androidTestImplementation(project(":shared:tests"))
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.org.skyscreamer.jsonassert)


    kspAndroidTest(libs.com.google.dagger.android.processor)

    /* Dagger2 - We are going to use dagger.android which includes
     * support for Activity and fragment injection so we need to include
     * the following dependencies */
    ksp(libs.com.google.dagger.android.processor)
    ksp(libs.com.google.dagger.compiler)

    // ⭐⭐⭐ 简化：直接使用 ksp，让 resolutionStrategy 处理 ⭐⭐⭐
    ksp(project(":plugins:source"))

    // MainApp
    api(libs.com.uber.rxdogtag2.rxdogtag)
    // Remote config
    api(libs.com.google.firebase.config)
}

afterEvaluate {
    // ✅ 修正 1: 遍历 configurations（可选，通常不需要）
    configurations.configureEach {
        if (name.contains("fullRelease", ignoreCase = true)) {
            println("Configuring configuration: $name")
        }
    }

    // ✅ 修正 2: 遍历 tasks —— 使用无参 lambda，用 'this.name'
    tasks.configureEach {
        if (name.contains("fullRelease", ignoreCase = true)) {
            doFirst {
                println("Building task: $name")
            }
        }
    }
}


println("-------------------")
println("isMaster: ${isMaster()}")
println("gitAvailable: ${gitAvailable()}")
println("allCommitted: ${allCommitted()}")
println("-------------------")
if (!gitAvailable()) {
    throw GradleException("GIT system is not available. On Windows try to run Android Studio as an Administrator. Check if GIT is installed and Studio have permissions to use it")
}
if (isMaster() && !allCommitted()) {
    throw GradleException("There are uncommitted changes. Clone sources again as described in wiki and do not allow gradle update")
}
