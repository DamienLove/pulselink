package com.pulselink.ui.screens;

import android.app.Activity;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.widget.Toast;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.compose.foundation.layout.Arrangement;
import androidx.compose.material.icons.Icons;
import androidx.compose.material3.ButtonDefaults;
import androidx.compose.runtime.Composable;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.graphics.Brush;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.text.input.TextFieldValue;
import androidx.compose.ui.text.style.TextAlign;
import androidx.compose.ui.text.TextStyle;
import com.pulselink.R;
import com.pulselink.data.assistant.VoiceCommandResult;
import com.pulselink.domain.model.Contact;
import com.pulselink.domain.model.ContactMessage;
import com.pulselink.domain.model.LinkStatus;
import com.pulselink.domain.model.ManualMessageResult;
import com.pulselink.domain.model.MessageDirection;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000V\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\b\u001a@\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0012\u0010\u0004\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00010\u00052\f\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00010\u00072\f\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00010\u0007H\u0003\u001a\u00e1\u0001\u0010\t\u001a\u00020\u00012\b\u0010\n\u001a\u0004\u0018\u00010\u000b2\f\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u000e0\r2\u0006\u0010\u000f\u001a\u00020\u00102\f\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00010\u00072\f\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00010\u00072\"\u0010\u0013\u001a\u001e\b\u0001\u0012\u0004\u0012\u00020\u000b\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00010\u0015\u0012\u0006\u0012\u0004\u0018\u00010\u00160\u00142\"\u0010\u0017\u001a\u001e\b\u0001\u0012\u0004\u0012\u00020\u0018\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00190\u0015\u0012\u0006\u0012\u0004\u0018\u00010\u00160\u00142\u001c\u0010\u001a\u001a\u0018\b\u0001\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00100\u0015\u0012\u0006\u0012\u0004\u0018\u00010\u00160\u00052\"\u0010\u001b\u001a\u001e\b\u0001\u0012\u0004\u0012\u00020\u0018\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u001c0\u0015\u0012\u0006\u0012\u0004\u0018\u00010\u00160\u00142\f\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\u00010\u0007H\u0007\u00a2\u0006\u0002\u0010\u001e\u001a\u00df\u0001\u0010\u001f\u001a\u00020\u00012\u0006\u0010\n\u001a\u00020\u000b2\f\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u000e0\r2\u0006\u0010\u000f\u001a\u00020\u00102\f\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00010\u00072\f\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00010\u00072\"\u0010\u0013\u001a\u001e\b\u0001\u0012\u0004\u0012\u00020\u000b\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00010\u0015\u0012\u0006\u0012\u0004\u0018\u00010\u00160\u00142\"\u0010\u0017\u001a\u001e\b\u0001\u0012\u0004\u0012\u00020\u0018\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00190\u0015\u0012\u0006\u0012\u0004\u0018\u00010\u00160\u00142\u001c\u0010\u001a\u001a\u0018\b\u0001\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00100\u0015\u0012\u0006\u0012\u0004\u0018\u00010\u00160\u00052\"\u0010\u001b\u001a\u001e\b\u0001\u0012\u0004\u0012\u00020\u0018\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u001c0\u0015\u0012\u0006\u0012\u0004\u0018\u00010\u00160\u00142\f\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\u00010\u0007H\u0003\u00a2\u0006\u0002\u0010\u001e\u001a\u0016\u0010 \u001a\u00020\u00012\f\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00010\u0007H\u0003\u001a\u0010\u0010!\u001a\u00020\u00012\u0006\u0010\"\u001a\u00020\u000eH\u0003\u001a\u0010\u0010#\u001a\u00020\u00012\u0006\u0010\n\u001a\u00020\u000bH\u0003\u00a8\u0006$"}, d2 = {"ComposerRow", "", "input", "Landroidx/compose/ui/text/input/TextFieldValue;", "onInputChange", "Lkotlin/Function1;", "onSend", "Lkotlin/Function0;", "onVoice", "ContactConversationScreen", "contact", "Lcom/pulselink/domain/model/Contact;", "messages", "", "Lcom/pulselink/domain/model/ContactMessage;", "isProUser", "", "onBack", "onOpenSettings", "onCallContact", "Lkotlin/Function2;", "Lkotlin/coroutines/Continuation;", "", "onSendMessage", "", "Lcom/pulselink/domain/model/ManualMessageResult;", "onPing", "onVoiceCommand", "Lcom/pulselink/data/assistant/VoiceCommandResult;", "onUpgradeClick", "(Lcom/pulselink/domain/model/Contact;Ljava/util/List;ZLkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function2;Lkotlin/jvm/functions/Function2;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function2;Lkotlin/jvm/functions/Function0;)V", "ConversationBody", "EmptyConversationState", "MessageBubble", "message", "StatusRow", "androidApp_freeDebug"})
public final class ContactConversationScreenKt {
    
    @androidx.compose.runtime.Composable()
    public static final void ContactConversationScreen(@org.jetbrains.annotations.Nullable()
    com.pulselink.domain.model.Contact contact, @org.jetbrains.annotations.NotNull()
    java.util.List<com.pulselink.domain.model.ContactMessage> messages, boolean isProUser, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onBack, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onOpenSettings, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function2<? super com.pulselink.domain.model.Contact, ? super kotlin.coroutines.Continuation<? super kotlin.Unit>, ? extends java.lang.Object> onCallContact, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function2<? super java.lang.String, ? super kotlin.coroutines.Continuation<? super com.pulselink.domain.model.ManualMessageResult>, ? extends java.lang.Object> onSendMessage, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super kotlin.coroutines.Continuation<? super java.lang.Boolean>, ? extends java.lang.Object> onPing, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function2<? super java.lang.String, ? super kotlin.coroutines.Continuation<? super com.pulselink.data.assistant.VoiceCommandResult>, ? extends java.lang.Object> onVoiceCommand, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onUpgradeClick) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void EmptyConversationState(kotlin.jvm.functions.Function0<kotlin.Unit> onBack) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void ConversationBody(com.pulselink.domain.model.Contact contact, java.util.List<com.pulselink.domain.model.ContactMessage> messages, boolean isProUser, kotlin.jvm.functions.Function0<kotlin.Unit> onBack, kotlin.jvm.functions.Function0<kotlin.Unit> onOpenSettings, kotlin.jvm.functions.Function2<? super com.pulselink.domain.model.Contact, ? super kotlin.coroutines.Continuation<? super kotlin.Unit>, ? extends java.lang.Object> onCallContact, kotlin.jvm.functions.Function2<? super java.lang.String, ? super kotlin.coroutines.Continuation<? super com.pulselink.domain.model.ManualMessageResult>, ? extends java.lang.Object> onSendMessage, kotlin.jvm.functions.Function1<? super kotlin.coroutines.Continuation<? super java.lang.Boolean>, ? extends java.lang.Object> onPing, kotlin.jvm.functions.Function2<? super java.lang.String, ? super kotlin.coroutines.Continuation<? super com.pulselink.data.assistant.VoiceCommandResult>, ? extends java.lang.Object> onVoiceCommand, kotlin.jvm.functions.Function0<kotlin.Unit> onUpgradeClick) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void StatusRow(com.pulselink.domain.model.Contact contact) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void ComposerRow(androidx.compose.ui.text.input.TextFieldValue input, kotlin.jvm.functions.Function1<? super androidx.compose.ui.text.input.TextFieldValue, kotlin.Unit> onInputChange, kotlin.jvm.functions.Function0<kotlin.Unit> onSend, kotlin.jvm.functions.Function0<kotlin.Unit> onVoice) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void MessageBubble(com.pulselink.domain.model.ContactMessage message) {
    }
}