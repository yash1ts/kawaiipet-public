/**
 * [Kawaii pet waitlist](https://docs.google.com/forms/d/1DN1cR6s34pVeNp-83Lb2qv739JLvihSZtobVaIYHJjU/edit)
 * — field id from the live viewform HTML (changes if the email question is recreated).
 */
export const DEFAULT_GOOGLE_FORM_RESPONSE_URL =
  "https://docs.google.com/forms/d/e/1FAIpQLSdke2a82HI5fGh-3HhnzzkTYw9oLSwxos6i-8L8NWa7G36QSg/formResponse";

export const DEFAULT_GOOGLE_FORM_EMAIL_ENTRY_ID = "1783340529";

export function getGoogleWaitlistConfig() {
  return {
    responseUrl:
      process.env.GOOGLE_FORM_RESPONSE_URL?.trim() || DEFAULT_GOOGLE_FORM_RESPONSE_URL,
    emailEntryId:
      process.env.GOOGLE_FORM_EMAIL_ENTRY_ID?.trim() || DEFAULT_GOOGLE_FORM_EMAIL_ENTRY_ID,
  };
}

export function buildWaitlistFormBody(email: string, emailEntryId: string): URLSearchParams {
  const body = new URLSearchParams();
  body.set(`entry.${emailEntryId}`, email);
  body.set("fvv", "1");
  body.set("pageHistory", "0");
  return body;
}
