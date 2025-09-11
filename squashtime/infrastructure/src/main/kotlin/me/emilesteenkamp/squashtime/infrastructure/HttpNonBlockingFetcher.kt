package me.emilesteenkamp.squashtime.infrastructure

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.request
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.setCookie
import io.ktor.http.toHttpDate
import io.ktor.util.flattenEntries
import io.ktor.utils.io.InternalAPI
import it.skrape.fetcher.Cookie
import it.skrape.fetcher.Domain
import it.skrape.fetcher.Expires
import it.skrape.fetcher.Method
import it.skrape.fetcher.NonBlockingFetcher
import it.skrape.fetcher.Request
import it.skrape.fetcher.Result
import it.skrape.fetcher.SameSite
import it.skrape.fetcher.urlOrigin
import kotlin.collections.component1
import kotlin.collections.component2
import me.tatarka.inject.annotations.Inject

@OptIn(InternalAPI::class)
internal class HttpNonBlockingFetcher
@Inject
constructor(
    val httpClient: HttpClient
) : NonBlockingFetcher<Request> {
    override val requestBuilder: Request
        get() = Request()

    override suspend fun fetch(request: Request): Result =
        httpClient.request(request.toHttpRequest()).toResult()

    companion object {
        private fun Request.toHttpRequest(): HttpRequestBuilder {
            val request = this
            return HttpRequestBuilder().apply {
                method = request.method.toHttpMethod()
                url(request.url)
                headers {
                    request.headers.forEach { (k, v) ->
                        append(k, v)
                    }
                    append("User-Agent", request.userAgent)
                    cookies = request.cookies
                    request.authentication?.run {
                        append("Authorization", toHeaderValue())
                    }
                }
                request.body?.let { setBody(it) }
                timeout {
                    socketTimeoutMillis = request.timeout.toLong()
                }
            }
        }

        private fun Method.toHttpMethod(): HttpMethod = when (this) {
            Method.GET -> HttpMethod.Get
            Method.POST -> HttpMethod.Post
            Method.HEAD -> HttpMethod.Head
            Method.DELETE -> HttpMethod.Delete
            Method.PATCH -> HttpMethod.Patch
            Method.PUT -> HttpMethod.Put
        }

        private suspend fun HttpResponse.toResult(): Result = Result(
            responseBody = this.body(),
            responseStatus = this.toStatus(),
            contentType = this.contentType()?.toString()?.replace(" ", ""),
            headers = this.headers.flattenEntries()
                .associateBy({ item -> item.first }, { item -> this.headers[item.first]!! }),
            cookies = this.setCookie().map { cookie -> cookie.toDomainCookie(this.request.url.toString().urlOrigin) },
            baseUri = this.request.url.toString()
        )

        private fun HttpResponse.toStatus() = Result.Status(this.status.value, this.status.description)

        private fun io.ktor.http.Cookie.toDomainCookie(origin: String): Cookie {
            val path = this.path ?: "/"
            val expires = this.expires?.toHttpDate().toExpires()
            val domain = when (val domainUrl = this.domain) {
                null -> Domain(origin, false)
                else -> Domain(domainUrl.urlOrigin, true)
            }
            val sameSite = this.extensions["SameSite"].toSameSite()
            val maxAge = this.maxAge?.toMaxAge()

            return Cookie(this.name, this.value, expires, maxAge, domain, path, sameSite, this.secure, this.httpOnly)
        }

        private fun String?.toExpires(): Expires {
            return when (this) {
                null -> Expires.Session
                else -> Expires.Date(this)
            }
        }

        private fun String?.toSameSite(): SameSite = when (this?.lowercase()) {
            "strict" -> SameSite.STRICT
            "lax" -> SameSite.LAX
            "none" -> SameSite.NONE
            else -> SameSite.LAX
        }

        private fun Int.toMaxAge(): Int? = when (this) {
            0 -> null
            else -> this
        }
    }
}