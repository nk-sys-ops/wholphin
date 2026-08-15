package com.github.damontecres.wholphin.ui.detail.recording

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.github.damontecres.wholphin.data.model.isInProgressRecording
import com.github.damontecres.wholphin.preferences.UserPreferences
import com.github.damontecres.wholphin.ui.detail.movie.MovieDetails
import com.github.damontecres.wholphin.ui.detail.movie.MovieViewModel
import com.github.damontecres.wholphin.ui.nav.Destination

@Composable
fun RecordingDetails(
    preferences: UserPreferences,
    destination: Destination.MediaItem,
    modifier: Modifier = Modifier,
    viewModel: MovieViewModel =
        hiltViewModel<MovieViewModel, MovieViewModel.Factory>(
            creationCallback = { it.create(destination.itemId) },
        ),
) {
    val state by viewModel.state.collectAsState()
    val movie = state.movie

    if (movie != null && movie.isInProgressRecording) {
        LaunchedEffect(movie) {
            val channelOrItemId = movie.data.channelId ?: movie.id
            viewModel.navigationManager.navigateTo(
                Destination.Playback(
                    itemId = channelOrItemId,
                    positionMs = 0L,
                ),
            )
        }
    } else {
        MovieDetails(
            preferences = preferences,
            destination = destination,
            modifier = modifier,
            viewModel = viewModel,
        )
    }
}
