package com.helltar.artific_intellig_bot

import com.helltar.artific_intellig_bot.utils.Utils.getFirstRegexGroup
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileReader

object Strings {

    const val bad_request = "<code>Bad Request</code> \uD83D\uDE10" // 😐
    const val ban_and_reason = "❌ Ban, reason: <b>%s</b>"
    const val chat_as_text_already_enabled = "✅ ChatAsText already enabled"
    const val chat_as_text_ok = "✅ ChatAsText"
    const val chat_as_voice_already_enabled = "✅ ChatAsVoice already enabled"
    const val chat_as_voice_ok = "✅ ChatAsVoice"
    const val chat_exception = "Something is broken \uD83D\uDE48" // 🙈
    const val chat_hello = "\uD83D\uDC4B Hello, please ask your questions as replying to my messages" // 👋
    const val chat_context_removed = "Context has been removed \uD83D\uDC4C" // 👌
    const val chat_context_empty = "▫\uFE0F Empty"
    const val command_already_disabled = "✅ Command <b>%s</b> already disabled"
    const val command_already_enabled = "✅ Command <b>%s</b> already enabled"
    const val command_disabled = "✅ Command <b>%s</b> disabled"
    const val command_enabled = "✅ Command <b>%s</b> enabled"
    const val command_not_available = "Command <b>%s</b> not available: %s"
    const val command_not_supported_in_chat = "Command is not supported in this chat \uD83D\uDE48" // 🙈
    const val command_temporary_disabled = "Command temporary disabled \uD83D\uDC40" // 👀
    const val empty_args = "Please write a description of what you want to receive:\n\n<code>/dalle photo realistic portrait of young woman</code>"
    const val error_delete_lock_file = "❌ Error when delete lock file: <code>%s</code>"
    const val list_is_empty = "◻️ List is empty"
    const val many_characters = "Max <b>%d</b> characters \uD83D\uDC40" // 👀
    const val many_request = "Wait, let me deal with the last request \uD83E\uDD16" // 🤖
    const val stable_diffusion_empty_args = "Please write a description of what you want to receive:\n\n<code>/sdif photo realistic portrait of young woman</code>"
    const val image_must_be_less_than = "Image must be less than %s 😥" // 😥

    const val user_already_banned = "✅ User already banned"
    const val user_banned = "❌ User banned"
    const val user_not_banned = "✅ User not banned"
    const val user_unbanned = "✅ User unbanned"

    const val admin_added = "✅ Admin added"
    const val admin_exists = "✅ Admin already exists"
    const val admin_removed = "✅ Admin has been removed"
    const val admin_not_exists = "❌ Admin does not exist"

    const val chat_added = "✅ Chat added"
    const val chat_exists = "✅ Chat already exists"
    const val chat_removed = "✅ Chat has been removed"
    const val chat_not_exists = "❌ Chat does not exist"

    const val chat_gpt_system_message = "chat_gpt_system_message"
    const val chat_wait_message = "chat_wait_message"
}

private val log = LoggerFactory.getLogger(Strings.javaClass)

fun localizedString(key: String, languageCode: String): String {
    return try {
        var filename = "$DIR_LOCALE/${languageCode.lowercase()}.xml"

        if (!File(filename).exists())
            filename = "$DIR_LOCALE/en.xml"

        val regex = """<string name="$key">(\X*?)<\/string>"""
        getFirstRegexGroup(FileReader(filename).readText(), regex).trimIndent().ifEmpty { key }
    } catch (e: Exception) {
        log.error(e.message)
        key
    }
}