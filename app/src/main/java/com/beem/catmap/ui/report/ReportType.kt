package com.beem.catmap.ui.report

enum class ReportType(val title: String, val reasons: List<String>) {
    POST(
        title = "Bu Gönderiyi Neden Bildiriyorsunuz?",
        reasons = listOf(
            "Gönderi görseli rahatsız edici / uygunsuz",
            "Kediye şiddet veya kötü muamele içeriyor",
            "Yanıltıcı veya yanlış konum bilgisi",
            "Spam veya dolandırıcılık içerikli paylaşım",
            "Nefret söylemi veya taciz"
        )
    ),
    COMMENT(
        title = "Bu Yorumu Neden Bildiriyorsunuz?",
        reasons = listOf(
            "Yorum içeriği rahatsız edici / hakaret içeriyor",
            "Spam, reklam veya bağlantı paylaşımı",
            "Nefret söylemi, şiddet veya taciz",
            "Konuyla tamamen alakasız içerik"
        )
    ),
    REPLY(
        title = "Bu Yanıtı Neden Bildiriyorsunuz?",
        reasons = listOf(
            "Yanıt içeriği rahatsız edici / kaba",
            "Kullanıcıya yönelik doğrudan taciz veya nefret",
            "Spam veya alakasız yanıt döşeme",
            "Uygunsuz dil kullanımı"
        )
    ),
    CAT(
        title = "Bu Kediyi Neden Bildiriyorsunuz?",
        reasons = listOf(
            "Kedi profil bilgileri sahte veya yanıltıcı",
            "Sokak hayvanı değil / sahipli ev kedisi",
            "Görsel kediye ait değil veya uygunsuz"
        )
    ),
    PROFILE(
        title = "Bu Profili Neden Bildiriyorsunuz?",
        reasons = listOf(
            "Kullanıcı adı veya profil fotoğrafı uygunsuz",
            "Taklit hesap (Başka birini veya kurumu taklit ediyor)",
            "Dolandırıcılık veya topluluğu manipüle etme",
            "Sürekli olarak kuralları ihlal eden paylaşımlar"
        )
    )
}