package com.motionforge.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.motionforge.app.data.ProjectRepository
import com.motionforge.app.model.Project
import com.motionforge.app.ui.dashboard.DashboardScreen
import com.motionforge.app.ui.editor.EditorScreen
import com.motionforge.app.ui.editor.EditorViewModel
import com.motionforge.app.ui.theme.BgDark
import com.motionforge.app.ui.theme.MotionForgeTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = ProjectRepository(applicationContext)

        setContent {
            MotionForgeTheme {
                val coroutineScope = rememberCoroutineScope()
                var projects by remember { mutableStateOf<List<Project>>(repository.createDemoProjects()) }
                var activeProjectId by remember { mutableStateOf<String?>(null) }

                // Safe runtime permissions launcher
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { _ -> }

                LaunchedEffect(Unit) {
                    try {
                        val loaded = repository.getAllProjects()
                        if (loaded.isNotEmpty()) {
                            projects = loaded
                        }
                    } catch (_: Exception) {}

                    // Request media permissions safely in background after UI is composed
                    try {
                        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            arrayOf(
                                Manifest.permission.READ_MEDIA_VIDEO,
                                Manifest.permission.READ_MEDIA_IMAGES,
                                Manifest.permission.READ_MEDIA_AUDIO
                            )
                        } else {
                            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                        val missing = permissions.filter {
                            ContextCompat.checkSelfPermission(this@MainActivity, it) != PackageManager.PERMISSION_GRANTED
                        }
                        if (missing.isNotEmpty()) {
                            permissionLauncher.launch(missing.toTypedArray())
                        }
                    } catch (_: Exception) {}
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BgDark
                ) {
                    val currentProject = activeProjectId?.let { id ->
                        projects.firstOrNull { it.id == id }
                    }

                    if (currentProject != null) {
                        val editorViewModel = remember(currentProject.id) {
                            EditorViewModel(repository, currentProject)
                        }

                        EditorScreen(
                            viewModel = editorViewModel,
                            onBackToDashboard = {
                                activeProjectId = null
                                coroutineScope.launch {
                                    try {
                                        projects = repository.getAllProjects()
                                    } catch (_: Exception) {}
                                }
                            }
                        )
                    } else {
                        DashboardScreen(
                            projects = projects,
                            onOpenProject = { id -> activeProjectId = id },
                            onCreateProject = { newProj ->
                                coroutineScope.launch {
                                    try {
                                        repository.saveProject(newProj)
                                        projects = repository.getAllProjects()
                                    } catch (_: Exception) {
                                        projects = projects + newProj
                                    }
                                    activeProjectId = newProj.id
                                }
                            },
                            onDuplicateProject = { proj ->
                                coroutineScope.launch {
                                    try {
                                        val dup = repository.duplicateProject(proj)
                                        projects = repository.getAllProjects()
                                    } catch (_: Exception) {}
                                }
                            },
                            onDeleteProject = { id ->
                                coroutineScope.launch {
                                    try {
                                        repository.deleteProject(id)
                                        projects = repository.getAllProjects()
                                    } catch (_: Exception) {
                                        projects = projects.filterNot { it.id == id }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
