package com.beem.catmap.ui.manager

import com.beem.catmap.data.model.CatModel

sealed class CatMapEvent {
    data class Created(val cat: CatModel) : CatMapEvent()
    data class Updated(val cat: CatModel) : CatMapEvent()
    data class Deleted(val catId: String) : CatMapEvent()
}