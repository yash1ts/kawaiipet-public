import "jsr:@supabase/functions-js/edge-runtime.d.ts"
import { getUserFromRequest } from "../_shared/auth.ts"
import { geminiGenerateContent } from "../_shared/gemini.ts"

const EXTRACT_PROMPT = `You are a fact extractor. Read the conversation below and list only concrete personal facts about the user (name, preferences, hobbies, relationships, etc.).
Rules:
- One fact per line, written as a short plain-English sentence.
- Do NOT output any labels, tags, headers, or meta-text (no "output:", "instruction:", "thought", etc.).
- If there are no personal facts, respond with exactly: NONE

Conversation:
`

Deno.serve(async (req) => {
  if (req.method !== "POST") {
    return new Response(JSON.stringify({ error: "Method not allowed" }), {
      status: 405,
      headers: { "Content-Type": "application/json" },
    })
  }

  const { user, error: authErr } = await getUserFromRequest(req)
  if (!user) {
    return new Response(JSON.stringify({ error: authErr ?? "Unauthorized" }), {
      status: 401,
      headers: { "Content-Type": "application/json" },
    })
  }

  let body: { snippet?: string }
  try {
    body = await req.json()
  } catch {
    return new Response(JSON.stringify({ error: "Invalid JSON" }), {
      status: 400,
      headers: { "Content-Type": "application/json" },
    })
  }

  const snippet = typeof body.snippet === "string" ? body.snippet : ""
  if (!snippet.trim()) {
    return new Response(JSON.stringify({ lines: [] as string[] }), {
      headers: { "Content-Type": "application/json" },
    })
  }

  const apiKey = Deno.env.get("GEMINI_API_KEY")
  if (!apiKey) {
    return new Response(JSON.stringify({ error: "Server misconfigured" }), {
      status: 500,
      headers: { "Content-Type": "application/json" },
    })
  }

  const model = Deno.env.get("GEMINI_MODEL")?.trim() || "gemini-2.0-flash"
  const userPrompt = EXTRACT_PROMPT + snippet

  try {
    const raw = await geminiGenerateContent(model, apiKey, "", [
      { role: "user", parts: [{ text: userPrompt }] },
    ])
    const lines = raw
      .split("\n")
      .map((l) => l.trim())
      .filter((l) =>
        l.length > 0 &&
        !l.startsWith("#") &&
        !l.startsWith("```") &&
        l.toLowerCase() !== "none"
      )
    return new Response(JSON.stringify({ lines }), {
      headers: { "Content-Type": "application/json" },
    })
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e)
    return new Response(JSON.stringify({ error: msg }), {
      status: 502,
      headers: { "Content-Type": "application/json" },
    })
  }
})
