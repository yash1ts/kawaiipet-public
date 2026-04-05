import { existsSync } from "node:fs";
import path from "node:path";

/**
 * Resolves a working APK download URL, or null if none is configured.
 * Priority: APK_LINK → APP_LINK (alias) → NEXT_PUBLIC_APK_LINK → NEXT_PUBLIC_APK_URL → public/kawaiipet.apk
 */
export function resolveApkDownloadHref(): string | null {
  const fromEnv =
    process.env.APK_LINK?.trim() ||
    process.env.APP_LINK?.trim() ||
    process.env.NEXT_PUBLIC_APK_LINK?.trim() ||
    process.env.NEXT_PUBLIC_APK_URL?.trim();
  if (fromEnv) return fromEnv;
  const apkPath = path.join(process.cwd(), "public", "kawaiipet.apk");
  if (existsSync(apkPath)) return "/kawaiipet.apk";
  return null;
}
