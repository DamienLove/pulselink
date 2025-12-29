package com.RingerSong.free.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class FirebaseSyncRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    
    private val _currentUser = MutableStateFlow(auth.currentUser)
    val currentUser: StateFlow<com.google.firebase.auth.FirebaseUser?> = _currentUser.asStateFlow()

    private var commandListener: ListenerRegistration? = null
    private var songsListener: ListenerRegistration? = null

    // Callback for when a command is received
    var onCommandReceived: ((String, String) -> Unit)? = null

    // Callback for when songs are added/changed from webapp
    var onSongsChanged: ((List<SongEntry>) -> Unit)? = null

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
            if (firebaseAuth.currentUser != null) {
                startListening(firebaseAuth.currentUser!!.uid)
            } else {
                stopListening()
            }
        }
    }
    
    private fun startListening(uid: String) {
        stopListening()
        Log.d("FirebaseSync", "Starting listener for $uid")
        val docRef = db.collection("users").document(uid).collection("ringersong").document("commands")

        commandListener = docRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("FirebaseSync", "Listen failed.", e)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val data = snapshot.data
                val command = data?.get("command") as? String
                val payload = data?.get("payload") as? String
                val timestamp = data?.get("timestamp") as? Long ?: 0L

                // Simple dedup based on time (in real app use IDs)
                if (command != null && System.currentTimeMillis() - timestamp < 30000) {
                     // Only process recent commands ( < 30s old)
                     onCommandReceived?.invoke(command, payload ?: "")

                     // Clear command to avoid re-processing
                     docRef.update("command", null, "payload", null)
                }
            }
        }

        // Listen for song changes from webapp
        val songsRef = db.collection("users").document(uid).collection("ringer_playlist")
        songsListener = songsRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("FirebaseSync", "Songs listen failed.", e)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val songs = snapshot.documents.mapNotNull { doc ->
                    try {
                        val uri = doc.getString("uri") ?: return@mapNotNull null
                        val title = doc.getString("title") ?: "Unknown Track"
                        val artist = doc.getString("artist") ?: "Unknown Artist"
                        val combinedTitle = "$title - $artist"
                        val durationMs = doc.getLong("durationMs")
                        val addedAt = doc.getTimestamp("addedAt")?.toDate()?.time ?: System.currentTimeMillis()

                        SongEntry(
                            id = doc.id,
                            title = combinedTitle,
                            uri = uri,
                            durationMs = durationMs,
                            addedAt = addedAt
                        )
                    } catch (ex: Exception) {
                        Log.w("FirebaseSync", "Failed to parse song: ${ex.message}")
                        null
                    }
                }
                Log.d("FirebaseSync", "Loaded ${songs.size} songs from Firestore")
                onSongsChanged?.invoke(songs)
            }
        }
    }
    
    private fun stopListening() {
        commandListener?.remove()
        commandListener = null
        songsListener?.remove()
        songsListener = null
    }

    suspend fun signInAnonymously(): Result<Boolean> {
        return try {
            auth.signInAnonymously().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<Boolean> {
        return try {
            auth.signInWithEmailAndPassword(email, pass).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // In a real app, you'd use Google Sign In, but for now we rely on the fact 
    // that if they are on the same device, we might want to link them? 
    // Actually, RingerSong is standalone. 
    // We will just expose the current user status. 
    // To sync with Web, user must sign in with same credentials.
    // We will assume Email/Password or Google Sign In is needed.
    // For now, I'll add a helper to print UID so user can verify.
}
