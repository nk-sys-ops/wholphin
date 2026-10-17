package com.github.damontecres.wholphin.preferences

import androidx.compose.ui.graphics.toArgb
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.github.damontecres.wholphin.ui.preferences.subtitle.SubtitleSettings
import com.google.protobuf.CodedInputStream
import com.google.protobuf.InvalidProtocolBufferException
import com.google.protobuf.WireFormat
import org.jellyfin.sdk.model.api.BaseItemKind
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

class AppPreferencesSerializer
    @Inject
    constructor() : Serializer<AppPreferences> {
        override val defaultValue: AppPreferences =
            AppPreferences
                .newBuilder()
                .apply {
                    updateUrl = AppPreference.UpdateUrl.defaultValue
                    autoCheckForUpdates = AppPreference.AutoCheckForUpdates.defaultValue
                    sendCrashReports = AppPreference.SendCrashReports.defaultValue
                    debugLogging = AppPreference.DebugLogging.defaultValue
                    signInAutomatically = AppPreference.SignInAuto.defaultValue

                    playbackPreferences =
                        PlaybackPreferences
                            .newBuilder()
                            .apply {
                                skipForwardMs =
                                    AppPreference.SkipForward.defaultValue.seconds.inWholeMilliseconds
                                skipBackMs =
                                    AppPreference.SkipBack.defaultValue.seconds.inWholeMilliseconds
                                controllerTimeoutMs = AppPreference.ControllerTimeout.defaultValue
                                seekBarSteps = AppPreference.SeekBarSteps.defaultValue.toInt()
                                showDebugInfo = AppPreference.PlaybackDebugInfo.defaultValue
                                autoPlayNext = AppPreference.AutoPlayNextUp.defaultValue
                                autoPlayNextDelaySeconds =
                                    AppPreference.AutoPlayNextDelay.defaultValue
                                skipBackOnResumeSeconds =
                                    AppPreference.SkipBackOnResume.defaultValue.seconds.inWholeMilliseconds
                                maxBitrate = AppPreference.DEFAULT_BITRATE
                                skipIntros = AppPreference.SkipIntros.defaultValue
                                skipOutros = AppPreference.SkipOutros.defaultValue
                                skipCommercials = AppPreference.SkipCommercials.defaultValue
                                skipPreviews = AppPreference.SkipPreviews.defaultValue
                                skipRecaps = AppPreference.SkipRecaps.defaultValue
                                passOutProtectionMs =
                                    AppPreference.PassOutProtection.defaultValue.hours.inWholeMilliseconds
                                showNextUpWhen = AppPreference.ShowNextUpTiming.defaultValue
                                playerBackend = AppPreference.PlayerBackendPref.defaultValue
                                refreshRateSwitching =
                                    AppPreference.RefreshRateSwitching.defaultValue
                                resolutionSwitching = AppPreference.ResolutionSwitching.defaultValue
                                cinemaMode = AppPreference.CinemaMode.defaultValue
                                dpadSeekMode = AppPreference.DpadSeekModePref.defaultValue

                                overrides =
                                    PlaybackOverrides
                                        .newBuilder()
                                        .apply {
                                            ac3Supported = AppPreference.Ac3Supported.defaultValue
                                            downmixStereo = AppPreference.DownMixStereo.defaultValue
//                                            directPlayAss = AppPreference.DirectPlayAss.defaultValue
                                            directPlayPgs = AppPreference.DirectPlayPgs.defaultValue
                                            mediaExtensionsEnabled =
                                                AppPreference.FfmpegPreference.defaultValue
                                            assPlaybackMode =
                                                AppPreference.AssSubtitleMode.defaultValue
                                        }.build()

                                mpvOptions =
                                    MpvOptions
                                        .newBuilder()
                                        .apply {
                                            enableHardwareDecoding =
                                                AppPreference.MpvHardwareDecoding.defaultValue
                                            useGpuNext = AppPreference.MpvGpuNext.defaultValue
                                        }.build()
                            }.build()
                    homePagePreferences =
                        HomePagePreferences
                            .newBuilder()
                            .apply {
                                maxItemsPerRow = AppPreference.HomePageItems.defaultValue.toInt()
                                enableRewatchingNextUp = AppPreference.RewatchNextUp.defaultValue
                                maxDaysNextUp = AppPreference.MaxDaysNextUp.defaultValue.toInt()
                            }.build()
                    interfacePreferences =
                        InterfacePreferences
                            .newBuilder()
                            .apply {
                                playThemeSongs = AppPreference.PlayThemeMusic.defaultValue
                                appThemeColors = AppPreference.ThemeColors.defaultValue
                                navDrawerSwitchOnFocus =
                                    AppPreference.NavDrawerSwitchOnFocus.defaultValue
                                showClock = AppPreference.ShowClock.defaultValue
                                backdropStyle = AppPreference.BackdropStylePref.defaultValue
                                showLogos = AppPreference.ShowLogos.defaultValue
                                clearDisplayToggles()
                                addAllDisplayToggles(AppPreference.DisplayTogglesPref.defaultValue)

                                searchPreferences =
                                    SearchPreferences
                                        .newBuilder()
                                        .apply {
                                            combinedSearchResults = false
                                            showVoiceSearchButton = true
                                        }.build()

                                subtitlesPreferences =
                                    SubtitlePreferences
                                        .newBuilder()
                                        .apply {
                                            resetSubtitles()
                                        }.build()
                                hdrSubtitlesPreferences =
                                    SubtitlePreferences
                                        .newBuilder()
                                        .apply {
                                            resetSubtitles()
                                        }.build()

                                liveTvPreferences =
                                    LiveTvPreferences
                                        .newBuilder()
                                        .apply {
                                            showHeader = AppPreference.LiveTvShowHeader.defaultValue
                                            favoriteChannelsAtBeginning =
                                                AppPreference.LiveTvFavoriteChannelsBeginning.defaultValue
                                            sortByRecentlyWatched =
                                                AppPreference.LiveTvChannelSortByWatched.defaultValue
                                            colorCodePrograms =
                                                AppPreference.LiveTvColorCodePrograms.defaultValue
                                        }.build()

                                screensaverPreference =
                                    ScreensaverPreferences
                                        .newBuilder()
                                        .apply {
                                            startDelay = ScreensaverPreference.DEFAULT_START_DELAY
                                            duration = ScreensaverPreference.DEFAULT_DURATION
                                            animate = ScreensaverPreference.Animate.defaultValue
                                            maxAgeFilter = ScreensaverPreference.DEFAULT_MAX_AGE
                                            showClock = ScreensaverPreference.ShowClock.defaultValue
                                            clearItemTypes()
                                            addItemTypes(BaseItemKind.MOVIE.serialName)
                                            addItemTypes(BaseItemKind.SERIES.serialName)
                                        }.build()
                            }.build()

                    advancedPreferences =
                        AdvancedPreferences
                            .newBuilder()
                            .apply {
                                imageDiskCacheSizeBytes =
                                    AppPreference.ImageDiskCacheSize.defaultValue * AppPreference.MEGA_BIT
                            }.build()

                    photoPreferences =
                        PhotoPreferences
                            .newBuilder()
                            .apply {
                                slideshowDuration = AppPreference.SlideshowDuration.defaultValue
                                slideshowPlayVideos = AppPreference.SlideshowPlayVideos.defaultValue
                            }.build()

                    musicPreferences =
                        MusicPreferences
                            .newBuilder()
                            .apply {
                                showBackdrop = true
                                showLyrics = true
                                showAlbumArt = true
                            }.build()

                    experimentalPreferences =
                        ExperimentalPreferences
                            .newBuilder()
                            .apply {
                                enabled = false
                                videoTunnelingEnabled = false
                                preferAc3Surround = ExperimentalPreference.PreferAc3ForSurround.defaultValue
                                disableAudioOffload = ExperimentalPreference.DisableAudioOffload.defaultValue
                                preferDolbyVisionOverHdr10Plus =
                                    ExperimentalPreference.PreferDolbyVisionOverHdr10Plus.defaultValue
                                // Carries the Live TV AAC fix (AacAwareHlsExtractorFactory),
                                // which affected channels need to have any audio at all.
                                // Must be set here: this assignment replaces the whole
                                // message, so setting it elsewhere is silently discarded.
                                iptvAudioRecoveryEnabled = ExperimentalPreference.IptvAudioRecovery.defaultValue
                                tsDirectPlay = ExperimentalPreference.TsDirectPlay.defaultValue
                                forkPrefsVersion = FORK_PREFS_VERSION
                            }.build()
                }.build()

        override suspend fun readFrom(input: InputStream): AppPreferences {
            try {
                val rawBytes = input.readBytes()
                val parsed = AppPreferences.parseFrom(rawBytes)
                return migrateForkExperimentalPreferences(parsed, rawBytes)
            } catch (exception: InvalidProtocolBufferException) {
                throw CorruptionException("Cannot read proto.", exception)
            }
        }

        override suspend fun writeTo(
            t: AppPreferences,
            output: OutputStream,
        ) = t.writeTo(output)

        companion object {
            const val FORK_PREFS_VERSION = 1

            fun migrateForkExperimentalPreferences(
                parsed: AppPreferences,
                rawAppPrefsBytes: ByteArray,
            ): AppPreferences {
                if (parsed.hasExperimentalPreferences() &&
                    parsed.experimentalPreferences.hasForkPrefsVersion() &&
                    parsed.experimentalPreferences.forkPrefsVersion >= FORK_PREFS_VERSION
                ) {
                    return if (parsed.experimentalPreferences.hasIptvAudioRecoveryEnabled() &&
                        parsed.experimentalPreferences.hasTsDirectPlay()
                    ) {
                        parsed
                    } else {
                        parsed.updateExperimentalPreferences {
                            if (!hasIptvAudioRecoveryEnabled()) {
                                iptvAudioRecoveryEnabled =
                                    ExperimentalPreference.IptvAudioRecovery.defaultValue
                            }
                            if (!hasTsDirectPlay()) {
                                tsDirectPlay = ExperimentalPreference.TsDirectPlay.defaultValue
                            }
                        }
                    }
                }

                val rawExpBytes =
                    if (parsed.hasExperimentalPreferences()) {
                        extractExperimentalPreferencesBytes(rawAppPrefsBytes)
                    } else {
                        ByteArray(0)
                    }

                val migratedExp =
                    migrateExperimentalPreferencesBytes(
                        rawExpBytes = rawExpBytes,
                        isPresentInAppPrefs = parsed.hasExperimentalPreferences(),
                    )

                return parsed.toBuilder().setExperimentalPreferences(migratedExp).build()
            }

            fun migrateExperimentalPreferencesBytes(
                rawExpBytes: ByteArray,
                isPresentInAppPrefs: Boolean = rawExpBytes.isNotEmpty(),
            ): ExperimentalPreferences {
                var enabled = false
                var videoTunnelingEnabled = false
                var preferAc3Surround = ExperimentalPreference.PreferAc3ForSurround.defaultValue
                var disableAudioOffload = ExperimentalPreference.DisableAudioOffload.defaultValue
                var tag5 = false
                var tag6 = false
                var iptv1001: Boolean? = null
                var ts1002: Boolean? = null
                var version1003: Int? = null

                val input = CodedInputStream.newInstance(rawExpBytes)
                while (!input.isAtEnd) {
                    val tag = input.readTag()
                    if (tag == 0) break
                    when (WireFormat.getTagFieldNumber(tag)) {
                        1 -> enabled = input.readBool()
                        2 -> videoTunnelingEnabled = input.readBool()
                        3 -> preferAc3Surround = input.readBool()
                        4 -> disableAudioOffload = input.readBool()
                        5 -> tag5 = input.readBool()
                        6 -> tag6 = input.readBool()
                        7 -> input.skipField(tag) // drop unshipped rebase slot 7
                        1001 -> iptv1001 = input.readBool()
                        1002 -> ts1002 = input.readBool()
                        1003 -> version1003 = input.readInt32()
                        else -> input.skipField(tag)
                    }
                }

                if (version1003 != null && version1003 >= FORK_PREFS_VERSION) {
                    return ExperimentalPreferences
                        .newBuilder()
                        .apply {
                            this.enabled = enabled
                            this.videoTunnelingEnabled = videoTunnelingEnabled
                            this.preferAc3Surround = preferAc3Surround
                            this.disableAudioOffload = disableAudioOffload
                            this.preferDolbyVisionOverHdr10Plus = tag5
                            this.iptvAudioRecoveryEnabled =
                                iptv1001 ?: ExperimentalPreference.IptvAudioRecovery.defaultValue
                            this.tsDirectPlay =
                                ts1002 ?: ExperimentalPreference.TsDirectPlay.defaultValue
                            this.forkPrefsVersion = version1003
                        }.build()
                }

                val isFreshInstall = !isPresentInAppPrefs && rawExpBytes.isEmpty()
                return ExperimentalPreferences
                    .newBuilder()
                    .apply {
                        this.enabled = enabled
                        this.videoTunnelingEnabled = videoTunnelingEnabled
                        this.preferAc3Surround = preferAc3Surround
                        this.disableAudioOffload = disableAudioOffload
                        // Reset tag 5 so upstream's prefer_dolby_vision_over_hdr10_plus starts clean (false)
                        this.preferDolbyVisionOverHdr10Plus =
                            ExperimentalPreference.PreferDolbyVisionOverHdr10Plus.defaultValue
                        this.iptvAudioRecoveryEnabled =
                            iptv1001 ?: if (isFreshInstall) {
                                ExperimentalPreference.IptvAudioRecovery.defaultValue
                            } else {
                                tag5
                            }
                        this.tsDirectPlay =
                            ts1002 ?: if (isFreshInstall) {
                                ExperimentalPreference.TsDirectPlay.defaultValue
                            } else {
                                tag6
                            }
                        this.forkPrefsVersion = FORK_PREFS_VERSION
                    }.build()
            }

            private fun extractExperimentalPreferencesBytes(rawAppPrefsBytes: ByteArray): ByteArray {
                val out = ByteArrayOutputStream()
                val input = CodedInputStream.newInstance(rawAppPrefsBytes)
                while (!input.isAtEnd) {
                    val tag = input.readTag()
                    if (tag == 0) break
                    val fieldNumber = WireFormat.getTagFieldNumber(tag)
                    if (fieldNumber == AppPreferences.EXPERIMENTAL_PREFERENCES_FIELD_NUMBER &&
                        WireFormat.getTagWireType(tag) == WireFormat.WIRETYPE_LENGTH_DELIMITED
                    ) {
                        out.write(input.readByteArray())
                    } else {
                        input.skipField(tag)
                    }
                }
                return out.toByteArray()
            }
        }
    }

inline fun AppPreferences.update(block: AppPreferences.Builder.() -> Unit): AppPreferences = toBuilder().apply(block).build()

inline fun AppPreferences.updatePlaybackPreferences(block: PlaybackPreferences.Builder.() -> Unit): AppPreferences =
    update {
        playbackPreferences = playbackPreferences.toBuilder().apply(block).build()
    }

inline fun AppPreferences.updatePlaybackOverrides(block: PlaybackOverrides.Builder.() -> Unit): AppPreferences =
    updatePlaybackPreferences {
        overrides = overrides.toBuilder().apply(block).build()
    }

inline fun AppPreferences.updateMpvOptions(block: MpvOptions.Builder.() -> Unit): AppPreferences =
    updatePlaybackPreferences {
        mpvOptions = mpvOptions.toBuilder().apply(block).build()
    }

inline fun AppPreferences.updateHomePagePreferences(block: HomePagePreferences.Builder.() -> Unit): AppPreferences =
    update {
        homePagePreferences = homePagePreferences.toBuilder().apply(block).build()
    }

inline fun AppPreferences.updateInterfacePreferences(block: InterfacePreferences.Builder.() -> Unit): AppPreferences =
    update {
        interfacePreferences = interfacePreferences.toBuilder().apply(block).build()
    }

inline fun AppPreferences.updateSearchPreferences(block: SearchPreferences.Builder.() -> Unit): AppPreferences =
    updateInterfacePreferences {
        searchPreferences = searchPreferences.toBuilder().apply(block).build()
    }

inline fun AppPreferences.updateSubtitlePreferences(block: SubtitlePreferences.Builder.() -> Unit): AppPreferences =
    updateInterfacePreferences {
        subtitlesPreferences = subtitlesPreferences.toBuilder().apply(block).build()
    }

inline fun AppPreferences.updateLiveTvPreferences(block: LiveTvPreferences.Builder.() -> Unit): AppPreferences =
    updateInterfacePreferences {
        liveTvPreferences = liveTvPreferences.toBuilder().apply(block).build()
    }

inline fun AppPreferences.updateAdvancedPreferences(block: AdvancedPreferences.Builder.() -> Unit): AppPreferences =
    update {
        advancedPreferences = advancedPreferences.toBuilder().apply(block).build()
    }

inline fun AppPreferences.updatePhotoPreferences(block: PhotoPreferences.Builder.() -> Unit): AppPreferences =
    update {
        photoPreferences = photoPreferences.toBuilder().apply(block).build()
    }

inline fun AppPreferences.updateScreensaverPreferences(block: ScreensaverPreferences.Builder.() -> Unit): AppPreferences =
    updateInterfacePreferences {
        screensaverPreference = screensaverPreference.toBuilder().apply(block).build()
    }

inline fun AppPreferences.updateMusicPreferences(block: MusicPreferences.Builder.() -> Unit): AppPreferences =
    update {
        musicPreferences = musicPreferences.toBuilder().apply(block).build()
    }

inline fun AppPreferences.updateExperimentalPreferences(block: ExperimentalPreferences.Builder.() -> Unit): AppPreferences =
    update {
        experimentalPreferences = experimentalPreferences.toBuilder().apply(block).build()
    }

fun SubtitlePreferences.Builder.resetSubtitles() {
    fontSize = SubtitleSettings.FontSize.defaultValue.toInt()
    fontColor = SubtitleSettings.FontColor.defaultValue.toArgb()
    fontBold = SubtitleSettings.FontBold.defaultValue
    fontItalic = SubtitleSettings.FontItalic.defaultValue
    fontOpacity = SubtitleSettings.FontOpacity.defaultValue.toInt()
    edgeColor = SubtitleSettings.EdgeColor.defaultValue.toArgb()
    edgeStyle = SubtitleSettings.EdgeStylePref.defaultValue
    backgroundColor = SubtitleSettings.BackgroundColor.defaultValue.toArgb()
    backgroundOpacity = SubtitleSettings.BackgroundOpacity.defaultValue.toInt()
    backgroundStyle = SubtitleSettings.BackgroundStylePref.defaultValue
    margin = SubtitleSettings.Margin.defaultValue.toInt()
    edgeThickness = SubtitleSettings.EdgeThickness.defaultValue.toInt()
    imageSubtitleOpacity = SubtitleSettings.ImageOpacity.defaultValue.toInt()
}
