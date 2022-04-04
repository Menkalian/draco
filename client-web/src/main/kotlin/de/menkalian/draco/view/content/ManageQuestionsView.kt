package de.menkalian.draco.view.content

import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import de.menkalian.draco.data.quesstimate.Category
import de.menkalian.draco.data.quesstimate.Difficulty
import de.menkalian.draco.data.quesstimate.GuesstimateQuestion
import de.menkalian.draco.data.quesstimate.Language
import de.menkalian.draco.data.user.EntitledUser
import de.menkalian.draco.data.user.UserRight
import de.menkalian.draco.services.IAuthenticationService
import de.menkalian.draco.services.ISharedServiceFactory
import de.menkalian.draco.view.MainView
import de.menkalian.draco.view.dialog.IEditDialogListener
import de.menkalian.draco.view.dialog.QuestionEditDialog
import java.util.concurrent.CountDownLatch

@PageTitle("Fragenverwaltung")
@Route(value = "/manage/questions", layout = MainView::class)
class ManageQuestionsView(private val serviceFactory: ISharedServiceFactory) : VerticalLayout(), IEditDialogListener<GuesstimateQuestion> {
    var questionEditDialog: QuestionEditDialog? = null
    val grid: Grid<GuesstimateQuestion>

    init {
        grid = Grid()

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
            questionEditDialog = QuestionEditDialog(
                GuesstimateQuestion(-1, "", 0, Language.ENGLISH, Difficulty.HARD, Category.ART, "", 0.0, "", listOf()),
                this
            )

            add(questionEditDialog)

            grid.addComponentColumn { question ->
                val button = Button(Icon(VaadinIcon.PENCIL)) {
                    questionEditDialog?.open(question)
                }
                button.isEnabled = authService.currentUser?.hasRight(UserRight.QUESTION_UPDATE) == true
                button
            }?.apply { setHeader("Bearbeiten") }
            grid.addComponentColumn { question ->
                val button = Button(Icon(VaadinIcon.TRASH)) {
                    authService.clientWithAuthentication
                        .question
                        .delete(
                            question.id,
                            {
                                refreshData()
                                ui.get().access {
                                    Notification.show("Frage gelöscht.")
                                }
                            },
                            {
                                ui.get().access {
                                    Notification.show("Frage konnte nicht gelöscht werden: $it")
                                }
                            })
                }
                button.isEnabled = authService.currentUser?.hasRight(UserRight.QUESTION_DELETE) == true
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
        grid.addColumn { it.language }?.apply {
            setHeader("Sprache")
            isSortable = true
        }
        grid.addColumn { it.category }?.apply {
            setHeader("Kategorie")
            isSortable = true
        }
        grid.addColumn { it.difficulty }?.apply {
            setHeader("Schwierigkeit")
            isSortable = true
        }
        grid.addColumn { it.author }?.apply { setHeader("Autor") }
        grid.addColumn { it.question }?.apply { setHeader("Frage") }
        grid.addColumn { it.answer.toString() + " " + it.answerUnit }?.apply { setHeader("Antwort") }
        grid.addColumn { it.hints.count().toString() }?.apply {
            setHeader("Anz. Hinweise")
            isSortable = true
        }
        add(grid)
    }

    private fun refreshData() {
        serviceFactory.getSharedAuthService(ui.get()).clientWithAuthentication
            .question
            .getAll(
                { questions ->
                    ui.get().access {
                        grid.setItems(questions)
                    }
                },
                { error ->
                    ui.get().access {
                        Notification.show("Fehler beim Abrufen der Fragen: $error")
                    }
                }
            )
    }

    override fun onSaved(obj: GuesstimateQuestion): Boolean {
        val safeUi = ui.get()
        val authService = serviceFactory.getSharedAuthService(safeUi)
        val latch = CountDownLatch(1)
        var mayClose = true

        authService.clientWithAuthentication
            .question
            .update(
                obj,
                {
                    latch.countDown()
                    safeUi.access {
                        Notification.show("Frage erfolgreich aktualisiert.")
                        refreshData()
                    }
                },
                {
                    mayClose = false
                    latch.countDown()
                    safeUi.access {
                        Notification.show("Fehler beim Aktualisieren der Frage: $it.")
                    }
                }
            )
        latch.await()
        return mayClose
    }

    override fun onCancelled() {
    }
}