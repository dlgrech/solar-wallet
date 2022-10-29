package com.dgsd.android.solar.mobilewalletadapter

import android.app.Application
import com.solana.mobilewalletadapter.walletlib.scenario.AuthorizeRequest

class MobileWalletAdapterAuthorityManager(
  private val application: Application,
) {

  private val authorityPrefix = application.packageName

  fun isValidAuthority(callingPackage: String?, bytes: ByteArray): Boolean {
    val segments = bytes.decodeToString().split(DELIMITER)
    return if (segments.size < 2) {
      false
    } else if (segments.first() != authorityPrefix) {
      false
    } else {
      when (segments[1]) {
        AUTH_TYPE_LOCAL_APP -> {
          isValidLocalAppAuthority(callingPackage, segments)
        }

        AUTH_TYPE_WEB_APP -> {
          isValidLocalWebAuthority(segments)
        }

        else -> false
      }
    }
  }

  private fun isValidLocalAppAuthority(callingPackage: String?, segments: List<String>): Boolean {
    return if (segments.size != 4) {
      false
    } else if (callingPackage == null) {
      false
    } else if (callingPackage != segments[2]) {
      false
    } else {
      val callingPackageUid = getPackageUid(callingPackage)
      callingPackageUid != NO_UID && callingPackageUid == segments.last().toIntOrNull()
    }
  }

  private fun isValidLocalWebAuthority(segments: List<String>): Boolean {
    return segments.size == 3
  }

  fun createAuthority(callingPackage: String?, authorizeRequest: AuthorizeRequest): ByteArray {
    return when {
      callingPackage != null -> buildString {
        append(authorityPrefix)
        append(DELIMITER)
        append(AUTH_TYPE_LOCAL_APP)
        append(DELIMITER)
        append(callingPackage)
        append(DELIMITER)
        append(getPackageUid(callingPackage))
      }.toByteArray()

      authorizeRequest.identityUri != null -> buildString {
        append(authorityPrefix)
        append(DELIMITER)
        append(AUTH_TYPE_WEB_APP)
        append(DELIMITER)
        append(checkNotNull(authorizeRequest.identityUri).authority)
      }.toByteArray()

      else -> error("Remote authorizations not supported")
    }
  }

  private fun getPackageUid(packageName: String): Int {
    return runCatching {
      application.packageManager.getPackageUid(packageName, 0)
    }.getOrDefault(NO_UID)
  }

  companion object {
    private const val NO_UID = -1

    private const val DELIMITER = ":"

    private const val AUTH_TYPE_LOCAL_APP = "local-app"
    private const val AUTH_TYPE_WEB_APP = "web-app"
  }
}