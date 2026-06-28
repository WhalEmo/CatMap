package com.beem.catmap.Maps

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.models.CatModel
import com.beem.catmap.repository.CatRepository
import kotlinx.coroutines.launch

class MapViewModel : ViewModel() {

    private val repository = CatRepository()

    private val _catsList = MutableLiveData<List<CatModel>>()
    val catsList: LiveData<List<CatModel>> get() = _catsList

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    fun fetchAllCats() {
        if (_catsList.value != null && _catsList.value!!.isNotEmpty()) {
            return
        }
        viewModelScope.launch {
            _isLoading.postValue(true)

            try {
                val cats = repository.getAllCats()

                if (cats.isNotEmpty()) {
                    _catsList.postValue(cats)
                } else {
                    _errorMessage.postValue("Haritada hiç kedi bulunamadı.")
                }
            } catch (e: Exception) {
                _errorMessage.postValue("Bağlantı hatası: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun fetchCatsNearLocation(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _isLoading.postValue(true)

            try {
                val cats = repository.getCatsNearLocation(latitude, longitude)

                if (cats.isNotEmpty()) {
                    _catsList.postValue(cats)
                } else {
                    _errorMessage.postValue("Yakınlarda kedi bulunamadı.")
                }
            } catch (e: Exception) {
                _errorMessage.postValue("Bağlantı hatası: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

}