import { buildWaitlistFormBody, getGoogleWaitlistConfig } from "@/lib/google-waitlist-form";
import { NextResponse } from "next/server";

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export async function POST(req: Request) {
  let payload: unknown;
  try {
    payload = await req.json();
  } catch {
    return NextResponse.json(
      { code: "bad_request", message: "We couldn’t read that request. Please refresh and try again." },
      { status: 400 },
    );
  }

  if (!payload || typeof payload !== "object" || !("email" in payload)) {
    return NextResponse.json(
      { code: "missing_email", message: "Please enter your email address." },
      { status: 400 },
    );
  }

  const raw = (payload as { email: unknown }).email;
  if (typeof raw !== "string") {
    return NextResponse.json(
      { code: "invalid_email", message: "That doesn’t look like a valid email. Check for typos and try again." },
      { status: 400 },
    );
  }

  const email = raw.trim().toLowerCase();
  if (!email || !EMAIL_RE.test(email)) {
    return NextResponse.json(
      { code: "invalid_email", message: "That doesn’t look like a valid email. Check for typos and try again." },
      { status: 400 },
    );
  }

  const { responseUrl, emailEntryId } = getGoogleWaitlistConfig();
  const body = buildWaitlistFormBody(email, emailEntryId);

  const res = await fetch(responseUrl, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: body.toString(),
    redirect: "manual",
  });

  if (res.status >= 200 && res.status < 400) {
    return NextResponse.json({ ok: true });
  }

  return NextResponse.json(
    { code: "submission_failed", message: "We couldn’t reach the signup form. Please try again in a moment." },
    { status: 502 },
  );
}
