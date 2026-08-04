package com.beem.catmap.gonderi

import com.beem.catmap.data.repository.PostRepository

// SavePostToProfileUseCase.kt
class SavePostToProfileUseCase(
    private val postRepository: PostRepository = PostRepository
) {
    suspend operator fun invoke(userId: String, docId: String): Result<Unit> {
        return postRepository.kullaniciGonderiKaydet(userId, docId)
    }
}