package de.menkalian.draco.view.dialog

import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.formlayout.FormLayout
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Hr
import com.vaadin.flow.component.listbox.ListBox
import com.vaadin.flow.component.select.Select
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.value.ValueChangeMode
import de.menkalian.draco.data.quesstimate.Category
import de.menkalian.draco.data.quesstimate.Difficulty
import de.menkalian.draco.data.quesstimate.GuesstimateQuestion
import de.menkalian.draco.data.quesstimate.Language
import kotlin.properties.Delegates

class QuestionEditDialog(initialQuestion: GuesstimateQuestion, private val listener: IEditDialogListener<GuesstimateQuestion>) : Dialog() {
    private var savedState: GuesstimateQuestion
    private val ensureSaveDialog = EnsureSaveDialog(
        { saveInput(false) },
        { close() },
        {}
    )

    private var hints: List<String> by Delegates.observable(listOf()) { _, _, _ ->
        hintList.setItems(hints)
        hintList.value = hints.firstOrNull()
    }

    private val layout = FormLayout()
    private val titleText = H1()

    private val languageSelection = Select(*Language.values())
    private val difficultySelection = Select(*Difficulty.values())
    private val categorySelection = Select(*Category.values())
    private val questionText = TextField()
    private val answerText = TextField()
    private val answerUnitText = TextField()
    private val hintList = ListBox<String>()
    private val hintInput = TextField()
    private val hintAddButton = Button()
    private val hintRemoveButton = Button()

    private val saveButton = Button()
    private val closeButton = Button()

    init {
        isModal = true

        addDialogCloseActionListener {
            secureClose()
        }

        savedState = initialQuestion

        initBehaviour()
        initAppearance()
        initLayout()
    }

    fun open(question: GuesstimateQuestion) {
        if (this.isOpened.not()) {
            savedState = question
            updateValues()
            open()
        }
    }

    private fun updateValues() {
        titleText.text = "Fragen-ID: ${savedState.id}"
        titleText.isVisible = savedState.id != -1

        languageSelection.value = savedState.language
        difficultySelection.value = savedState.difficulty
        categorySelection.value = savedState.category

        questionText.value = savedState.question
        answerText.value = savedState.answer.toString()
        answerUnitText.value = savedState.answerUnit

        hints = savedState.hints
    }

    private fun initBehaviour() {
        layout.setResponsiveSteps(FormLayout.ResponsiveStep("0", 3))

        answerText.valueChangeMode = ValueChangeMode.EAGER
        answerText.pattern = "\\d+(\\.\\d+)?"

        languageSelection.isEmptySelectionAllowed = false
        difficultySelection.isEmptySelectionAllowed = false
        categorySelection.isEmptySelectionAllowed = false

        categorySelection.setTextRenderer {
            it?.toDisplayString(Language.GERMAN) ?: "-- Kein Wert --"
        }

        questionText.isRequired = true
        questionText.valueChangeMode = ValueChangeMode.EAGER

        questionText.addValueChangeListener {
            saveButton.isEnabled = isInputValid()
        }
        answerText.addValueChangeListener {
            saveButton.isEnabled = isInputValid()
        }

        hintInput.valueChangeMode = ValueChangeMode.EAGER
        hintInput.addValueChangeListener {
            hintAddButton.isEnabled = it.value.isNotBlank()
        }
        hintInput.value = ""

        hintList.addValueChangeListener {
            hintRemoveButton.isEnabled = it.value != null
        }

        hintAddButton.addClickListener {
            val hint = hintInput.value!!
            hintInput.clear()
            hints = hints + hint
        }

        hintRemoveButton.addClickListener {
            hints = hints - hintList.value
        }

        saveButton.addClickListener {
            saveInput()
        }
        closeButton.addClickListener {
            secureClose()
        }
    }

    private fun initAppearance() {
        titleText.text = "Fragen-ID: ${savedState.id}"
        titleText.isVisible = savedState.id != -1

        languageSelection.label = "Sprache"
        difficultySelection.label = "Schwierigkeit"
        categorySelection.label = "Kategorie"
        questionText.label = "Frage"
        answerText.label = "Antwort"
        answerUnitText.label = "Einheit"

        hintInput.label = "Hinweis"
        hintAddButton.text = "Hinzufügen"
        hintRemoveButton.text = "Entfernen"

        hintRemoveButton.addThemeVariants(ButtonVariant.MATERIAL_OUTLINED)
        hintAddButton.addThemeVariants(ButtonVariant.MATERIAL_OUTLINED)

        saveButton.text = "Speichern"
        closeButton.text = "Schließen"

        saveButton.addThemeVariants(ButtonVariant.MATERIAL_CONTAINED)
    }

    private fun initLayout() {
        layout.add(H2("Einstellungen"), 3)

        layout.add(languageSelection, 1)
        layout.add(difficultySelection, 1)
        layout.add(categorySelection, 1)

        layout.add(questionText, 3)

        layout.add(answerText, 2)
        layout.add(answerUnitText, 1)

        layout.add(H2("Hinweise"), 3)
        layout.add(hintList, 3)
        layout.add(hintInput, 3)

        layout.add(Div(), 1)
        layout.add(hintRemoveButton, 1)
        layout.add(hintAddButton, 1)

        layout.add(Hr(), 3)

        layout.add(Div(), 1)
        layout.add(closeButton, 1)
        layout.add(saveButton, 1)

        add(layout)
    }

    private fun buildQuestion(): GuesstimateQuestion {
        return savedState.copy(
            language = languageSelection.value,
            difficulty = difficultySelection.value,
            category = categorySelection.value,
            question = questionText.value,
            answer = answerText.value.toDoubleOrNull() ?: 0.0,
            answerUnit = answerUnitText.value,
            hints = hints
        )
    }

    private fun isInputValid(): Boolean {
        return questionText.isInvalid.not() && answerText.isInvalid.not()
    }

    private fun saveInput(mayClose: Boolean = true) {
        synchronized(savedState) {
            savedState = buildQuestion()
            if (listener.onSaved(savedState) && mayClose) {
                secureClose()
            } else {
                updateValues()
            }
        }
    }

    private fun secureClose() {
        synchronized(savedState) {
            if (savedState != buildQuestion()) {
                ensureSaveDialog.open()
            } else {
                close()
            }
        }
    }
}