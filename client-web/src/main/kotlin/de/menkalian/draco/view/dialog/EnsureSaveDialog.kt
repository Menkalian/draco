package de.menkalian.draco.view.dialog

import com.vaadin.flow.component.Text
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.formlayout.FormLayout
import com.vaadin.flow.component.html.Div

class EnsureSaveDialog(
    private val save: () -> Unit,
    private val closeParent: () -> Unit,
    private val cancel: () -> Unit,
) : Dialog() {
    val layout = FormLayout()
    val text = Text("")
    val cancelButton = Button()
    val discardButton = Button()
    val saveButton = Button()

    init {
        initBehaviour()
        initApperance()
        composeLayout()
    }

    private fun initBehaviour() {
        isModal = true
        addDialogCloseActionListener {
            cancel()
            close()
        }

        cancelButton.addClickListener {
            cancel()
            close()
        }

        discardButton.addClickListener {
            close()
            closeParent()
        }

        saveButton.addClickListener {
            save()
            close()
            closeParent()
        }
    }

    private fun initApperance() {
        setMaxWidth(70.0f, com.vaadin.flow.component.Unit.REM)

        layout.setResponsiveSteps(
            FormLayout.ResponsiveStep("0", 4)
        )

        text.text = "Es sind ungespeicherte Änderungen enthalten. Sollen die Änderungen gespeichert oder verworfen werden?"
        saveButton.text = "Speichern"
        discardButton.text = "Verwerfen"
        cancelButton.text = "Abbrechen"

        saveButton.addThemeVariants(ButtonVariant.MATERIAL_CONTAINED)
        discardButton.addThemeVariants(ButtonVariant.MATERIAL_OUTLINED)
    }

    private fun composeLayout() {
        layout.add(Div(text), 4)
        layout.add(cancelButton, 1)
        layout.add(Div(), 1)
        layout.add(discardButton, 1)
        layout.add(saveButton, 1)
        add(layout)
    }
}