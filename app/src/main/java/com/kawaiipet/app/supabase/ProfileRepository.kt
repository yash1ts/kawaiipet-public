package com.kawaiipet.app.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class ProfileRow(
    @SerialName("pet_name") val petName: String? = null,
    val personality: String? = null,
)

@Serializable
private data class ProfileUpsert(
    val id: String,
    @SerialName("pet_name") val petName: String,
    val personality: String,
)

@Singleton
class ProfileRepository @Inject constructor(
    private val supabase: SupabaseClient,
) {

    suspend fun fetchProfile(): Result<Pair<String?, String?>> = runCatching {
        val uid = supabase.auth.currentUserOrNull()?.id
            ?: error("Not signed in")
        val rows = supabase.from("profiles").select {
            filter { eq("id", uid) }
        }.decodeList<ProfileRow>()
        val row = rows.firstOrNull()
        (row?.petName to row?.personality)
    }

    suspend fun upsertProfile(petName: String, personality: String): Result<Unit> = runCatching {
        val uid = supabase.auth.currentUserOrNull()?.id
            ?: error("Not signed in")
        supabase.from("profiles").upsert(ProfileUpsert(id = uid, petName = petName, personality = personality))
    }
}
