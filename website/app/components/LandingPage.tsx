"use client";

import { AuroraBackground } from "@/components/ui/aurora-background";
import { StoreDownloadButtons } from "./StoreDownloadButtons";
import Image from "next/image";
import Link from "next/link";
import { motion, useReducedMotion } from "framer-motion";
import { MessageCircle, Smartphone, Sparkles } from "lucide-react";

type LandingPageProps = {
  apkHref: string | null;
  githubUrl: string | null;
};

const LOGO_PATH = "/transparent smile (1).png";

const fadeUp = {
  hidden: { opacity: 0, y: 24 },
  show: (i: number) => ({
    opacity: 1,
    y: 0,
    transition: { delay: i * 0.08, duration: 0.55, ease: [0.22, 1, 0.36, 1] as const },
  }),
};

const features = [
  {
    title: "Pet on your screen",
    body: "A companion overlay you can keep with you while you use other apps.",
    icon: Smartphone,
  },
  {
    title: "Talk or type",
    body: "Chat by voice or text so your pet can listen and respond when you need it.",
    icon: MessageCircle,
  },
  {
    title: "Make it yours",
    body: "Name your pet, tune personality, and customize how it feels on your phone.",
    icon: Sparkles,
  },
];

export function LandingPage({ apkHref, githubUrl }: LandingPageProps) {
  const reduceMotion = useReducedMotion();

  return (
    <AuroraBackground className="text-slate-900">
      <header className="relative z-10 border-b border-slate-200/40 bg-white/55 backdrop-blur-md">
        <div className="mx-auto flex max-w-5xl items-center justify-between gap-4 px-4 py-4 sm:px-6">
          <Link
            href="/"
            className="flex items-center gap-3 rounded-lg outline-offset-4 focus-visible:outline focus-visible:outline-2 focus-visible:outline-[var(--color-primary)]"
          >
            <Image
              src={LOGO_PATH}
              alt=""
              width={44}
              height={44}
              className="h-11 w-11 object-contain drop-shadow-[0_1px_8px_rgba(15,23,42,0.08)]"
              priority
            />
            <span className="text-lg font-semibold tracking-tight text-slate-900">KawaiiPet</span>
          </Link>
          <nav className="flex items-center gap-6 text-sm font-medium text-slate-600">
            <a href="#features" className="transition-colors hover:text-slate-900">
              Features
            </a>
            <a href="#download" className="transition-colors hover:text-slate-900">
              Download
            </a>
          </nav>
        </div>
      </header>

      <main className="relative z-10 flex flex-1 flex-col">
        <section className="mx-auto flex max-w-5xl flex-col items-center px-4 pb-20 pt-16 text-center sm:px-6 sm:pb-24 sm:pt-20">
          <motion.div
            initial={reduceMotion ? false : { opacity: 0, scale: 0.92, y: 12 }}
            animate={reduceMotion ? undefined : { opacity: 1, scale: 1, y: 0 }}
            transition={{ duration: 0.7, ease: [0.22, 1, 0.36, 1] }}
            className="mb-10"
          >
            <div className="relative">
              <div className="absolute inset-0 -m-10 rounded-full bg-violet-50/35 blur-3xl" />
              <Image
                src={LOGO_PATH}
                alt="KawaiiPet"
                width={160}
                height={160}
                className="relative h-36 w-36 object-contain drop-shadow-[0_4px_20px_rgba(15,23,42,0.06)] sm:h-40 sm:w-40"
                priority
              />
            </div>
          </motion.div>

          <motion.h1
            custom={0}
            variants={fadeUp}
            initial="hidden"
            animate="show"
            className="max-w-2xl text-4xl font-bold tracking-tight text-slate-900 sm:text-5xl sm:leading-[1.1]"
          >
            Your pocket-sized pet, always a tap away
          </motion.h1>
          <motion.p
            custom={1}
            variants={fadeUp}
            initial="hidden"
            animate="show"
            className="mt-5 max-w-lg text-lg leading-relaxed text-slate-600"
          >
            A calm, playful companion on Android—chat, customize, and keep them on screen while you go about your day.
          </motion.p>

          <motion.div
            id="download"
            custom={2}
            variants={fadeUp}
            initial="hidden"
            animate="show"
            className="scroll-mt-24 mt-10 w-full"
          >
            <StoreDownloadButtons apkHref={apkHref} />
          </motion.div>
        </section>

        <section id="features" className="relative border-t border-slate-200/50 bg-transparent py-20">
          <div className="mx-auto max-w-5xl px-4 sm:px-6">
            <h2 className="mb-12 text-center text-2xl font-semibold tracking-tight text-slate-900 sm:text-3xl">
              Built for quick hellos and longer hangs
            </h2>
            <ul className="grid gap-6 sm:grid-cols-3">
              {features.map((f, i) => {
                const Icon = f.icon;
                return (
                  <motion.li
                    key={f.title}
                    initial={reduceMotion ? false : { opacity: 0, y: 20 }}
                    whileInView={reduceMotion ? undefined : { opacity: 1, y: 0 }}
                    viewport={{ once: true, margin: "-40px" }}
                    transition={{ delay: i * 0.1, duration: 0.5, ease: [0.22, 1, 0.36, 1] }}
                    className="rounded-2xl border border-slate-200/60 bg-white/75 p-6 shadow-sm shadow-slate-200/40 backdrop-blur-sm"
                  >
                    <div className="mb-4 inline-flex rounded-xl bg-slate-50 p-3 text-slate-700 ring-1 ring-slate-200/80">
                      <Icon className="h-6 w-6" aria-hidden strokeWidth={1.75} />
                    </div>
                    <h3 className="text-lg font-semibold text-slate-900">{f.title}</h3>
                    <p className="mt-2 text-sm leading-relaxed text-slate-600">{f.body}</p>
                  </motion.li>
                );
              })}
            </ul>
          </div>
        </section>

      </main>

      <footer className="relative z-10 border-t border-slate-200/40 bg-white/55 py-10 backdrop-blur-md">
        <div className="mx-auto flex max-w-5xl flex-col items-center justify-center gap-4 px-4 text-center text-sm text-slate-500 sm:flex-row sm:justify-between sm:px-6 sm:text-left">
          <p>© {new Date().getFullYear()} KawaiiPet</p>
          <div className="flex flex-wrap items-center justify-center gap-6">
            <Link href="/privacy" className="text-slate-600 transition-colors hover:text-slate-900">
              Privacy
            </Link>
            {githubUrl ? (
              <a
                href={githubUrl}
                className="text-slate-600 transition-colors hover:text-slate-900"
                rel="noopener noreferrer"
                target="_blank"
              >
                GitHub
              </a>
            ) : null}
          </div>
        </div>
      </footer>
    </AuroraBackground>
  );
}
