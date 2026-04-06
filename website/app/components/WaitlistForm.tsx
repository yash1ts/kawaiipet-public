"use client";

import { motion } from "framer-motion";
import { Mail } from "lucide-react";
import { useState } from "react";

type WaitlistFormProps = {
  motionIndex: number;
};

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const GENERIC_ERROR = "Something went wrong. Please try again in a moment.";

export function WaitlistForm({ motionIndex }: WaitlistFormProps) {
  const [email, setEmail] = useState("");
  const [status, setStatus] = useState<"idle" | "loading" | "success">("idle");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (status === "loading") return;

    setErrorMessage(null);

    const normalized = email.trim().toLowerCase();
    if (!normalized) {
      setErrorMessage("Please enter your email address.");
      return;
    }
    if (!EMAIL_RE.test(normalized)) {
      setErrorMessage("That doesn’t look like a valid email. Check for typos and try again.");
      return;
    }

    setStatus("loading");
    try {
      const res = await fetch("/api/waitlist", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: normalized }),
      });

      if (res.ok) {
        setStatus("success");
        setEmail("");
        return;
      }

      let message = GENERIC_ERROR;
      try {
        const data: unknown = await res.json();
        if (
          data &&
          typeof data === "object" &&
          "message" in data &&
          typeof (data as { message: unknown }).message === "string"
        ) {
          message = (data as { message: string }).message;
        }
      } catch {
        /* keep default */
      }
      setErrorMessage(message);
      setStatus("idle");
    } catch {
      setErrorMessage(GENERIC_ERROR);
      setStatus("idle");
    }
  }

  const fadeUp = {
    hidden: { opacity: 0, y: 24 },
    show: (i: number) => ({
      opacity: 1,
      y: 0,
      transition: { delay: i * 0.08, duration: 0.55, ease: [0.22, 1, 0.36, 1] as const },
    }),
  };

  return (
    <motion.div
      custom={motionIndex}
      variants={fadeUp}
      initial="hidden"
      animate="show"
      className="mt-10 flex w-full max-w-[min(100%,26rem)] flex-col items-center sm:max-w-xl"
    >
      <p className="mb-4 flex items-center justify-center gap-2 text-sm font-medium tracking-tight text-slate-600">
        <span className="inline-flex rounded-lg bg-violet-100/80 p-1.5 text-violet-600 ring-1 ring-violet-200/60">
          <Mail className="h-4 w-4" aria-hidden strokeWidth={1.75} />
        </span>
        Join for updates
      </p>
      <form
        onSubmit={onSubmit}
        className="flex w-full flex-col gap-3 transition-[border-color,box-shadow] sm:flex-row sm:items-center sm:gap-0 sm:overflow-hidden sm:rounded-2xl sm:border sm:border-slate-200/70 sm:bg-white/[0.82] sm:p-1.5 sm:shadow-[0_4px_28px_rgba(91,33,182,0.09),0_1px_3px_rgba(15,23,42,0.06)] sm:ring-1 sm:ring-white/90 sm:backdrop-blur-md sm:focus-within:border-violet-300/55 sm:focus-within:shadow-[0_4px_32px_rgba(91,33,182,0.14),0_1px_3px_rgba(15,23,42,0.06)]"
        noValidate
      >
        <label htmlFor="waitlist-email" className="sr-only">
          Email address
        </label>
        <input
          id="waitlist-email"
          name="email"
          type="email"
          inputMode="email"
          autoComplete="email"
          required
          placeholder="you@example.com"
          value={email}
          onChange={(ev) => {
            setEmail(ev.target.value);
            setErrorMessage(null);
            if (status === "success") {
              setStatus("idle");
            }
          }}
          disabled={status === "loading" || status === "success"}
          className="h-12 w-full rounded-2xl border border-slate-200/90 bg-white px-4 text-base text-slate-900 shadow-sm shadow-slate-200/25 outline-none transition-[border-color,box-shadow] placeholder:text-slate-400 focus:border-violet-300 focus:shadow-[0_0_0_3px_rgba(167,139,250,0.25)] disabled:cursor-not-allowed disabled:opacity-60 sm:border-0 sm:bg-transparent sm:shadow-none sm:focus:border-transparent sm:focus:shadow-none sm:focus:ring-0"
        />
        <button
          type="submit"
          disabled={status === "loading" || status === "success"}
          className="flex h-12 w-full shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br from-violet-600 via-violet-600 to-indigo-600 px-8 text-sm font-semibold tracking-tight text-white shadow-md shadow-violet-500/30 transition-[transform,box-shadow,filter] hover:brightness-[1.05] hover:shadow-lg hover:shadow-violet-500/35 active:scale-[0.98] focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-violet-500 disabled:cursor-not-allowed disabled:opacity-60 disabled:active:scale-100 sm:ml-1 sm:w-auto sm:rounded-xl sm:px-7"
        >
          {status === "loading" ? "Joining…" : status === "success" ? "You’re in" : "Notify me"}
        </button>
      </form>
      {status === "success" ? (
        <p className="mt-3 max-w-sm text-center text-sm leading-relaxed text-emerald-700" role="status">
          Thanks — we’ll email you when there’s news.
        </p>
      ) : null}
      {errorMessage ? (
        <p className="mt-3 max-w-sm text-center text-sm leading-relaxed text-red-600" role="alert">
          {errorMessage}
        </p>
      ) : null}
    </motion.div>
  );
}
