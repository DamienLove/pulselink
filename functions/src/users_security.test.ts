// @ts-nocheck
import * as admin from "firebase-admin";
import * as functions from "firebase-functions";

jest.mock("firebase-admin", () => {
  const mAdd = jest.fn().mockResolvedValue({});
  const mCountGet = jest.fn().mockResolvedValue({data: () => ({count: 0})});
  const mCount = jest.fn().mockReturnValue({get: mCountGet});

  const queryObj: any = {
    get: jest.fn(),
    count: mCount,
    add: mAdd,
  };
  queryObj.where = jest.fn().mockReturnValue(queryObj);

  const mUserGet = jest.fn().mockResolvedValue({
      exists: true,
      data: () => ({deviceId: "dev123", ownerName: "Test User"}),
  });
  const mUsersCollection = {
      doc: jest.fn().mockReturnValue({get: mUserGet})
  };

  const mCollection = jest.fn((name) => {
    if (name === "rate_limits") return queryObj;
    if (name === "users") return mUsersCollection;
    return {
      doc: jest.fn().mockReturnValue({get: jest.fn()}),
    };
  });

  const mFirestore: any = jest.fn().mockReturnValue({
    collection: mCollection,
    FieldValue: {
      serverTimestamp: jest.fn().mockReturnValue("TIMESTAMP"),
    },
    Timestamp: {
      fromMillis: jest.fn().mockReturnValue("TIME_OBJ"),
    },
  });

  // Attach statics
  mFirestore.Timestamp = {
      fromMillis: jest.fn().mockReturnValue("TIME_OBJ"),
  };
  mFirestore.FieldValue = {
      serverTimestamp: jest.fn().mockReturnValue("TIMESTAMP"),
  };

  const mAuth = {
    getUserByPhoneNumber: jest.fn().mockResolvedValue({uid: "targetUid"}),
    getUserByEmail: jest.fn(),
  };

  return {
    initializeApp: jest.fn(),
    apps: ["app"],
    auth: () => mAuth,
    firestore: mFirestore,
  };
});

jest.mock("firebase-functions", () => {
  class MockHttpsError extends Error {
    code: string;
    constructor(code: string, message: string) {
      super(message);
      this.code = code;
    }
  }
  return {
    https: {
      onCall: (handler: any) => handler,
      HttpsError: MockHttpsError,
    },
  };
});

import {findUser} from "./users";

describe("findUser Security", () => {
  const context = {
    auth: {uid: "callerUid", token: {}},
  } as any;

  let db: any;
  let rateLimitQuery: any;
  let mCountGet: any;
  let mAdd: any;

  beforeEach(() => {
    jest.clearAllMocks();
    db = admin.firestore();
    // Re-acquire references to mocks
    rateLimitQuery = db.collection("rate_limits");
    mCountGet = rateLimitQuery.count().get;
    mAdd = rateLimitQuery.add;

    // Reset default behaviors
    mCountGet.mockResolvedValue({data: () => ({count: 0})});

    // Ensure auth mock is set (access via admin.auth())
    (admin.auth() as any).getUserByPhoneNumber.mockResolvedValue({uid: "targetUid"});
  });

  it("should block request if rate limit exceeded", async () => {
    // Setup: count returns 30
    mCountGet.mockResolvedValue({data: () => ({count: 30})});

    await expect((findUser as any)({phoneNumber: "+1234567890"}, context))
        .rejects
        .toThrow("Too many user lookups");

    expect(db.collection).toHaveBeenCalledWith("rate_limits");
  });

  it("should allow request and log attempt if under rate limit", async () => {
    // Setup: count returns 29
    mCountGet.mockResolvedValue({data: () => ({count: 29})});

    const result = await (findUser as any)({phoneNumber: "+1234567890"}, context);

    expect(result.found).toBe(true);
    expect(db.collection).toHaveBeenCalledWith("rate_limits");
    expect(mAdd).toHaveBeenCalledWith({
      uid: "callerUid",
      action: "findUser",
      timestamp: "TIMESTAMP",
    });
  });

  it("should check against correct timestamp window", async () => {
    mCountGet.mockResolvedValue({data: () => ({count: 0})});
    await (findUser as any)({phoneNumber: "+1234567890"}, context);

    // Verify chain: collection -> where(uid) -> where(action) -> where(timestamp)
    const whereCalls = rateLimitQuery.where.mock.calls;
    expect(whereCalls).toEqual(expect.arrayContaining([
      ["uid", "==", "callerUid"],
      ["action", "==", "findUser"],
      ["timestamp", ">", "TIME_OBJ"],
    ]));
  });
});
