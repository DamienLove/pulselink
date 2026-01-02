package com.RingerSong.free.ui

import android.app.Activity
import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.RingerSong.free.ads.AdServices
import com.RingerSong.free.ui.theme.RingerSongTheme
import com.RingerSong.free.viewmodel.AddSongsResult
import com.RingerSong.free.viewmodel.RingerViewModel
import kotlinx.coroutines.launch

@Composable
fun RingerSongApp(
    viewModel: RingerViewModel,
    sharedUri: Uri?,
    onSharedConsumed: () -> Unit
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val activity = LocalContext.current as? Activity
    val state = viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(sharedUri) {
        if (sharedUri != null) {
            viewModel.addSongs(listOf(sharedUri)) { result ->
                coroutineScope.launch { snackbarHostState.showResult(result) }
                if (result.addedCount > 0) {
                    activity?.let { AdServices.showInterstitial(it) }
                }
            }
            onSharedConsumed()
        }
    }

    RingerSongTheme(themeConfig = state.value.theme) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            AppNavHost(
                navController = navController,
                modifier = Modifier.padding(padding),
                viewModel = viewModel,
                snackbarHostState = snackbarHostState
            )
        }
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier,
    viewModel: RingerViewModel,
    snackbarHostState: SnackbarHostState
) {
    val coroutineScope = rememberCoroutineScope()
    val activity = LocalContext.current as? Activity
    val currentUser = viewModel.currentUser.collectAsStateWithLifecycle().value

    val startDest = if (currentUser == null) Routes.Login else Routes.Home

    NavHost(
        navController = navController,
        startDestination = startDest,
        modifier = modifier
    ) {
        composable(Routes.Login) {
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    navController.navigate(Routes.Home) {
                        popUpTo(Routes.Login) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.Home) {
            HomeScreen(
                state = viewModel.state.collectAsStateWithLifecycle().value,
                searchState = viewModel.searchState.collectAsStateWithLifecycle().value,
                youtubeSearchState = viewModel.youtubeSearchState.collectAsStateWithLifecycle().value,
                onOpenSettings = {
                    activity?.let { AdServices.showInterstitial(it) }
                    navController.navigate(Routes.Settings)
                },
                onToggleEnabled = viewModel::toggleEnabled,
                onToggleShuffle = viewModel::toggleShuffle,
                onToggleNotifications = viewModel::toggleNotifications,
                onAddSongs = { uris ->
                    viewModel.addSongs(uris) { result ->
                        coroutineScope.launch { snackbarHostState.showResult(result) }
                        if (result.addedCount > 0) {
                            activity?.let { AdServices.showInterstitial(it) }
                        }
                    }
                },
                onRemoveSong = viewModel::removeSong,
                onMoveSong = viewModel::moveSong,
                onAddOrUpdateContact = viewModel::addOrUpdateContact,
                onAssignContactSong = viewModel::assignContactSong,
                onUpdateUrgencyThreshold = viewModel::updateUrgencyThreshold,
                onSetUrgencyTone = viewModel::setUrgencyTone,
                onClearUrgencyTone = viewModel::clearUrgencyTone,
                onQueryChange = viewModel::updateSearchQuery,
                onSearch = viewModel::searchSpotify,
                onClearSearch = viewModel::clearSearch,
                onAddSpotifyTrack = { track ->
                    viewModel.addSpotifyTrack(track) { message ->
                        coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                        activity?.let { AdServices.showInterstitial(it) }
                    }
                },
                onYouTubeQueryChange = viewModel::updateYouTubeSearchQuery,
                onYouTubeSearch = viewModel::searchYouTubeMusic,
                onClearYouTubeSearch = viewModel::clearYouTubeSearch,
                onAddYouTubeTrack = { track ->
                    viewModel.addSpotifyTrack(track) { message ->
                        coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                        activity?.let { AdServices.showInterstitial(it) }
                    }
                }
            )
        }
        composable(Routes.Settings) {
            SettingsScreen(
                state = viewModel.state.collectAsStateWithLifecycle().value,
                currentUser = currentUser,
                onBack = { navController.popBackStack() },
                onSegmentSecondsChange = viewModel::updateSegmentSeconds,
                onMaxSongsChange = viewModel::updateMaxSongs,
                onResetProgress = viewModel::resetGlobalProgress
            )
        }
    }
}

private suspend fun SnackbarHostState.showResult(result: AddSongsResult) {
    val message = when {
        result.addedCount > 0 && result.skippedCount > 0 ->
            "Added ${result.addedCount} tracks, skipped ${result.skippedCount} (limit reached)"
        result.addedCount > 0 -> "Added ${result.addedCount} track${if (result.addedCount == 1) "" else "s"}"
        else -> "Track limit reached"
    }
    showSnackbar(message)
}

object Routes {
    const val Login = "login"
    const val Home = "home"
    const val Settings = "settings"
}
