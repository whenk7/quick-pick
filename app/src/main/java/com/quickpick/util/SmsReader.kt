package com.quickpick.util

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SmsMessage(
    val id: String,
    val body: String,
    val timestamp: Long,
    val address: String,
    val threadId: Long?
)

object SmsReader {

    private val SMS_URI: Uri = Telephony.Sms.Inbox.CONTENT_URI
    private val PROJECTION = arrayOf(
        Telephony.Sms._ID,
        Telephony.Sms.BODY,
        Telephony.Sms.DATE,
        Telephony.Sms.ADDRESS,
        Telephony.Sms.THREAD_ID
    )

    /**
     * 读取所有收件箱短信（IO 线程执行）。
     *
     * 注意：不要在这里捕获 SecurityException！
     * 无权限时让它抛出，由调用方区分「无权限」和「收件箱真空」——
     * 吞掉异常返回空表会把空收件箱误判成无权限。
     */
    suspend fun readAll(context: Context): List<SmsMessage> = query(context, null, null)

    /**
     * 读取时间窗口内的收件箱短信（DATE >= cutoffMs）。
     * SecurityException 处理原则同 readAll：不吞异常。
     */
    suspend fun readSince(context: Context, cutoffMs: Long): List<SmsMessage> =
        query(context, "${Telephony.Sms.DATE} >= ?", arrayOf(cutoffMs.toString()))

    private suspend fun query(
        context: Context,
        selection: String?,
        selectionArgs: Array<String>?
    ): List<SmsMessage> = withContext(Dispatchers.IO) {
        val messages = mutableListOf<SmsMessage>()
        context.contentResolver.query(
            SMS_URI,
            PROJECTION,
            selection, selectionArgs,
            "${Telephony.Sms.DATE} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
            val bodyCol = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateCol = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val addressCol = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val threadIdCol = cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            while (cursor.moveToNext()) {
                messages.add(
                    SmsMessage(
                        id = cursor.getString(idCol),
                        body = cursor.getString(bodyCol),
                        timestamp = cursor.getLong(dateCol),
                        address = cursor.getString(addressCol) ?: "",
                        threadId = if (cursor.isNull(threadIdCol)) null else cursor.getLong(threadIdCol)
                    )
                )
            }
        }
        messages
    }
}
