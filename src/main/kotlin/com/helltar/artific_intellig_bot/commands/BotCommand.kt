package com.helltar.artific_intellig_bot.commands

import com.annimon.tgbotsmodule.api.methods.Methods
import com.annimon.tgbotsmodule.commands.context.MessageContext
import com.helltar.artific_intellig_bot.BotConfig
import com.helltar.artific_intellig_bot.DIR_DB
import com.helltar.artific_intellig_bot.EXT_DISABLED
import com.helltar.artific_intellig_bot.db.Database
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import java.io.File
import java.io.Serializable

abstract class BotCommand(val ctx: MessageContext) : BotConfig() {

    protected companion object {
        const val FUEL_TIMEOUT = 60000
    }

    protected val userId = ctx.user().id
    protected val message = ctx.message()
    protected val args: Array<String> = ctx.arguments()
    protected val argsText: String = ctx.argumentsAsString()

    abstract fun run()

    fun isCommandDisabled(commandName: String) =
        File("$DIR_DB/commandName$EXT_DISABLED").exists()

    fun isChatInWhiteList() =
        Database.chatWhiteList.isChatExists(ctx.chatId())

    fun isUserBanned(userId: Long) =
        Database.banList.isUserBanned(userId)

    fun isNotAdmin() =
        !Database.sudoers.isAdmin(userId)

    fun isAdmin() =
        !isNotAdmin()

    fun isCreator(userId: Long = this.userId) =
        Database.sudoers.isCreator(userId)

    fun replyToMessage(
        text: String,
        messageId: Int = ctx.messageId(),
        enableWebPagePreview: Boolean = false,
        markdown: Boolean = false
    ): Int =
        ctx.replyToMessage(text)
            .setReplyToMessageId(messageId)
            .setParseMode(if (!markdown) ParseMode.HTML else ParseMode.MARKDOWN)
            .setWebPagePreviewEnabled(enableWebPagePreview)
            .call(ctx.sender)
            .messageId

    protected fun replyToMessageWithMarkup(text: String, replyMarkup: InlineKeyboardMarkup): Int =
        ctx.replyToMessage(text)
            .setReplyMarkup(replyMarkup)
            .setReplyToMessageId(ctx.messageId())
            .call(ctx.sender)
            .messageId

    protected fun editMessageText(text: String, messageId: Int, replyMarkup: InlineKeyboardMarkup): Serializable =
        Methods.editMessageText(ctx.chatId(), messageId, text)
            .setReplyMarkup(replyMarkup)
            .call(ctx.sender)

    protected fun replyToMessageWithPhoto(file: File, caption: String): Message =
        ctx.replyToMessageWithPhoto()
            .setFile(file)
            .setCaption(caption)
            .setParseMode(ParseMode.HTML)
            .call(ctx.sender)

    protected fun replyToMessageWithPhoto(url: String, caption: String = "", messageId: Int = ctx.messageId()): Message =
        ctx.replyToMessageWithPhoto()
            .setFile(InputFile(url))
            .setCaption(caption)
            .setReplyToMessageId(messageId)
            .setParseMode(ParseMode.HTML)
            .call(ctx.sender)

    protected fun replyToMessageWithDocument(fileId: String, caption: String): Int =
        ctx.replyWithDocument()
            .setFile(fileId)
            .setCaption(caption)
            .setReplyToMessageId(ctx.messageId())
            .call(ctx.sender)
            .messageId

    protected fun sendVoice(file: File, messageId: Int): Message =
        ctx.replyToMessageWithAudio()
            .setFile(file)
            .setReplyToMessageId(messageId)
            .call(ctx.sender)

    protected fun deleteMessage(messageId: Int) =
        ctx.deleteMessage().setMessageId(messageId).callAsync(ctx.sender)
}