# Guía Paso a Paso 2: Enrutamiento de Vistas (Navigation Compose) y Gestión de Tareas (Room + Hilt)

Esta guía complementa la guía previa de Room y explica cómo implementar **Navegación/Enrutamiento de Vistas** en Jetpack Compose (equivalente a `wouter` en React) y construir una segunda funcionalidad: **Gestión de Tareas (ToDo List)**, por ahora solo con **crear** y **listar** tareas (`id`, `nombre`, `estado` booleano de completada).

---

## 🚦 1. Enrutamiento de Vistas: Navigation Compose vs Wouter (React)

En el desarrollo web con React, librerías como **`wouter`** permiten alternar entre componentes según la URL actual:

```jsx
// Ejemplo equivalente en React (wouter)
import { Route, Switch, Link } from "wouter";

<Switch>
  <Route path="/contacts" component={ContactScreen} />
  <Route path="/tasks" component={TaskScreen} />
</Switch>
```

En **Android Jetpack Compose**, la solución estándar equivalente recomendada por Google es **`Navigation Compose`** (`androidx.navigation:navigation-compose`).

### Analogías de Conceptos

| React (`wouter`) | Android Compose (`Navigation Compose`) | Descripción |
| :--- | :--- | :--- |
| `useLocation()` / `history.push("/tasks")` | `navController.navigate("tasks")` | Cambiar de vista programáticamente |
| `<Switch>` | `NavHost(navController, startDestination)` | Contenedor principal de rutas |
| `<Route path="/tasks">` | `composable("tasks") { TaskScreen(...) }` | Declaración de una pantalla/ruta |
| `<Link to="/tasks">` | `Button(onClick = { navController.navigate("tasks") })` | Elemento UI para navegar |

---

## 🛠️ 2. Dependencia de Gradle

Se incluye en `app/build.gradle.kts` (ya agregada en `gradle/libs.versions.toml` como `androidx-navigation-compose`):

```kotlin
dependencies {
    implementation(libs.androidx.navigation.compose) // Version 2.8.5
}
```

---

## 📋 3. Modelo de Dominio (`domain/model/Task.kt`)

Entidad pura del dominio para representar la tarea, solo con los campos necesarios por ahora:

```kotlin
package com.rojas.consumodatos.domain.model

data class Task(
    val id: Long = 0,
    val name: String,
    val isCompleted: Boolean = false
)
```

---

## 💾 4. Capa Local de Tareas (`data/local`)

### A. Entidad Room (`TaskEntity.kt`)
```kotlin
package com.rojas.consumodatos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val isCompleted: Boolean = false
)
```

### B. DAO (`TaskDao.kt`)
Por ahora solo las operaciones de **crear** e **insertar**/**listar**:
```kotlin
package com.rojas.consumodatos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY id DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)
}
```

### C. Actualización de `AppDatabase.kt`
Se añade `TaskEntity` a la lista de entidades de Room:

```kotlin
package com.rojas.consumodatos.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ContactEntity::class, TaskEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun taskDao(): TaskDao
}
```

### D. Fuente de Datos Local (`TaskLocalDataSource.kt`)
```kotlin
package com.rojas.consumodatos.data.local

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface TaskLocalDataSource {
    fun getTasks(): Flow<List<TaskEntity>>
    suspend fun saveTask(task: TaskEntity)
}

class TaskLocalDataSourceImpl @Inject constructor(
    private val taskDao: TaskDao
) : TaskLocalDataSource {
    override fun getTasks(): Flow<List<TaskEntity>> = taskDao.getAllTasks()
    override suspend fun saveTask(task: TaskEntity) = taskDao.insertTask(task)
}
```

---

## 🔄 5. Mapeador de Datos (`data/mapper/TaskMapper.kt`)

```kotlin
package com.rojas.consumodatos.data.mapper

import com.rojas.consumodatos.data.local.TaskEntity
import com.rojas.consumodatos.domain.model.Task

fun TaskEntity.toDomain() = Task(id = id, name = name, isCompleted = isCompleted)
fun Task.toEntity() = TaskEntity(id = id, name = name, isCompleted = isCompleted)
```

---

## 🏢 6. Repositorio (`domain/repository` y `data/repository`)

### Interfaz (`domain/repository/TaskRepository.kt`)
```kotlin
package com.rojas.consumodatos.domain.repository

import com.rojas.consumodatos.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getTasks(): Flow<List<Task>>
    suspend fun addTask(name: String)
}
```

### Implementación (`data/repository/TaskRepositoryImpl.kt`)
```kotlin
package com.rojas.consumodatos.data.repository

import com.rojas.consumodatos.data.local.TaskLocalDataSource
import com.rojas.consumodatos.data.mapper.toDomain
import com.rojas.consumodatos.data.mapper.toEntity
import com.rojas.consumodatos.domain.model.Task
import com.rojas.consumodatos.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val localDataSource: TaskLocalDataSource
) : TaskRepository {

    override fun getTasks(): Flow<List<Task>> {
        return localDataSource.getTasks().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addTask(name: String) {
        val task = Task(name = name, isCompleted = false)
        localDataSource.saveTask(task.toEntity())
    }
}
```

---

## 💉 7. Configuración Hilt (`di`)

Añade los proveedores de `TaskDao`, `TaskLocalDataSource` y la interfaz del `TaskRepository` en los módulos existentes:

```kotlin
// En DatabaseModule.kt
@Provides
fun provideTaskDao(db: AppDatabase): TaskDao = db.taskDao()

@Provides
@Singleton
fun provideTaskLocalDataSource(impl: TaskLocalDataSourceImpl): TaskLocalDataSource = impl

// En RepositoryModule.kt
@Binds
@Singleton
abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository
```

---

## 📱 8. ViewModel y UI de Tareas (`ui/task`)

### A. Estado (`TaskUiState.kt`)
```kotlin
package com.rojas.consumodatos.ui.task

import com.rojas.consumodatos.domain.model.Task

data class TaskUiState(
    val tasks: List<Task> = emptyList(),
    val taskNameInput: String = "",
    val isLoading: Boolean = false
)
```

### B. ViewModel (`TaskViewModel.kt`)
Solo expone **crear** (`addTask`) y observar/**listar** (`observeTasks`):
```kotlin
package com.rojas.consumodatos.ui.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rojas.consumodatos.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val repository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    init {
        observeTasks()
    }

    private fun observeTasks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getTasks().collect { list ->
                _uiState.update { it.copy(tasks = list, isLoading = false) }
            }
        }
    }

    fun onTaskNameChanged(name: String) {
        _uiState.update { it.copy(taskNameInput = name) }
    }

    fun addTask() {
        val name = _uiState.value.taskNameInput.trim()
        if (name.isBlank()) return

        viewModelScope.launch {
            repository.addTask(name)
            _uiState.update { it.copy(taskNameInput = "") }
        }
    }
}
```

### C. Vista Compose de Tareas (`TaskScreen.kt`)
Formulario para **crear** una tarea y lista para **listar** las existentes, mostrando su estado (`isCompleted`) de forma solo lectura por ahora:
```kotlin
package com.rojas.consumodatos.ui.task

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
fun TaskScreen(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Lista de Tareas",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.taskNameInput,
                onValueChange = viewModel::onTaskNameChanged,
                label = { Text("Nueva Tarea") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = viewModel::addTask) {
                Text("Agregar")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (uiState.tasks.isEmpty()) {
            Text("No hay tareas registradas.", modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.tasks, key = { it.id }) { task ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = task.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = if (task.isCompleted) "Completada" else "Pendiente",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (task.isCompleted)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }
}
```

> Más adelante se puede agregar la capacidad de marcar una tarea como completada o eliminarla (`updateTaskStatus` / `deleteTask` en el DAO), pero por ahora el alcance es solo **crear** y **listar**.

---

## 🚦 9. Router Principal con Navigation Compose (`ui/navigation/AppNavigation.kt`)

Aquí se configura el enrutador que conmuta entre la pantalla de **Contactos** y la pantalla de **Tareas**:

```kotlin
package com.rojas.consumodatos.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rojas.consumodatos.ui.contact.ContactScreen
import com.rojas.consumodatos.ui.task.TaskScreen

sealed class Screen(val route: String, val title: String) {
    object Contacts : Screen("contacts", "Contactos")
    object Tasks : Screen("tasks", "Tareas")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val items = listOf(Screen.Contacts, Screen.Tasks)

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        label = { Text(screen.title) },
                        icon = { Text(screen.title.take(1)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Contacts.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Contacts.route) {
                ContactScreen(viewModel = hiltViewModel())
            }
            composable(Screen.Tasks.route) {
                TaskScreen(viewModel = hiltViewModel())
            }
        }
    }
}
```

### Uso en `MainActivity.kt`

Reemplaza la llamada directa a `ContactScreen` por `AppNavigation()` para que ambas vistas queden enrutadas:

```kotlin
setContent {
    ConsumoDatosTheme {
        AppNavigation()
    }
}
```
