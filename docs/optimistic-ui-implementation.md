# Optimistic UI and Message Status Implementation Guide

## Overview
This document provides a comprehensive implementation guide for adding optimistic UI updates and message status tracking to PulseLink.

## Phase 1: Core Status Infrastructure ✅

### Completed:
1. **MessageStatus enum** - Created at `androidApp/src/main/java/com/pulselink/domain/model/MessageStatus.kt`
   - Enum values: SENDING, SENT, DELIVERED, READ
   - String conversion helpers for Firestore mapping

2. **ContactMessage model updated** - Added `status: MessageStatus` field with default value `MessageStatus.SENT`

## Phase 2: Repository and ViewModel Updates

### TODO: Update MessageRepositoryImpl

File: `androidApp/src/main/java/com/pulselink/data/db/MessageRepositoryImpl.kt`

Add method to update message status:
```kotlin
override suspend fun updateMessageStatus(
    messageId: String,
    conversationId: Long,
    status: MessageStatus
) {
    dao.updateStatus(messageId, status)
}
```

### TODO: Add DAO method

In ContactMessageDao, add:
```kotlin
@Query("UPDATE ContactMessage SET status = :status WHERE id = :messageId")
suspend fun updateStatus(messageId: String, status: MessageStatus)
```

### TODO: Create ViewModel with Optimistic Updates

Create or update conversation ViewModel with pending message management:

```kotlin
class ContactConversationViewModel @Inject constructor(
    private val repository: MessageRepository,
    private val linkManager: ContactLinkManager
) : ViewModel() {
    
    private val _pendingMessages = MutableStateFlow<List<ContactMessage>>(emptyList())
    private val firestoreMessages: Flow<List<ContactMessage>> = 
        repository.observeForContact(contactId)
    
    val messages: StateFlow<List<ContactMessage>> = combine(
        firestoreMessages,
        _pendingMessages
    ) { remote, pending ->
        val remoteIds = remote.map { it.id }.toSet()
        val filteredPending = pending.filter { it.id !in remoteIds }
        (remote + filteredPending).sortedByDescending { it.timestamp }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    
    fun sendMessage(text: String) {
        val tempId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val optimisticMessage = ContactMessage(
            id = tempId,
            contactId = currentContactId,
            message = text,
            timestamp = now,
            isSent = true,
            status = MessageStatus.SENDING
        )
        
        // Add to pending immediately
        _pendingMessages.update { it + optimisticMessage }
        
        // Send via backend
        viewModelScope.launch {
            try {
                linkManager.sendMessage(contact, text)
                // Message will appear in Firestore flow with SENT status
            } catch (e: Exception) {
                // Handle error: mark as failed or retry
                _pendingMessages.update { list -> 
                    list.filterNot { it.id == tempId } 
                }
            }
        }
    }
}
```

## Phase 3: UI Status Display

### TODO: Update MessageBubble Composable

File: Update message bubble components to show status

```kotlin
@Composable
fun MessageBubble(
    message: ContactMessage,
    isOwnMessage: Boolean
) {
    Column {
        // Existing message content
        Text(text = message.message)
        
        if (isOwnMessage) {
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (message.status) {
                        MessageStatus.SENDING -> "Sending…"
                        MessageStatus.SENT -> "Sent"
                        MessageStatus.DELIVERED -> "Delivered"
                        MessageStatus.READ -> "Read"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Optional: Add status icon
                Icon(
                    imageVector = when (message.status) {
                        MessageStatus.SENDING -> Icons.Default.Schedule
                        MessageStatus.SENT -> Icons.Default.Done
                        MessageStatus.DELIVERED -> Icons.Default.DoneAll
                        MessageStatus.READ -> Icons.Default.DoneAll
                    },
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = if (message.status == MessageStatus.READ)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
```

## Phase 4: Backend Delivery Tracking

### TODO: Update Cloud Function

File: `functions/src/index.ts`

When writing a message to Firestore, set initial status:
```typescript
await admin.firestore()
  .collection('conversations')
  .doc(conversationId)
  .collection('messages')
  .doc(messageId)
  .set({
    ...messageData,
    status: 'SENT'
  });
```

### TODO: FCM Delivery Confirmation

On recipient device, when FCM is received:

File: Create or update `MyFirebaseMessagingService.kt`

```kotlin
class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val messageId = remoteMessage.data["messageId"] ?: return
        val conversationId = remoteMessage.data["conversationId"] ?: return
        
        // Update status to DELIVERED
        lifecycleScope.launch {
            val firestore = Firebase.firestore
            firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(messageId)
                .update("status", "DELIVERED")
        }
    }
}
```

## Phase 5: Read Receipt Tracking

### TODO: Mark Messages as Read

In ContactConversationScreen:

```kotlin
@Composable
fun ContactConversationScreen(
    conversationId: String,
    viewModel: ContactConversationViewModel
) {
    val messages by viewModel.messages.collectAsState()
    
    LaunchedEffect(conversationId) {
        // Mark all visible unread messages as READ
        viewModel.markMessagesAsRead()
    }
    
    // UI code...
}
```

In ViewModel:
```kotlin
fun markMessagesAsRead() {
    viewModelScope.launch {
        val unreadMessages = messages.value.filter { msg ->
            !msg.isSent && msg.status != MessageStatus.READ
        }
        
        unreadMessages.forEach { msg ->
            firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(msg.id)
                .update("status", "READ")
        }
    }
}
```

## Phase 6: SMS Status Handling

### TODO: SmsViewModel Optimistic Updates

Apply the same pattern to SmsViewModel for SMS messages:
- Use Android SMS delivery intents for DELIVERED status
- Mark as READ when thread is opened

```kotlin
class SmsViewModel : ViewModel() {
    fun sendSms(phoneNumber: String, text: String) {
        val tempId = UUID.randomUUID().toString()
        val optimisticMessage = SmsMessage(
            id = tempId,
            address = phoneNumber,
            body = text,
            date = System.currentTimeMillis(),
            status = MessageStatus.SENDING
        )
        
        _pendingMessages.update { it + optimisticMessage }
        
        val sentIntent = createSentIntent(tempId)
        val deliveredIntent = createDeliveredIntent(tempId)
        
        SmsManager.getDefault().sendTextMessage(
            phoneNumber,
            null,
            text,
            sentIntent,
            deliveredIntent
        )
    }
}
```

## Testing Checklist

- [ ] Message appears immediately with "Sending" status
- [ ] Status updates to "Sent" after Firestore write
- [ ] Status updates to "Delivered" when recipient receives FCM
- [ ] Status updates to "Read" when recipient opens conversation
- [ ] Failed messages show error state
- [ ] Works for both Link Channel and SMS messages
- [ ] No duplicate messages in UI
- [ ] Status persists across app restarts

## Next Steps

1. Implement remaining TODOs in order
2. Test each phase before moving to the next
3. Add error handling and retry logic
4. Consider adding message send failure UI
5. Add analytics for status tracking

