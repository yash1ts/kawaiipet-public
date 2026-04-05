package com.kawaiipet.app.util

import com.kawaiipet.app.BuildConfig
import com.posthog.PostHog

/** No-ops when PostHog is not configured (empty API key). */
object Analytics {
    private val enabled: Boolean get() = BuildConfig.POSTHOG_API_KEY.isNotBlank()

    fun capture(event: String, properties: Map<String, Any>? = null) {
        if (!enabled) return
        if (properties != null) {
            PostHog.capture(event = event, properties = properties)
        } else {
            PostHog.capture(event = event)
        }
    }

    fun identify(
        distinctId: String,
        userProperties: Map<String, Any>,
        userPropertiesSetOnce: Map<String, Any>? = null,
    ) {
        if (!enabled) return
        if (userPropertiesSetOnce != null) {
            PostHog.identify(
                distinctId = distinctId,
                userProperties = userProperties,
                userPropertiesSetOnce = userPropertiesSetOnce,
            )
        } else {
            PostHog.identify(distinctId = distinctId, userProperties = userProperties)
        }
    }

    fun reset() {
        if (!enabled) return
        PostHog.reset()
    }
}
