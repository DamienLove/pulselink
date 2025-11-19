package com.pulselink.data.ads;

import com.pulselink.BuildConfig;

/**
 * Centralizes AdMob configuration derived from build variants.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001c\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\t\n\u0002\u0010\u000b\n\u0002\b\u0006\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u0011\u0010\u0003\u001a\u00020\u00048F\u00a2\u0006\u0006\u001a\u0004\b\u0005\u0010\u0006R\u0011\u0010\u0007\u001a\u00020\u00048F\u00a2\u0006\u0006\u001a\u0004\b\b\u0010\u0006R\u0011\u0010\t\u001a\u00020\u00048F\u00a2\u0006\u0006\u001a\u0004\b\n\u0010\u0006R\u0011\u0010\u000b\u001a\u00020\u00048F\u00a2\u0006\u0006\u001a\u0004\b\f\u0010\u0006R\u0011\u0010\r\u001a\u00020\u000e8F\u00a2\u0006\u0006\u001a\u0004\b\r\u0010\u000fR\u0011\u0010\u0010\u001a\u00020\u00048F\u00a2\u0006\u0006\u001a\u0004\b\u0011\u0010\u0006R\u0011\u0010\u0012\u001a\u00020\u00048F\u00a2\u0006\u0006\u001a\u0004\b\u0013\u0010\u0006\u00a8\u0006\u0014"}, d2 = {"Lcom/pulselink/data/ads/AdConfig;", "", "()V", "appId", "", "getAppId", "()Ljava/lang/String;", "appOpenUnitId", "getAppOpenUnitId", "bannerUnitId", "getBannerUnitId", "interstitialUnitId", "getInterstitialUnitId", "isAdsEnabled", "", "()Z", "nativeAdvancedUnitId", "getNativeAdvancedUnitId", "rewardedInterstitialUnitId", "getRewardedInterstitialUnitId", "androidApp_proRelease"})
public final class AdConfig {
    @org.jetbrains.annotations.NotNull()
    public static final com.pulselink.data.ads.AdConfig INSTANCE = null;
    
    private AdConfig() {
        super();
    }
    
    public final boolean isAdsEnabled() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getAppId() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getBannerUnitId() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getInterstitialUnitId() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getRewardedInterstitialUnitId() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getNativeAdvancedUnitId() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getAppOpenUnitId() {
        return null;
    }
}