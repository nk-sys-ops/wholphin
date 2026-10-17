package com.github.damontecres.wholphin.preferences

import com.google.protobuf.CodedInputStream
import com.google.protobuf.CodedOutputStream
import com.google.protobuf.WireFormat
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class AppPreferencesSerializerTest {
    private val serializer = AppPreferencesSerializer()

    private fun buildRawExperimentalBytes(block: CodedOutputStream.() -> Unit): ByteArray {
        val out = ByteArrayOutputStream()
        val coded = CodedOutputStream.newInstance(out)
        coded.block()
        coded.flush()
        return out.toByteArray()
    }

    private fun wrapInAppPreferences(expBytes: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        val coded = CodedOutputStream.newInstance(out)
        coded.writeByteArray(AppPreferences.EXPERIMENTAL_PREFERENCES_FIELD_NUMBER, expBytes)
        coded.flush()
        return out.toByteArray()
    }

    private fun collectWireFieldNumbers(bytes: ByteArray): Set<Int> {
        val fields = mutableSetOf<Int>()
        val input = CodedInputStream.newInstance(bytes)
        while (!input.isAtEnd) {
            val tag = input.readTag()
            if (tag == 0) break
            fields.add(WireFormat.getTagFieldNumber(tag))
            input.skipField(tag)
        }
        return fields
    }

    @Test
    fun `migrate v1_0_8 experimental preferences with enabled=true tag5=true tag6=true`() =
        runTest {
            // (a) bytes of a v1.0.8-fork ExperimentalPreferences {enabled=true, tag5=true, tag6=true}
            val legacyExpBytes =
                buildRawExperimentalBytes {
                    writeBool(1, true) // enabled = true
                    writeBool(5, true) // v1.0.8 iptv_audio_recovery_enabled = true
                    writeBool(6, true) // v1.0.8 ts_direct_play = true
                    writeBool(7, true) // stale tag 7 should also be cleared
                }
            val appPrefsBytes = wrapInAppPreferences(legacyExpBytes)

            val migrated = serializer.readFrom(ByteArrayInputStream(appPrefsBytes)).experimentalPreferences

            assertTrue("enabled should remain true", migrated.enabled)
            assertFalse("dv (tag 5) must be reset to false", migrated.preferDolbyVisionOverHdr10Plus)
            assertTrue("iptv (1001) must be migrated as true", migrated.iptvAudioRecoveryEnabled)
            assertTrue("iptv (1001) presence must be set", migrated.hasIptvAudioRecoveryEnabled())
            assertTrue("ts (1002) must be migrated as true", migrated.tsDirectPlay)
            assertTrue("ts (1002) presence must be set", migrated.hasTsDirectPlay())
            assertTrue("fork_prefs_version marker (1003) must be set", migrated.hasForkPrefsVersion())
            assertEquals(1, migrated.forkPrefsVersion)

            // Verify legacy tags 6 and 7 are completely stripped from wire output
            val migratedWireFields = collectWireFieldNumbers(migrated.toByteArray())
            assertFalse("legacy tag 6 must be cleared", migratedWireFields.contains(6))
            assertFalse("legacy tag 7 must be cleared", migratedWireFields.contains(7))
        }

    @Test
    fun `migrate v1_0_8 experimental preferences with tag5=false`() =
        runTest {
            // (b) {tag5=false} -> iptv=false, dv=false
            // Case b1: explicit tag 5 = false on the wire
            val explicitFalseBytes =
                wrapInAppPreferences(
                    buildRawExperimentalBytes {
                        writeBool(5, false)
                    },
                )
            val migratedExplicit = serializer.readFrom(ByteArrayInputStream(explicitFalseBytes)).experimentalPreferences
            assertFalse("dv must be false", migratedExplicit.preferDolbyVisionOverHdr10Plus)
            assertFalse("iptv must be false when legacy tag5=false", migratedExplicit.iptvAudioRecoveryEnabled)
            assertTrue("iptv presence must be set", migratedExplicit.hasIptvAudioRecoveryEnabled())
            assertFalse("ts must be false", migratedExplicit.tsDirectPlay)
            assertTrue("marker must be set", migratedExplicit.hasForkPrefsVersion())

            // Case b2: proto3 v1.0.8 message where enabled=true and tag5=false (omitted on wire by proto3)
            val proto3FalseBytes =
                wrapInAppPreferences(
                    buildRawExperimentalBytes {
                        writeBool(1, true)
                    },
                )
            val migratedProto3 = serializer.readFrom(ByteArrayInputStream(proto3FalseBytes)).experimentalPreferences
            assertTrue("enabled should remain true", migratedProto3.enabled)
            assertFalse("dv must be false", migratedProto3.preferDolbyVisionOverHdr10Plus)
            assertFalse("iptv must be false when omitted in legacy proto3 message", migratedProto3.iptvAudioRecoveryEnabled)
            assertFalse("ts must be false", migratedProto3.tsDirectPlay)
            assertTrue("marker must be set", migratedProto3.hasForkPrefsVersion())
        }

    @Test
    fun `empty or fresh install defaults to iptv=true ts=false dv=false`() =
        runTest {
            // (c) empty/fresh -> iptv=true, ts=false, dv=false
            val fromDefault = serializer.defaultValue.experimentalPreferences
            assertFalse("defaultValue dv must be false", fromDefault.preferDolbyVisionOverHdr10Plus)
            assertTrue("defaultValue iptv must be true", fromDefault.iptvAudioRecoveryEnabled)
            assertFalse("defaultValue ts must be false", fromDefault.tsDirectPlay)
            assertTrue("defaultValue marker must be set", fromDefault.hasForkPrefsVersion())

            val fromEmptyStream = serializer.readFrom(ByteArrayInputStream(ByteArray(0))).experimentalPreferences
            assertFalse("empty stream dv must be false", fromEmptyStream.preferDolbyVisionOverHdr10Plus)
            assertTrue("empty stream iptv must be true", fromEmptyStream.iptvAudioRecoveryEnabled)
            assertTrue("empty stream iptv presence must be set", fromEmptyStream.hasIptvAudioRecoveryEnabled())
            assertFalse("empty stream ts must be false", fromEmptyStream.tsDirectPlay)
            assertTrue("empty stream ts presence must be set", fromEmptyStream.hasTsDirectPlay())
            assertTrue("empty stream marker must be set", fromEmptyStream.hasForkPrefsVersion())
        }

    @Test
    fun `already-migrated data with dv=true set by the user is unchanged`() =
        runTest {
            // (d) already-migrated data with dv=true set by the user -> unchanged (migration runs once)
            val initialMigrated =
                serializer
                    .readFrom(ByteArrayInputStream(ByteArray(0)))
                    .updateExperimentalPreferences {
                        enabled = true
                        preferDolbyVisionOverHdr10Plus = true
                        iptvAudioRecoveryEnabled = false
                        tsDirectPlay = true
                    }

            val savedOut = ByteArrayOutputStream()
            serializer.writeTo(initialMigrated, savedOut)

            val reloaded = serializer.readFrom(ByteArrayInputStream(savedOut.toByteArray())).experimentalPreferences
            assertTrue("enabled must stay true", reloaded.enabled)
            assertTrue("user-set dv=true must be preserved after migration marker is set", reloaded.preferDolbyVisionOverHdr10Plus)
            assertFalse("user-set iptv=false must be preserved", reloaded.iptvAudioRecoveryEnabled)
            assertTrue("user-set ts=true must be preserved", reloaded.tsDirectPlay)
            assertTrue("marker must remain set", reloaded.hasForkPrefsVersion())
            assertEquals(1, reloaded.forkPrefsVersion)
        }
}
