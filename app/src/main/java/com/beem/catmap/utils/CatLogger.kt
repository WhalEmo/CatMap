package com.beem.catmap.utils


import android.util.Log

interface LoggerEngine {
    fun d(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable? = null)
    fun w(tag: String, message: String)
}

/**
 * Android Log framework'ünün default implementasyonu
 */
object DefaultLoggerEngine : LoggerEngine {
    override fun d(tag: String, message: String) {
        Log.d(tag, message)
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        Log.e(tag, message, throwable)
    }

    override fun w(tag: String, message: String) {
        Log.w(tag, message)
    }
}

/**
 * Repository ve servislerde log takibini kolaylaştıran merkezi log sınıfı.
 */
object CatLogger {

    // Unit testlerde logları test edebilmek veya sahte logger bağlayabilmek için
    var engine: LoggerEngine = DefaultLoggerEngine

    /**
     * Hata durumlarında Catch blokları içerisinde tek satırda çağrılabilir.
     */
    fun logError(tag: String, action: String, throwable: Throwable? = null) {
        val errorMessage = "❌ [$action] Başarısız! Hata: ${throwable?.localizedMessage ?: "Bilinmeyen Hata"}"
        engine.e(tag, errorMessage, throwable)
    }

    /**
     * Başarılı veya bilgi verici işlemlerde çağrılır.
     */
    fun logInfo(tag: String, message: String) {
        engine.d(tag, "ℹ️ $message")
    }

    /**
     * Uyarı durumlarında çağrılır.
     */
    fun logWarning(tag: String, message: String) {
        engine.w(tag, "⚠️ $message")
    }
}