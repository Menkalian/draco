package de.menkalian.draco.view.content

import com.vaadin.flow.component.Text
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import de.menkalian.draco.data.quesstimate.Suggestion
import de.menkalian.draco.data.quesstimate.SuggestionState
import de.menkalian.draco.data.user.EntitledUser
import de.menkalian.draco.data.user.UserRight
import de.menkalian.draco.services.IAuthenticationService
import de.menkalian.draco.services.ISharedServiceFactory
import de.menkalian.draco.view.MainView
import de.menkalian.draco.view.dialog.SuggestionViewDialog

@PageTitle("Vorschlagsverwaltung")
@Route(value = "/manage/suggestions", layout = MainView::class)
class ManageSuggestionsView(private val serviceFactory: ISharedServiceFactory) : VerticalLayout() {
    var suggestionViewDialog: SuggestionViewDialog? = null
    val grid: Grid<Suggestion>
    val noResultsWarning = Div(Text("Es wurden keine Vorschläge gefunden."))
    val filterClosedSuggestions: Checkbox

    init {
        grid = Grid()
        filterClosedSuggestions = Checkbox(true)

        filterClosedSuggestions.label = "Geschlossene Vorschläge ausblenden"
        filterClosedSuggestions.addValueChangeListener {
            refreshData()
        }
        add(filterClosedSuggestions)

        val authListener = object : IAuthenticationService.IAuthenticationListener {
            override fun onLoggedIn(user: EntitledUser) {
                if (!user.hasRight(UserRight.SUGGESTION_READ)) {
                    ui.get().navigate(WelcomeContent::class.java)
                } else {
                    refreshData()
                }
            }

            override fun onLoggedOut() {
                ui.get().navigate(WelcomeContent::class.java)
            }
        }

        addAttachListener {
            val authService = serviceFactory.getSharedAuthService(it.ui)
            authService.addListener(authListener, true)
            suggestionViewDialog = SuggestionViewDialog(authService)
            suggestionViewDialog?.addOpenedChangeListener {
                refreshData()
            }

            add(suggestionViewDialog)

            grid.addComponentColumn { suggestion ->
                val button = Button(Icon(VaadinIcon.PENCIL)) {
                    suggestionViewDialog?.open(suggestion)
                }
                button.isEnabled = authService.currentUser?.hasRight(UserRight.SUGGESTION_UPDATE) == true
                button
            }?.apply { setHeader("Bearbeiten") }
            grid.addComponentColumn { suggestion ->
                val button = Button(Icon(VaadinIcon.TRASH)) {
                    authService.clientWithAuthentication
                        .suggestion
                        .delete(
                            suggestion.uuid,
                            {
                                refreshData()
                                ui.get().access {
                                    Notification.show("Vorschlag gelöscht.")
                                }
                            },
                            {
                                ui.get().access {
                                    Notification.show("Vorschlag konnte nicht gelöscht werden: $it")
                                }
                            })
                }
                button.isEnabled = authService.currentUser?.hasRight(UserRight.SUGGESTION_DELETE) == true
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

        grid.addColumn { it.uuid }?.apply { setHeader("UUID") }
        grid.addColumn { it.state }?.apply {
            setHeader("Zustand")
            isSortable = true
        }
        grid.addColumn { it.suggestedQuestion.author }?.apply { setHeader("Autor") }
        grid.addColumn { it.suggestedQuestion.question }?.apply { setHeader("Frage") }
        add(grid)
        add(noResultsWarning)
    }

    private fun refreshData() {
        serviceFactory.getSharedAuthService(ui.get()).clientWithAuthentication
            .suggestion
            .getAll(
                { suggestions ->
                    val filtered = suggestions
                        .filter {
                            filterClosedSuggestions.value == false ||
                                    (it.state != SuggestionState.ACCEPTED && it.state != SuggestionState.CLOSED)
                        }

                    ui.get().access {
                        if (filtered.isEmpty()) {
                            grid.isVisible = false
                            noResultsWarning.isVisible = true
                        } else {
                            grid.isVisible = true
                            noResultsWarning.isVisible = false
                            grid.setItems(filtered)
                        }
                    }
                },
                { error ->
                    ui.get().access {
                        Notification.show("Fehler beim Abrufen der Vorschläge: $error")
                    }
                }
            )
    }
}