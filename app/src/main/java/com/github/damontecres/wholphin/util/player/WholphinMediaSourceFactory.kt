package com.github.damontecres.wholphin.util.player

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.extractor.text.SubtitleParser

/**
 * Routes HLS playback through [AacAwareHlsExtractorFactory] and everything else through the
 * supplied default factory.
 *
 * [androidx.media3.exoplayer.source.DefaultMediaSourceFactory] builds its HLS factory internally
 * and exposes no hook for the HLS extractor factory, so the only way to substitute one is to
 * handle the HLS content type ourselves.
 */
@UnstableApi
class WholphinMediaSourceFactory(
    private val default: MediaSource.Factory,
    dataSourceFactory: DataSource.Factory,
    subtitleParserFactory: SubtitleParser.Factory? = null,
) : MediaSource.Factory {
    private val hls =
        HlsMediaSource
            .Factory(dataSourceFactory)
            .setExtractorFactory(AacAwareHlsExtractorFactory())
            .setAllowChunklessPreparation(false)
            .also { factory ->
                subtitleParserFactory?.let { factory.setSubtitleParserFactory(it) }
            }

    override fun setDrmSessionManagerProvider(drmSessionManagerProvider: DrmSessionManagerProvider): MediaSource.Factory {
        default.setDrmSessionManagerProvider(drmSessionManagerProvider)
        hls.setDrmSessionManagerProvider(drmSessionManagerProvider)
        return this
    }

    override fun setLoadErrorHandlingPolicy(loadErrorHandlingPolicy: LoadErrorHandlingPolicy): MediaSource.Factory {
        default.setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
        hls.setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
        return this
    }

    override fun getSupportedTypes(): IntArray = default.supportedTypes

    override fun createMediaSource(mediaItem: MediaItem): MediaSource {
        val uri = mediaItem.localConfiguration?.uri
        val type =
            if (uri != null) {
                Util.inferContentType(uri)
            } else {
                C.CONTENT_TYPE_OTHER
            }
        return if (type == C.CONTENT_TYPE_HLS) {
            hls.createMediaSource(mediaItem)
        } else {
            default.createMediaSource(mediaItem)
        }
    }
}
