package com.pulselink.data.link;

import android.content.Context;
import com.pulselink.data.sms.SmsSender;
import com.pulselink.domain.repository.AlertRepository;
import com.pulselink.domain.repository.BlockedContactRepository;
import com.pulselink.domain.repository.ContactRepository;
import com.pulselink.domain.repository.MessageRepository;
import com.pulselink.domain.repository.SettingsRepository;
import com.pulselink.util.CallStateMonitor;
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
public final class ContactLinkManager_Factory implements Factory<ContactLinkManager> {
  private final Provider<Context> contextProvider;

  private final Provider<SmsSender> smsSenderProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<AlertRepository> alertRepositoryProvider;

  private final Provider<ContactRepository> contactRepositoryProvider;

  private final Provider<BlockedContactRepository> blockedContactRepositoryProvider;

  private final Provider<RemoteActionHandler> remoteActionHandlerProvider;

  private final Provider<MessageRepository> messageRepositoryProvider;

  private final Provider<CallStateMonitor> callStateMonitorProvider;

  private final Provider<LinkChannelService> linkChannelServiceProvider;

  public ContactLinkManager_Factory(Provider<Context> contextProvider,
      Provider<SmsSender> smsSenderProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<AlertRepository> alertRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<BlockedContactRepository> blockedContactRepositoryProvider,
      Provider<RemoteActionHandler> remoteActionHandlerProvider,
      Provider<MessageRepository> messageRepositoryProvider,
      Provider<CallStateMonitor> callStateMonitorProvider,
      Provider<LinkChannelService> linkChannelServiceProvider) {
    this.contextProvider = contextProvider;
    this.smsSenderProvider = smsSenderProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.alertRepositoryProvider = alertRepositoryProvider;
    this.contactRepositoryProvider = contactRepositoryProvider;
    this.blockedContactRepositoryProvider = blockedContactRepositoryProvider;
    this.remoteActionHandlerProvider = remoteActionHandlerProvider;
    this.messageRepositoryProvider = messageRepositoryProvider;
    this.callStateMonitorProvider = callStateMonitorProvider;
    this.linkChannelServiceProvider = linkChannelServiceProvider;
  }

  @Override
  public ContactLinkManager get() {
    return newInstance(contextProvider.get(), smsSenderProvider.get(), settingsRepositoryProvider.get(), alertRepositoryProvider.get(), contactRepositoryProvider.get(), blockedContactRepositoryProvider.get(), remoteActionHandlerProvider.get(), messageRepositoryProvider.get(), callStateMonitorProvider.get(), linkChannelServiceProvider.get());
  }

  public static ContactLinkManager_Factory create(Provider<Context> contextProvider,
      Provider<SmsSender> smsSenderProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<AlertRepository> alertRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<BlockedContactRepository> blockedContactRepositoryProvider,
      Provider<RemoteActionHandler> remoteActionHandlerProvider,
      Provider<MessageRepository> messageRepositoryProvider,
      Provider<CallStateMonitor> callStateMonitorProvider,
      Provider<LinkChannelService> linkChannelServiceProvider) {
    return new ContactLinkManager_Factory(contextProvider, smsSenderProvider, settingsRepositoryProvider, alertRepositoryProvider, contactRepositoryProvider, blockedContactRepositoryProvider, remoteActionHandlerProvider, messageRepositoryProvider, callStateMonitorProvider, linkChannelServiceProvider);
  }

  public static ContactLinkManager newInstance(Context context, SmsSender smsSender,
      SettingsRepository settingsRepository, AlertRepository alertRepository,
      ContactRepository contactRepository, BlockedContactRepository blockedContactRepository,
      RemoteActionHandler remoteActionHandler, MessageRepository messageRepository,
      CallStateMonitor callStateMonitor, LinkChannelService linkChannelService) {
    return new ContactLinkManager(context, smsSender, settingsRepository, alertRepository, contactRepository, blockedContactRepository, remoteActionHandler, messageRepository, callStateMonitor, linkChannelService);
  }
}
