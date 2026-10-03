package com.beem.catmap.data.local

import java.util.concurrent.ConcurrentHashMap

object CacheHelperPostLikeV2 {
    private val myLikes: MutableSet<String> = ConcurrentHashMap.newKeySet()

    @JvmStatic
    fun setLikesList(list: Set<String>?) {
        myLikes.clear()
        if (!list.isNullOrEmpty()) {
            myLikes.addAll(list)
        }
    }

    @JvmStatic
    fun isLiked(postId: String?): Boolean {
        if (postId.isNullOrBlank()) return false
        return myLikes.contains(postId)
    }

    @JvmStatic
    fun like(postId: String?) {
        if (!postId.isNullOrBlank()) {
            myLikes.add(postId)
        }
    }

    @JvmStatic
    fun unLike(postId: String?) {
        if (!postId.isNullOrBlank()) {
            myLikes.remove(postId)
        }
    }

    @JvmStatic
    fun clear() {
        myLikes.clear()
    }
}