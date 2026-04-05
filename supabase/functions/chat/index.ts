import "jsr:@supabase/functions-js/edge-runtime.d.ts"
import { getUserFromRequest } from "../_shared/auth.ts"
import { geminiGenerateContent } from "../_shared/gemini.ts"

type ChatMessage = { role: "user" | "assistant"; text: string }

const DEFAULT_PERSONALITY =
  "You are a cute, friendly virtual pet. Speak only in character as this pet—warm and playful. " +
  "Remember what the user tells you and mention it naturally when it fits. " +
  "Prefer one very short sentence; two short sentences at most. No bullet lists, markdown, or long explanations."

function buildSystemPrompt(
  petName: string,
  personality: string,
  factTexts: string[],
): string {
  const name = petName.trim() || "Mochi"
  const p = personality.trim() || DEFAULT_PERSONALITY
  let s = `Your name is ${name}.\n\n${p}`
  if (factTexts.length > 0) {
    s += "\n\nThings you remember about the user:\n"
    for (const f of factTexts) {
      s += `- ${f}\n`
    }
  }
  s +=
    "\n\nSTAY IN CHARACTER: You are " +
    name +
    " the pet only. Never say you are an AI, a language model, an assistant, or \"trained on\" anything. " +
    "Do not break the fourth wall, give policy lectures, or refuse in a robotic corporate tone—stay cute and in-world. " +
    "Do not prefix with meta lines (e.g. \"As your pet,\" \"Here's my response\"). Just speak as the pet. " +
    "Cap the reply at about 35–45 words before the tag (shorter is better). " +
    "End with exactly one emotion tag at the very end: [happy], [sad], [thinking], or [idle]."
  return s
}

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

  let body: { messages?: ChatMessage[]; factTexts?: string[] }
  try {
    body = await req.json()
  } catch {
    return new Response(JSON.stringify({ error: "Invalid JSON" }), {
      status: 400,
      headers: { "Content-Type": "application/json" },
    })
  }

  const messages = body.messages
  if (!Array.isArray(messages) || messages.length === 0) {
    return new Response(JSON.stringify({ error: "messages required" }), {
      status: 400,
      headers: { "Content-Type": "application/json" },
    })
  }

  const factTexts = Array.isArray(body.factTexts)
    ? body.factTexts.filter((x): x is string => typeof x === "string")
    : []

  const apiKey = Deno.env.get("GEMINI_API_KEY")
  if (!apiKey) {
    return new Response(JSON.stringify({ error: "Server misconfigured" }), {
      status: 500,
      headers: { "Content-Type": "application/json" },
    })
  }

  const model = Deno.env.get("GEMINI_MODEL")?.trim() || "gemini-2.0-flash"

  const { createClient } = await import("npm:@supabase/supabase-js@2")
  const authHeader = req.headers.get("Authorization") ?? ""
  const supabase = createClient(
    Deno.env.get("SUPABASE_URL") ?? "",
    Deno.env.get("SUPABASE_ANON_KEY") ?? "",
    { global: { headers: { Authorization: authHeader } } },
  )

  const { data: profile, error: profErr } = await supabase
    .from("profiles")
    .select("pet_name, personality")
    .eq("id", user.id)
    .maybeSingle()

  if (profErr) {
    return new Response(JSON.stringify({ error: profErr.message }), {
      status: 500,
      headers: { "Content-Type": "application/json" },
    })
  }

  const petName = profile?.pet_name ?? "Mochi"
  const personality = profile?.personality ?? ""
  const systemPrompt = buildSystemPrompt(petName, personality, factTexts)

  const contents = messages.map((m) => ({
    role: m.role === "assistant" ? "model" : "user",
    parts: [{ text: m.text }],
  }))

  try {
    const text = await geminiGenerateContent(model, apiKey, systemPrompt, contents, {
      maxOutputTokens: 120,
      temperature: 0.85,
    })
    return new Response(JSON.stringify({ text }), {
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
