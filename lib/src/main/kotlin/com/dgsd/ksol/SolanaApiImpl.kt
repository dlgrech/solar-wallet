package com.dgsd.ksol

import com.dgsd.ksol.jsonrpc.RpcRequestFactory
import com.dgsd.ksol.jsonrpc.SolanaJsonRpcConstants
import com.dgsd.ksol.jsonrpc.networking.RpcError
import com.dgsd.ksol.jsonrpc.networking.RpcException
import com.dgsd.ksol.jsonrpc.networking.RpcIOException
import com.dgsd.ksol.jsonrpc.networking.util.await
import com.dgsd.ksol.jsonrpc.types.*
import com.dgsd.ksol.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

private val MEDIA_TYPE_JSON = "application/json; charset=utf-8".toMediaType()

/**
 * Internal implementation of [SolanaApi] interface
 */
internal class SolanaApiImpl(
    private val cluster: Cluster,
    private val okHttpClient: OkHttpClient,
) : SolanaApi {

    private val moshiJson: Moshi = Moshi.Builder().build()

    private val requestJsonAdapter = moshiJson.adapter(RpcRequest::class.java)

    override suspend fun getBalance(accountHash: String, commitment: Commitment): Long {
        val request = RpcRequestFactory.create(
            SolanaJsonRpcConstants.Methods.GET_BALANCE,
            accountHash,
            commitment.toRequestBody()
        )

        val response = executeRequest<GetBalanceResponseBody>(request)

        return response.value
    }

    override suspend fun getRecentBlockhash(commitment: Commitment): RecentBlockhashResult {
        val request = RpcRequestFactory.create(
            SolanaJsonRpcConstants.Methods.GET_RECENT_BLOCKHASH,
            commitment.toRequestBody()
        )

        val response = executeRequest<RecentBlockhashResponseBody>(request)

        return RecentBlockhashResult(
            response.value.blockhash,
            response.value.feeCalculator.value
        )
    }

    override suspend fun getBlockTime(blockSlotNumber: Long): Long? {
        val request = RpcRequestFactory.create(
            SolanaJsonRpcConstants.Methods.GET_BLOCK_TIME,
            blockSlotNumber
        )

        return executeRequest(request)
    }

    override suspend fun getBlockHeight(commitment: Commitment): Long {
        val request = RpcRequestFactory.create(
            SolanaJsonRpcConstants.Methods.GET_BLOCK_HEIGHT,
            commitment.toRequestBody()
        )

        return executeRequest(request)
    }

    override suspend fun getLargestAccounts(
        circulatingStatus: AccountCirculatingStatus?,
        commitment: Commitment,
    ): List<AccountBalance> {
        val request = RpcRequestFactory.create(
            SolanaJsonRpcConstants.Methods.GET_LARGEST_ACCOUNTS,
            GetLargestAccountsRequestBody(
                commitment.toRpcValue(),
                when (circulatingStatus) {
                    null -> null
                    AccountCirculatingStatus.CIRCULATING -> GetLargestAccountsRequestBody.FILTER_CIRCULATING
                    AccountCirculatingStatus.NON_CIRCULATING -> GetLargestAccountsRequestBody.FILTER_NON_CIRCULATING
                }
            )
        )

        val response = executeRequest<GetLargestAccountsResponseBody>(request)

        return response.value.map {
            AccountBalance(it.address, it.lamports)
        }
    }

    override suspend fun getTransactionCount(commitment: Commitment): Long {
        val request = RpcRequestFactory.create(
            SolanaJsonRpcConstants.Methods.GET_TRANSACTION_COUNT,
            commitment.toRequestBody()
        )

        return executeRequest(request)
    }

    private suspend inline fun <reified T> executeRequest(rpcRequest: RpcRequest): T {
        try {
            val httpRequest = rpcRequest.asHttpRequest()
            val httpResponse = okHttpClient.newCall(httpRequest).await()

            val responseJson = checkNotNull(httpResponse.body).string()
            val responseParser = moshiJson.adapter<RpcResponse<T>>(
                Types.newParameterizedType(RpcResponse::class.java, T::class.java)
            )

            val parsedResponse = responseParser.fromJson(responseJson)

            if (parsedResponse?.error != null) {
                throw RpcError(parsedResponse.error.code, parsedResponse.error.message)
            } else if (parsedResponse?.result == null && null !is T) {
                throw RpcException("Missing result body when executing request: ${rpcRequest.methodName}")
            } else {
                return parsedResponse?.result as T
            }
        } catch (e: RpcError) {
            throw e
        } catch (e: RpcException) {
            throw e
        } catch (e: IOException) {
            throw RpcIOException("Error executing request: ${rpcRequest.methodName}", e)
        } catch (e: Throwable) {
            throw RpcException("Error executing request: ${rpcRequest.methodName}", e)
        }
    }

    private fun Commitment.toRequestBody(): CommitmentConfigRequestBody {
        return CommitmentConfigRequestBody(this.toRpcValue())
    }

    private fun Commitment.toRpcValue(): String {
        return when (this) {
            Commitment.FINALIZED -> CommitmentConfigRequestBody.FINALIZED
            Commitment.CONFIRMED -> CommitmentConfigRequestBody.CONFIRMED
            Commitment.PROCESSED -> CommitmentConfigRequestBody.PROCESSED
        }
    }

    private fun RpcRequest.asHttpRequest(): Request {
        return Request.Builder()
            .url(cluster.endpoint)
            .post(requestJsonAdapter.toJson(this).toRequestBody(MEDIA_TYPE_JSON))
            .build()
    }
}