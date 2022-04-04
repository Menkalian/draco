package de.menkalian.draco.view

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.ComponentUtil
import com.vaadin.flow.component.HasComponents
import com.vaadin.flow.component.Text
import com.vaadin.flow.component.Unit
import com.vaadin.flow.component.applayout.AppLayout
import com.vaadin.flow.component.applayout.DrawerToggle
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.dependency.CssImport
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.H3
import com.vaadin.flow.component.html.Image
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.tabs.Tab
import com.vaadin.flow.component.tabs.Tabs
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.RouterLink
import de.menkalian.draco.data.user.EntitledUser
import de.menkalian.draco.data.user.UserRight
import de.menkalian.draco.logger
import de.menkalian.draco.services.IAuthenticationService
import de.menkalian.draco.services.ISharedServiceFactory
import de.menkalian.draco.view.content.MakeSuggestionContent
import de.menkalian.draco.view.content.ManageQuestionsView
import de.menkalian.draco.view.content.ManageSuggestionsView
import de.menkalian.draco.view.content.ManageUsersView
import de.menkalian.draco.view.content.ReportProblemContent
import de.menkalian.draco.view.content.WelcomeContent
import de.menkalian.draco.view.dialog.LoginDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@CssImport("./main.css")
@CssImport("./applayout-customization.css", themeFor = "vaadin-app-layout")
class MainView(
    private val serviceFactory: ISharedServiceFactory
) : AppLayout() {
    private var log: Logger? = null
    private val localScope = CoroutineScope(Dispatchers.Default)

    private val menu: Tabs
    private val viewTitle: H3

    init {
        addAttachListener {
            val safeUi = ui.get()
            val authService = serviceFactory.getSharedAuthService(safeUi)
            var currentUser = authService.currentUser

            log = LoggerFactory.getLogger("${javaClass.canonicalName}#${safeUi.uiId}")
            authService.addListener(object : IAuthenticationService.IAuthenticationListener {
                override fun onLoggedIn(user: EntitledUser) {
                    currentUser = user
                    safeUi.access {
                        Notification
                            .show("Erfolgreich angemeldet als Benutzer ${user.name}", 2000, Notification.Position.BOTTOM_CENTER)
                    }
                }

                override fun onLoggedOut() {
                    safeUi.access {
                        if (currentUser != authService.currentUser) {
                            currentUser = authService.currentUser
                            Notification
                                .show("Erfolgreich abgemeldet", 2000, Notification.Position.BOTTOM_CENTER)
                        } else {
                            if (authService.hasAuthToken)
                                Notification
                                    .show("Anmeldung fehlgeschlagen", 2000, Notification.Position.BOTTOM_CENTER)
                        }
                    }
                }
            })
        }
        addDetachListener {
            serviceFactory.releaseSharedAuthService(it.ui)
        }

        primarySection = Section.DRAWER

        viewTitle = H3()
        menu = createMenu()

        addToNavbar(true, createHeaderContent())
        addToDrawer(createDrawerContent(menu))
    }

    override fun afterNavigation() {
        super.afterNavigation()

        getTabForComponent(content)?.let { menu.selectedTab = it }
        viewTitle.text = getCurrentPageTitle()
    }

    private fun createHeaderContent(): Component {
        val layout = HorizontalLayout()

        layout.setWidthFull()
        layout.isSpacing = false
        layout.themeList.set("dark", true)
        layout.alignItems = FlexComponent.Alignment.CENTER

        viewTitle.style.set("flex-grow", "1")
        viewTitle.style.set("margin-top", "0")
        viewTitle.style.set("margin-bottom", "0")

        layout.add(DrawerToggle())
        layout.add(viewTitle)
        createLoginComponentsIn(layout)

        return layout
    }

    private fun createDrawerContent(menu: Tabs): Component {
        val drawerLayout = VerticalLayout()
        val logoLayout = HorizontalLayout()
        val logo = Image("images/logo.png", "Draco Logo")
        val appTitle = H1("Draco")

        drawerLayout.setSizeFull()
        drawerLayout.isPadding = false
        drawerLayout.isSpacing = false
        drawerLayout.themeList["spacing-s"] = true
        drawerLayout.alignItems = FlexComponent.Alignment.STRETCH

        logoLayout.alignItems = FlexComponent.Alignment.CENTER

        logo.maxWidth = "5rem"

        appTitle.style.set("margin", "0")

        // Display the logo and the menu in the drawer
        drawerLayout.add(logoLayout, menu)
        logoLayout.add(logo, appTitle)
        return drawerLayout
    }

    private fun createMenu(): Tabs {
        val tabs = Tabs()
        tabs.orientation = Tabs.Orientation.VERTICAL
        tabs.add(*createMenuItems())
        return tabs
    }

    private fun createMenuItems(): Array<Component> {
        return arrayOf(
            createTab("Startseite", WelcomeContent::class.java),
            createTab("Vorschläge einreichen", MakeSuggestionContent::class.java),
            createTab(
                "Benutzerverwaltung", ManageUsersView::class.java,
                UserRight.USER_CREATE, UserRight.USER_READ, UserRight.USER_UPDATE, UserRight.USER_DELETE
            ),
            createTab(
                "Fragenverwaltung", ManageQuestionsView::class.java,
                UserRight.QUESTION_CREATE, UserRight.QUESTION_READ, UserRight.QUESTION_UPDATE, UserRight.QUESTION_DELETE
            ),
            createTab(
                "Vorschlagsverwaltung", ManageSuggestionsView::class.java,
                UserRight.SUGGESTION_COMMENT_CREATE, UserRight.SUGGESTION_READ, UserRight.SUGGESTION_UPDATE, UserRight.SUGGESTION_DELETE
            ),
            createTab("Ein Problem melden", ReportProblemContent::class.java),
        )
    }

    private fun <T : Component> createTab(text: String, target: Class<T>, vararg accessableRights: UserRight): Tab {
        val tab = Tab()

        // Setup routing
        tab.add(RouterLink(text, target))
        ComponentUtil.setData(tab, Class::class.java, target)

        // Setup visibility
        if (accessableRights.isNotEmpty()) {
            addAttachListener {
                val safeUi = ui.get()
                val authService = serviceFactory.getSharedAuthService(safeUi)

                setupVisibilityControls(tab, authService) {
                    accessableRights.any { right -> it?.hasRight(right) == true }
                }
            }
        }

        return tab
    }

    private fun createLoginComponentsIn(layout: HasComponents) {
        addAttachListener {
            val safeUi = ui.get()
            val authService = serviceFactory.getSharedAuthService(safeUi)

            // Create components
            val loginDialog = LoginDialog(authService)
            val notificationText = Text("")
            val spacerDiv = Div()
            val loginButton = Button("Login")
            val logoutButton = Button("Logout")

            // Wrap notificationText in a Div, so it is possible to control its visibility
            val notificationTextContainer = Div()
            notificationTextContainer.add(notificationText)

            // Setup styles
            spacerDiv.setWidth(1.0f, Unit.REM)
            loginButton.style.set("margin", "0.4rem")
            logoutButton.style.set("margin", "0.4rem")

            // Setup visibilities
            setupVisibilityControls(notificationTextContainer, authService) {
                notificationText.text = "Angemeldet als ${it?.name}"
                it != null
            }
            setupVisibilityControls(loginButton, authService) { it == null }
            setupVisibilityControls(logoutButton, authService) { it != null }

            // Setup actions
            loginButton.addClickListener {
                loginDialog.open()
            }
            logoutButton.addClickListener {
                authService.logout()
            }

            // Add components
            layout.add(
                loginDialog,
                notificationTextContainer,
                spacerDiv,
                loginButton,
                logoutButton
            )
        }
    }

    private fun getTabForComponent(component: Component): Tab? {
        return menu.children.toList()
            .firstOrNull {
                ComponentUtil.getData(it, Class::class.java) == component.javaClass
            }?.let { if (it is Tab) it else null }
    }

    private fun getCurrentPageTitle(): String {
        return content.javaClass.getAnnotation(PageTitle::class.java)?.value ?: "N/A"
    }

    private fun setupVisibilityControls(component: Component, authService: IAuthenticationService, visibleWhen: (user: EntitledUser?) -> Boolean) {
        val listener = object : IAuthenticationService.IAuthenticationListener {
            override fun onLoggedIn(user: EntitledUser) {
                ui.ifPresentOrElse(
                    {
                        it.access {
                            component.isVisible = visibleWhen(user)
                            logger().info("Making $component visible: ${component.isVisible}")
                        }
                    }, {
                        // If not present retry a bit later
                        localScope.launch {
                            delay(100)
                            onLoggedIn(user)
                        }
                    })
            }

            override fun onLoggedOut() {
                ui.ifPresentOrElse(
                    {
                        it.access {
                            component.isVisible = visibleWhen(null)
                        }
                    }, {
                        // If not present retry a bit later
                        localScope.launch {
                            delay(100)
                            onLoggedOut()
                        }
                    })
            }
        }
        authService.addListener(listener, true)

        addDetachListener {
            authService.removeListener(listener)
        }
    }
}