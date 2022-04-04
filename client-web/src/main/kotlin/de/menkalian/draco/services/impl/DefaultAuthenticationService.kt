package de.menkalian.draco.services.impl

import com.vaadin.flow.server.VaadinResponse
import com.vaadin.flow.server.VaadinService
import de.menkalian.draco.data.user.EntitledUser
import de.menkalian.draco.restclient.DracoClient
import de.menkalian.draco.services.IAuthenticationService
import de.menkalian.draco.services.IClientProviderService
import org.slf4j.LoggerFactory
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicLong
import javax.servlet.http.Cookie
import kotlin.properties.Delegates

class DefaultAuthenticationService(private val clientProvider: IClientProviderService) : IAuthenticationService {
    companion object {
        val instanceCounter = AtomicLong()
        const val AUTH_COOKIE_NAME = "DracoAuthToken"
    }

    val id = instanceCounter.incrementAndGet()

    private fun logger() = LoggerFactory.getLogger(this.javaClass.canonicalName + "#" + id)

    private val userMutex = Any()

    private val authenticationListeners = mutableListOf<IAuthenticationService.IAuthenticationListener>()
    private var authToken: String? by Delegates.observable(null) { _, _, _ ->
        logger().trace("Changed AuthToken to $authToken")
        updateTokenCookie()
        loadAuthenticatedUser()
        clientWithAuthentication = clientProvider.provideAuthorizedClient(authToken)
    }
    private val initLatch = CountDownLatch(1)

    override val hasAuthToken: Boolean
        get() = authToken?.isNotBlank() == true

    override var currentUser: EntitledUser? by Delegates.observable(null) { _, _, _ ->
        logger().trace("Updated currentUser to $currentUser")
        onAuthenticatedUserChanged()
    }

    override var clientWithAuthentication: DracoClient =
        clientProvider.provideAuthorizedClient(authToken)


    init {
        authToken = VaadinService.getCurrentRequest()
            .cookies
            .firstOrNull { it.name.equals(AUTH_COOKIE_NAME, true) }
            ?.value
        initLatch.await()
    }

    override fun addListener(listener: IAuthenticationService.IAuthenticationListener, fireImmediately: Boolean) {
        logger().debug("Registering listener ($fireImmediately)")
        synchronized(authenticationListeners) {
            authenticationListeners.add(listener)

            if (fireImmediately) {
                synchronized(userMutex) {
                    if (currentUser != null) {
                        listener.onLoggedIn(currentUser!!)
                    } else {
                        listener.onLoggedOut()
                    }
                }
            }
        }
    }

    override fun removeListener(listener: IAuthenticationService.IAuthenticationListener) {
        logger().debug("Unregistering listener")
        synchronized(authenticationListeners) {
            authenticationListeners.remove(listener)
        }
    }

    override fun login(token: String) {
        logger().info("Logging in")
        authToken = token
    }

    override fun logout() {
        logger().info("Logging out")
        authToken = null
    }

    private fun loadAuthenticatedUser() {
        logger().info("Loading user")
        val client = clientProvider.provideAuthorizedClient(authToken)
        client.user.getMe(
            onRead = {
                logger().debug("Read user: $it")
                synchronized(userMutex) {
                    currentUser = it
                }
                initLatch.countDown()
            },
            onError = {
                logger().debug("Error: $it")
                synchronized(userMutex) {
                    currentUser = null
                }
                initLatch.countDown()

                // Do not log request errors
                if ((400..499).contains(it.code).not()) {
                    logger().error("An error occured when querying the current user: $it")
                }
            }
        )
    }

    private fun updateTokenCookie() {
        logger().info("Rewriting token")
        try {
            VaadinResponse.getCurrent()
                .addCookie(Cookie(AUTH_COOKIE_NAME, authToken))
        } catch (ex: java.lang.IllegalArgumentException) {
            VaadinResponse.getCurrent()
                .addCookie(Cookie(AUTH_COOKIE_NAME, "INVALID"))
        }
    }

    private fun onAuthenticatedUserChanged() {
        synchronized(userMutex) {
            synchronized(authenticationListeners) {
                logger().info("Calling listeners: $authenticationListeners")
                authenticationListeners.forEach {
                    if (currentUser != null) {
                        it.onLoggedIn(currentUser!!)
                    } else {
                        it.onLoggedOut()
                    }
                }
            }
        }
    }
}