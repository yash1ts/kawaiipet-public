export function getOptionalGithubUrl(): string | null {
  const u = process.env.NEXT_PUBLIC_GITHUB_URL?.trim();
  return u || null;
}

export function getOptionalContactEmail(): string | null {
  const e = process.env.NEXT_PUBLIC_CONTACT_EMAIL?.trim();
  return e || null;
}
