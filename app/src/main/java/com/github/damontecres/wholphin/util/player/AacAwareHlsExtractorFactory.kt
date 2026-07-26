package com.github.damontecres.wholphin.util.player

import android.net.Uri
import androidx.media3.common.Format
import androidx.media3.common.util.TimestampAdjuster
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.analytics.PlayerId
import androidx.media3.exoplayer.hls.DefaultHlsExtractorFactory
import androidx.media3.exoplayer.hls.HlsExtractorFactory
import androidx.media3.exoplayer.hls.HlsMediaChunkExtractor
import androidx.media3.extractor.ExtractorInput
import androidx.media3.extractor.text.SubtitleParser
import timber.log.Timber

/**
 * Works around in-band AAC audio being silently discarded on Jellyfin Live TV.
 *
 * [DefaultHlsExtractorFactory.createTsExtractor] does this:
 *
 * ```
 * if (!MimeTypes.containsCodecsCorrespondingToMimeType(codecs, MimeTypes.AUDIO_AAC)) {
 *     payloadReaderFactoryFlags |= DefaultTsPayloadReaderFactory.FLAG_IGNORE_AAC_STREAM;
 * }
 * ```
 *
 * i.e. when the HLS variant does not advertise an AAC codec, the TS demuxer is told to ignore
 * AAC elementary streams outright. That is a reasonable guard in general, but it breaks Jellyfin
 * Live TV: when Jellyfin's probe of a live tuner stream fails to detect the audio stream it
 * advertises a video-only codecs string, even though the transport stream it then serves does
 * contain a perfectly good AAC track with its PID declared in the PMT. ExoPlayer discards it,
 * builds no audio track group at all, and playback is silent forever. MPV, ffprobe, jellyfin-web
 * and any direct TS player all play the same bytes with sound.
 *
 * Rather than reimplement the whole TS/ADTS/MP3/MP4/WebVTT selection logic just to clear one flag,
 * this factory rewrites the codecs string to include AAC before delegating, so the guard does not
 * trigger. The worst case is that an audio track the playlist did not declare becomes selectable —
 * which is exactly the behaviour we want here.
 *
 * A blank codecs string is deliberately left untouched: the same method also sets
 * FLAG_IGNORE_H264_STREAM when the codecs string lacks H.264, so injecting an audio-only codecs
 * string where there was none would suppress the *video* track instead.
 */
@UnstableApi
class AacAwareHlsExtractorFactory : HlsExtractorFactory {
    private val delegate = DefaultHlsExtractorFactory()

    override fun createExtractor(
        uri: Uri,
        format: Format,
        muxedCaptionFormats: MutableList<Format>?,
        timestampAdjuster: TimestampAdjuster,
        responseHeaders: MutableMap<String, MutableList<String>>,
        sniffingExtractorInput: ExtractorInput,
        playerId: PlayerId,
    ): HlsMediaChunkExtractor {
        val patched = withAacAdvertised(format)
        return delegate.createExtractor(
            uri,
            patched,
            muxedCaptionFormats,
            timestampAdjuster,
            responseHeaders,
            sniffingExtractorInput,
            playerId,
        )
    }

    override fun setSubtitleParserFactory(subtitleParserFactory: SubtitleParser.Factory): HlsExtractorFactory {
        delegate.setSubtitleParserFactory(subtitleParserFactory)
        return this
    }

    override fun experimentalParseSubtitlesDuringExtraction(parseSubtitlesDuringExtraction: Boolean): HlsExtractorFactory {
        delegate.experimentalParseSubtitlesDuringExtraction(parseSubtitlesDuringExtraction)
        return this
    }

    override fun getOutputTextFormat(sourceFormat: Format): Format = delegate.getOutputTextFormat(sourceFormat)

    private fun withAacAdvertised(format: Format): Format {
        val codecs = format.codecs
        if (codecs.isNullOrBlank() || codecs.contains(AAC_CODEC_PREFIX, ignoreCase = true)) {
            return format
        }
        val patched = "$codecs,$AAC_CODEC"
        Timber.i(
            "WHOLPHIN_DIAG hls_codecs_patched from=%s to=%s (avoids FLAG_IGNORE_AAC_STREAM)",
            codecs,
            patched,
        )
        return format.buildUpon().setCodecs(patched).build()
    }

    companion object {
        private const val AAC_CODEC_PREFIX = "mp4a"
        private const val AAC_CODEC = "mp4a.40.2"
    }
}
