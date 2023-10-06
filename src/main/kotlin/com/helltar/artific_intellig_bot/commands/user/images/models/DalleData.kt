package com.helltar.artific_intellig_bot.commands.user.images.models

object DalleData {

    /* https://beta.openai.com/docs/guides/images/usage?lang=curl */

    data class RequestData(
        val prompt: String,
        val n: Int,
        val size: String = "256x256"
    )
}