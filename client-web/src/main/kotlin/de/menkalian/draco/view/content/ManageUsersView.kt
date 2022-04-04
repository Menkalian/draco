package de.menkalian.draco.view.content

import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import de.menkalian.draco.data.user.EntitledUser
import de.menkalian.draco.data.user.UserRight
import de.menkalian.draco.services.IAuthenticationService
import de.menkalian.draco.services.ISharedServiceFactory
import de.menkalian.draco.view.MainView
import de.menkalian.draco.view.dialog.IEditDialogListener
import de.menkalian.draco.view.dialog.UserEditDialog
import java.util.concurrent.CountDownLatch

@PageTitle("Benutzerverwaltung")
@Route(value = "/manage/users", layout = MainView::class)
class ManageUsersView(private val serviceFactory: ISharedServiceFactory) : VerticalLayout(), IEditDialogListener<EntitledUser> {
    var suggestionViewDialog: UserEditDialog? = null

    val grid = Grid<EntitledUser>()
    val addUserButton = Button(Icon(VaadinIcon.PLUS)) {
        val safeUi = ui.get()
        val authService = serviceFactory.getSharedAuthService(safeUi)
        authService
            .clientWithAuthentication
            .user
            .create(
                "NewUser",
                listOf(),
                {
                    safeUi.access {
                        suggestionViewDialog
                            ?.open(it!!, authService.currentUser!!)
                    }
                },
                {
                    safeUi.access {
                        Notification.show("Fehler beim Anlegen eines neuen Benutzers: $it")
                    }
                }
            )
    }

    init {
        val authListener = object : IAuthenticationService.IAuthenticationListener {
            override fun onLoggedIn(user: EntitledUser) {
                if (!user.hasRight(UserRight.USER_READ)) {
                    ui.get().navigate(WelcomeContent::class.java)
                } else {
                    refreshData()
                }

                addUserButton.isEnabled = user.hasRight(UserRight.USER_CREATE)
            }

            override fun onLoggedOut() {
                ui.get().navigate(WelcomeContent::class.java)
            }
        }

        addAttachListener {
            val authService = serviceFactory.getSharedAuthService(it.ui)
            authService.addListener(authListener, true)
            suggestionViewDialog = UserEditDialog(this)

            add(suggestionViewDialog)

            grid.addComponentColumn { suggestion ->
                val button = Button(Icon(VaadinIcon.PENCIL)) {
                    suggestionViewDialog?.open(suggestion, authService.currentUser!!)
                }
                button.isEnabled = authService.currentUser?.hasRight(UserRight.USER_UPDATE) == true
                button
            }?.apply { setHeader("Bearbeiten") }
            grid.addComponentColumn { user ->
                val button = Button(Icon(VaadinIcon.TRASH)) {
                    authService.clientWithAuthentication
                        .user
                        .delete(
                            user.id,
                            {
                                refreshData()
                                ui.get().access {
                                    Notification.show("Benutzer gelöscht.")
                                }
                            },
                            {
                                ui.get().access {
                                    Notification.show("Benutzer konnte nicht gelöscht werden: $it")
                                }
                            })
                }
                button.isEnabled = authService.currentUser?.hasRight(UserRight.USER_DELETE) == true
                button
            }?.apply { setHeader("Löschen") }
            grid.prependHeaderRow()
        }

        addDetachListener {
            if (serviceFactory.hasSharedAuthService(ui.get())) {
                val authService = serviceFactory.getSharedAuthService(ui.get())
                authService.removeListener(authListener)
            }
        }

        grid.addColumn { it.id }?.apply {
            setHeader("ID")
            isSortable = true
        }
        grid.addColumn { it.name }?.apply {
            setHeader("Name")
            isSortable = true
        }
        grid.addColumn { it.rights.joinToString() }?.apply { setHeader("Berechtigungen") }
        add(grid)
        add(addUserButton)
    }

    private fun refreshData() {
        serviceFactory.getSharedAuthService(ui.get()).clientWithAuthentication
            .user
            .getAll(
                { suggestions ->
                    ui.get().access {
                        grid.setItems(suggestions)
                    }
                },
                { error ->
                    ui.get().access {
                        Notification.show("Fehler beim Abrufen der Benutzer: $error")
                    }
                }
            )
    }

    override fun onSaved(obj: EntitledUser): Boolean {
        val safeUi = ui.get()
        val authService = serviceFactory.getSharedAuthService(safeUi)
        val latch = CountDownLatch(1)
        var mayClose = true

        authService.clientWithAuthentication
            .user
            .update(
                obj,
                {
                    latch.countDown()
                    safeUi.access {
                        Notification.show("Nutzer erfolgreich aktualisiert.")
                        refreshData()
                    }
                },
                {
                    mayClose = false
                    latch.countDown()
                    safeUi.access {
                        Notification.show("Fehler beim Aktualisieren des Nutzers.")
                    }
                }
            )
        latch.await()
        return mayClose
    }

    override fun onCancelled() {}
}