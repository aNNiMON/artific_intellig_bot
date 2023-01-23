package com.helltar.artific_intellig_bot.commands

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.httpPost
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.*
import com.helltar.artific_intellig_bot.BotConfig
import com.helltar.artific_intellig_bot.DIR_DB
import com.helltar.artific_intellig_bot.EXT_DISABLED
import com.helltar.artific_intellig_bot.commands.Commands.commandsList
import org.json.simple.JSONValue
import java.io.File

abstract class BotCommand(val bot: Bot, val message: Message, val args: List<String> = listOf()) : BotConfig() {

    protected val userId = message.from!!.id
    private val chatId = ChatId.fromId(message.chat.id)
    private val replyToMessageId = message.messageId

    abstract fun run()

    fun isCommandEnable(commandName: String): Boolean {
        if (isAdmin()) return true
        return !File(DIR_DB + commandName + EXT_DISABLED).exists()
    }

    fun isChatInWhiteList(commandName: String): Boolean {
        if (isNotAdmin())
            if (commandsList.isNotEmpty() && commandsList.contains(commandName))
                return getChatsWhiteList().contains(chatId.id.toString())

        return true
    }

    fun sendMessage(
        text: String, replyTo: Long = replyToMessageId,
        disableWebPagePreview: Boolean = true, replyMarkup: ReplyMarkup? = null
    ) =
        bot.sendMessage(
            chatId, text, ParseMode.HTML, disableWebPagePreview,
            replyToMessageId = replyTo, allowSendingWithoutReply = true,
            replyMarkup = replyMarkup
        ).get().messageId

    protected fun isNotAdmin() =
        !getSudoers().contains(userId.toString())

    private fun isAdmin() =
        !isNotAdmin()

    protected fun sendPhoto(photo: TelegramFile, caption: String, replyTo: Long = replyToMessageId) =
        bot.sendPhoto(
            chatId, photo, caption, replyToMessageId = replyTo, allowSendingWithoutReply = true
        )

    protected fun sendVoice(audio: ByteArray) =
        bot.sendVoice(
            chatId, TelegramFile.ByByteArray(audio),
            replyToMessageId = replyToMessageId, allowSendingWithoutReply = true
        )

    protected data class ReqData(
        val url: String,
        val apiKey: String,
        val json: String,
        val prompt: String,
        val additionHeader: Map<String, String> = mapOf()
    )

    protected fun sendPrompt(reqData: ReqData) =
        reqData.run {
            url.httpPost()
                .header("Content-Type", "application/json")
                .header("Authorization", apiKey)
                .header(additionHeader)
                .timeout(60000)
                .timeoutRead(60000)
                .jsonBody(String.format(json, JSONValue.escape(prompt)))
                .responseString()
        }
}
