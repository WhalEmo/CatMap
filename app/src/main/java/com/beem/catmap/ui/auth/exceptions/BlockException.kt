package com.beem.catmap.ui.auth.exceptions

import com.beem.catmap.data.model.PublicUser

class IsBlockedByException(val publicProfile: PublicUser?) : Exception("Bu kullanıcı sizi engelledi.")