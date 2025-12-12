package com.pulselink.data.webrtc

import android.util.Log
import com.pulselink.data.realtime.FcmPushClient
import com.pulselink.domain.model.Contact
import com.pulselink.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

@Singleton
class WebRtcSignaler @Inject constructor(
    private val fcmPushClient: FcmPushClient,
    private val settingsRepository: SettingsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val factory: PeerConnectionFactory by lazy {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(null)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
        )
        val encoderFactory = DefaultVideoEncoderFactory(
            /* eglContext = */ null,
            /* enableIntelVp8Encoder = */ true,
            /* enableH264HighProfile = */ true
        )
        val decoderFactory = DefaultVideoDecoderFactory(null)
        PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
    }

    private val peers = mutableMapOf<Long, PeerConnection>()

    suspend fun startSession(contact: Contact) = withContext(Dispatchers.Default) {
        val pc = ensurePeerConnection(contact) ?: return@withContext
        val audioSource: AudioSource = factory.createAudioSource(MediaConstraintsFactory.audioConstraints())
        val audioTrack: AudioTrack = factory.createAudioTrack("pulse_audio", audioSource)
        pc.addTrack(audioTrack)
        pc.createDataChannel("pulse_data", MediaConstraintsFactory.dataChannel())
        pc.createOffer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                if (desc == null) return
                pc.setLocalDescription(SimpleSdpObserver(), desc)
                scope.launch {
                    sendSignaling(
                        contact,
                        "WEBRTC_OFFER",
                        mapOf("sdp" to desc.description, "sdpType" to desc.type.canonicalForm())
                    )
                }
            }
        }, MediaConstraintsFactory.offerAnswer())
    }

    suspend fun handleOffer(contact: Contact, sdp: String) = withContext(Dispatchers.Default) {
        val pc = ensurePeerConnection(contact) ?: return@withContext
        val remote = SessionDescription(SessionDescription.Type.OFFER, sdp)
        pc.setRemoteDescription(SimpleSdpObserver(), remote)
        pc.createAnswer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                if (desc == null) return
                pc.setLocalDescription(SimpleSdpObserver(), desc)
                scope.launch {
                    sendSignaling(
                        contact,
                        "WEBRTC_ANSWER",
                        mapOf("sdp" to desc.description, "sdpType" to desc.type.canonicalForm())
                    )
                }
            }
        }, MediaConstraintsFactory.offerAnswer())
    }

    suspend fun handleAnswer(contact: Contact, sdp: String) = withContext(Dispatchers.Default) {
        peers[contact.id]?.setRemoteDescription(
            SimpleSdpObserver(),
            SessionDescription(SessionDescription.Type.ANSWER, sdp)
        )
    }

    suspend fun handleIce(contact: Contact, mid: String, index: Int, candidate: String) = withContext(Dispatchers.Default) {
        peers[contact.id]?.addIceCandidate(IceCandidate(mid, index, candidate))
    }

    private suspend fun ensurePeerConnection(contact: Contact): PeerConnection? = withContext(Dispatchers.Default) {
        peers[contact.id]?.let { return@withContext it }
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )
        val pc = factory.createPeerConnection(iceServers, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate?) {
                if (candidate != null) {
                    scope.launch {
                        sendSignaling(
                            contact,
                            "WEBRTC_ICE",
                            mapOf(
                                "sdpMid" to candidate.sdpMid.orEmpty(),
                                "sdpMLineIndex" to candidate.sdpMLineIndex.toString(),
                                "candidate" to candidate.sdp
                            )
                        )
                    }
                }
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                Log.d(TAG, "Peer connection ${contact.displayName} state=$newState")
            }

            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
                Log.d(TAG, "ICE state ${contact.displayName} = $newState")
            }
            override fun onIceConnectionReceivingChange(p0: Boolean) {}

            override fun onDataChannel(channel: org.webrtc.DataChannel?) {
                // no-op for now
            }

            override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onSignalingChange(newState: PeerConnection.SignalingState?) {}
            override fun onAddStream(stream: org.webrtc.MediaStream?) {}
            override fun onRemoveStream(stream: org.webrtc.MediaStream?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out org.webrtc.MediaStream>?) {}
        })
        if (pc == null) {
            Log.w(TAG, "Unable to create peer connection for ${contact.displayName}")
        } else {
            peers[contact.id] = pc
        }
        pc
    }

    private suspend fun sendSignaling(contact: Contact, type: String, extras: Map<String, String>) {
        val token = contact.remoteFcmToken?.takeIf { it.isNotBlank() } ?: return
        val deviceId = settingsRepository.ensureDeviceId()
        val data = buildMap<String, String> {
            put("id", java.util.UUID.randomUUID().toString())
            put("type", type)
            put("senderId", deviceId)
            contact.linkCode?.let { put("linkCode", it) }
            put("timestamp", System.currentTimeMillis().toString())
            putAll(extras)
        }
        fcmPushClient.send(token, data)
    }

    private object MediaConstraintsFactory {
        fun offerAnswer(): org.webrtc.MediaConstraints = org.webrtc.MediaConstraints().apply {
            mandatory.add(org.webrtc.MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(org.webrtc.MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
        }

        fun audioConstraints(): org.webrtc.MediaConstraints = org.webrtc.MediaConstraints().apply {
            optional.add(org.webrtc.MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"))
        }

        fun dataChannel(): org.webrtc.DataChannel.Init = org.webrtc.DataChannel.Init().apply {
            ordered = true
            maxRetransmits = -1
        }
    }

    private open class SimpleSdpObserver : SdpObserver {
        override fun onCreateSuccess(sessionDescription: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(p0: String?) { Log.w(TAG, "SDP create failure $p0") }
        override fun onSetFailure(p0: String?) { Log.w(TAG, "SDP set failure $p0") }
    }

    companion object {
        private const val TAG = "WebRtcSignaler"
    }
}
