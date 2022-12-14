package com.dgsd.android.solar.mobilewalletadapter

import android.app.Application
import android.net.Uri
import com.dgsd.android.solar.R
import com.dgsd.android.solar.cluster.manager.ClusterManager
import com.dgsd.android.solar.session.manager.SessionManager
import com.solana.mobilewalletadapter.walletlib.association.AssociationUri
import com.solana.mobilewalletadapter.walletlib.association.LocalAssociationUri
import com.solana.mobilewalletadapter.walletlib.authorization.AuthIssuerConfig
import com.solana.mobilewalletadapter.walletlib.protocol.MobileWalletAdapterConfig

class MobileWalletAdapterCoordinatorFactory(
  private val application: Application,
  private val sessionManager: SessionManager,
  private val clusterManager: ClusterManager,
  private val authorityManager: MobileWalletAdapterAuthorityManager,
) {

  fun createFromUri(
    uri: Uri,
    callingPackage: String?,
  ): MobileWalletAdapterCoordinator? {
    val associationUri = AssociationUri.parse(uri)
    if (associationUri == null) {
      // Not a valid URI
      return null
    }

    if (associationUri !is LocalAssociationUri) {
      // We only support local associations
      return null
    }

    val callbacks = MobileWalletAdapterScenarioCallbacks()
    val scenario = associationUri.createScenario(
      application.applicationContext,
      MobileWalletAdapterConfig(
        true,
        10,
        10,
        arrayOf(MobileWalletAdapterConfig.LEGACY_TRANSACTION_VERSION)
      ),
      AuthIssuerConfig(application.getString(R.string.app_name)),
      callbacks
    )

    return MobileWalletAdapterCoordinator(
      sessionManager,
      clusterManager,
      authorityManager,
      callingPackage,
      scenario,
    ).also {
      callbacks.attach(it)
    }
  }
}