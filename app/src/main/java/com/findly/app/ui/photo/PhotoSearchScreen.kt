package com.findly.app.ui.photo

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.findly.app.R
import com.findly.app.data.repository.PhotoSearchRepository
import com.findly.app.ui.common.colorLabelRes
import com.findly.app.ui.common.colorSwatch
import com.findly.app.ui.common.containerViewModel
import com.findly.app.ui.common.labelRes
import com.findly.app.ui.components.CircleIconButton
import com.findly.app.ui.components.DemoBadge
import com.findly.app.ui.components.InfoBanner
import com.findly.app.ui.components.SkeletonBlock
import com.findly.core.visual.VisualAnalysis
import com.findly.core.visual.VisualSearchMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class PhotoSearchUiState(
    val image: Uri? = null,
    val analyzing: Boolean = false,
    val analysis: VisualAnalysis? = null,
    val error: Boolean = false,
    /** In local mode the user tells us what the product is (a search word such as "кроссовки"). */
    val chosenType: String? = null,
    val chosenColors: Set<String> = emptySet(),
)

class PhotoSearchViewModel(private val repo: PhotoSearchRepository) : ViewModel() {
    private val _state = MutableStateFlow(PhotoSearchUiState())
    val state: StateFlow<PhotoSearchUiState> = _state.asStateFlow()
    val mode: VisualSearchMode get() = repo.mode

    fun onImage(uri: Uri) {
        _state.value = PhotoSearchUiState(image = uri, analyzing = true)
        viewModelScope.launch {
            try {
                val analysis = repo.analyze(uri)
                _state.update {
                    it.copy(
                        analyzing = false,
                        analysis = analysis,
                        chosenColors = analysis.dominantColors.take(1).toSet(),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(analyzing = false, error = true) }
            }
        }
    }

    fun chooseType(type: String) = _state.update { it.copy(chosenType = if (it.chosenType == type) null else type) }

    fun toggleColor(color: String) = _state.update {
        it.copy(chosenColors = if (color in it.chosenColors) it.chosenColors - color else it.chosenColors + color)
    }

    /** Builds a query only from what was detected or explicitly chosen by the user. */
    fun buildQuery(): String? {
        val s = _state.value
        val analysis = s.analysis ?: return null
        if (analysis.mode == VisualSearchMode.REMOTE && analysis.suggestedQuery.isNotBlank()) return analysis.suggestedQuery
        val type = s.chosenType ?: return null
        return (s.chosenColors.toList() + type).joinToString(" ")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhotoSearchScreen(onBack: () -> Unit, onSearch: (String) -> Unit) {
    val vm = containerViewModel { PhotoSearchViewModel(it.photoSearchRepository) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var captureTarget by rememberSaveable { mutableStateOf<String?>(null) }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(vm::onImage) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val target = captureTarget
        if (ok && target != null) vm.onImage(Uri.parse(target))
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircleIconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.photo_search_title), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            if (vm.mode != VisualSearchMode.REMOTE) DemoBadge()
        }

        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            val image = state.image
            if (image != null) {
                AsyncImage(model = image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(painterResource(R.drawable.ic_camera), contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.photo_search_placeholder), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    val uri = newCaptureUri(context)
                    captureTarget = uri.toString()
                    takePicture.launch(uri)
                },
                modifier = Modifier.weight(1f),
            ) {
                Icon(painterResource(R.drawable.ic_camera), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_take_photo))
            }
            OutlinedButton(
                onClick = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.weight(1f),
            ) {
                Icon(painterResource(R.drawable.ic_gallery), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_pick_photo))
            }
        }

        if (vm.mode != VisualSearchMode.REMOTE) {
            InfoBanner(stringResource(R.string.photo_search_local_note))
        }

        when {
            state.analyzing -> {
                SkeletonBlock(Modifier.fillMaxWidth().height(20.dp))
                SkeletonBlock(Modifier.fillMaxWidth(0.7f).height(20.dp))
            }
            state.error -> InfoBanner(stringResource(R.string.photo_search_error), warning = true)
            state.analysis != null -> {
                val analysis = state.analysis!!
                if (analysis.labels.isNotEmpty()) {
                    Text(stringResource(R.string.photo_detected, analysis.labels.joinToString(", ")), style = MaterialTheme.typography.bodyMedium)
                }
                Text(stringResource(R.string.photo_colors), style = MaterialTheme.typography.titleMedium)
                if (analysis.dominantColors.isEmpty()) {
                    Text(stringResource(R.string.photo_colors_none), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    analysis.dominantColors.forEach { c ->
                        FilterChip(
                            selected = c in state.chosenColors,
                            onClick = { vm.toggleColor(c) },
                            label = { Text(colorLabelRes(c)?.let { stringResource(it) } ?: c) },
                            leadingIcon = { Box(Modifier.size(14.dp).background(Color(colorSwatch(c)), CircleShape)) },
                        )
                    }
                }
                if (analysis.mode != VisualSearchMode.REMOTE) {
                    Text(stringResource(R.string.photo_choose_type), style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        productTypes.forEach { res ->
                            val word = stringResource(res)
                            FilterChip(
                                selected = state.chosenType == word,
                                onClick = { vm.chooseType(word) },
                                label = { Text(word) },
                            )
                        }
                    }
                } else {
                    analysis.category?.let {
                        Text(stringResource(R.string.photo_detected_type, stringResource(it.labelRes())), style = MaterialTheme.typography.titleMedium)
                    }
                }
                val query = vm.buildQuery()
                Button(onClick = { query?.let(onSearch) }, enabled = query != null, modifier = Modifier.fillMaxWidth()) {
                    Text(if (query != null) stringResource(R.string.photo_search_action, query) else stringResource(R.string.photo_choose_type_first))
                }
            }
        }
    }
}

/** A fresh file per capture so image caches never show the previous photo. */
private fun newCaptureUri(context: Context): Uri {
    val dir = File(context.cacheDir, "photo_search").apply { mkdirs() }
    dir.listFiles()?.forEach { it.delete() }
    val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

/** Product types offered in local mode; each label is also the search word (the parser understands them). */
private val productTypes = listOf(
    R.string.photo_type_sneakers,
    R.string.photo_type_hoodie,
    R.string.photo_type_tshirt,
    R.string.photo_type_headphones,
    R.string.photo_type_smartphone,
    R.string.photo_type_mouse,
    R.string.photo_type_keyboard,
    R.string.photo_type_backpack,
    R.string.photo_type_lamp,
)
