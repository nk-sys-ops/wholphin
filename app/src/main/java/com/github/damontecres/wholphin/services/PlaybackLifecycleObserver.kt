package com.github.damontecres.wholphin.services

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject

/**
 * Observes the activity lifecycle in order to pause/resume/stop playback
 */
@ActivityRetainedScoped
class PlaybackLifecycleObserver
    @Inject
    constructor(
        private val navigationManager: NavigationManager,
        private val playerFactory: PlayerFactory,
        private val themeSongPlayer: ThemeSongPlayer,
    ) : DefaultLifecycleObserver {
        private var wasPlaying: Boolean? = null

        override fun onStart(owner: LifecycleOwner) {
            wasPlaying = null
        }

        override fun onResume(owner: LifecycleOwner) {
            if (wasPlaying == true) {
                playerFactory.currentPlayer?.let {
                    if (!it.isReleased) it.play()
                }
            }
        }

        override fun onPause(owner: LifecycleOwner) {
            playerFactory.currentPlayer?.let {
                // onResume already guards on isReleased; onPause did not. A
                // player released while the activity was still foregrounded
                // left a stale reference here, and the next backgrounding
                // called pause() on it -- media3 then posted to a dead
                // internal thread and logged IllegalStateException "sending
                // message to a Handler on a dead thread". Not fatal, but it
                // left the UI wedged in a way that reads as a crash.
                if (!it.isReleased) {
                    wasPlaying = it.isPlaying
                    it.pause()
                }
            }
            themeSongPlayer.stop()
        }

        override fun onStop(owner: LifecycleOwner) {
            themeSongPlayer.stop()
        }
    }
