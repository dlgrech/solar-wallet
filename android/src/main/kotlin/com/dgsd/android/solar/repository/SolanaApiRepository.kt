package com.dgsd.android.solar.repository

import com.dgsd.android.solar.cache.CacheStrategy
import com.dgsd.android.solar.common.model.Resource
import com.dgsd.android.solar.model.LamportsWithTimestamp
import com.dgsd.android.solar.model.TransactionOrSignature
import com.dgsd.ksol.core.model.*
import kotlinx.coroutines.flow.Flow
import java.io.Closeable

interface SolanaApiRepository : Closeable {

  fun getBalance(
    cacheStrategy: CacheStrategy = CacheStrategy.CACHE_IF_PRESENT,
    commitment: Commitment = Commitment.FINALIZED
  ): Flow<Resource<LamportsWithTimestamp>>

  fun getBalanceOfAccount(
    account: PublicKey,
    cacheStrategy: CacheStrategy = CacheStrategy.CACHE_IF_PRESENT,
    commitment: Commitment = Commitment.FINALIZED
  ): Flow<Resource<LamportsWithTimestamp>>

  fun getTransactions(
    cacheStrategy: CacheStrategy = CacheStrategy.CACHE_IF_PRESENT,
    beforeSignature: TransactionSignature? = null,
    commitment: Commitment = Commitment.FINALIZED
  ): Flow<Resource<List<Resource<TransactionOrSignature>>>>

  fun getTransaction(
    cacheStrategy: CacheStrategy = CacheStrategy.CACHE_IF_PRESENT,
    transactionSignature: TransactionSignature,
    commitment: Commitment = Commitment.FINALIZED
  ): Flow<Resource<Transaction>>

  fun getRecentBlockhash(): Flow<Resource<RecentBlockhashResult>>

  fun send(
    privateKey: PrivateKey,
    recipient: PublicKey,
    lamports: Lamports,
    memo: String? = null
  ): Flow<Resource<TransactionSignature>>

  fun signAndSend(
    privateKey: PrivateKey,
    localTransaction: LocalTransaction,
  ): Flow<Resource<TransactionSignature>>

  fun subscribeToUpdates(
    transactionSignature: TransactionSignature,
    commitment: Commitment = Commitment.FINALIZED
  ): SubscriptionHandle<TransactionSignatureStatus>
}