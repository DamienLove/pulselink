import {onThemeSubmitted} from "./themes";
import * as admin from "firebase-admin";

// --- Mocks Setup ---
jest.mock("firebase-admin", () => {
  const mSet = jest.fn().mockResolvedValue({});
  const mDelete = jest.fn().mockResolvedValue({});
  const mUpdate = jest.fn().mockResolvedValue({});
  const mDoc = jest.fn().mockReturnValue({
    set: mSet,
    delete: mDelete,
    update: mUpdate,
  });
  const mCollection = jest.fn().mockReturnValue({doc: mDoc});
  const mRunTransaction = jest.fn().mockImplementation(async (cb: any) => {
    const t = {
      set: mSet,
      delete: mDelete,
    };
    await cb(t);
  });

  const mFirestore: any = jest.fn().mockReturnValue({
    collection: mCollection,
    runTransaction: mRunTransaction,
  });
  mFirestore.FieldValue = {
    serverTimestamp: jest.fn().mockReturnValue("TIMESTAMP"),
  };

  return {
    firestore: mFirestore,
    initializeApp: jest.fn(),
    apps: [],
  };
});

jest.mock("firebase-functions/v2/firestore", () => ({
  onDocumentWritten: (trigger: string, handler: any) => handler,
}));

jest.mock("firebase-functions/logger", () => ({
  error: jest.fn(),
  info: jest.fn(),
  warn: jest.fn(),
}));

describe("onThemeSubmitted CSS Injection", () => {
  let firestoreMock: any;
  let deleteMock: any;
  let setMock: any;
  let updateMock: any;

  beforeEach(() => {
    jest.clearAllMocks();
    firestoreMock = (admin.firestore as unknown as jest.Mock);
    const db = firestoreMock();
    const collection = db.collection();
    const doc = collection.doc();
    deleteMock = doc.delete;
    setMock = doc.set;
    updateMock = doc.update;
  });

  const createEvent = (data: any, exists = true) => ({
    data: {
      after: {
        exists,
        data: () => data,
        ref: {delete: deleteMock, update: updateMock},
      },
    },
    params: {themeId: "test-theme-css-1"},
  });

  it("should REQUIRE REVIEW/REJECT themes with CSS injection in color fields",
      async () => {
        const cssInjectionTheme = {
          name: "CSS Injection Theme",
          status: "pending",
          theme: {
            primaryColor: "url(http://attacker.com/pixel)",
            // No explicit image fields like backgroundImageUrl
          },
        };
        const event = createEvent(cssInjectionTheme);

        await (onThemeSubmitted as any)(event);

        // If vulnerability exists, it will auto-approve (setMock called)
        // We want to ensure it DOES NOT auto-approve.
        expect(setMock).not.toHaveBeenCalled();
      });

  it("should REQUIRE REVIEW/REJECT themes with CSS injection in bubble fields",
      async () => {
        const cssInjectionTheme = {
          name: "CSS Injection Bubble",
          status: "pending",
          theme: {
            bubbleIncoming:
              "red; background-image: url(http://attacker.com/pixel);",
          },
        };
        const event = createEvent(cssInjectionTheme);

        await (onThemeSubmitted as any)(event);

        expect(setMock).not.toHaveBeenCalled();
      });

  it("should REQUIRE REVIEW/REJECT themes with uppercase URL(...) injection",
      async () => {
        const cssInjectionTheme = {
          name: "Uppercase CSS Injection",
          status: "pending",
          theme: {
            primaryColor: "URL(http://attacker.com/pixel)",
          },
        };
        const event = createEvent(cssInjectionTheme);

        await (onThemeSubmitted as any)(event);

        expect(setMock).not.toHaveBeenCalled();
      });
});
