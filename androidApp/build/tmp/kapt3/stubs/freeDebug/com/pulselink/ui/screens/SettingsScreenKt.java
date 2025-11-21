package com.pulselink.ui.screens;

import android.os.Build;
import androidx.compose.foundation.layout.Arrangement;
import androidx.compose.material.icons.Icons;
import androidx.compose.material3.ExperimentalMaterial3Api;
import androidx.compose.runtime.Composable;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.graphics.vector.ImageVector;
import androidx.compose.ui.text.font.FontWeight;
import com.pulselink.R;
import com.pulselink.domain.model.PulseLinkSettings;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u00002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u000e\u001a<\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\n\b\u0002\u0010\u0004\u001a\u0004\u0018\u00010\u00032\u0006\u0010\u0005\u001a\u00020\u00032\f\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00010\u00072\b\b\u0002\u0010\b\u001a\u00020\tH\u0003\u001a\u00be\u0001\u0010\n\u001a\u00020\u00012\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u000e2\u0012\u0010\u000f\u001a\u000e\u0012\u0004\u0012\u00020\u000e\u0012\u0004\u0012\u00020\u00010\u00102\f\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00010\u00072\u0012\u0010\u0012\u001a\u000e\u0012\u0004\u0012\u00020\u000e\u0012\u0004\u0012\u00020\u00010\u00102\f\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00010\u00072\f\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00010\u00072\f\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u00010\u00072\f\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u00010\u00072\f\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u00010\u00072\f\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u00010\u00072\f\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\u00010\u00072\f\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u00010\u0007H\u0007\u001a4\u0010\u001b\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00032\u0006\u0010\u001c\u001a\u00020\u000e2\u0012\u0010\u001d\u001a\u000e\u0012\u0004\u0012\u00020\u000e\u0012\u0004\u0012\u00020\u00010\u0010H\u0003\u00a8\u0006\u001e"}, d2 = {"SettingsActionRow", "", "title", "", "subtitle", "actionLabel", "onAction", "Lkotlin/Function0;", "leadingIcon", "Landroidx/compose/ui/graphics/vector/ImageVector;", "SettingsScreen", "settings", "Lcom/pulselink/domain/model/PulseLinkSettings;", "hasDndAccess", "", "onToggleIncludeLocation", "Lkotlin/Function1;", "onRequestDndAccess", "onToggleAutoAllowRemoteSoundChange", "onEditEmergencyTone", "onEditCheckInTone", "onEditCallTone", "onReportBug", "onBetaTesters", "onOpenHelp", "onSignOut", "onBack", "SettingsToggleRow", "checked", "onCheckedChange", "androidApp_freeDebug"})
public final class SettingsScreenKt {
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void SettingsScreen(@org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.PulseLinkSettings settings, boolean hasDndAccess, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Boolean, kotlin.Unit> onToggleIncludeLocation, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onRequestDndAccess, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Boolean, kotlin.Unit> onToggleAutoAllowRemoteSoundChange, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onEditEmergencyTone, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onEditCheckInTone, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onEditCallTone, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onReportBug, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onBetaTesters, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onOpenHelp, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onSignOut, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onBack) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void SettingsToggleRow(java.lang.String title, java.lang.String subtitle, boolean checked, kotlin.jvm.functions.Function1<? super java.lang.Boolean, kotlin.Unit> onCheckedChange) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void SettingsActionRow(java.lang.String title, java.lang.String subtitle, java.lang.String actionLabel, kotlin.jvm.functions.Function0<kotlin.Unit> onAction, androidx.compose.ui.graphics.vector.ImageVector leadingIcon) {
    }
}