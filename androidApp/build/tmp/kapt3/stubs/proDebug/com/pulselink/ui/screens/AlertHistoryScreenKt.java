package com.pulselink.ui.screens;

import android.text.format.DateUtils;
import androidx.annotation.StringRes;
import androidx.compose.foundation.layout.Arrangement;
import androidx.compose.material.icons.Icons;
import androidx.compose.material3.CardDefaults;
import androidx.compose.material3.ExperimentalMaterial3Api;
import androidx.compose.runtime.Composable;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.graphics.vector.ImageVector;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.text.style.TextAlign;
import androidx.compose.ui.text.style.TextOverflow;
import com.pulselink.R;
import com.pulselink.domain.model.AlertEvent;
import com.pulselink.domain.model.Contact;
import com.pulselink.domain.model.EscalationTier;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000Z\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\t\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\u001a*\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u0007H\u0003\u00f8\u0001\u0000\u00a2\u0006\u0004\b\b\u0010\t\u001a.\u0010\n\u001a\u00020\u00012\u0006\u0010\u000b\u001a\u00020\f2\b\u0010\r\u001a\u0004\u0018\u00010\u000e2\u0012\u0010\u000f\u001a\u000e\u0012\u0004\u0012\u00020\u0011\u0012\u0004\u0012\u00020\u00010\u0010H\u0003\u001a\u0012\u0010\u0012\u001a\u00020\u00012\b\b\u0002\u0010\u0013\u001a\u00020\u0014H\u0003\u001aF\u0010\u0015\u001a\u00020\u00012\f\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\f0\u00172\f\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u000e0\u00172\f\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\u00010\u001a2\u0012\u0010\u000f\u001a\u000e\u0012\u0004\u0012\u00020\u0011\u0012\u0004\u0012\u00020\u00010\u0010H\u0007\u001a\u0010\u0010\u001b\u001a\u00020\u00012\u0006\u0010\u001c\u001a\u00020\u0003H\u0003\u001a\u0018\u0010\u001d\u001a\u00020\u001e2\u0006\u0010\u001f\u001a\u00020\u00112\u0006\u0010 \u001a\u00020!H\u0002\u0082\u0002\u0007\n\u0005\b\u00a1\u001e0\u0001\u00a8\u0006\""}, d2 = {"AlertHistoryAvatar", "", "initial", "", "icon", "Landroidx/compose/ui/graphics/vector/ImageVector;", "tint", "Landroidx/compose/ui/graphics/Color;", "AlertHistoryAvatar-mxwnekA", "(Ljava/lang/String;Landroidx/compose/ui/graphics/vector/ImageVector;J)V", "AlertHistoryCard", "event", "Lcom/pulselink/domain/model/AlertEvent;", "contact", "Lcom/pulselink/domain/model/Contact;", "onContactClick", "Lkotlin/Function1;", "", "AlertHistoryEmptyState", "modifier", "Landroidx/compose/ui/Modifier;", "AlertHistoryScreen", "alerts", "", "contacts", "onBack", "Lkotlin/Function0;", "AlertHistorySectionHeader", "title", "sectionForTimestamp", "Lcom/pulselink/ui/screens/AlertHistorySection;", "timestamp", "zoneId", "Ljava/time/ZoneId;", "androidApp_proDebug"})
public final class AlertHistoryScreenKt {
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void AlertHistoryScreen(@org.jetbrains.annotations.NotNull()
    java.util.List<com.pulselink.domain.model.AlertEvent> alerts, @org.jetbrains.annotations.NotNull()
    java.util.List<com.pulselink.domain.model.Contact> contacts, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onBack, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Long, kotlin.Unit> onContactClick) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void AlertHistoryCard(com.pulselink.domain.model.AlertEvent event, com.pulselink.domain.model.Contact contact, kotlin.jvm.functions.Function1<? super java.lang.Long, kotlin.Unit> onContactClick) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void AlertHistorySectionHeader(java.lang.String title) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void AlertHistoryEmptyState(androidx.compose.ui.Modifier modifier) {
    }
    
    private static final com.pulselink.ui.screens.AlertHistorySection sectionForTimestamp(long timestamp, java.time.ZoneId zoneId) {
        return null;
    }
}