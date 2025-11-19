
import { genkit, z } from "genkit";
import { vertexAI, gemini15Pro } from "@genkit-ai/vertexai";
import { onCall } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import { HttpsError } from "firebase-functions/v2/https";

const apiKey = defineSecret("GOOGLE_GENAI_API_KEY");

const ai = genkit({
  plugins: [
    vertexAI({ location: "us-central1" }),
  ],
});

export const naturalLanguageQueryFlow = ai.defineFlow({
  name: "naturalLanguageQueryFlow",
  inputSchema: z.string().describe("A natural language query from a PulseLink user"),
  outputSchema: z.object({
    intent: z.string().describe("One of the supported PulseLink intents"),
    entities: z.record(z.string()).describe("Key/value pairs that parameterize the intent"),
  }),
}, async (query) => {
  const prompt = `
    You are the natural language interface for PulseLink, an emergency alert + escalation app.
    You MUST classify the user's sentence into one of the intents below and capture any relevant entities.

    Supported intents (return the lowercase identifier exactly as written):
    - send_emergency_alert: user wants to trigger the highest tier alert.
    - message_contact: user wants to send a manual message to a specific contact. Requires entity contactName. Optional entity messageBody.
    - activate_contact_siren: user wants to raise/activate a linked contact's siren/ping. Requires entity contactName.
    - cancel_emergency: user wants to cancel the currently active emergency / mark themselves safe.

    Entities:
    - contactName: the display name of the contact referenced (e.g., "mom", "Alex Johnson").
    - messageBody: free-form text the user wants to send.

    Always respond with valid JSON:
    {
      "intent": "<intent identifier>",
      "entities": {
        "contactName": "...",
        "messageBody": "...",
        ...
      }
    }

    If the query cannot be mapped, set intent to "unknown" and return an empty entities object.

    Query: ${query}
  `;

  const response = await ai.generate({
    model: gemini15Pro,
    prompt,
    output: {
      format: 'json',
      schema: z.object({
        intent: z.string(),
        entities: z.record(z.string()),
      }),
    },
    config: {
      temperature: 0.1,
    },
  });

  const structuredOutput = response.output;
  if (!structuredOutput) {
    throw new Error("Failed to get structured output from the model.");
  }
  return structuredOutput;
});

export const naturalLanguageQuery = onCall({
  secrets: [apiKey],
}, async (request) => {
    if (!request.auth) {
        throw new HttpsError('unauthenticated', 'The function must be called while authenticated.');
    }
    if (request.auth.token.pro !== true) {
        throw new HttpsError('permission-denied', `The function must be called by a user with the 'pro' claim.`);
    }
    return await naturalLanguageQueryFlow.run(request.data);
});
