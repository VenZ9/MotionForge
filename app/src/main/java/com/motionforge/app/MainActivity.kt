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
import androidx.lifecycle.viewmodel.compose.viewModel
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
                var projects by remember { mutableStateOf<List<Project>>(emptyList()) }
                var activeProjectId by remember { mutableStateOf<String?>(null) }

                // Request Permissions
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { _ -> }

                LaunchedEffect(Unit) {
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

                    projects = repository.getAllProjects()
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
                                    projects = repository.getAllProjects()
                                }
                            }
                        )
                    } else {
                        DashboardScreen(
                            projects = projects,
                            onOpenProject = { id -> activeProjectId = id },
                            onCreateProject = { newProj ->
                                coroutineScope.launch {
                                    repository.saveProject(newProj)
                                    projects = repository.getAllProjects()
                                    activeProjectId = newProj.id
                                }
                            },
                            onDuplicateProject = { proj ->
                                coroutineScope.launch {
                                    val dup = repository.duplicateProject(proj)
                                    projects = repository.getAllProjects()
                                }
                            },
                            onDeleteProject = { id ->
                                coroutineScope.launch {
                                    repository.deleteProject(id)
                                    projects = repository.getAllProjects()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
