package com.example.nestblr.feature.owner

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.nestblr.core.navigation.Route
import com.example.nestblr.data.repository.OwnerRepository
import com.example.nestblr.domain.model.Photo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PhotoManagerState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val photos: List<Photo> = emptyList(),
    val isUploading: Boolean = false,
    /** Failure of the initial detail-fetch. Drives the full-screen error state. */
    val loadError: String? = null,
    /** Failure of the compress + upload (or delete) pipeline. Drives inline UI. */
    val uploadError: String? = null,
)

@HiltViewModel
class PhotoManagerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: OwnerRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val listingId: String = savedStateHandle.toRoute<Route.ManageListingPhotos>().listingId

    private val _state = MutableStateFlow(PhotoManagerState())
    val state: StateFlow<PhotoManagerState> = _state.asStateFlow()

    init {
        loadPhotos()
    }

    fun loadPhotos() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadError = null) }
            repo.getListingPhotos(listingId).fold(
                onSuccess = { photos ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            photos = photos.sortedBy { p -> p.displayOrder }
                        )
                    }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(isLoading = false, loadError = e.message ?: "Couldn't load photos")
                    }
                }
            )
        }
    }

    /** Pull-to-refresh: re-fetches the photo grid (e.g. to verify a delete or
     *  sync state) without the full-screen spinner. */
    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, loadError = null) }
            repo.getListingPhotos(listingId).fold(
                onSuccess = { photos ->
                    _state.update {
                        it.copy(
                            isRefreshing = false,
                            photos = photos.sortedBy { p -> p.displayOrder }
                        )
                    }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(isRefreshing = false, loadError = e.message ?: "Couldn't load photos")
                    }
                }
            )
        }
    }

    fun onPhotoPicked(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isUploading = true, uploadError = null) }

            val compressed = runCatching { compressImage(context, uri) }
            compressed.fold(
                onSuccess = { file ->
                    repo.uploadPhoto(listingId, file).fold(
                        onSuccess = { photo ->
                            _state.update {
                                it.copy(
                                    isUploading = false,
                                    photos = (it.photos + photo).sortedBy { p -> p.displayOrder }
                                )
                            }
                            file.delete()
                        },
                        onFailure = { e ->
                            file.delete()
                            _state.update {
                                it.copy(
                                    isUploading = false,
                                    uploadError = "Upload failed: ${e.message}"
                                )
                            }
                        }
                    )
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            isUploading = false,
                            uploadError = "Upload failed: ${e.message}"
                        )
                    }
                }
            )
        }
    }

    fun deletePhoto(photoId: String) {
        viewModelScope.launch {
            repo.deletePhoto(listingId, photoId).fold(
                onSuccess = {
                    _state.update { it.copy(photos = it.photos.filterNot { p -> p.id == photoId }) }
                },
                onFailure = { e ->
                    _state.update { it.copy(uploadError = "Delete failed: ${e.message}") }
                }
            )
        }
    }

    /** Called when the user taps Add photo so a previous error doesn't linger. */
    fun clearUploadError() {
        _state.update { it.copy(uploadError = null) }
    }
}
