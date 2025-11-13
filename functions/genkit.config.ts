import { defineConfig } from "genkit";
import { vertexAI } from "@genkit-ai/vertexai";

export default defineConfig({
  plugins: [
    vertexAI({ location: "us-central1" }),
  ],
});
