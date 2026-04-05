export type GeminiGenerationConfig = {
  maxOutputTokens?: number
  temperature?: number
}

export async function geminiGenerateContent(
  model: string,
  apiKey: string,
  systemInstruction: string,
  contents: { role: string; parts: { text: string }[] }[],
  generationConfig?: GeminiGenerationConfig,
): Promise<string> {
  const url =
    `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${encodeURIComponent(apiKey)}`
  const body: Record<string, unknown> = { contents }
  if (generationConfig != null && Object.keys(generationConfig).length > 0) {
    body.generationConfig = generationConfig
  }
  const trimmed = systemInstruction.trim()
  if (trimmed.length > 0) {
    body.systemInstruction = { parts: [{ text: trimmed }] }
  }
  const res = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  })
  if (!res.ok) {
    const t = await res.text()
    throw new Error(`Gemini HTTP ${res.status}: ${t}`)
  }
  const json = (await res.json()) as {
    candidates?: { content?: { parts?: { text?: string }[] } }[]
  }
  const text = json.candidates?.[0]?.content?.parts?.[0]?.text
  if (typeof text !== "string") {
    throw new Error("Gemini response missing text")
  }
  return text
}
