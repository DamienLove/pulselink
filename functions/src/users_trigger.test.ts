// Mock mocks
const mockSendMulticast = jest.fn().mockResolvedValue({ successCount: 1 });
const mockMessaging = jest.fn(() => ({
  sendMulticast: mockSendMulticast,
}));

const mockCollection = jest.fn();
const mockFirestore = jest.fn(() => ({
  collection: mockCollection,
}));

jest.mock('firebase-admin', () => ({
  apps: [],
  initializeApp: jest.fn(),
  firestore: mockFirestore,
  messaging: mockMessaging,
  auth: () => ({
      getUserByPhoneNumber: jest.fn(),
      getUserByEmail: jest.fn(),
      deleteUser: jest.fn(),
  }),
}));

const mockOnUpdate = jest.fn();
jest.mock('firebase-functions', () => ({
  firestore: {
    document: jest.fn(() => ({
      onUpdate: mockOnUpdate,
    })),
  },
  https: {
      onCall: jest.fn(),
      HttpsError: class extends Error {},
  }
}));

// Import the module under test
import { onUserUpdated } from './users';

describe('onUserUpdated Trigger', () => {
  let wrapped: any;

  beforeAll(() => {
    // Force usage to avoid TS unused error
    void onUserUpdated;

    // Extract the handler function from the mock
    wrapped = mockOnUpdate.mock.calls[0][0];
  });

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should send SYNC_REQUEST when premium status becomes active', async () => {
    const beforeData = { premiumSubscriptionStatus: 'free', remoteWebAccessEnabled: false };
    const afterData = { premiumSubscriptionStatus: 'SUBSCRIPTION_STATE_ACTIVE', remoteWebAccessEnabled: false };

    const change = {
      before: { data: () => beforeData },
      after: { data: () => afterData },
    };
    const context = { params: { uid: 'test_uid' } };

    // Mock Firestore response for devices
    const mockGet = jest.fn().mockResolvedValue({
      empty: false,
      forEach: (cb: any) => {
        cb({ data: () => ({ fcmToken: 'token1' }) });
        cb({ data: () => ({ fcmToken: 'token2' }) });
      },
    });
    const mockWhere = jest.fn().mockReturnValue({ get: mockGet });
    mockCollection.mockReturnValue({ where: mockWhere });

    await wrapped(change, context);

    expect(mockCollection).toHaveBeenCalledWith('devices');
    expect(mockWhere).toHaveBeenCalledWith('uid', '==', 'test_uid');
    expect(mockSendMulticast).toHaveBeenCalledWith(expect.objectContaining({
      tokens: ['token1', 'token2'],
      data: expect.objectContaining({ type: 'SYNC_REQUEST' }),
    }));
  });

  it('should send SYNC_REQUEST when remoteWebAccessEnabled becomes true', async () => {
    const beforeData = { premiumSubscriptionStatus: 'free', remoteWebAccessEnabled: false };
    const afterData = { premiumSubscriptionStatus: 'free', remoteWebAccessEnabled: true };

    const change = {
      before: { data: () => beforeData },
      after: { data: () => afterData },
    };
    const context = { params: { uid: 'test_uid' } };

    const mockGet = jest.fn().mockResolvedValue({
        empty: false,
        forEach: (cb: any) => {
          cb({ data: () => ({ fcmToken: 'token1' }) });
        },
      });
      const mockWhere = jest.fn().mockReturnValue({ get: mockGet });
      mockCollection.mockReturnValue({ where: mockWhere });

    await wrapped(change, context);

    expect(mockSendMulticast).toHaveBeenCalled();
  });

  it('should NOT send SYNC_REQUEST when unrelated fields change', async () => {
    const beforeData = { premiumSubscriptionStatus: 'free', remoteWebAccessEnabled: false, name: 'Old' };
    const afterData = { premiumSubscriptionStatus: 'free', remoteWebAccessEnabled: false, name: 'New' };

    const change = {
      before: { data: () => beforeData },
      after: { data: () => afterData },
    };
    const context = { params: { uid: 'test_uid' } };

    await wrapped(change, context);

    expect(mockSendMulticast).not.toHaveBeenCalled();
  });

   it('should NOT send SYNC_REQUEST when already premium', async () => {
      const beforeData = { premiumSubscriptionStatus: 'SUBSCRIPTION_STATE_ACTIVE', remoteWebAccessEnabled: true };
      const afterData = { premiumSubscriptionStatus: 'SUBSCRIPTION_STATE_ACTIVE', remoteWebAccessEnabled: true, name: 'New' };

      const change = {
        before: { data: () => beforeData },
        after: { data: () => afterData },
      };
      const context = { params: { uid: 'test_uid' } };

      await wrapped(change, context);

      expect(mockSendMulticast).not.toHaveBeenCalled();
    });
});
