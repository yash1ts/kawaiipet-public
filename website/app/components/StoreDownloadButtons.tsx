"use client";

import Image from "next/image";
import { motion, useReducedMotion } from "framer-motion";
import { Download } from "lucide-react";

type StoreDownloadButtonsProps = {
  apkHref: string | null;
};

const PLAY_BADGE = "/badges/google-play-badge.svg";
const APP_STORE_BADGE = "/badges/app-store-badge.svg";
const BADGE_W = 200;
const BADGE_H = 60;

function ComingSoonRibbon() {
  return (
    <span className="pointer-events-none absolute right-2 top-2 z-10 rounded-full border border-amber-200/90 bg-amber-50 px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider text-amber-900 shadow-sm">
      Coming soon
    </span>
  );
}

export function StoreDownloadButtons({ apkHref }: StoreDownloadButtonsProps) {
  const reduceMotion = useReducedMotion();

  const playBadge = (
    <Image
      src={PLAY_BADGE}
      alt=""
      width={BADGE_W}
      height={BADGE_H}
      className="h-auto w-full max-w-[200px] drop-shadow-sm"
      aria-hidden
    />
  );

  const appStoreBadge = (
    <Image
      src={APP_STORE_BADGE}
      alt=""
      width={220}
      height={BADGE_H}
      className="h-auto w-full max-w-[220px] opacity-90 drop-shadow-sm"
      aria-hidden
    />
  );

  const downloadButton = apkHref ? (
    <motion.a
      href={apkHref}
      rel="noopener noreferrer"
      className="inline-flex min-h-12 w-full max-w-sm items-center justify-center gap-2 rounded-full bg-[var(--color-primary)] px-8 text-base font-semibold text-white shadow-md shadow-sky-500/25 outline-offset-4 transition-transform hover:opacity-[0.96] focus-visible:outline focus-visible:outline-2 focus-visible:outline-sky-500 sm:w-auto sm:min-w-[240px]"
      whileTap={reduceMotion ? undefined : { scale: 0.98 }}
    >
      <Download className="h-5 w-5 shrink-0" aria-hidden strokeWidth={2.25} />
      Download APK
    </motion.a>
  ) : (
    <span
      className="inline-flex min-h-12 w-full max-w-sm cursor-not-allowed items-center justify-center gap-2 rounded-full bg-slate-200 px-8 text-base font-semibold text-slate-500 sm:w-auto sm:min-w-[240px]"
      aria-label="APK download not configured"
    >
      <Download className="h-5 w-5 shrink-0 opacity-60" aria-hidden strokeWidth={2.25} />
      Download APK
    </span>
  );

  return (
    <div className="mx-auto flex w-full max-w-2xl flex-col items-center gap-8">
      <div className="flex w-full flex-col items-center">{downloadButton}</div>

      <div className="flex w-full flex-col items-center gap-8 sm:flex-row sm:justify-center sm:gap-12">
        <div
          className="relative flex w-full flex-col items-center sm:w-auto"
          role="group"
          aria-label="Google Play — coming soon"
        >
          <div className="relative inline-block rounded-lg opacity-[0.6] saturate-[0.9]">
            <ComingSoonRibbon />
            <span className="pointer-events-none block select-none">{playBadge}</span>
          </div>
        </div>

        <div
          className="relative flex w-full flex-col items-center sm:w-auto"
          role="group"
          aria-label="Download on the App Store — coming soon"
        >
          <div className="relative inline-block rounded-lg opacity-[0.55]">
            <ComingSoonRibbon />
            <span className="pointer-events-none block select-none">{appStoreBadge}</span>
          </div>
        </div>
      </div>
    </div>
  );
}
