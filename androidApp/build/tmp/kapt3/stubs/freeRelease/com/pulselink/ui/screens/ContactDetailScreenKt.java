package com.pulselink.ui.screens;

import androidx.compose.foundation.layout.Arrangement;
import androidx.compose.material.icons.Icons;
import android.widget.Toast;
import androidx.compose.material3.CardDefaults;
import androidx.compose.material3.ExperimentalMaterial3Api;
import androidx.compose.runtime.Composable;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.text.font.FontWeight;
import com.pulselink.R;
import com.pulselink.domain.model.Contact;
import com.pulselink.domain.model.LinkStatus;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000<\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0010\u000b\n\u0002\b\u0011\u001a&\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00032\f\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00010\u0006H\u0003\u001a\u0091\u0002\u0010\u0007\u001a\u00020\u00012\b\u0010\b\u001a\u0004\u0018\u00010\t2\f\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u00010\u00062\"\u0010\u000b\u001a\u001e\b\u0001\u0012\u0004\u0012\u00020\t\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00010\r\u0012\u0006\u0012\u0004\u0018\u00010\u000e0\f2\f\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00010\u00062\f\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00010\u00062\u0012\u0010\u0011\u001a\u000e\u0012\u0004\u0012\u00020\u0013\u0012\u0004\u0012\u00020\u00010\u00122\u0012\u0010\u0014\u001a\u000e\u0012\u0004\u0012\u00020\u0013\u0012\u0004\u0012\u00020\u00010\u00122\u0012\u0010\u0015\u001a\u000e\u0012\u0004\u0012\u00020\u0013\u0012\u0004\u0012\u00020\u00010\u00122\u0012\u0010\u0016\u001a\u000e\u0012\u0004\u0012\u00020\u0013\u0012\u0004\u0012\u00020\u00010\u00122\u0012\u0010\u0017\u001a\u000e\u0012\u0004\u0012\u00020\u0013\u0012\u0004\u0012\u00020\u00010\u00122\f\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u00010\u00062\f\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\u00010\u00062\u001c\u0010\u001a\u001a\u0018\b\u0001\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00130\r\u0012\u0006\u0012\u0004\u0018\u00010\u000e0\u00122\f\u0010\u001b\u001a\b\u0012\u0004\u0012\u00020\u00010\u0006H\u0007\u00a2\u0006\u0002\u0010\u001c\u001a\u0010\u0010\u001d\u001a\u00020\u00012\u0006\u0010\b\u001a\u00020\tH\u0003\u001aN\u0010\u001e\u001a\u00020\u00012\u0006\u0010\b\u001a\u00020\t2\f\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u00010\u00062\f\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\u00010\u00062\u0012\u0010\u0017\u001a\u000e\u0012\u0004\u0012\u00020\u0013\u0012\u0004\u0012\u00020\u00010\u00122\f\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u00010\u0006H\u0003\u001a\u009e\u0001\u0010\u001f\u001a\u00020\u00012\u0006\u0010\b\u001a\u00020\t2\u0012\u0010\u0011\u001a\u000e\u0012\u0004\u0012\u00020\u0013\u0012\u0004\u0012\u00020\u00010\u00122\u0012\u0010\u0014\u001a\u000e\u0012\u0004\u0012\u00020\u0013\u0012\u0004\u0012\u00020\u00010\u00122\u0012\u0010\u0015\u001a\u000e\u0012\u0004\u0012\u00020\u0013\u0012\u0004\u0012\u00020\u00010\u00122\f\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00010\u00062\f\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00010\u00062\u0012\u0010\u0016\u001a\u000e\u0012\u0004\u0012\u00020\u0013\u0012\u0004\u0012\u00020\u00010\u00122\u0012\u0010\u0017\u001a\u000e\u0012\u0004\u0012\u00020\u0013\u0012\u0004\u0012\u00020\u00010\u00122\f\u0010\u001b\u001a\b\u0012\u0004\u0012\u00020\u00010\u0006H\u0003\u001a>\u0010 \u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00032\u0006\u0010!\u001a\u00020\u00132\u0012\u0010\"\u001a\u000e\u0012\u0004\u0012\u00020\u0013\u0012\u0004\u0012\u00020\u00010\u00122\b\b\u0002\u0010#\u001a\u00020\u0013H\u0003\u00a8\u0006$"}, d2 = {"ActionRow", "", "title", "", "subtitle", "onClick", "Lkotlin/Function0;", "ContactDetailScreen", "contact", "Lcom/pulselink/domain/model/Contact;", "onBack", "onCallContact", "Lkotlin/Function2;", "Lkotlin/coroutines/Continuation;", "", "onEditEmergencyAlert", "onEditCheckInAlert", "onToggleLocation", "Lkotlin/Function1;", "", "onToggleCamera", "onToggleAutoCall", "onToggleRemoteOverride", "onToggleRemoteSound", "onSendLink", "onApproveLink", "onPing", "onDelete", "(Lcom/pulselink/domain/model/Contact;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function2;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function0;)V", "Header", "LinkStatusSection", "SettingsCard", "ToggleRow", "checked", "onCheckedChange", "enabled", "androidApp_freeRelease"})
public final class ContactDetailScreenKt {
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void ContactDetailScreen(@org.jetbrains.annotations.Nullable()
    com.pulselink.domain.model.Contact contact, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onBack, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function2<? super com.pulselink.domain.model.Contact, ? super kotlin.coroutines.Continuation<? super kotlin.Unit>, ? extends java.lang.Object> onCallContact, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onEditEmergencyAlert, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onEditCheckInAlert, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Boolean, kotlin.Unit> onToggleLocation, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Boolean, kotlin.Unit> onToggleCamera, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Boolean, kotlin.Unit> onToggleAutoCall, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Boolean, kotlin.Unit> onToggleRemoteOverride, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Boolean, kotlin.Unit> onToggleRemoteSound, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onSendLink, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onApproveLink, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super kotlin.coroutines.Continuation<? super java.lang.Boolean>, ? extends java.lang.Object> onPing, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onDelete) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void Header(com.pulselink.domain.model.Contact contact) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void LinkStatusSection(com.pulselink.domain.model.Contact contact, kotlin.jvm.functions.Function0<kotlin.Unit> onSendLink, kotlin.jvm.functions.Function0<kotlin.Unit> onApproveLink, kotlin.jvm.functions.Function1<? super java.lang.Boolean, kotlin.Unit> onToggleRemoteSound, kotlin.jvm.functions.Function0<kotlin.Unit> onPing) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void SettingsCard(com.pulselink.domain.model.Contact contact, kotlin.jvm.functions.Function1<? super java.lang.Boolean, kotlin.Unit> onToggleLocation, kotlin.jvm.functions.Function1<? super java.lang.Boolean, kotlin.Unit> onToggleCamera, kotlin.jvm.functions.Function1<? super java.lang.Boolean, kotlin.Unit> onToggleAutoCall, kotlin.jvm.functions.Function0<kotlin.Unit> onEditEmergencyAlert, kotlin.jvm.functions.Function0<kotlin.Unit> onEditCheckInAlert, kotlin.jvm.functions.Function1<? super java.lang.Boolean, kotlin.Unit> onToggleRemoteOverride, kotlin.jvm.functions.Function1<? super java.lang.Boolean, kotlin.Unit> onToggleRemoteSound, kotlin.jvm.functions.Function0<kotlin.Unit> onDelete) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void ToggleRow(java.lang.String title, java.lang.String subtitle, boolean checked, kotlin.jvm.functions.Function1<? super java.lang.Boolean, kotlin.Unit> onCheckedChange, boolean enabled) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void ActionRow(java.lang.String title, java.lang.String subtitle, kotlin.jvm.functions.Function0<kotlin.Unit> onClick) {
    }
}