import com.android.build.api.attributes.ProductFlavorAttr
import com.android.build.api.variant.LibraryAndroidComponentsExtension

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.dagger.hilt)
    id("kotlin-android")
    id("android-module-dependencies")
    id("test-module-dependencies")
    id("jacoco-module-dependencies")
}

androidComponents {
    beforeVariants { variant ->
        if (variant.name !in listOf("fullDebug", "fullRelease")) {
            variant.enable = false  // ✅ 修复：使用新的 enable API
        }
    }
}

android {
    namespace = "app.aaps.plugins.source"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
        // targetSdk = 35  // ❌ 已移除（在library模块中已弃用）
    }

    // ✅ 替代方案：如果需要设置targetSdk，可以在这里设置
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        aidl = true
        buildConfig = true
    }
}

ksp {
    arg("room.incremental", "true")
    arg("ksp.incremental", "true")
}

// 👇 修正后的完整代码块
// 👇 替代方案：使用工厂方法创建 ProductFlavorAttr
configurations.configureEach {
    if (isCanBeResolved && name.lowercase().contains("ksp") && name.contains("Full")) {
        attributes {
            // 创建一个 ProductFlavorAttr 实例
            val fullFlavor = project.objects.named(com.android.build.api.attributes.ProductFlavorAttr::class.java, "full")
            val standardAttr = com.android.build.api.attributes.ProductFlavorAttr.of("standard")
            
            attribute(standardAttr, fullFlavor)
            attribute(Attribute.of("artifactType", String::class.java), "jar")
        }
    }
}
dependencies {
    // Core 子模块
    api(project(":core:data")) 
    api(project(":core:graph"))
    api(project(":core:graphview"))
    api(project(":core:interfaces"))
    api(project(":core:keys"))
    api(project(":core:libraries")) 
    api(project(":core:nssdk")) 
    api(project(":core:objects"))
    api(project(":core:utils")) 
    api(project(":core:ui"))
    api(project(":core:validators")) 

    // Shared 子模块
    api(project(":shared:impl")) 

    // 其他插件
    implementation(project(":plugins:main"))

    implementation("com.jakewharton.timber:timber:5.0.1")

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Tests
    testImplementation(libs.androidx.work.testing)
    testImplementation(project(":shared:tests"))
}
