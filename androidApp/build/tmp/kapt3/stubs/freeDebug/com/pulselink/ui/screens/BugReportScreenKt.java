package com.pulselink.ui.screens;

import android.widget.Toast;
import androidx.compose.foundation.layout.Arrangement;
import androidx.compose.material.icons.Icons;
import androidx.compose.material3.ExperimentalMaterial3Api;
import androidx.compose.material3.ExposedDropdownMenuDefaults;
import androidx.compose.material3.TextFieldDefaults;
import androidx.compose.runtime.Composable;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.graphics.Brush;
import androidx.compose.ui.text.font.FontWeight;
import com.pulselink.R;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u00002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0010\u000e\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0010\b\n\u0000\u001aF\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\u00022\u0012\u0010\f\u001a\u000e\u0012\u0004\u0012\u00020\u0002\u0012\u0004\u0012\u00020\n0\r2\f\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\n0\u000f2\u0012\u0010\u0010\u001a\u000e\u0012\u0004\u0012\u00020\u0002\u0012\u0004\u0012\u00020\n0\rH\u0007\u001a>\u0010\u0011\u001a\u00020\n2\u0006\u0010\u0012\u001a\u00020\u00042\u0006\u0010\u0013\u001a\u00020\u00042\u0012\u0010\u0014\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\n0\r2\u0006\u0010\u0015\u001a\u00020\u00042\b\b\u0002\u0010\u0016\u001a\u00020\u0017H\u0003\"#\u0010\u0000\u001a\u0014\u0012\u0004\u0012\u00020\u0002\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00040\u00030\u0001\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\"\u0014\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\"\u0014\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0018"}, d2 = {"BugReportDataSaver", "Landroidx/compose/runtime/saveable/Saver;", "Lcom/pulselink/ui/screens/BugReportData;", "", "", "getBugReportDataSaver", "()Landroidx/compose/runtime/saveable/Saver;", "FREQUENCY_OPTIONS", "SEVERITY_OPTIONS", "BugReportScreen", "", "data", "onDataChange", "Lkotlin/Function1;", "onBack", "Lkotlin/Function0;", "onSubmit", "BugReportTextField", "label", "value", "onValueChange", "placeholder", "minLines", "", "androidApp_freeDebug"})
public final class BugReportScreenKt {
    @org.jetbrains.annotations.NotNull()
    private static final androidx.compose.runtime.saveable.Saver<com.pulselink.ui.screens.BugReportData, java.util.List<java.lang.String>> BugReportDataSaver = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.util.List<java.lang.String> FREQUENCY_OPTIONS = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.util.List<java.lang.String> SEVERITY_OPTIONS = null;
    
    @org.jetbrains.annotations.NotNull()
    public static final androidx.compose.runtime.saveable.Saver<com.pulselink.ui.screens.BugReportData, java.util.List<java.lang.String>> getBugReportDataSaver() {
        return null;
    }
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void BugReportScreen(@org.jetbrains.annotations.NotNull()
    com.pulselink.ui.screens.BugReportData data, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.pulselink.ui.screens.BugReportData, kotlin.Unit> onDataChange, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onBack, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.pulselink.ui.screens.BugReportData, kotlin.Unit> onSubmit) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void BugReportTextField(java.lang.String label, java.lang.String value, kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onValueChange, java.lang.String placeholder, int minLines) {
    }
}