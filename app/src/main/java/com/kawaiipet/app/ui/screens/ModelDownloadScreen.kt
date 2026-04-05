package com.kawaiipet.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.kawaiipet.app.audio.AudioPipeline
import com.kawaiipet.app.audio.FeaturedVoiceModels
import com.kawaiipet.app.audio.ModelManager
import com.kawaiipet.app.audio.ModelType
import com.kawaiipet.app.util.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ModelDownloadUi(
    val progress: Float,
    val phase: String
)

@HiltViewModel
class ModelDownloadViewModel @Inject constructor(
    private val modelManager: ModelManager,
    private val preferenceManager: PreferenceManager,
    private val audioPipeline: AudioPipeline
) : ViewModel() {

    val sttModels = modelManager.availableSTTModels
    val ttsModels = modelManager.availableTTSModels

    private val _downloadProgress = MutableStateFlow<Map<String, ModelDownloadUi>>(emptyMap())
    val downloadProgress = _downloadProgress.asStateFlow()

    private val _downloadedModels = MutableStateFlow<Set<String>>(emptySet())
    val downloadedModels = _downloadedModels.asStateFlow()

    fun refreshDownloadedFromDisk() {
        viewModelScope.launch {
            _downloadedModels.value = modelManager.getDownloadedModelIds()
        }
    }

    suspend fun refreshDownloadedSnapshot(): Set<String> {
        val ids = modelManager.getDownloadedModelIds()
        _downloadedModels.value = ids
        return ids
    }

    suspend fun selectSttIfDownloaded(modelId: String) {
        if (!modelManager.isModelDownloaded(modelId)) return
        preferenceManager.setSttModelId(modelId)
        withContext(Dispatchers.Default) {
            audioPipeline.initializeSTT(modelId)
        }
    }

    suspend fun selectTtsIfDownloaded(modelId: String) {
        if (!modelManager.isModelDownloaded(modelId)) return
        preferenceManager.setTtsModelId(modelId)
        withContext(Dispatchers.Default) {
            audioPipeline.initializeTTS(modelId)
        }
    }

    fun downloadModel(modelId: String, url: String, type: ModelType) {
        viewModelScope.launch {
            _downloadProgress.value = _downloadProgress.value + (
                modelId to ModelDownloadUi(0f, "Starting…")
                )
            val ok = modelManager.downloadModel(modelId, url) { progress, phase ->
                viewModelScope.launch(Dispatchers.Main.immediate) {
                    _downloadProgress.value = _downloadProgress.value + (
                        modelId to ModelDownloadUi(progress.coerceIn(0f, 1f), phase)
                        )
                }
            }
            _downloadProgress.value = _downloadProgress.value.filterKeys { it != modelId }
            if (ok) {
                when (type) {
                    ModelType.STT -> {
                        preferenceManager.setSttModelId(modelId)
                        withContext(Dispatchers.Default) {
                            audioPipeline.initializeSTT(modelId)
                        }
                    }
                    ModelType.TTS -> {
                        preferenceManager.setTtsModelId(modelId)
                        withContext(Dispatchers.Default) {
                            audioPipeline.initializeTTS(modelId)
                        }
                    }
                }
            }
            _downloadedModels.value = modelManager.getDownloadedModelIds()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelDownloadScreen(
    navController: NavController,
    autoStartKind: String = "",
    viewModel: ModelDownloadViewModel = hiltViewModel()
) {
    val progress by viewModel.downloadProgress.collectAsState()
    val downloaded by viewModel.downloadedModels.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.refreshDownloadedFromDisk()
    }

    LaunchedEffect(autoStartKind) {
        if (autoStartKind.isBlank()) return@LaunchedEffect
        val snap = viewModel.refreshDownloadedSnapshot()
        when (autoStartKind) {
            FeaturedVoiceModels.NAV_STT_MOONSHINE_TINY_EN -> {
                val m = viewModel.sttModels.find { it.id == FeaturedVoiceModels.MOONSHINE_TINY_EN_V2 }
                if (m != null) {
                    if (m.id in snap) {
                        viewModel.selectSttIfDownloaded(m.id)
                    } else {
                        viewModel.downloadModel(m.id, m.url, m.type)
                    }
                }
            }
            FeaturedVoiceModels.NAV_TTS_KITTEN_NANO_EN -> {
                val m = viewModel.ttsModels.find { it.id == FeaturedVoiceModels.KITTEN_NANO_EN_V0_1 }
                if (m != null) {
                    if (m.id in snap) {
                        viewModel.selectTtsIfDownloaded(m.id)
                    } else {
                        viewModel.downloadModel(m.id, m.url, m.type)
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice Models") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Speech-to-Text Models", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(viewModel.sttModels) { model ->
                ModelCard(
                    name = model.name,
                    description = model.description,
                    size = model.sizeMb,
                    isDownloaded = model.id in downloaded,
                    downloadUi = progress[model.id],
                    onDownload = { viewModel.downloadModel(model.id, model.url, model.type) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Text-to-Speech Models", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(viewModel.ttsModels) { model ->
                ModelCard(
                    name = model.name,
                    description = model.description,
                    size = model.sizeMb,
                    isDownloaded = model.id in downloaded,
                    downloadUi = progress[model.id],
                    onDownload = { viewModel.downloadModel(model.id, model.url, model.type) }
                )
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun ModelCard(
    name: String,
    description: String,
    size: Int,
    isDownloaded: Boolean,
    downloadUi: ModelDownloadUi?,
    onDownload: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "$description · ${size}MB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                when {
                    isDownloaded -> Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Downloaded",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    downloadUi != null -> CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp
                    )
                    else -> IconButton(onClick = onDownload) {
                        Icon(Icons.Default.Download, contentDescription = "Download")
                    }
                }
            }

            if (downloadUi != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = downloadUi.phase,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { downloadUi.progress },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "${(downloadUi.progress * 100f).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
