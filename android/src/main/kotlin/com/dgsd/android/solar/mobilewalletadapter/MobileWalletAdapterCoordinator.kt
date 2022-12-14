package com.dgsd.android.solar.mobilewalletadapter

import com.dgsd.android.solar.cluster.manager.ClusterManager
import com.dgsd.android.solar.flow.MutableEventFlow
import com.dgsd.android.solar.flow.SimpleMutableEventFlow
import com.dgsd.android.solar.flow.asEventFlow
import com.dgsd.android.solar.flow.call
import com.dgsd.android.solar.mobilewalletadapter.model.MobileWalletAuthRequestCluster
import com.dgsd.android.solar.session.manager.SessionManager
import com.dgsd.android.solar.session.model.WalletSession
import com.dgsd.ksol.core.model.PublicKey
import com.solana.mobilewalletadapter.walletlib.scenario.*

class MobileWalletAdapterCoordinator internal constructor(
  private val sessionManager: SessionManager,
  private val clusterManager: ClusterManager,
  private val authorityManager: MobileWalletAdapterAuthorityManager,
  val callingPackage: String?,
  private val scenario: Scenario,
) {

  sealed interface Destination {
    object Authorize : Destination
    object SignTransactions : Destination
    object SignMessages : Destination
    object SignAndSendTransactions : Destination
  }

  private val _destination = MutableEventFlow<Destination>()
  val destination = _destination.asEventFlow()

  private val _terminate = SimpleMutableEventFlow()
  val terminate = _terminate.asEventFlow()

  var authorizationRequest: AuthorizeRequest? = null
    private set

  var signTransactionsRequest: SignTransactionsRequest? = null
    private set

  var signMessagesRequest: SignMessagesRequest? = null
    private set

  var signAndSendTransactionsRequest: SignAndSendTransactionsRequest? = null
    private set

  fun start() {
    scenario.start()
  }

  fun close() {
    scenario.close()
  }

  internal fun navigateToAuthorizationRequest(request: AuthorizeRequest) {
    if (!isValidCluster(request.cluster)) {
      // Mismatched cluster
      request.completeWithDecline()
    } else {
      authorizationRequest = request
      _destination.tryEmit(Destination.Authorize)
    }
  }

  internal fun navigateWithReauthorizationRequest(request: ReauthorizeRequest) {
    if (!isValidCluster(request.cluster)) {
      request.completeWithDecline()
    } else if (!isValidAuthorizationScope(request.authorizationScope)) {
      request.completeWithDecline()
    } else {
      request.completeWithReauthorize()
    }
  }

  internal fun navigateWithSignTransactionsRequest(request: SignTransactionsRequest) {
    if (!isValidCluster(request.cluster)) {
      request.completeWithDecline()
    } else if (!isValidAuthorizationScope(request.authorizationScope)) {
      request.completeWithAuthorizationNotValid()
    } else if (!isForCurrentSessionWallet(request.authorizedPublicKey)) {
      request.completeWithAuthorizationNotValid()
    } else {
      signTransactionsRequest = request
      _destination.tryEmit(Destination.SignTransactions)
    }
  }

  internal fun navigateWithSignMessagesRequest(request: SignMessagesRequest) {
    if (!isValidCluster(request.cluster)) {
      request.completeWithDecline()
    } else if (!isValidAuthorizationScope(request.authorizationScope)) {
      request.completeWithAuthorizationNotValid()
    } else if (!isForCurrentSessionWallet(request.authorizedPublicKey)) {
      request.completeWithAuthorizationNotValid()
    } else {
      signMessagesRequest = request
      _destination.tryEmit(Destination.SignMessages)
    }
  }

  internal fun navigateWithSignAndSendTransactionsRequest(request: SignAndSendTransactionsRequest) {
    if (!isValidCluster(request.cluster)) {
      request.completeWithDecline()
    } else if (!isValidAuthorizationScope(request.authorizationScope)) {
      request.completeWithAuthorizationNotValid()
    } else if (!isForCurrentSessionWallet(request.publicKey)) {
      request.completeWithAuthorizationNotValid()
    } else {
      signAndSendTransactionsRequest = request
      _destination.tryEmit(Destination.SignAndSendTransactions)
    }
  }

  internal fun onTeardownComplete() {
    _terminate.call()
  }

  private fun isValidCluster(requestCluster: String): Boolean {
    val activeCluster = clusterManager.activeCluster.value
    val incomingRequestCluster =
      MobileWalletAuthRequestCluster.fromClusterName(requestCluster).toCluster()

    return activeCluster == incomingRequestCluster
  }

  private fun isValidAuthorizationScope(authorizationScope: ByteArray): Boolean {
    return authorityManager.isValidAuthority(callingPackage, authorizationScope)
  }

  private fun isForCurrentSessionWallet(publicKeyBytes: ByteArray): Boolean {
    val activeWallet = (sessionManager.activeSession.value as? WalletSession)?.publicKey
    return if (activeWallet == null) {
      false
    } else {
      publicKeyBytes.toPublicKeyOrNull() == activeWallet
    }
  }

  private fun ByteArray.toPublicKeyOrNull(): PublicKey? {
    return runCatching { PublicKey.fromByteArray(this) }.getOrNull()
  }
}