package com.dgsd.android.solar.mobilewalletadapter.util

import android.content.Context
import com.dgsd.android.solar.R
import com.dgsd.android.solar.common.ui.PublicKeyFormatter
import com.dgsd.android.solar.common.ui.RichTextFormatter
import com.dgsd.android.solar.common.ui.SolTokenFormatter
import com.dgsd.android.solar.extensions.extractBestDisplayRecipient
import com.dgsd.android.solar.extensions.getMemoMessage
import com.dgsd.android.solar.extensions.getSystemProgramInstruction
import com.dgsd.android.solar.session.model.WalletSession
import com.dgsd.ksol.core.model.LocalTransaction
import com.dgsd.ksol.core.programs.system.SystemProgramInstruction

fun createTransactionSummaryString(
  context: Context,
  session: WalletSession,
  transaction: LocalTransaction,
  publicKeyFormatter: PublicKeyFormatter,
): CharSequence {
  val activeWallet = session.publicKey
  val recipient = transaction.message.extractBestDisplayRecipient(activeWallet)

  val systemProgramInfo = transaction.message.getSystemProgramInstruction()
  val transferText = if (systemProgramInfo == null) {
    null
  } else {
    if (systemProgramInfo.instruction == SystemProgramInstruction.TRANSFER ||
      systemProgramInfo.instruction == SystemProgramInstruction.TRANSFER_WITH_SEED
    ) {
      RichTextFormatter.expandTemplate(
        context,
        R.string.mobile_wallet_adapter_sign_transaction_summary_transfer_template,
        RichTextFormatter.bold(SolTokenFormatter.format(systemProgramInfo.lamports))
      )
    } else {
      RichTextFormatter.expandTemplate(
        context,
        R.string.mobile_wallet_adapter_sign_transaction_summary_transaction_template,
        RichTextFormatter.bold(SolTokenFormatter.format(systemProgramInfo.lamports))
      )
    }
  }

  val memoMessage = transaction.message.getMemoMessage()
  val memoText = if (memoMessage == null) {
    null
  } else {
    RichTextFormatter.expandTemplate(
      context,
      R.string.mobile_wallet_adapter_sign_transaction_summary_memo_template,
      RichTextFormatter.bold(
        context.getString(R.string.mobile_wallet_adapter_sign_transaction_summary_create_memo)
      ),
      memoMessage
    )
  }

  return when {
    transferText != null && memoMessage != null -> {
      "$transferText ($memoMessage)"
    }

    transferText != null -> transferText
    memoText != null -> memoText
    recipient == null -> context.getString(R.string.mobile_wallet_adapter_sign_transaction_unknown)
    else -> {
      RichTextFormatter.expandTemplate(
        context,
        R.string.mobile_wallet_adapter_sign_transaction_to_template,
        publicKeyFormatter.format(recipient)
      )
    }
  }
}