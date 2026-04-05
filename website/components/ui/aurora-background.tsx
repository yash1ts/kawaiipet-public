"use client";

import { cn } from "@/lib/utils";
import type { HTMLAttributes, ReactNode } from "react";

export interface AuroraBackgroundProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode;
  /** When true, gently fades the aurora at the bottom of the viewport only (still full-width). */
  showRadialGradient?: boolean;
}

/**
 * Full-viewport aurora on white: visible soft vertical blue + lavender streaks (readable but not “blue page”).
 */
export function AuroraBackground({
  className,
  children,
  showRadialGradient = false,
  ...props
}: AuroraBackgroundProps) {
  return (
    <div
      className={cn("relative flex min-h-full flex-col bg-transparent text-slate-900", className)}
      {...props}
    >
      <div
        aria-hidden
        className="pointer-events-none fixed inset-0 z-0 overflow-hidden bg-white"
      >
        {/* Cool + violet haze, still mostly white */}
        <div
          className={cn(
            "absolute inset-0",
            "[background:radial-gradient(ellipse_90%_70%_at_82%_-8%,#eef2ff_0%,transparent_50%),radial-gradient(ellipse_75%_60%_at_15%_20%,#faf5ff_0%,transparent_45%),#ffffff]",
          )}
        />
        {/* Vertical light bands — pastel stops you can actually see after blur */}
        <div
          className={cn(
            "aurora-layer",
            `
            [--beam:repeating-linear-gradient(100deg,#ffffff_0%,#ffffff_3%,#f1f5f9_6%,transparent_8%,transparent_11%,#e8f0fe_13%,#ede9fe_15%,transparent_18%,#ffffff_22%)]
            [--bands:repeating-linear-gradient(100deg,#ffffff_0%,#dbeafe_4%,#e9d5ff_9%,#f0f9ff_14%,#ddd6fe_19%,#e0f2fe_24%,#ffffff_30%)]
            pointer-events-none absolute inset-[-18px]
            [background-image:var(--beam),var(--bands)]
            [background-size:210%,190%]
            [background-position:0%_50%,32%_50%]
            opacity-[0.82] blur-[9px] saturate-[1.08]
            after:pointer-events-none after:absolute after:inset-0 after:animate-aurora
            after:[background-attachment:fixed]
            after:[background-image:var(--beam),var(--bands)]
            after:[background-size:190%,150%]
            after:[background-position:50%_50%,50%_50%]
            after:opacity-[0.72] after:blur-[7px] after:content-[""]
            after:mix-blend-multiply`,

            showRadialGradient &&
              `[mask-image:linear-gradient(to_bottom,black_0%,black_80%,transparent_100%)]`,
          )}
        />
      </div>
      <div className="relative z-10 flex min-h-full flex-1 flex-col">{children}</div>
    </div>
  );
}
