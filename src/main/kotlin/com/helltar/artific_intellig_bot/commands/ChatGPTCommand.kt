package com.helltar.artific_intellig_bot.commands

import com.annimon.tgbotsmodule.commands.context.MessageContext
import com.github.kittinunf.fuel.core.ResponseResultOf
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.isSuccessful
import com.github.kittinunf.fuel.httpPost
import com.helltar.artific_intellig_bot.DIR_DB
import com.helltar.artific_intellig_bot.DIR_OUT_TEXT_TO_SPEECH
import com.helltar.artific_intellig_bot.Strings
import com.helltar.artific_intellig_bot.Utils
import com.helltar.artific_intellig_bot.Utils.detectLangCode
import com.helltar.artific_intellig_bot.commands.Commands.cmdChatAsVoice
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONException
import org.json.JSONObject
import org.json.simple.JSONValue
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*

@Serializable
private data class Chat(val model: String, val messages: List<ChatMessage>)

@Serializable
private data class ChatMessage(val role: String, val content: String)

private val userContext = hashMapOf<Long, LinkedList<ChatMessage>>()

class ChatGPTCommand(ctx: MessageContext, args: List<String> = listOf(), private val chatSystemMessage: String) : BotCommand(ctx, args) {

    private val log = LoggerFactory.getLogger(javaClass)

    private companion object {
        const val MAX_USER_MESSAGE_TEXT_LENGTH = 300
        const val MAX_ADMIN_MESSAGE_TEXT_LENGTH = 1024
        const val CHAT_GPT_MODEL = "gpt-3.5-turbo"
        const val DIALOG_CONTEXT_SIZE = 15
    }

    /* todo: refact. */

    override fun run() {
        val message = ctx.message()
        var text = message.text ?: return
        var messageId = ctx.messageId()
        val isReply = message.isReply

        if (!isReply) {
            if (args.isEmpty()) {
                replyToMessage(Strings.chat_hello)
                return
            }
        } else {
            if (message.replyToMessage.from.id != ctx.sender.me.id) {
                text = message.replyToMessage.text ?: return
                messageId = message.replyToMessage.messageId
            }
        }

        if (args.isNotEmpty())
            when (args[0]) {
                "rm" -> { // remove dialog context (/chat rm)
                    if (userContext.containsKey(userId))
                        userContext[userId]?.clear()

                    replyToMessage(Strings.context_removed)

                    return
                }

                "ctx" -> { // show dialog context (/chat ctx)
                    var userId = this.userId

                    if (isReply)
                        userId = message.replyToMessage.from.id

                    var msg = ""

                    if (userContext.containsKey(userId)) {
                        userContext[userId]?.forEachIndexed { index, chatMessage ->
                            if (chatMessage.role == "user")
                                msg += "*$index*: " + chatMessage.content + "\n"
                        }
                    }

                    if (msg.isEmpty())
                        msg = "▫\uFE0F Empty"

                    replyToMessage(msg, markdown = true)

                    return
                }
            }

        val username = message.from.firstName
        val chatTitle = message.chat.title ?: username

        val textLength =
            if (isNotAdmin())
                MAX_USER_MESSAGE_TEXT_LENGTH
            else
                MAX_ADMIN_MESSAGE_TEXT_LENGTH

        if (text.length > textLength) {
            replyToMessage(String.format(Strings.many_characters, textLength))
            return
        }

        if (userContext.containsKey(userId))
            userContext[userId]?.add(ChatMessage("user", text))
        else
            userContext[userId] = LinkedList(listOf(ChatMessage("user", text)))

        val json: String

        sendPrompt(username, chatTitle).run {
            if (second.isSuccessful)
                json = third.get()
            else {
                replyToMessage(Strings.chat_exception)
                log.error(third.component2()?.message)
                userContext[userId]?.removeLast()
                return
            }
        }

        try {
            val answer =
                JSONObject(json)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")

            userContext[userId]?.add(ChatMessage("assistant", answer))

            if (!File(DIR_DB + cmdChatAsVoice).exists()) {
                replyToMessage(answer, messageId, markdown = true)
            } else {
                textToSpeech(answer, detectLangCode(answer))?.let {
                    sendVoice(it, messageId)
                    it.delete()
                }
                    ?: replyToMessage(Strings.chat_exception)
            }
        } catch (e: JSONException) {
            log.error(e.message)
        }
    }

    private fun sendPrompt(username: String, chatName: String): ResponseResultOf<String> {
        val messages = arrayListOf(ChatMessage("system", String.format(chatSystemMessage, chatName, username, userId)))

        if (userContext[userId]!!.size > DIALOG_CONTEXT_SIZE) {
            userContext[userId]?.removeFirst()
            userContext[userId]?.removeFirst()
        }

        messages.addAll(userContext[userId]!!)

        return "https://api.openai.com/v1/chat/completions".httpPost()
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $openaiKey")
            .timeout(60000).timeoutRead(60000)
            .jsonBody(Json.encodeToString(Chat(CHAT_GPT_MODEL, messages)))
            .responseString()
    }

    private fun textToSpeech(text: String, languageCode: String): File? {
        var json: String

        "https://texttospeech.googleapis.com/v1/text:synthesize?fields=audioContent&key=$googleCloudKey".httpPost()
            .header("Content-Type", "application/json; charset=utf-8")
            .jsonBody(String.format(getJsonTextToSpeech(), JSONValue.escape(text), languageCode))
            .responseString().run {
                json =
                    if (second.isSuccessful)
                        third.get()
                    else {
                        log.error(this.third.component2()?.message)
                        return null
                    }
            }

        return try {
            File("$DIR_OUT_TEXT_TO_SPEECH${userId}_${Utils.randomUUID()}.ogg").run {
                writeBytes(Base64.getDecoder().decode(JSONObject(json).getString("audioContent")))
                this
            }
        } catch (e: Exception) {
            replyToMessage(Strings.chat_exception)
            log.error(e.message)
            null
        }
    }
}