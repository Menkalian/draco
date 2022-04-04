package de.menkalian.draco.view.dialog

import com.vaadin.flow.component.Unit
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.formlayout.FormLayout
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.H3
import com.vaadin.flow.component.html.Hr
import com.vaadin.flow.component.html.Pre
import com.vaadin.flow.component.messages.MessageInput
import com.vaadin.flow.component.messages.MessageList
import com.vaadin.flow.component.messages.MessageListItem
import com.vaadin.flow.component.notification.Notification
import de.menkalian.draco.data.quesstimate.Category
import de.menkalian.draco.data.quesstimate.Difficulty
import de.menkalian.draco.data.quesstimate.GuesstimateQuestion
import de.menkalian.draco.data.quesstimate.Language
import de.menkalian.draco.data.quesstimate.Suggestion
import de.menkalian.draco.data.quesstimate.SuggestionState
import de.menkalian.draco.data.user.EntitledUser
import de.menkalian.draco.data.user.UserRight
import de.menkalian.draco.services.IAuthenticationService
import java.time.Instant
import java.util.concurrent.CountDownLatch

class SuggestionViewDialog(private val authService: IAuthenticationService) : Dialog(), IEditDialogListener<GuesstimateQuestion> {
    private val layout = FormLayout()
    private val title = H3("")

    private val stateDisplay = Pre("")
    private val editQuestionButton = Button()

    private val commentDisplay = MessageList()
    private val commentInput = MessageInput()

    private val acceptButton = Button()
    private val declineButton = Button()
    private val closeButton = Button()

    private var savedState: Suggestion
    private val questionEditDialog: QuestionEditDialog

    init {
        isModal = true
        setMaxWidth(80.0f, Unit.PERCENTAGE)

        savedState = Suggestion(
            "",
            GuesstimateQuestion(-1, "", 0, Language.ENGLISH, Difficulty.EASY, Category.ART, "", 0.0, "", listOf()),
            SuggestionState.CREATED,
            listOf()
        )
        questionEditDialog = QuestionEditDialog(
            savedState.suggestedQuestion,
            this
        )

        addDialogCloseActionListener {
            close()
        }

        initBehaviour()
        initAppearance()
        initLayout()
    }

    fun open(suggestion: Suggestion) {
        if (this.isOpened.not()) {
            savedState = suggestion
            updateValues()
            open()
        }
    }

    private fun updateValues() {
        title.text = "Vorschlag #${savedState.uuid}"
        stateDisplay.text = """
            Zustand: ${savedState.state}
            
            Autor: ${savedState.suggestedQuestion.author}
            Frage: ${savedState.suggestedQuestion.question}
            Sprache: ${savedState.suggestedQuestion.language}
            Schwierigkeit: ${savedState.suggestedQuestion.difficulty}
            Kategorie: ${savedState.suggestedQuestion.category}
            Antwort: ${savedState.suggestedQuestion.answer} ${savedState.suggestedQuestion.answerUnit}
            Hinweise:
            
        """.trimIndent() +
                savedState.suggestedQuestion.hints.joinToString("\n") { "  - $it" }

        commentDisplay.setItems(
            savedState.notes.map { it.toMessageItem() }
        )

        ui.ifPresent { ui ->
            val user = authService.currentUser

            val hasUpdatePerms = user != null && user hasRight UserRight.SUGGESTION_UPDATE
            val isClosed = savedState.state == SuggestionState.ACCEPTED || savedState.state == SuggestionState.CLOSED

            acceptButton.isEnabled = hasUpdatePerms && !isClosed && user!! hasRight UserRight.QUESTION_CREATE
            declineButton.isEnabled = hasUpdatePerms && !isClosed
            closeButton.isEnabled = hasUpdatePerms && !isClosed
        }
    }

    private fun initBehaviour() {
        layout.setResponsiveSteps(FormLayout.ResponsiveStep("0", 3))

        commentInput.addSubmitListener {
            if (authService.currentUser?.hasRight(UserRight.SUGGESTION_COMMENT_CREATE) == true) {
                val author = authService.currentUser?.name ?: "Anonym"
                authService.clientWithAuthentication.suggestion
                    .comment(savedState.uuid, Suggestion.SuggestionComment(author, it.value, System.currentTimeMillis() / 1000),
                             { suggestion ->
                                 if (suggestion != null) {
                                     ui.get().access {
                                         savedState = suggestion
                                         updateValues()
                                         Notification.show("Der Kommentar wurde erfolgreich hinzugefügt", 2000, Notification.Position.BOTTOM_CENTER)
                                     }
                                 }
                             },
                             {
                                 ui.get().access {
                                     Notification.show("Ein Fehler ist beim Hinzufügen des Kommentars aufgetreten: $it.")
                                 }
                             })

            } else {
                ui.get().access {
                    Notification.show("Du hast keine Berechtigung zum Hinzufügen von Kommentaren.")
                }
            }
        }

        editQuestionButton.addClickListener {
            questionEditDialog.open(savedState.suggestedQuestion)
        }
        acceptButton.addClickListener {
            authService.clientWithAuthentication
                .suggestion.accept(
                    savedState.uuid,
                    {
                        if (it != null) {
                            ui.get().access {
                                savedState = it
                                updateValues()
                                Notification.show("Der Vorschlag wurde akzeptiert und in die Datenbank übernommen.", 2000, Notification.Position.BOTTOM_CENTER)
                            }
                        }
                    },
                    {
                        ui.get().access {
                            Notification.show("Ein Fehler ist beim Akzeptieren des Vorschlags aufgetreten: $it.")
                        }
                    }
                )
        }
        declineButton.addClickListener {
            authService.clientWithAuthentication
                .suggestion.decline(
                    savedState.uuid,
                    {
                        if (it != null) {
                            ui.get().access {
                                savedState = it
                                updateValues()
                                Notification.show("Der Vorschlag wurde als \"NEEDS WORK\" markiert.", 2000, Notification.Position.BOTTOM_CENTER)
                            }
                        }
                    },
                    {
                        ui.get().access {
                            Notification.show("Ein Fehler ist beim Ablehnen des Vorschlags aufgetreten: $it.")
                        }
                    }
                )
        }
        closeButton.addClickListener {
            authService.clientWithAuthentication
                .suggestion.close(
                    savedState.uuid,
                    {
                        if (it != null) {
                            ui.get().access {
                                savedState = it
                                updateValues()
                                Notification.show(
                                    "Der Vorschlag wurde geschlossen ohne in die Datenbank übernommen zu werden.",
                                    2000,
                                    Notification.Position.BOTTOM_CENTER
                                )
                            }
                        }
                    },
                    {
                        ui.get().access {
                            Notification.show("Ein Fehler ist beim Schließen des Vorschlags aufgetreten: $it.")
                        }
                    }
                )
        }

        val authListener = object : IAuthenticationService.IAuthenticationListener {
            override fun onLoggedIn(user: EntitledUser) {
                ui.ifPresent {
                    if (!(user hasRight UserRight.SUGGESTION_UPDATE)) {
                        it.access {
                            close()
                        }
                    }
                    it.access {
                        commentInput.isEnabled = user hasRight UserRight.SUGGESTION_COMMENT_CREATE
                        editQuestionButton.isEnabled = user hasRight UserRight.SUGGESTION_UPDATE
                    }
                }
            }

            override fun onLoggedOut() {
                ui.ifPresent {
                    close()
                }
            }
        }

        addDetachListener {
            authService.removeListener(authListener)
        }
        addAttachListener {
            authService.addListener(authListener, true)
        }
    }

    private fun initAppearance() {
        editQuestionButton.text = "Frage bearbeiten"
        acceptButton.text = "Annehmen"
        declineButton.text = "Ablehnen"
        closeButton.text = "Frage schließen"
    }

    private fun initLayout() {
        layout.add(title, 3)

        layout.add(Div(stateDisplay), 3)

        layout.add(Div(), 2)
        layout.add(editQuestionButton, 1)

        layout.add(commentDisplay, 3)
        layout.add(commentInput, 3)

        layout.add(Div(Hr()), 3)

        layout.add(closeButton, 1)
        layout.add(declineButton, 1)
        layout.add(acceptButton, 1)
        add(layout)
        add(questionEditDialog)
    }

    override fun onSaved(obj: GuesstimateQuestion): Boolean {
        val updatedSuggestion = savedState.copy(suggestedQuestion = obj)
        var close = true
        val latch = CountDownLatch(1)
        authService
            .clientWithAuthentication
            .suggestion
            .update(
                updatedSuggestion,
                {
                    if (it != null) {
                        savedState = it
                        ui.get().access {
                            Notification.show(
                                "Frage erfolgreich aktualisiert", 3000, Notification.Position.BOTTOM_CENTER
                            )
                            updateValues()
                        }
                    }
                    latch.countDown()
                },
                {
                    close = false
                    ui.get().access {
                        Notification.show(
                            "Fehler beim aktualisieren der Frage: $it", 5000, Notification.Position.MIDDLE
                        )
                    }
                    latch.countDown()
                }
            )
        latch.await()
        return close
    }

    override fun onCancelled() {
    }

    private fun Suggestion.SuggestionComment.toMessageItem(): MessageListItem {
        return MessageListItem(comment, Instant.ofEpochSecond(timestamp), author)
    }
}
