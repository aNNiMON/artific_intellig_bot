package com.helltar.aibot.commands.user.chat

import com.annimon.tgbotsmodule.commands.context.MessageContext
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.core.isSuccessful
import com.helltar.aibot.Strings
import com.helltar.aibot.Strings.localizedString
import com.helltar.aibot.commands.BotCommand
import com.helltar.aibot.commands.Commands
import com.helltar.aibot.commands.user.chat.models.Chat
import com.helltar.aibot.utils.NetworkUtils.postJson
import kotlinx.serialization.encodeToString
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream

class ChatGPT(ctx: MessageContext) : BotCommand(ctx) {

    private companion object {
        const val MAX_USER_MESSAGE_TEXT_LENGTH = 2048
        const val MAX_ADMIN_MESSAGE_TEXT_LENGTH = 4096
        const val MAX_USER_DIALOG_HISTORY_LENGH = 10000
        const val VOICE_OUT_TEXT_TAG = "#voice"
    }

    private val chatHistoryManager = ChatHistoryManager(userId)

    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun run() {
        processUserMessage()
    }

    override fun getCommandName() =
        Commands.CMD_CHAT

    private suspend fun processUserMessage() {
        var messageId = ctx.messageId()
        var text = argumentsString
        var isVoiceOut = false

        if (isReply) {
            if (isNotMe(replyMessage!!.from?.userName)) {
                text =
                    replyMessage.text ?: replyMessage.caption ?: run {
                        replyToMessage(Strings.MESSAGE_TEXT_NOT_FOUND)
                        return
                    }

                isVoiceOut = argumentsString == VOICE_OUT_TEXT_TAG
                messageId = replyMessage.messageId
            } else
                text = message.text
        } else
            if (argumentsString.isEmpty()) {
                replyToMessage(Strings.CHAT_HELLO)
                return
            }

        val textLength =
            if (!isAdmin())
                MAX_USER_MESSAGE_TEXT_LENGTH
            else
                MAX_ADMIN_MESSAGE_TEXT_LENGTH

        if (text.length > textLength) {
            replyToMessage(String.format(Strings.MANY_CHARACTERS, textLength))
            return
        }

        val username = message.from.firstName
        val chatTitle = message.chat.title ?: username

        // todo: isVoiceOut
        if (!isVoiceOut) {
            isVoiceOut = text.contains(VOICE_OUT_TEXT_TAG)

            if (isVoiceOut)
                text = text.replace(VOICE_OUT_TEXT_TAG, "").trim()
        }

        if (chatHistoryManager.userChatDialogHistory.isEmpty()) {
            val systemPromt = localizedString(Strings.CHAT_GPT_SYSTEM_MESSAGE, userLanguageCode)
            val systemPromtData = Chat.MessageData(Chat.CHAT_ROLE_SYSTEM, systemPromt.format(chatTitle, username, userId))
            chatHistoryManager.addMessage(systemPromtData)
        }

        chatHistoryManager.addMessage(Chat.MessageData(Chat.CHAT_ROLE_USER, text))

        var dialogLength = chatHistoryManager.getMessagesLengthSum()

        if (dialogLength > MAX_USER_DIALOG_HISTORY_LENGH)
            while (dialogLength > MAX_USER_DIALOG_HISTORY_LENGH) {
                chatHistoryManager.removeSecondMessage()
                dialogLength = chatHistoryManager.getMessagesLengthSum()
            }

        getBotReply()?.let { answer ->
            chatHistoryManager.addMessage(Chat.MessageData(Chat.CHAT_ROLE_ASSISTANT, answer))

            if (!isVoiceOut) {
                try {
                    replyToMessage(answer, messageId, markdown = true)
                } catch (e: Exception) {
                    errorReplyToMessageWithTextDocument(answer, Strings.TELEGRAM_API_EXCEPTION_RESPONSE_SAVED_TO_FILE)
                    log.error(e.message)
                }
            } else
                sendVoice("voice-$messageId", ByteArrayInputStream(textToSpeech(answer)), messageId)
        }
            ?: replyToMessage(Strings.CHAT_EXCEPTION)
    }

    private suspend fun getBotReply(): String? {
        val gptModel = if (!isAdmin()) Chat.CHAT_GPT_MODEL_4_MINI else Chat.CHAT_GPT_MODEL_4

        val response = sendPrompt(chatHistoryManager.userChatDialogHistory, gptModel)
        val responseJson = response.data.decodeToString()

        return if (response.isSuccessful) {
            try {
                json.decodeFromString<Chat.ResponseData>(responseJson).choices.first().message.content
            } catch (e: Exception) {
                log.error(e.message)
                null
            }
        } else {
            log.error("$response")
            null
        }
    }

    private suspend fun sendPrompt(messages: List<Chat.MessageData>, gptModel: String): Response {
        val url = "https://api.openai.com/v1/chat/completions"
        val body = json.encodeToString(Chat.RequestData(gptModel, messages))
        return postJson(url, getOpenAIHeaders(), body)
    }

    private suspend fun textToSpeech(input: String): ByteArray {
        val url = "https://api.openai.com/v1/audio/speech"
        val body = json.encodeToString(Chat.SpeechRequestData(input = input))
        return postJson(url, getOpenAIHeaders(), body).data
    }
}