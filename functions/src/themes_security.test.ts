/* eslint-disable @typescript-eslint/no-explicit-any */
import * as admin from "firebase-admin";
import {approveTheme} from "./themes";

// Mock firebase-admin
jest.mock("firebase-admin", () => {
  const firestoreMock = {
    collection: jest.fn(),
    runTransaction: jest.fn(),
  };

  const firestoreFn: any = jest.fn(() => firestoreMock);
  firestoreFn.FieldValue = {
    serverTimestamp: jest.fn(() => "MOCK_TIMESTAMP"),
  };

  return {
    apps: ["mock-app"],
    initializeApp: jest.fn(),
    firestore: firestoreFn,
  };
});

describe("approveTheme Security", () => {
  let firestoreMock: any;
  let transactionMock: any;
  let collectionMock: any;
  let docMock: any;

  beforeEach(() => {
    jest.clearAllMocks();
    firestoreMock = admin.firestore();

    transactionMock = {
      set: jest.fn(),
      delete: jest.fn(),
    };

    firestoreMock.runTransaction.mockImplementation(async (callback: any) => {
      return callback(transactionMock);
    });

    docMock = {
      get: jest.fn(),
    };

    collectionMock = {
      doc: jest.fn(() => docMock),
    };

    firestoreMock.collection.mockImplementation((name: string) => {
      if (name === "themes_submissions" || name === "themes_public") {
        return collectionMock;
      }
      return {doc: jest.fn()};
    });
  });

  it("should only copy allowed fields to public theme", async () => {
    // Setup context with admin claim
    const context = {
      auth: {
        uid: "admin-user",
        token: {admin: true},
      },
    } as any;

    // Malicious data in submission
    const maliciousSubmission = {
      name: "Safe Theme",
      theme: {primaryColor: "#000"},
      ownerUid: "user-123",
      // Malicious fields that should NOT be copied
      isAdmin: true,
      verified: true,
      internalNotes: "Secret notes",
      maliciousScript: "<script>alert(1)</script>",
      // Fields that will be overwritten anyway, but checking behavior
      status: "pending",
    };

    docMock.get.mockResolvedValue({
      exists: true,
      data: () => maliciousSubmission,
    });

    const data = {themeId: "submission-123"};

    await approveTheme.run(data, context);

    expect(transactionMock.set).toHaveBeenCalledTimes(1);

    const [, savedData] = transactionMock.set.mock.calls[0];

    // Check that we are writing to the public collection

    // Verify allowed fields exist
    expect(savedData).toHaveProperty("name", "Safe Theme");
    expect(savedData).toHaveProperty("ownerUid", "user-123");
    expect(savedData).toHaveProperty("status", "approved");

    // Verify MALICIOUS fields are NOT present
    // This expectation will FAIL currently, which confirms the vulnerability
    expect(savedData).not.toHaveProperty("isAdmin");
    expect(savedData).not.toHaveProperty("verified");
    expect(savedData).not.toHaveProperty("internalNotes");
    expect(savedData).not.toHaveProperty("maliciousScript");
  });
});
