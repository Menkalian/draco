package de.menkalian.draco.view.content

import com.vaadin.flow.component.Text
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.formlayout.FormLayout
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.value.ValueChangeMode
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import de.menkalian.draco.data.quesstimate.Category
import de.menkalian.draco.data.quesstimate.Difficulty
import de.menkalian.draco.data.quesstimate.GuesstimateQuestion
import de.menkalian.draco.data.quesstimate.Language
import de.menkalian.draco.data.quesstimate.Suggestion
import de.menkalian.draco.data.quesstimate.SuggestionState
import de.menkalian.draco.data.user.EntitledUser
import de.menkalian.draco.services.IAuthenticationService
import de.menkalian.draco.services.ISharedServiceFactory
import de.menkalian.draco.view.MainView
import de.menkalian.draco.view.dialog.IEditDialogListener
import de.menkalian.draco.view.dialog.QuestionEditDialog
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@PageTitle("Frage vorschlagen")
@Route(value = "suggest", layout = MainView::class)
class MakeSuggestionContent(
    private val serviceFactory: ISharedServiceFactory
) : VerticalLayout(), IAuthenticationService.IAuthenticationListener, IEditDialogListener<GuesstimateQuestion> {
    private val innerLayout = FormLayout()
    private val nameField = TextField()
    private val createButton = Button()

    private val questionCreationDialog = QuestionEditDialog(getNewQuestion(), this)

    init {
        addAttachListener {
            val authService = serviceFactory.getSharedAuthService(it.ui)
            authService.addListener(this, true)
        }

        addDetachListener {
            if (serviceFactory.hasSharedAuthService(it.ui)) {
                val authService = serviceFactory.getSharedAuthService(it.ui)
                authService.removeListener(this)
            }
        }

        initBehaviour()
        initAppearance()
        initLayout()
    }

    private fun initBehaviour() {
        createButton.addClickListener {
            questionCreationDialog.open(getNewQuestion())
        }
        createButton.isEnabled = nameField.value.isNotBlank()

        nameField.valueChangeMode = ValueChangeMode.EAGER
        nameField.addValueChangeListener {
            createButton.isEnabled = it.value.isNotBlank()
        }
    }

    private fun initAppearance() {
        nameField.label = "Name"
        createButton.text = "Frage vorschlagen"
    }

    private fun initLayout() {
        val descText = """
            An dieser Stelle können neue Fragen vorgeschlagen werden, die dann möglicherweise in den Fragenkatalog übernommen werden.
            Eine Frage kann auch unvollständig eingesendet werden.
            Wir benötigen lediglich deinen Namen, damit du entsprechenden Credit erhältst.
            Fehlende Informationen werden dann vom Reviewer ergänzt.
        """.trimIndent()

        innerLayout.add(Div(Text(descText)), 2)
        innerLayout.add(Div(), 2)
        innerLayout.add(nameField, 1)
        innerLayout.add(createButton, 1)

        add(innerLayout)
        add(questionCreationDialog)

        add(questionCreationDialog)
    }

    private fun getNewQuestion(): GuesstimateQuestion {
        return GuesstimateQuestion(
            -1, nameField.value, System.currentTimeMillis() / 1000,
            Language.GERMAN, Difficulty.EASY, Category.ART,
            "", 0.0, "",
            listOf()
        )
    }

    override fun onLoggedIn(user: EntitledUser) {
        ui.get().access {
            nameField.value = user.name
        }
    }

    override fun onLoggedOut() {
        ui.get().access {
            nameField.value = ""
        }
    }

    override fun onSaved(obj: GuesstimateQuestion): Boolean {
        val safeUi = ui.get()
        val client = serviceFactory.getSharedAuthService(safeUi).clientWithAuthentication
        val latch = CountDownLatch(1)
        var close = true

        val suggestion = Suggestion("", obj.copy(createdAt = System.currentTimeMillis() / 1000), SuggestionState.CREATED, listOf())
        client.suggestion.create(
            suggestion,
            onCreated = {
                latch.countDown()
                ui.get().access {
                    Notification.show("Der Vorschlag wurde erfolgreich übermittelt.", 2000, Notification.Position.BOTTOM_CENTER)
                }
            },
            onError = {
                ui.get().access {
                    Notification.show("Der Vorschlag konnte aufgrund eines Fehlers nicht übermittelt werden: $it.", 5000, Notification.Position.MIDDLE)
                }
                close = false
                latch.countDown()
            }
        )

        latch.await(10, TimeUnit.SECONDS)
        return close
    }

    override fun onCancelled() {
        // Can be ignored
    }
}