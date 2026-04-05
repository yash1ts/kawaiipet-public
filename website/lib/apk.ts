import { existsSync } from "node:fs";
import path from "node:path";

/** Resolves a working APK download URL, or null if none is configured. */
export function resolveApkDownloadHref(): string | null {
  const fromEnv = process.env.NEXT_PUBLIC_APK_URL?.trim();
  if (fromEnv) return fromEnv;
  const apkPath = path.join(process.cwd(), "public", "kawaiipet.apk");
  if (existsSync(apkPath)) return "/kawaiipet.apk";
  return null;
}
