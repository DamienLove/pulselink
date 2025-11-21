package com.pulselink.data.link;

import android.util.Log;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.pulselink.auth.FirebaseAuthManager;
import com.pulselink.domain.model.Contact;
import com.pulselink.domain.model.LinkStatus;
import com.pulselink.domain.repository.ContactRepository;
import com.pulselink.domain.repository.SettingsRepository;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import javax.inject.Singleton;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.flow.SharedFlow;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000x\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010%\n\u0002\u0010\u000e\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\t\n\u0002\b\u0005\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010 \n\u0002\b\u0002\b\u0007\u0018\u0000 .2\u00020\u0001:\u0001.B\'\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u00a2\u0006\u0002\u0010\nJ \u0010\u001b\u001a\u00020\u001c2\u0006\u0010\u001d\u001a\u00020\u00142\u0006\u0010\u001e\u001a\u00020\u001f2\u0006\u0010 \u001a\u00020\u0014H\u0002J\u0018\u0010!\u001a\u00020\u00142\u0006\u0010\"\u001a\u00020\u00142\u0006\u0010#\u001a\u00020\u0014H\u0002J\u001e\u0010$\u001a\u00020%2\u0006\u0010&\u001a\u00020\'2\u0006\u0010(\u001a\u00020\u0014H\u0086@\u00a2\u0006\u0002\u0010)J\u0006\u0010*\u001a\u00020\u001cJ\u001e\u0010+\u001a\u00020\u001c2\u0006\u0010 \u001a\u00020\u00142\f\u0010,\u001a\b\u0012\u0004\u0012\u00020\'0-H\u0002R\u0014\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\r0\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\r0\u000f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u0011R\u001a\u0010\u0012\u001a\u000e\u0012\u0004\u0012\u00020\u0014\u0012\u0004\u0012\u00020\u00150\u0013X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0016\u001a\u0004\u0018\u00010\u0014X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0017\u001a\u00020\u0018X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0019\u001a\u00020\u001aX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006/"}, d2 = {"Lcom/pulselink/data/link/LinkChannelService;", "", "firestore", "Lcom/google/firebase/firestore/FirebaseFirestore;", "settingsRepository", "Lcom/pulselink/domain/repository/SettingsRepository;", "contactRepository", "Lcom/pulselink/domain/repository/ContactRepository;", "authManager", "Lcom/pulselink/auth/FirebaseAuthManager;", "(Lcom/google/firebase/firestore/FirebaseFirestore;Lcom/pulselink/domain/repository/SettingsRepository;Lcom/pulselink/domain/repository/ContactRepository;Lcom/pulselink/auth/FirebaseAuthManager;)V", "_inboundMessages", "Lkotlinx/coroutines/flow/MutableSharedFlow;", "Lcom/pulselink/data/link/LinkChannelPayload;", "inboundMessages", "Lkotlinx/coroutines/flow/SharedFlow;", "getInboundMessages", "()Lkotlinx/coroutines/flow/SharedFlow;", "listeners", "", "", "Lcom/google/firebase/firestore/ListenerRegistration;", "localDeviceId", "scope", "Lkotlinx/coroutines/CoroutineScope;", "started", "Ljava/util/concurrent/atomic/AtomicBoolean;", "attachListener", "", "channelId", "contactId", "", "localId", "channelIdFor", "a", "b", "sendManualMessage", "", "contact", "Lcom/pulselink/domain/model/Contact;", "body", "(Lcom/pulselink/domain/model/Contact;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "start", "syncListeners", "contacts", "", "Companion", "androidApp_proDebug"})
public final class LinkChannelService {
    @org.jetbrains.annotations.NotNull()
    private final com.google.firebase.firestore.FirebaseFirestore firestore = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.domain.repository.SettingsRepository settingsRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.domain.repository.ContactRepository contactRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.auth.FirebaseAuthManager authManager = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.CoroutineScope scope = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.concurrent.atomic.AtomicBoolean started = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, com.google.firebase.firestore.ListenerRegistration> listeners = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableSharedFlow<com.pulselink.data.link.LinkChannelPayload> _inboundMessages = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.SharedFlow<com.pulselink.data.link.LinkChannelPayload> inboundMessages = null;
    @kotlin.jvm.Volatile()
    @org.jetbrains.annotations.Nullable()
    private volatile java.lang.String localDeviceId;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "LinkChannelService";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String COLLECTION_CHANNELS = "linkChannels";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String COLLECTION_MESSAGES = "messages";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String FIELD_ID = "id";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String FIELD_SENDER_ID = "senderId";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String FIELD_RECEIVER_ID = "receiverId";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String FIELD_BODY = "body";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String FIELD_TIMESTAMP = "timestamp";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String FIELD_TYPE = "type";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String FIELD_LINK_CODE = "linkCode";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String FIELD_PHONE = "phoneNumber";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TYPE_MANUAL = "manual";
    @org.jetbrains.annotations.NotNull()
    public static final com.pulselink.data.link.LinkChannelService.Companion Companion = null;
    
    @javax.inject.Inject()
    public LinkChannelService(@org.jetbrains.annotations.NotNull()
    com.google.firebase.firestore.FirebaseFirestore firestore, @org.jetbrains.annotations.NotNull()
    com.pulselink.domain.repository.SettingsRepository settingsRepository, @org.jetbrains.annotations.NotNull()
    com.pulselink.domain.repository.ContactRepository contactRepository, @org.jetbrains.annotations.NotNull()
    com.pulselink.auth.FirebaseAuthManager authManager) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.SharedFlow<com.pulselink.data.link.LinkChannelPayload> getInboundMessages() {
        return null;
    }
    
    public final void start() {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object sendManualMessage(@org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.Contact contact, @org.jetbrains.annotations.NotNull()
    java.lang.String body, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    private final void syncListeners(java.lang.String localId, java.util.List<com.pulselink.domain.model.Contact> contacts) {
    }
    
    private final void attachListener(java.lang.String channelId, long contactId, java.lang.String localId) {
    }
    
    private final java.lang.String channelIdFor(java.lang.String a, java.lang.String b) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\f\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0010"}, d2 = {"Lcom/pulselink/data/link/LinkChannelService$Companion;", "", "()V", "COLLECTION_CHANNELS", "", "COLLECTION_MESSAGES", "FIELD_BODY", "FIELD_ID", "FIELD_LINK_CODE", "FIELD_PHONE", "FIELD_RECEIVER_ID", "FIELD_SENDER_ID", "FIELD_TIMESTAMP", "FIELD_TYPE", "TAG", "TYPE_MANUAL", "androidApp_proDebug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}