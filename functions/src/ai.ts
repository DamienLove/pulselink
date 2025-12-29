/* eslint-disable max-len */
import {genkit, z} from "genkit";
import {vertexAI, gemini15Flash} from "@genkit-ai/vertexai";
import {onCall, HttpsError} from "firebase-functions/v2/https";

const ai = genkit({
  plugins: [
    vertexAI({location: "us-central1"}),
  ],
});

const MODEL = gemini15Flash;

const summarizeSmsThreadFlow = ai.defineFlow({
  name: "summarizeSmsThreadFlow",
  inputSchema: z.object({
    messages: z.array(z.string()).min(1),
    contactName: z.string().optional(),
  }),
  outputSchema: z.object({
    summary: z.string(),
  }),
}, async (input) => {
  const contactName = input.contactName ?? "the contact";
  const prompt = `
You summarize SMS threads for PulseLink Premium.
Write a concise 1-2 sentence summary (max 240 chars).
If the thread suggests urgency or danger, mention it.
Do NOT invent facts. Use plain language.

Contact: ${contactName}
Messages:
${input.messages.join("\n")}
`;

  const response = await ai.generate({
    model: MODEL,
    prompt,
    output: {
      format: "json",
      schema: z.object({
        summary: z.string(),
      }),
    },
    config: {
      temperature: 0.2,
    },
  });

  const structuredOutput = response.output;
  if (!structuredOutput) {
    throw new Error("Failed to summarize thread.");
  }
  return structuredOutput;
});

const composeSmsAssistFlow = ai.defineFlow({
  name: "composeSmsAssistFlow",
  inputSchema: z.object({
    action: z.string(),
    draft: z.string().optional(),
    lastMessage: z.string().optional(),
  }),
  outputSchema: z.object({
    text: z.string(),
  }),
}, async (input) => {
  const prompt = `
You assist with composing SMS replies for PulseLink Premium.
Action: ${input.action}
Draft: ${input.draft ?? ""}
LastMessage: ${input.lastMessage ?? ""}

Rules:
- Return a single message under 320 characters.
- Keep tone empathetic and clear.
- If action is "reply", respond to LastMessage.
- If action is "urgent", make it firm but calm.
- If no draft is provided for rewrite/shorten/expand/polish, return a helpful suggestion anyway.

Return JSON: {"text":"..."} only.
`;

  const response = await ai.generate({
    model: MODEL,
    prompt,
    output: {
      format: "json",
      schema: z.object({
        text: z.string(),
      }),
    },
    config: {
      temperature: 0.4,
    },
  });

  const structuredOutput = response.output;
  if (!structuredOutput) {
    throw new Error("Failed to generate suggestion.");
  }
  return structuredOutput;
});

const classifySmsUrgencyFlow = ai.defineFlow({
  name: "classifySmsUrgencyFlow",
  inputSchema: z.object({
    message: z.string(),
  }),
  outputSchema: z.object({
    urgency: z.enum(["standard", "urgent", "emergency"]),
    confidence: z.number(),
  }),
}, async (input) => {
  const prompt = `
You classify inbound SMS urgency for PulseLink.
Return one of: standard, urgent, emergency.
Emergency = imminent danger or immediate action required.
Urgent = time-sensitive but not imminent danger.
Standard = normal conversation, ads, spam, or low priority.

Message: ${input.message}

Return JSON: {"urgency":"standard|urgent|emergency","confidence":0.0-1.0}
`;

  const response = await ai.generate({
    model: MODEL,
    prompt,
    output: {
      format: "json",
      schema: z.object({
        urgency: z.enum(["standard", "urgent", "emergency"]),
        confidence: z.number(),
      }),
    },
    config: {
      temperature: 0.1,
    },
  });

  const structuredOutput = response.output;
  if (!structuredOutput) {
    throw new Error("Failed to classify urgency.");
  }
  return structuredOutput;
});

export const summarizeSmsThread = onCall({}, async (request) => {
  if (!request.auth) {
    throw new HttpsError(
        "unauthenticated",
        "The function must be called while authenticated.",
    );
  }
  return await summarizeSmsThreadFlow.run(request.data);
});

export const composeSmsAssist = onCall({}, async (request) => {
  if (!request.auth) {
    throw new HttpsError(
        "unauthenticated",
        "The function must be called while authenticated.",
    );
  }
  return await composeSmsAssistFlow.run(request.data);
});

export const classifySmsUrgency = onCall({}, async (request) => {
  if (!request.auth) {
    throw new HttpsError(
        "unauthenticated",
        "The function must be called while authenticated.",
    );
  }
  return await classifySmsUrgencyFlow.run(request.data);
});
