import { LandingPage } from "./components/LandingPage";
import { resolveApkDownloadHref } from "../lib/apk";
import { getOptionalGithubUrl } from "../lib/site";

export default function Home() {
  return (
    <LandingPage apkHref={resolveApkDownloadHref()} githubUrl={getOptionalGithubUrl()} />
  );
}
