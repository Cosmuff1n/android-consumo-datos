# Guía Paso a Paso: Implementación de Room, Retrofit y Hilt (Directorio Telefónico)

Esta guía tutorial explica paso a paso cómo construir una aplicación de **Listado Telefónico** en Android utilizando las mejores prácticas recomendadas por Google: **Clean Architecture**, **MVVM**, **Room**, **Retrofit**, **Corrutinas / Flow**, **Jetpack Compose** e **Inyección de Dependencias con Hilt**.

---

## 📐 1. Arquitectura y Estructura del Proyecto

El flujo de datos sigue un patrón unireccional desacoplado:

```text
[Jetpack Compose UI] ◄──(StateFlow)── [ContactViewModel]
                                             │
                                     (ContactRepository)
                                       /           \
               (ContactLocalDataSource)             (ContactRemoteDataSource)
                       │                                    │
                  [Room DAO]                         [Retrofit API]
                       │                                    │
                 (SQLite DB)                          (Servidor REST)
```

La estructura de paquetes sugerida en `com.rojas.consumodatos` es:

```text
com.rojas.consumodatos/
 ├── domain/
 │    ├── model/Contact.kt                  # Modelo de Dominio
 │    └── repository/ContactRepository.kt    # Interfaz del Repositorio
 ├── data/
 │    ├── local/
 │    │    ├── ContactEntity.kt            # Entidad Room (Tabla 'contacts')
 │    │    ├── ContactDao.kt               # DAO con Consultas SQL
 │    │    ├── AppDatabase.kt              # RoomDatabase
 │    │    └── ContactLocalDataSource.kt   # Fuente de Datos Local
 │    ├── remote/
 │    │    ├── ContactDto.kt               # Data Transfer Object (JSON)
 │    │    ├── ContactApiService.kt        # Interfaz Retrofit
 │    │    └── ContactRemoteDataSource.kt  # Fuente de Datos Remota
 │    ├── mapper/
 │    │    └── ContactMapper.kt            # Mapeadores DTO <-> Entity <-> Domain
 │    └── repository/
 │         └── ContactRepositoryImpl.kt    # Implementación del Repositorio
 ├── di/
 │    ├── DatabaseModule.kt               # Proveedor Hilt para Room
 │    ├── NetworkModule.kt                # Proveedor Hilt para Retrofit
 │    └── RepositoryModule.kt             # Vinculación Hilt para Repositorio
 └── ui/
      └── contact/
           ├── ContactUiState.kt           # Estado inmutable de la UI
           ├── ContactViewModel.kt         # ViewModel
           └── ContactScreen.kt            # Vista Compose (Formulario y Lista)
```

---

## 📝 2. Paso 1: Modelo de Dominio (`domain/model/Contact.kt`)

Crea la clase de modelo de dominio pura. Esta clase no tiene anotaciones de Room ni de Retrofit:

```kotlin
package com.rojas.consumodatos.domain.model

data class Contact(
    val id: Long = 0,
    val name: String,
    val phone: String
)
```

---

## 💾 3. Paso 2: Capa Local con Room (`data/local`)

### A. Entidad Room (`ContactEntity.kt`)
Representa la estructura de la tabla `contacts` en SQLite:

```kotlin
package com.rojas.consumodatos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String
)
```

### B. Interface DAO (`ContactDao.kt`)
Define las operaciones de base de datos. Retorna `Flow` para observación reactiva:

```kotlin
package com.rojas.consumodatos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<ContactEntity>)

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteContactById(id: Long)
}
```

### C. Base de Datos Room (`AppDatabase.kt`)
```kotlin
package com.rojas.consumodatos.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ContactEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
}
```

### D. Fuente de Datos Local (`ContactLocalDataSource.kt`)
Abstrae las llamadas directas al DAO:

```kotlin
package com.rojas.consumodatos.data.local

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface ContactLocalDataSource {
    fun getContacts(): Flow<List<ContactEntity>>
    suspend fun saveContact(contact: ContactEntity)
    suspend fun saveContacts(contacts: List<ContactEntity>)
    suspend fun deleteContact(id: Long)
}

class ContactLocalDataSourceImpl @Inject constructor(
    private val contactDao: ContactDao
) : ContactLocalDataSource {
    override fun getContacts(): Flow<List<ContactEntity>> = contactDao.getAllContacts()
    override suspend fun saveContact(contact: ContactEntity) = contactDao.insertContact(contact)
    override suspend fun saveContacts(contacts: List<ContactEntity>) = contactDao.insertContacts(contacts)
    override suspend fun deleteContact(id: Long) = contactDao.deleteContactById(id)
}
```

---

## 🌐 4. Paso 3: Capa Remota con Retrofit y OkHttp (`data/remote`)

### A. DTO Red (`ContactDto.kt`)
```kotlin
package com.rojas.consumodatos.data.remote

import com.google.gson.annotations.SerializedName

data class ContactDto(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("name") val name: String,
    @SerializedName("phone") val phone: String
)
```

### B. Servicio API Retrofit (`ContactApiService.kt`)
```kotlin
package com.rojas.consumodatos.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ContactApiService {
    @GET("contacts")
    suspend fun getContacts(): List<ContactDto>

    @POST("contacts")
    suspend fun createContact(@Body contact: ContactDto): ContactDto
}
```

### C. Fuente de Datos Remota (`ContactRemoteDataSource.kt`)
```kotlin
package com.rojas.consumodatos.data.remote

import javax.inject.Inject

interface ContactRemoteDataSource {
    suspend fun fetchContacts(): List<ContactDto>
    suspend fun addContact(contact: ContactDto): ContactDto
}

class ContactRemoteDataSourceImpl @Inject constructor(
    private val apiService: ContactApiService
) : ContactRemoteDataSource {
    override suspend fun fetchContacts(): List<ContactDto> = apiService.getContacts()
    override suspend fun addContact(contact: ContactDto): ContactDto = apiService.createContact(contact)
}
```

---

## 🔄 5. Paso 4: Mapeadores de Datos (`data/mapper/ContactMapper.kt`)

Funciones de extensión para convertir modelos entre capas sin acoplarlas:

```kotlin
package com.rojas.consumodatos.data.mapper

import com.rojas.consumodatos.data.local.ContactEntity
import com.rojas.consumodatos.data.remote.ContactDto
import com.rojas.consumodatos.domain.model.Contact

fun ContactEntity.toDomain() = Contact(id = id, name = name, phone = phone)
fun Contact.toEntity() = ContactEntity(id = id, name = name, phone = phone)
fun ContactDto.toEntity() = ContactEntity(id = id ?: 0, name = name, phone = phone)
fun Contact.toDto() = ContactDto(id = if (id == 0L) null else id, name = name, phone = phone)
```

---

## 🏢 6. Paso 5: Capa Repositorio (`domain/repository` y `data/repository`)

### A. Interfaz del Repositorio (`domain/repository/ContactRepository.kt`)
```kotlin
package com.rojas.consumodatos.domain.repository

import com.rojas.consumodatos.domain.model.Contact
import kotlinx.coroutines.flow.Flow

interface ContactRepository {
    fun getContacts(): Flow<List<Contact>>
    suspend fun addContact(name: String, phone: String)
    suspend fun syncRemoteContacts()
    suspend fun deleteContact(id: Long)
}
```

### B. Implementación (`data/repository/ContactRepositoryImpl.kt`)
```kotlin
package com.rojas.consumodatos.data.repository

import com.rojas.consumodatos.data.local.ContactLocalDataSource
import com.rojas.consumodatos.data.mapper.toDomain
import com.rojas.consumodatos.data.mapper.toEntity
import com.rojas.consumodatos.data.remote.ContactRemoteDataSource
import com.rojas.consumodatos.domain.model.Contact
import com.rojas.consumodatos.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ContactRepositoryImpl @Inject constructor(
    private val localDataSource: ContactLocalDataSource,
    private val remoteDataSource: ContactRemoteDataSource
) : ContactRepository {

    override fun getContacts(): Flow<List<Contact>> {
        return localDataSource.getContacts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addContact(name: String, phone: String) {
        val contact = Contact(name = name, phone = phone)
        localDataSource.saveContact(contact.toEntity())
    }

    override suspend fun syncRemoteContacts() {
        runCatching {
            val remoteDtos = remoteDataSource.fetchContacts()
            val entities = remoteDtos.map { it.toEntity() }
            localDataSource.saveContacts(entities)
        }
    }

    override suspend fun deleteContact(id: Long) {
        localDataSource.deleteContact(id)
    }
}
```

---

## 💉 7. Paso 6: Inversión e Inyección de Dependencias (`di`)

### A. Módulo de Base de Datos (`DatabaseModule.kt`)
```kotlin
package com.rojas.consumodatos.di

import android.content.Context
import androidx.room.Room
import com.rojas.consumodatos.data.local.AppDatabase
import com.rojas.consumodatos.data.local.ContactDao
import com.rojas.consumodatos.data.local.ContactLocalDataSource
import com.rojas.consumodatos.data.local.ContactLocalDataSourceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "contacts_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideContactDao(db: AppDatabase): ContactDao = db.contactDao()

    @Provides
    @Singleton
    fun provideContactLocalDataSource(impl: ContactLocalDataSourceImpl): ContactLocalDataSource = impl
}
```

### B. Módulo de Red (`NetworkModule.kt`)
```kotlin
package com.rojas.consumodatos.di

import com.rojas.consumodatos.data.remote.ContactApiService
import com.rojas.consumodatos.data.remote.ContactRemoteDataSource
import com.rojas.consumodatos.data.remote.ContactRemoteDataSourceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideContactApiService(okHttpClient: OkHttpClient): ContactApiService {
        return Retrofit.Builder()
            .baseUrl("https://jsonplaceholder.typicode.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ContactApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideContactRemoteDataSource(impl: ContactRemoteDataSourceImpl): ContactRemoteDataSource = impl
}
```

### C. Módulo de Repositorio (`RepositoryModule.kt`)
```kotlin
package com.rojas.consumodatos.di

import com.rojas.consumodatos.data.repository.ContactRepositoryImpl
import com.rojas.consumodatos.domain.repository.ContactRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {

    @Binds
    @Singleton
    fun bindContactRepository(
        impl: ContactRepositoryImpl
    ): ContactRepository
}
```

---

## 📱 8. Paso 7: Capa de Presentación (`ui/contact`)

### A. Estado de la UI (`ContactUiState.kt`)
```kotlin
package com.rojas.consumodatos.ui.contact

import com.rojas.consumodatos.domain.model.Contact

data class ContactUiState(
    val contacts: List<Contact> = emptyList(),
    val nameInput: String = "",
    val phoneInput: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
```

### B. ViewModel (`ContactViewModel.kt`)
```kotlin
package com.rojas.consumodatos.ui.contact

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rojas.consumodatos.domain.repository.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactViewModel @Inject constructor(
    private val repository: ContactRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactUiState())
    val uiState: StateFlow<ContactUiState> = _uiState.asStateFlow()

    init {
        observeContacts()
    }

    private fun observeContacts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getContacts()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                }
                .collect { list ->
                    _uiState.update { it.copy(contacts = list, isLoading = false) }
                }
        }
    }

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(nameInput = name) }
    }

    fun onPhoneChanged(phone: String) {
        _uiState.update { it.copy(phoneInput = phone) }
    }

    fun addContact() {
        val name = _uiState.value.nameInput.trim()
        val phone = _uiState.value.phoneInput.trim()

        if (name.isBlank() || phone.isBlank()) return

        viewModelScope.launch {
            repository.addContact(name, phone)
            _uiState.update { it.copy(nameInput = "", phoneInput = "") }
        }
    }

    fun deleteContact(id: Long) {
        viewModelScope.launch {
            repository.deleteContact(id)
        }
    }
}
```

### C. Vista en Jetpack Compose (`ContactScreen.kt`)
```kotlin
package com.rojas.consumodatos.ui.contact

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ContactScreen(
    viewModel: ContactViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Directorio Telefónico",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Formulario de Ingreso
        OutlinedTextField(
            value = uiState.nameInput,
            onValueChange = viewModel::onNameChanged,
            label = { Text("Nombre") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = uiState.phoneInput,
            onValueChange = viewModel::onPhoneChanged,
            label = { Text("Teléfono") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = viewModel::addContact,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guardar Contacto en Room")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Lista de Contactos
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (uiState.contacts.isEmpty()) {
            Text(
                text = "No hay contactos guardados.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(uiState.contacts, key = { it.id }) { contact ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = contact.name, style = MaterialTheme.typography.titleMedium)
                                Text(text = contact.phone, style = MaterialTheme.typography.bodyMedium)
                            }
                            TextButton(onClick = { viewModel.deleteContact(contact.id) }) {
                                Text("Eliminar", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
```

---

## ⚡ 9. Paso 8: Inicialización y Punto de Entrada

### A. Application (`ConsumoDatosApp.kt`)
```kotlin
package com.rojas.consumodatos

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ConsumoDatosApp : Application()
```

### B. Manifest (`AndroidManifest.xml`)
Registra la clase `ConsumoDatosApp`:
```xml
<application
    android:name=".ConsumoDatosApp"
    ... >
</application>
```

### C. Activity (`MainActivity.kt`)
```kotlin
package com.rojas.consumodatos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.rojas.consumodatos.ui.contact.ContactScreen
import com.rojas.consumodatos.ui.theme.ConsumoDatosTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ConsumoDatosTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ContactScreen(
                        viewModel = hiltViewModel(),
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
```
