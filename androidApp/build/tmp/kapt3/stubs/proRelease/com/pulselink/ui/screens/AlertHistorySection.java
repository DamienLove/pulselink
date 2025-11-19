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

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0000\n\u0002\u0010\b\n\u0002\b\b\b\u0082\u0081\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0011\b\u0002\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006j\u0002\b\u0007j\u0002\b\bj\u0002\b\tj\u0002\b\n\u00a8\u0006\u000b"}, d2 = {"Lcom/pulselink/ui/screens/AlertHistorySection;", "", "titleRes", "", "(Ljava/lang/String;II)V", "getTitleRes", "()I", "TODAY", "YESTERDAY", "THIS_WEEK", "OLDER", "androidApp_proRelease"})
enum AlertHistorySection {
    /*public static final*/ TODAY /* = new TODAY(0) */,
    /*public static final*/ YESTERDAY /* = new YESTERDAY(0) */,
    /*public static final*/ THIS_WEEK /* = new THIS_WEEK(0) */,
    /*public static final*/ OLDER /* = new OLDER(0) */;
    private final int titleRes = 0;
    
    AlertHistorySection(@androidx.annotation.StringRes()
    int titleRes) {
    }
    
    public final int getTitleRes() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public static kotlin.enums.EnumEntries<com.pulselink.ui.screens.AlertHistorySection> getEntries() {
        return null;
    }
}