import { createClient } from "npm:@supabase/supabase-js@2"

export async function getUserFromRequest(req: Request) {
  const authHeader = req.headers.get("Authorization")
  if (!authHeader?.startsWith("Bearer ")) {
    return { user: null as null, error: "Missing or invalid Authorization header" }
  }
  const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? ""
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY") ?? ""
  const supabase = createClient(supabaseUrl, anonKey, {
    global: { headers: { Authorization: authHeader } },
  })
  const { data: { user }, error } = await supabase.auth.getUser()
  if (error || !user) {
    return { user: null as null, error: error?.message ?? "Invalid session" }
  }
  return { user, error: null as null }
}
