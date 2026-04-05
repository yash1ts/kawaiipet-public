import type { Metadata } from "next";
import Link from "next/link";
import { AuroraBackground } from "@/components/ui/aurora-background";
import { getOptionalContactEmail } from "../../lib/site";

export const metadata: Metadata = {
  title: "Privacy Policy | KawaiiPet",
  description: "How KawaiiPet handles your data, analytics, and third-party services.",
};

export default function PrivacyPage() {
  const contactEmail = getOptionalContactEmail();
  const updated = "April 5, 2026";

  return (
    <AuroraBackground className="text-slate-800">
      <header className="relative z-10 border-b border-slate-200/40 bg-white/55 backdrop-blur-md">
        <div className="mx-auto flex max-w-3xl items-center justify-between px-4 py-4 sm:px-6">
          <Link
            href="/"
            className="text-sm font-semibold text-[var(--color-primary-deep)] transition-colors hover:text-slate-900"
          >
            ← KawaiiPet
          </Link>
        </div>
      </header>

      <article className="relative z-10 mx-auto max-w-3xl px-4 py-12 sm:px-6 sm:py-16">
        <h1 className="text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl">Privacy Policy</h1>
        <p className="mt-2 text-sm text-slate-500">Last updated: {updated}</p>

        <div className="prose-custom mt-10 space-y-8 text-slate-700">
          <section className="space-y-3">
            <h2 className="text-xl font-semibold text-slate-900">Introduction</h2>
            <p className="leading-relaxed">
              This policy describes how the KawaiiPet mobile application (“App”) and this marketing website
              (“Site”) handle information. The App is provided for Android. We aim to collect only what we need
              to run the service and improve the product.
            </p>
            {contactEmail ? (
              <p className="leading-relaxed">
                For privacy questions, contact us at{" "}
                <a className="text-[var(--color-primary)] underline-offset-2 hover:underline" href={`mailto:${contactEmail}`}>
                  {contactEmail}
                </a>
                .
              </p>
            ) : (
              <p className="leading-relaxed">
                For privacy questions, please reach out through the contact options provided inside the App, if
                available.
              </p>
            )}
          </section>

          <section className="space-y-3">
            <h2 className="text-xl font-semibold text-slate-900">Information we hold in the cloud</h2>
            <p className="leading-relaxed">
              When you create an account or sign in, we store basic account information with our backend
              provider (for example, Supabase). This typically includes identifiers needed for authentication
              (such as your user ID) and your email address. This data is used to operate sign-in, protect your
              account, and enable cloud-backed features you choose to use.
            </p>
          </section>

          <section className="space-y-3">
            <h2 className="text-xl font-semibold text-slate-900">Analytics (PostHog)</h2>
            <p className="leading-relaxed">
              We use PostHog for product analytics—for example, to understand how features are used and to
              improve stability and experience. PostHog may process events and, when you are signed in, associate
              activity with your account identifier and properties such as email as configured in the App. When
              you sign out, the App clears that analytics session where implemented.
            </p>
            <p className="leading-relaxed">
              PostHog’s handling of data is described in their policy:{" "}
              <a
                className="text-[var(--color-primary)] underline-offset-2 hover:underline"
                href="https://posthog.com/privacy"
                rel="noopener noreferrer"
                target="_blank"
              >
                posthog.com/privacy
              </a>
              .
            </p>
          </section>

          <section className="space-y-3">
            <h2 className="text-xl font-semibold text-slate-900">Google APIs and AI</h2>
            <p className="leading-relaxed">
              Certain features send content to Google services (for example, the Gemini API) to generate
              assistant responses when you use cloud-backed chat. That content is processed according to
              Google’s terms and policies for the relevant APIs. We do not sell your personal information.
            </p>
            <p className="leading-relaxed">
              You can review Google’s privacy documentation for consumers and developers on{" "}
              <a
                className="text-[var(--color-primary)] underline-offset-2 hover:underline"
                href="https://policies.google.com/privacy"
                rel="noopener noreferrer"
                target="_blank"
              >
                Google’s Privacy &amp; Terms site
              </a>
              .
            </p>
          </section>

          <section className="space-y-3">
            <h2 className="text-xl font-semibold text-slate-900">Information stored only on your device</h2>
            <p className="leading-relaxed">
              Much of what makes your pet yours—such as preferences, memories, and similar data—may be stored
              locally on your device (for example, in an on-device database). That data is not uploaded to our
              servers unless a specific feature clearly states otherwise. Uninstalling the App or clearing app
              data may erase local information.
            </p>
          </section>

          <section className="space-y-3">
            <h2 className="text-xl font-semibold text-slate-900">Third-party services</h2>
            <p className="leading-relaxed">
              Depending on how you use KawaiiPet, processing may involve PostHog (analytics), Google (APIs /
              AI), our authentication and backend provider (e.g. Supabase), and infrastructure used to host this
              Site. Each provider processes data under their own policies.
            </p>
          </section>

          <section className="space-y-3">
            <h2 className="text-xl font-semibold text-slate-900">Retention and your choices</h2>
            <p className="leading-relaxed">
              We retain account-related data as long as your account exists and as needed to operate the
              service. You may be able to delete your account or request deletion through the App or by
              contacting us; some records may be retained where required by law or for legitimate security and
              fraud-prevention purposes. Analytics vendors may retain data according to their own retention
              settings.
            </p>
          </section>

          <section className="space-y-3">
            <h2 className="text-xl font-semibold text-slate-900">Children</h2>
            <p className="leading-relaxed">
              KawaiiPet is not directed at children under 13 (or the minimum age required in your region). If
              you believe we have collected information from a child inappropriately, please contact us so we
              can take appropriate steps.
            </p>
          </section>

          <section className="space-y-3">
            <h2 className="text-xl font-semibold text-slate-900">Changes</h2>
            <p className="leading-relaxed">
              We may update this policy from time to time. We will post the updated version on this page and
              revise the “Last updated” date. Continued use of the App or Site after changes means you accept
              the updated policy.
            </p>
          </section>

          <p className="border-t border-slate-200 pt-8 text-sm text-slate-500">
            This policy is provided for transparency and is not legal advice. You may wish to consult a lawyer
            for your specific situation.
          </p>
        </div>
      </article>
    </AuroraBackground>
  );
}
