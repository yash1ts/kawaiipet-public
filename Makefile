# KawaiiPet — copy release APK and upload to Supabase Storage (public download URL).
#
# Setup:
#   cp apk-upload.env.example apk-upload.env
#   # fill SUPABASE_URL + SUPABASE_SERVICE_ROLE_KEY + bucket/path
#
# Usage:
#   make assemble-apk              # build release APK (Gradle)
#   make copy-apk                  # copy newest release APK -> dist/kawaiipet.apk
#   make upload-apk                # copy + upload to Supabase Storage
#   make print-apk-url             # print public URL (after upload; bucket must be public)
#   make deploy-supabase-functions # deploy Edge Functions (chat, extract-facts) to linked project
#   # without link: SUPABASE_PROJECT_REF=... make deploy-supabase-functions
#
# Supabase: create a Storage bucket (e.g. "apk"), add policy for public read on that path
# if you want the link to work without signed URLs. Upload uses the service role key
# (Dashboard → Settings → API → service_role secret). Never commit apk-upload.env.

-include apk-upload.env

GRADLEW := ./gradlew
APK_RELEASE_DIR := app/build/outputs/apk/release
DIST_DIR := dist
DIST_APK := $(DIST_DIR)/kawaiipet.apk

# Optional defaults; required vars checked in verify-upload-env.
SUPABASE_APK_OBJECT_PATH ?= releases/kawaiipet.apk

# Edge Functions: names under supabase/functions/
SUPABASE_EDGE_FUNCTIONS ?= chat extract-facts
SUPABASE_CLI ?= npx --yes supabase@latest
# Set when the CLI is not linked (e.g. CI): SUPABASE_PROJECT_REF=iutndoomufkwzilxjfhz
SUPABASE_PROJECT_REF ?=

.DEFAULT_GOAL := help

.PHONY: help assemble-apk copy-apk upload-apk print-apk-url verify-upload-env deploy-supabase-functions

help:
	@echo "Targets:"
	@echo "  make assemble-apk   Build :app:assembleRelease"
	@echo "  make copy-apk       Copy newest $(APK_RELEASE_DIR)/*.apk -> $(DIST_APK)"
	@echo "  make upload-apk     copy-apk + upload to Supabase Storage"
	@echo "  make print-apk-url  Echo public object URL (bucket must allow public read)"
	@echo "  make deploy-supabase-functions  Deploy Edge Functions ($(SUPABASE_EDGE_FUNCTIONS))"
	@echo ""
	@echo "Config: apk-upload.env (see apk-upload.env.example)"

assemble-apk:
	$(GRADLEW) :app:assembleRelease

# Pick the newest .apk in the release output folder (signed release or unsigned).
copy-apk:
	@mkdir -p "$(DIST_DIR)"
	@apk=$$(ls -t "$(APK_RELEASE_DIR)"/*.apk 2>/dev/null | head -n1); \
	if [ -z "$$apk" ]; then \
	  echo "No APK in $(APK_RELEASE_DIR). Run: make assemble-apk"; \
	  exit 1; \
	fi; \
	cp -f "$$apk" "$(DIST_APK)"; \
	echo "Copied $$apk -> $(DIST_APK)"

verify-upload-env:
	@if [ ! -f apk-upload.env ]; then echo "Create apk-upload.env from apk-upload.env.example"; exit 1; fi
	@if [ -z "$(SUPABASE_URL)" ]; then echo "Missing SUPABASE_URL in apk-upload.env"; exit 1; fi
	@if [ -z "$(SUPABASE_SERVICE_ROLE_KEY)" ]; then echo "Missing SUPABASE_SERVICE_ROLE_KEY in apk-upload.env"; exit 1; fi
	@if [ -z "$(SUPABASE_STORAGE_BUCKET)" ]; then echo "Missing SUPABASE_STORAGE_BUCKET in apk-upload.env"; exit 1; fi

upload-apk: copy-apk verify-upload-env
	@test -f "$(DIST_APK)" || (echo "Missing $(DIST_APK)"; exit 1)
	@echo "Uploading to $(SUPABASE_STORAGE_BUCKET)/$(SUPABASE_APK_OBJECT_PATH) ..."
	@curl -f -sS -X POST \
	  "$(SUPABASE_URL)/storage/v1/object/$(SUPABASE_STORAGE_BUCKET)/$(SUPABASE_APK_OBJECT_PATH)?upsert=true" \
	  -H "Authorization: Bearer $(SUPABASE_SERVICE_ROLE_KEY)" \
	  -H "apikey: $(SUPABASE_SERVICE_ROLE_KEY)" \
	  -H "Content-Type: application/vnd.android.package-archive" \
	  --data-binary "@$(DIST_APK)"
	@echo ""
	@echo "Upload OK."
	@$(MAKE) --no-print-directory print-apk-url

print-apk-url:
	@echo "Public download URL (requires bucket/prefix to be publicly readable):"
	@echo "$(SUPABASE_URL)/storage/v1/object/public/$(SUPABASE_STORAGE_BUCKET)/$(SUPABASE_APK_OBJECT_PATH)"

deploy-supabase-functions:
	@$(SUPABASE_CLI) functions deploy $(SUPABASE_EDGE_FUNCTIONS) --yes $(if $(strip $(SUPABASE_PROJECT_REF)),--project-ref $(SUPABASE_PROJECT_REF),)
