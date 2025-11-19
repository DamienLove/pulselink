package com.pulselink.data.sms;

import android.content.Context;
import android.telephony.SmsManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class SmsSender_Factory implements Factory<SmsSender> {
  private final Provider<Context> contextProvider;

  private final Provider<SmsManager> smsManagerProvider;

  public SmsSender_Factory(Provider<Context> contextProvider,
      Provider<SmsManager> smsManagerProvider) {
    this.contextProvider = contextProvider;
    this.smsManagerProvider = smsManagerProvider;
  }

  @Override
  public SmsSender get() {
    return newInstance(contextProvider.get(), smsManagerProvider.get());
  }

  public static SmsSender_Factory create(Provider<Context> contextProvider,
      Provider<SmsManager> smsManagerProvider) {
    return new SmsSender_Factory(contextProvider, smsManagerProvider);
  }

  public static SmsSender newInstance(Context context, SmsManager smsManager) {
    return new SmsSender(context, smsManager);
  }
}
