package com.dgsd.android.solar.session.manager

import com.dgsd.android.solar.session.model.Session
import com.dgsd.ksol.model.KeyPair
import com.dgsd.ksol.model.PublicKey
import kotlinx.coroutines.flow.StateFlow

interface SessionManager {

    /**
     * The current session being used to interact with the underlying wallet
     */
    val activeSession: StateFlow<Session>

    /**
     * Resets the state of the [SessionManager] such that there will be no active session
     */
    fun clear()

    /**
     * Sets the active session to represent the wallet with the given [PublicKey].
     */
    fun setActiveSession(publicKey: PublicKey)

    /**
     * Sets the active session to represent the wallet with the given [KeyPair].
     *
     * This should be used when the user has granted access to their private key
     */
    fun setActiveSession(keyPair: KeyPair)
}