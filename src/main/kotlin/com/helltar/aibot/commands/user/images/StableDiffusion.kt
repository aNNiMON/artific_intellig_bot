package com.helltar.aibot.commands.user.images

import com.annimon.tgbotsmodule.commands.context.MessageContext
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.core.isSuccessful
import com.google.gson.Gson
import com.helltar.aibot.BotConfig.stableDiffusionApiKey
import com.helltar.aibot.Strings
import com.helltar.aibot.commands.BotCommand
import com.helltar.aibot.commands.user.images.models.StableDiffusionData
import com.helltar.aibot.commands.user.images.models.StableDiffusionData.ENGINE_ID
import com.helltar.aibot.commands.user.images.models.StableDiffusionData.TextPromptData
import com.helltar.aibot.utils.NetworkUtils.httpPost
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*

class StableDiffusion(ctx: MessageContext) : BotCommand(ctx) {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run() {
        if (args.isEmpty()) {
            replyToMessage(Strings.stable_diffusion_empty_args)
            return
        }

        if (argsText.length > 2000) {
            replyToMessage(String.format(Strings.many_characters, 2000))
            return
        }

        val response = sendPrompt(argsText)
        val responseJson = response.data.decodeToString()

        if (response.isSuccessful) {
            try {
                val base64 = JSONObject(responseJson).getJSONArray("artifacts").getJSONObject(0).getString("base64")
                // todo: tempFile
                val photo = File.createTempFile("tmp", ".png").apply { writeBytes(Base64.getDecoder().decode(base64)) }
                replyToMessageWithPhoto(photo, argsText)
                return
            } catch (e: Exception) {
                log.error(e.message)
            }
        } else
            try {
                replyToMessage(JSONObject(responseJson).getString("message"))
                return
            } catch (e: Exception) {
                log.error(e.message)
            }

        replyToMessage(Strings.bad_request)
        log.error("$responseJson: $args")
    }

    private fun sendPrompt(prompt: String): Response {
        val url = "https://api.stability.ai/v1/generation/$ENGINE_ID/text-to-image"
        val headers = mapOf("Accept" to "application/json", "Authorization" to "Bearer $stableDiffusionApiKey")
        val body = Gson().toJson(StableDiffusionData.RequestData(text_prompts = listOf(TextPromptData(prompt))))
        return httpPost(url, headers, body)
    }
}