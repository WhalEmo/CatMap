package com.beem.catmap.KullaniciAuth


fun Kullanici.copy(
    id: String = this.id,
    kullaniciAdi: String = this.kullaniciAdi,
    ad: String = this.ad,
    soyad: String = this.soyad,
    biyografi: String = this.biyografi,
    fotoUrl: String = this.fotoUrl,
    takipciSayisi: Long = this.takipciSayisi ?: 0L,
    takipEdilenSayisi: Long = this.takipEdilenSayisi ?: 0L,
    gonderiSayisi: Long = this.gonderiSayisi ?: 0L
): Kullanici {
    val newInstance = Kullanici()
    newInstance.id = id
    newInstance.kullaniciAdi = kullaniciAdi
    newInstance.ad = ad
    newInstance.soyad = soyad
    newInstance.biyografi = biyografi
    newInstance.fotoUrl = fotoUrl
    newInstance.takipciSayisi = takipciSayisi
    newInstance.takipEdilenSayisi = takipEdilenSayisi
    newInstance.gonderiSayisi = gonderiSayisi

    // Diğer alanları koru
    newInstance.latitude = this.latitude
    newInstance.longitude = this.longitude
    newInstance.isCevrimiciMi = this.isCevrimiciMi
    newInstance.takipEdiyorMuyum = this.takipEdiyorMuyum
    newInstance.takipciMi = this.takipciMi
    newInstance.isProfileLoaded = this.isProfileLoaded

    return newInstance
}