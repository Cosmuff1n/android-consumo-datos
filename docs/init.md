# Documentación de Inicialización del Proyecto: Librerías Base

Este documento registra los cambios, configuraciones e instalación de librerías bases para la persistencia local, programación asíncrona, consumo de APIs HTTP e inyección de dependencias en la aplicación **ConsumoDatos**.

---

## 🚀 Resumen de Acciones Realizadas

1. **Instalación de Dependencias**:
   - **Room Database** (v2.6.1): Persistencia de datos local en SQLite.
   - **Kotlin Coroutines** (v1.10.1): Manejo asíncrono y reactivo (`suspend`, `Flow`).
   - **Retrofit** (v2.11.0): Abstracción e interfaz client REST HTTP.
   - **OkHttp** (v4.12.0) & **HttpLoggingInterceptor**: Cliente HTTP y logging de peticiones/respuestas.
   - **Hilt / Dagger** (v2.55 & Navigation Compose v1.2.0): Inversión e Inyección de Dependencias.
   - **Google KSP** (v2.1.0-1.0.29): Procesador de anotaciones para Room y Hilt en Kotlin.

2. **Configuración del Entorno y Build**:
   - Actualización del Catálogo de Versiones en `gradle/libs.versions.toml`.
   - Inclusión de las librerías en `build.gradle.kts` (raíz y módulo `:app`).
   - Adición del permiso `android.permission.INTERNET` en `AndroidManifest.xml`.
   - Ajuste de compatibilidad en `gradle.properties` (`android.disallowKotlinSourceSets=false`).
   - Confirmación de compilación del proyecto (`BUILD SUCCESSFUL`).

---

## 📦 Detalle de Archivos Modificados

### 1. `gradle/libs.versions.toml`
Se definieron las versiones y librerías base dentro del Version Catalog:

```toml
[versions]
agp = "9.4.1"
coreKtx = "1.19.1"
junit = "4.13.2"
junitVersion = "1.3.0"
espressoCore = "3.7.0"
lifecycleRuntimeKtx = "2.11.0"
activityCompose = "1.13.0"
kotlin = "2.1.0"
composeBom = "2026.02.01"

ksp = "2.1.0-1.0.29"
room = "2.6.1"
coroutines = "1.10.1"
retrofit = "2.11.0"
okhttp = "4.12.0"
hilt = "2.55"
hiltNavigationCompose = "1.2.0"

[libraries]
# Room
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

# Coroutines
kotlinx-coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }

# Retrofit
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-converter-gson = { group = "com.squareup.retrofit2", name = "converter-gson", version.ref = "retrofit" }

# OkHttp
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-logging-interceptor = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }

# Hilt (Inversión / Inyección de Dependencias)
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigationCompose" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

### 2. `build.gradle.kts` (Raíz)
```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
```

### 3. `app/build.gradle.kts`
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.rojas.consumodatos"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.rojas.consumodatos"
        minSdk = 27
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // ...
}

dependencies {
    // AndroidX & Compose Base
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Corrutinas
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Room (Persistencia local)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // OkHttp & Retrofit (Networking HTTP)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)

    // Hilt (Inversión / Inyección de dependencias)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
}
```

### 4. `app/src/main/AndroidManifest.xml`
Se añadió la directiva para solicitar acceso a internet:
```xml
<uses-permission android:name="android.permission.INTERNET" />
```

### 5. `gradle.properties`
Se configuró el flag de soporte para source sets con KSP:
```properties
android.disallowKotlinSourceSets=false
```

---

## 📘 Descripción Teórica de las Librerías Base

### 1. Inversión e Inyección de Dependencias (`Hilt / Dagger`)

#### ¿Qué es la Inversión de Dependencias (Dependency Inversion)?
La **Inversión de Dependencias** es el 5º principio **SOLID** (Dependency Inversion Principle - DIP). Establece que:
1. Los módulos de alto nivel no deben depender de los módulos de bajo nivel; ambos deben depender de abstracciones (interfaces).
2. Las abstracciones no deben depender de los detalles; los detalles deben depender de las abstracciones.

En lugar de que una clase cree sus propias dependencias instanciándolas directamente (ej. `val repository = ContactRepositoryImpl(ContactDaoImpl())`), la **Inyección de Dependencias (DI)** invierte el control: un contenedor externo instancía y provee (inyecta) las dependencias requeridas.

#### ¿Por qué usar Hilt en Android?
- **Desacoplamiento**: Facilita cambiar la implementación de un componente (ej. reemplazar un DataSource simulado por uno real de red) sin modificar las clases consumidoras.
- **Testabilidad**: Permite inyectar dependencias mock/fake fácilmente en pruebas unitarias.
- **Gestión Automática de Ciclo de Vida**: Hilt vincula las dependencias al ciclo de vida de la aplicación (`SingletonComponent`), Activity (`ActivityComponent`) o ViewModel (`ViewModelComponent`).

#### Componentes Clave de Hilt:
- `@HiltAndroidApp`: Anotación para la clase `Application` que genera la jerarquía de componentes Dagger a nivel de app.
- `@AndroidEntryPoint`: Anota Activities o Fragments para habilitar la inyección de dependencias en componentes de Android.
- `@HiltViewModel` & `@Inject constructor(...)`: Permite a Hilt inyectar automáticamente repositorios y casos de uso en los `ViewModel`.
- `@Module` & `@InstallIn(...)`: Módulos que le indican a Hilt cómo construir dependencias que no tienen constructor inyectable (interfaces, librerías de terceros como Retrofit, OkHttp y Room).
- `@Provides` / `@Binds`: Métodos dentro de un `@Module` que construyen y retornan la instancia requerida.

---

### 2. Room Database (`androidx.room`)
- **Propósito**: Proporciona una capa de abstracción sobre SQLite para la persistencia local de datos.
- **Componentes**:
  - `@Entity`: Representa la estructura de las tablas en la BD.
  - `@Dao`: Define los métodos de acceso a los datos (consultas SQL, inserción, actualización). Soporta retornos de tipo `Flow<T>` y funciones `suspend`.
  - `@Database`: Define la instancia principal de la base de datos.

---

### 3. Kotlin Coroutines (`kotlinx.coroutines`)
- **Propósito**: Manejo asíncrono no bloqueante en Android.
- **Dispatchers Principales**:
  - `Dispatchers.IO`: Para llamadas de red y operaciones de lectura/escritura en disco (Room, SharedPreferences/DataStore).
  - `Dispatchers.Main`: Para actualizar la interfaz de usuario.
  - `Dispatchers.Default`: Para tareas pesadas de procesamiento de CPU.

---

### 4. Retrofit (`com.squareup.retrofit2`)
- **Propósito**: Cliente REST type-safe para Android que convierte interfaces Kotlin en llamadas HTTP.
- **Características**:
  - Permite definir peticiones asíncronas con la palabra clave `suspend`.
  - Integra convertidores JSON (ej. `GsonConverterFactory`) para mapear respuestas HTTP directamente a objetos de datos (`DTO`).

---

### 5. OkHttp (`com.squareup.okhttp3`)
- **Propósito**: Cliente de transporte de red HTTP subyacente a Retrofit.
- **Características**:
  - Maneja la reutilización de conexiones TCP, caché HTTP y compresión.
  - Soporta **Interceptores** (`HttpLoggingInterceptor`) para auditar y depurar peticiones y respuestas HTTP.

---

## ✅ Verificación de Compilación

Se ejecutó la tarea Gradle `:app:assembleDebug` obteniendo el resultado:
- **Status**: `Build finished successfully.`
