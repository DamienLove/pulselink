package com.pulselink.receiver;

import com.pulselink.data.sms.SmsSender;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@QualifierMetadata
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
public final class SmsSendReceiver_MembersInjector implements MembersInjector<SmsSendReceiver> {
  private final Provider<SmsSender> smsSenderProvider;

  public SmsSendReceiver_MembersInjector(Provider<SmsSender> smsSenderProvider) {
    this.smsSenderProvider = smsSenderProvider;
  }

  public static MembersInjector<SmsSendReceiver> create(Provider<SmsSender> smsSenderProvider) {
    return new SmsSendReceiver_MembersInjector(smsSenderProvider);
  }

  @Override
  public void injectMembers(SmsSendReceiver instance) {
    injectSmsSender(instance, smsSenderProvider.get());
  }

  @InjectedFieldSignature("com.pulselink.receiver.SmsSendReceiver.smsSender")
  public static void injectSmsSender(SmsSendReceiver instance, SmsSender smsSender) {
    instance.smsSender = smsSender;
  }
}
