package com.pulselink.receiver;

import com.pulselink.data.link.ContactLinkManager;
import com.pulselink.service.AlertRouter;
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
public final class PulseLinkSmsReceiver_MembersInjector implements MembersInjector<PulseLinkSmsReceiver> {
  private final Provider<AlertRouter> alertRouterProvider;

  private final Provider<ContactLinkManager> contactLinkManagerProvider;

  public PulseLinkSmsReceiver_MembersInjector(Provider<AlertRouter> alertRouterProvider,
      Provider<ContactLinkManager> contactLinkManagerProvider) {
    this.alertRouterProvider = alertRouterProvider;
    this.contactLinkManagerProvider = contactLinkManagerProvider;
  }

  public static MembersInjector<PulseLinkSmsReceiver> create(
      Provider<AlertRouter> alertRouterProvider,
      Provider<ContactLinkManager> contactLinkManagerProvider) {
    return new PulseLinkSmsReceiver_MembersInjector(alertRouterProvider, contactLinkManagerProvider);
  }

  @Override
  public void injectMembers(PulseLinkSmsReceiver instance) {
    injectAlertRouter(instance, alertRouterProvider.get());
    injectContactLinkManager(instance, contactLinkManagerProvider.get());
  }

  @InjectedFieldSignature("com.pulselink.receiver.PulseLinkSmsReceiver.alertRouter")
  public static void injectAlertRouter(PulseLinkSmsReceiver instance, AlertRouter alertRouter) {
    instance.alertRouter = alertRouter;
  }

  @InjectedFieldSignature("com.pulselink.receiver.PulseLinkSmsReceiver.contactLinkManager")
  public static void injectContactLinkManager(PulseLinkSmsReceiver instance,
      ContactLinkManager contactLinkManager) {
    instance.contactLinkManager = contactLinkManager;
  }
}
