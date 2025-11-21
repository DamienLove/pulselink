package com.pulselink.ui.screens;

import android.content.Context;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.Toast;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.compose.foundation.ExperimentalFoundationApi;
import androidx.compose.foundation.layout.Arrangement;
import androidx.compose.material.icons.Icons;
import androidx.compose.material3.ButtonDefaults;
import androidx.compose.material3.CardDefaults;
import androidx.compose.material3.OutlinedTextFieldDefaults;
import androidx.compose.runtime.Composable;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.graphics.Brush;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.semantics.Role;
import androidx.compose.ui.text.input.TextFieldValue;
import androidx.compose.ui.text.style.TextAlign;
import com.pulselink.R;
import com.pulselink.domain.model.Contact;
import com.pulselink.domain.model.LinkStatus;
import com.pulselink.ui.state.PulseLinkUiState;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000\u0086\u0001\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\t\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\t\n\u0002\b\u0003\n\u0002\u0010 \n\u0002\b\t\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u000f\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\u001a\u0086\u0001\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0012\u0010\u0004\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00010\u00052\u0006\u0010\u0006\u001a\u00020\u00032\u0012\u0010\u0007\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00010\u00052\u0006\u0010\b\u001a\u00020\t2\u0012\u0010\n\u001a\u000e\u0012\u0004\u0012\u00020\t\u0012\u0004\u0012\u00020\u00010\u00052\f\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\u00010\f2\f\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u00010\f2\f\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00010\fH\u0003\u001a \u0010\u000f\u001a\u00020\u00012\b\b\u0002\u0010\u0010\u001a\u00020\u00112\f\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00010\fH\u0003\u001a(\u0010\u0013\u001a\u00020\u00012\u0006\u0010\u0014\u001a\u00020\t2\f\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u00010\f2\b\b\u0002\u0010\u0010\u001a\u00020\u0011H\u0003\u001af\u0010\u0016\u001a\u00020\u00012\u0006\u0010\u0017\u001a\u00020\u00182\f\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\u00010\f2\f\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u00010\f2\f\u0010\u001b\u001a\b\u0012\u0004\u0012\u00020\u00010\f2\f\u0010\u001c\u001a\b\u0012\u0004\u0012\u00020\u00010\f2\f\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\u00010\f2\u0006\u0010\u001e\u001a\u00020\t2\u0006\u0010\u001f\u001a\u00020\tH\u0003\u001a\u0096\u0001\u0010 \u001a\u00020\u00012\u0006\u0010!\u001a\u00020\"2\u0006\u0010#\u001a\u00020$2\u0012\u0010%\u001a\u000e\u0012\u0004\u0012\u00020\u0018\u0012\u0004\u0012\u00020\u00010\u00052\u0012\u0010&\u001a\u000e\u0012\u0004\u0012\u00020\'\u0012\u0004\u0012\u00020\u00010\u00052\u0012\u0010(\u001a\u000e\u0012\u0004\u0012\u00020\'\u0012\u0004\u0012\u00020\u00010\u00052\u0012\u0010)\u001a\u000e\u0012\u0004\u0012\u00020\'\u0012\u0004\u0012\u00020\u00010\u00052\u0012\u0010\u001c\u001a\u000e\u0012\u0004\u0012\u00020\'\u0012\u0004\u0012\u00020\u00010\u00052\u0018\u0010*\u001a\u0014\u0012\n\u0012\b\u0012\u0004\u0012\u00020\'0+\u0012\u0004\u0012\u00020\u00010\u0005H\u0003\u001aH\u0010,\u001a\u00020\u00012\u0006\u0010!\u001a\u00020\"2\f\u0010-\u001a\b\u0012\u0004\u0012\u00020\u00010\f2\f\u0010.\u001a\b\u0012\u0004\u0012\u00020\u00010\f2\f\u0010/\u001a\b\u0012\u0004\u0012\u00020\u00010\f2\f\u00100\u001a\b\u0012\u0004\u0012\u00020\u00010\fH\u0003\u001a\u00c3\u0002\u00101\u001a\u00020\u00012\u0006\u0010!\u001a\u00020\"2\f\u0010-\u001a\b\u0012\u0004\u0012\u00020\u00010\f2\f\u00102\u001a\b\u0012\u0004\u0012\u00020\u00010\f2\f\u00103\u001a\b\u0012\u0004\u0012\u00020\u00010\f2\u0012\u00104\u001a\u000e\u0012\u0004\u0012\u00020\u0018\u0012\u0004\u0012\u00020\u00010\u00052\u0012\u0010&\u001a\u000e\u0012\u0004\u0012\u00020\'\u0012\u0004\u0012\u00020\u00010\u00052\u0012\u0010(\u001a\u000e\u0012\u0004\u0012\u00020\'\u0012\u0004\u0012\u00020\u00010\u00052\u0012\u0010)\u001a\u000e\u0012\u0004\u0012\u00020\'\u0012\u0004\u0012\u00020\u00010\u00052\u0012\u0010\u001c\u001a\u000e\u0012\u0004\u0012\u00020\'\u0012\u0004\u0012\u00020\u00010\u00052\"\u0010%\u001a\u001e\b\u0001\u0012\u0004\u0012\u00020\u0018\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u000106\u0012\u0006\u0012\u0004\u0018\u000107052\u0018\u0010*\u001a\u0014\u0012\n\u0012\b\u0012\u0004\u0012\u00020\'0+\u0012\u0004\u0012\u00020\u00010\u00052\f\u00108\u001a\b\u0012\u0004\u0012\u00020\u00010\f2\b\b\u0002\u00109\u001a\u00020\t2\u000e\b\u0002\u0010.\u001a\b\u0012\u0004\u0012\u00020\u00010\f2\u000e\b\u0002\u0010/\u001a\b\u0012\u0004\u0012\u00020\u00010\f2\b\b\u0002\u0010:\u001a\u00020\t2\u000e\b\u0002\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00010\f2\u000e\b\u0002\u00100\u001a\b\u0012\u0004\u0012\u00020\u00010\fH\u0007\u00a2\u0006\u0002\u0010;\u001a,\u0010<\u001a\u00020\u00012\u0006\u0010\u0017\u001a\u00020\u00182\f\u0010\u001b\u001a\b\u0012\u0004\u0012\u00020\u00010\f2\f\u0010\u001c\u001a\b\u0012\u0004\u0012\u00020\u00010\fH\u0003\u001a&\u0010=\u001a\u00020\u00012\u0006\u0010>\u001a\u00020?2\u0006\u0010@\u001a\u00020$2\f\u0010A\u001a\b\u0012\u0004\u0012\u00020\u00010\fH\u0003\u001a:\u0010B\u001a\u00020\u00012\f\u0010.\u001a\b\u0012\u0004\u0012\u00020\u00010\f2\f\u0010/\u001a\b\u0012\u0004\u0012\u00020\u00010\f2\f\u00100\u001a\b\u0012\u0004\u0012\u00020\u00010\f2\u0006\u0010C\u001a\u00020\tH\u0003\u001a:\u0010D\u001a\u00020\u00012\b\b\u0002\u0010\u0010\u001a\u00020\u00112\u0006\u0010@\u001a\u00020$2\u0006\u0010E\u001a\u00020F2\f\u0010A\u001a\b\u0012\u0004\u0012\u00020\u00010\f2\b\b\u0002\u0010G\u001a\u00020\tH\u0003\u001aB\u0010H\u001a\u00020\u00012\f\u00102\u001a\b\u0012\u0004\u0012\u00020\u00010\f2\f\u0010I\u001a\b\u0012\u0004\u0012\u00020\u00010\f2\u0006\u0010J\u001a\u00020\t2\f\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u00010\f2\u0006\u00109\u001a\u00020\tH\u0003\u001a\u001a\u0010K\u001a\u00020\u00012\u0006\u0010L\u001a\u00020\t2\b\b\u0002\u0010\u0010\u001a\u00020\u0011H\u0003\u001a\b\u0010M\u001a\u00020\u0001H\u0003\u001a2\u0010N\u001a\u00020\u00012\u0006\u0010O\u001a\u00020\u00032\u0012\u0010P\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00010\u00052\f\u0010Q\u001a\b\u0012\u0004\u0012\u00020\u00010\fH\u0003\u001a\u001e\u0010R\u001a\u00020\u00012\u0006\u0010S\u001a\u00020\t2\f\u00100\u001a\b\u0012\u0004\u0012\u00020\u00010\fH\u0003\u001a6\u0010T\u001a\u00020\u00012\b\b\u0002\u0010\u0010\u001a\u00020\u00112\u0006\u0010C\u001a\u00020\t2\f\u00100\u001a\b\u0012\u0004\u0012\u00020\u00010\f2\f\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u00010\fH\u0003\u001a&\u0010U\u001a\u0010\u0012\u0004\u0012\u00020$\u0012\u0004\u0012\u00020$\u0018\u00010V2\u0006\u0010W\u001a\u00020X2\u0006\u0010Y\u001a\u00020ZH\u0002\u00a8\u0006["}, d2 = {"AddContactDialog", "", "name", "Landroidx/compose/ui/text/input/TextFieldValue;", "onNameChange", "Lkotlin/Function1;", "phone", "onPhoneChange", "allowRemoteSound", "", "onAllowRemoteSoundChange", "onImport", "Lkotlin/Function0;", "onDismiss", "onSave", "AddLoginCard", "modifier", "Landroidx/compose/ui/Modifier;", "onAddLoginClick", "CancelEmergencyCard", "isCanceling", "onCancelEmergency", "ContactRow", "contact", "Lcom/pulselink/domain/model/Contact;", "onOpenMessages", "onOpenSettings", "onSendLinkRequest", "onApproveLink", "onCall", "reorderEnabled", "isDragging", "ContactsList", "state", "Lcom/pulselink/ui/state/PulseLinkUiState;", "searchQuery", "", "onCallContact", "onContactSelected", "", "onContactSettings", "onSendLink", "onReorderContacts", "", "HeaderSection", "onDismissAssistantShortcuts", "onAlertsClick", "onSettingsClick", "onUpgradeClick", "HomeScreen", "onTriggerEmergency", "onSendCheckIn", "onAddContact", "Lkotlin/Function2;", "Lkotlin/coroutines/Continuation;", "", "onRequestCancelEmergency", "isCancelingEmergency", "showAddLoginPrompt", "(Lcom/pulselink/ui/state/PulseLinkUiState;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function2;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function0;ZLkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;ZLkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;)V", "LinkActionButtons", "NavButton", "icon", "Landroidx/compose/ui/graphics/vector/ImageVector;", "label", "onClick", "NavigationRow", "isProUser", "QuickActionTile", "background", "Landroidx/compose/ui/graphics/Brush;", "enabled", "QuickActionsRow", "onSendCheckInAll", "isEmergencyActive", "ReachabilityBadge", "hasSmsFallback", "ScrollHintCard", "SearchAndAddRow", "searchValue", "onSearchChange", "onAddClick", "UpgradeCard", "isPro", "VoiceTipsCard", "resolveContact", "Lkotlin/Pair;", "context", "Landroid/content/Context;", "uri", "Landroid/net/Uri;", "androidApp_proDebug"})
public final class HomeScreenKt {
    
    @androidx.compose.runtime.Composable()
    public static final void HomeScreen(@org.jetbrains.annotations.NotNull()
    com.pulselink.ui.state.PulseLinkUiState state, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onDismissAssistantShortcuts, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onTriggerEmergency, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onSendCheckIn, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.pulselink.domain.model.Contact, kotlin.Unit> onAddContact, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Long, kotlin.Unit> onContactSelected, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Long, kotlin.Unit> onContactSettings, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Long, kotlin.Unit> onSendLink, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Long, kotlin.Unit> onApproveLink, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function2<? super com.pulselink.domain.model.Contact, ? super kotlin.coroutines.Continuation<? super kotlin.Unit>, ? extends java.lang.Object> onCallContact, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.util.List<java.lang.Long>, kotlin.Unit> onReorderContacts, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onRequestCancelEmergency, boolean isCancelingEmergency, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onAlertsClick, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onSettingsClick, boolean showAddLoginPrompt, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onAddLoginClick, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onUpgradeClick) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void HeaderSection(com.pulselink.ui.state.PulseLinkUiState state, kotlin.jvm.functions.Function0<kotlin.Unit> onDismissAssistantShortcuts, kotlin.jvm.functions.Function0<kotlin.Unit> onAlertsClick, kotlin.jvm.functions.Function0<kotlin.Unit> onSettingsClick, kotlin.jvm.functions.Function0<kotlin.Unit> onUpgradeClick) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void AddLoginCard(androidx.compose.ui.Modifier modifier, kotlin.jvm.functions.Function0<kotlin.Unit> onAddLoginClick) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void VoiceTipsCard(androidx.compose.ui.Modifier modifier, boolean isProUser, kotlin.jvm.functions.Function0<kotlin.Unit> onUpgradeClick, kotlin.jvm.functions.Function0<kotlin.Unit> onDismiss) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void NavigationRow(kotlin.jvm.functions.Function0<kotlin.Unit> onAlertsClick, kotlin.jvm.functions.Function0<kotlin.Unit> onSettingsClick, kotlin.jvm.functions.Function0<kotlin.Unit> onUpgradeClick, boolean isProUser) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void NavButton(androidx.compose.ui.graphics.vector.ImageVector icon, java.lang.String label, kotlin.jvm.functions.Function0<kotlin.Unit> onClick) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void QuickActionsRow(kotlin.jvm.functions.Function0<kotlin.Unit> onTriggerEmergency, kotlin.jvm.functions.Function0<kotlin.Unit> onSendCheckInAll, boolean isEmergencyActive, kotlin.jvm.functions.Function0<kotlin.Unit> onCancelEmergency, boolean isCancelingEmergency) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void QuickActionTile(androidx.compose.ui.Modifier modifier, java.lang.String label, androidx.compose.ui.graphics.Brush background, kotlin.jvm.functions.Function0<kotlin.Unit> onClick, boolean enabled) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void ScrollHintCard() {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void CancelEmergencyCard(boolean isCanceling, kotlin.jvm.functions.Function0<kotlin.Unit> onCancelEmergency, androidx.compose.ui.Modifier modifier) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void SearchAndAddRow(androidx.compose.ui.text.input.TextFieldValue searchValue, kotlin.jvm.functions.Function1<? super androidx.compose.ui.text.input.TextFieldValue, kotlin.Unit> onSearchChange, kotlin.jvm.functions.Function0<kotlin.Unit> onAddClick) {
    }
    
    @kotlin.OptIn(markerClass = {androidx.compose.foundation.ExperimentalFoundationApi.class})
    @androidx.compose.runtime.Composable()
    private static final void ContactsList(com.pulselink.ui.state.PulseLinkUiState state, java.lang.String searchQuery, kotlin.jvm.functions.Function1<? super com.pulselink.domain.model.Contact, kotlin.Unit> onCallContact, kotlin.jvm.functions.Function1<? super java.lang.Long, kotlin.Unit> onContactSelected, kotlin.jvm.functions.Function1<? super java.lang.Long, kotlin.Unit> onContactSettings, kotlin.jvm.functions.Function1<? super java.lang.Long, kotlin.Unit> onSendLink, kotlin.jvm.functions.Function1<? super java.lang.Long, kotlin.Unit> onApproveLink, kotlin.jvm.functions.Function1<? super java.util.List<java.lang.Long>, kotlin.Unit> onReorderContacts) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void ContactRow(com.pulselink.domain.model.Contact contact, kotlin.jvm.functions.Function0<kotlin.Unit> onOpenMessages, kotlin.jvm.functions.Function0<kotlin.Unit> onOpenSettings, kotlin.jvm.functions.Function0<kotlin.Unit> onSendLinkRequest, kotlin.jvm.functions.Function0<kotlin.Unit> onApproveLink, kotlin.jvm.functions.Function0<kotlin.Unit> onCall, boolean reorderEnabled, boolean isDragging) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void ReachabilityBadge(boolean hasSmsFallback, androidx.compose.ui.Modifier modifier) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void LinkActionButtons(com.pulselink.domain.model.Contact contact, kotlin.jvm.functions.Function0<kotlin.Unit> onSendLinkRequest, kotlin.jvm.functions.Function0<kotlin.Unit> onApproveLink) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void UpgradeCard(boolean isPro, kotlin.jvm.functions.Function0<kotlin.Unit> onUpgradeClick) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void AddContactDialog(androidx.compose.ui.text.input.TextFieldValue name, kotlin.jvm.functions.Function1<? super androidx.compose.ui.text.input.TextFieldValue, kotlin.Unit> onNameChange, androidx.compose.ui.text.input.TextFieldValue phone, kotlin.jvm.functions.Function1<? super androidx.compose.ui.text.input.TextFieldValue, kotlin.Unit> onPhoneChange, boolean allowRemoteSound, kotlin.jvm.functions.Function1<? super java.lang.Boolean, kotlin.Unit> onAllowRemoteSoundChange, kotlin.jvm.functions.Function0<kotlin.Unit> onImport, kotlin.jvm.functions.Function0<kotlin.Unit> onDismiss, kotlin.jvm.functions.Function0<kotlin.Unit> onSave) {
    }
    
    private static final kotlin.Pair<java.lang.String, java.lang.String> resolveContact(android.content.Context context, android.net.Uri uri) {
        return null;
    }
}