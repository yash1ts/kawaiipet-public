package com.kawaiipet.app.di

import com.kawaiipet.app.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        val url = BuildConfig.SUPABASE_URL.trim()
        val key = BuildConfig.SUPABASE_ANON_KEY.trim()
        require(url.isNotEmpty() && key.isNotEmpty()) {
            "Supabase URL/key missing in BuildConfig. Add supabase.url and supabase.anon.key to " +
                "local.properties at the project root (next to sdk.dir) or in app/local.properties, then rebuild."
        }
        return createSupabaseClient(supabaseUrl = url, supabaseKey = key) {
            defaultSerializer = KotlinXSerializer(
                Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                }
            )
            install(Auth) {
                scheme = BuildConfig.SUPABASE_AUTH_REDIRECT_SCHEME
                host = BuildConfig.SUPABASE_AUTH_REDIRECT_HOST
            }
            install(Postgrest)
            install(Functions)
        }
    }
}
