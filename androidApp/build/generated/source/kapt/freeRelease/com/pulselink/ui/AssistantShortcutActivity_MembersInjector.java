package com.pulselink.ui;

import com.pulselink.data.assistant.NaturalLanguageCommandProcessor;
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
public final class AssistantShortcutActivity_MembersInjector implements MembersInjector<AssistantShortcutActivity> {
  private final Provider<AlertRouter> alertRouterProvider;

  private final Provider<NaturalLanguageCommandProcessor> naturalLanguageCommandProcessorProvider;

  private final Provider<ContactLinkManager> contactLinkManagerProvider;

  public AssistantShortcutActivity_MembersInjector(Provider<AlertRouter> alertRouterProvider,
      Provider<NaturalLanguageCommandProcessor> naturalLanguageCommandProcessorProvider,
      Provider<ContactLinkManager> contactLinkManagerProvider) {
    this.alertRouterProvider = alertRouterProvider;
    this.naturalLanguageCommandProcessorProvider = naturalLanguageCommandProcessorProvider;
    this.contactLinkManagerProvider = contactLinkManagerProvider;
  }

  public static MembersInjector<AssistantShortcutActivity> create(
      Provider<AlertRouter> alertRouterProvider,
      Provider<NaturalLanguageCommandProcessor> naturalLanguageCommandProcessorProvider,
      Provider<ContactLinkManager> contactLinkManagerProvider) {
    return new AssistantShortcutActivity_MembersInjector(alertRouterProvider, naturalLanguageCommandProcessorProvider, contactLinkManagerProvider);
  }

  @Override
  public void injectMembers(AssistantShortcutActivity instance) {
    injectAlertRouter(instance, alertRouterProvider.get());
    injectNaturalLanguageCommandProcessor(instance, naturalLanguageCommandProcessorProvider.get());
    injectContactLinkManager(instance, contactLinkManagerProvider.get());
  }

  @InjectedFieldSignature("com.pulselink.ui.AssistantShortcutActivity.alertRouter")
  public static void injectAlertRouter(AssistantShortcutActivity instance,
      AlertRouter alertRouter) {
    instance.alertRouter = alertRouter;
  }

  @InjectedFieldSignature("com.pulselink.ui.AssistantShortcutActivity.naturalLanguageCommandProcessor")
  public static void injectNaturalLanguageCommandProcessor(AssistantShortcutActivity instance,
      NaturalLanguageCommandProcessor naturalLanguageCommandProcessor) {
    instance.naturalLanguageCommandProcessor = naturalLanguageCommandProcessor;
  }

  @InjectedFieldSignature("com.pulselink.ui.AssistantShortcutActivity.contactLinkManager")
  public static void injectContactLinkManager(AssistantShortcutActivity instance,
      ContactLinkManager contactLinkManager) {
    instance.contactLinkManager = contactLinkManager;
  }
}
