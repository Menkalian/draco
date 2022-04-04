package de.menkalian.draco.view.dialog

import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.H3
import com.vaadin.flow.component.listbox.MultiSelectListBox
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.value.ValueChangeMode
import com.vaadin.flow.function.SerializablePredicate
import de.menkalian.draco.data.user.EntitledUser
import de.menkalian.draco.data.user.UserRight

class UserEditDialog(private val listener: IEditDialogListener<EntitledUser>) : Dialog() {
    private var editingUser: EntitledUser
    private var currentState: EntitledUser
    private val ensureSaveDialog = EnsureSaveDialog(
        { saveInput(false) },
        { close() },
        {}
    )

    private val layout = VerticalLayout()
    private val titleText = H3()
    private val nameField = TextField()
    private val rightSelection = MultiSelectListBox<UserRight>()

    private val saveButton = Button()
    private val closeButton = Button()

    init {
        isModal = true

        addDialogCloseActionListener {
            secureClose()
        }

        currentState = EntitledUser(-1, "", "", listOf())
        editingUser = EntitledUser(-1, "", "", listOf())

        initBehaviour()
        initAppearance()
        initLayout()
    }

    private fun initBehaviour() {
        rightSelection.setItems(UserRight.values().toList())
        rightSelection.itemEnabledProvider = SerializablePredicate { editingUser hasRight it }

        nameField.isRequired = true
        nameField.valueChangeMode = ValueChangeMode.EAGER
        nameField.addValueChangeListener {
            saveButton.isEnabled = nameField.value.isNotBlank()
        }

        saveButton.addClickListener {
            saveInput()
        }
        closeButton.addClickListener {
            secureClose()
        }
    }

    private fun initAppearance() {
        nameField.value = "Name"

        saveButton.text = "Speichern"
        closeButton.text = "Schließen"

        saveButton.addThemeVariants(ButtonVariant.MATERIAL_CONTAINED)
    }

    private fun initLayout() {
        layout.add(titleText)
        layout.add(nameField)
        layout.add(rightSelection)
        layout.add(
            HorizontalLayout(
                closeButton,
                saveButton
            )
        )
        add(layout)
    }

    fun open(user: EntitledUser, editor: EntitledUser) {
        if (this.isOpened.not()) {
            currentState = user
            editingUser = editor
            updateValues()
            open()
        }
    }

    private fun saveInput(mayClose: Boolean = true) {
        synchronized(currentState) {
            currentState = buildUser()
            if (listener.onSaved(currentState) && mayClose) {
                secureClose()
            } else {
                updateValues()
            }
        }
    }

    private fun secureClose() {
        synchronized(currentState) {
            if (buildUser() != currentState) {
                ensureSaveDialog.open()
            } else {
                close()
            }
        }
    }

    private fun buildUser(): EntitledUser {
        return currentState.copy(
            name = nameField.value,
            rights = rightSelection.value.toList()
        )
    }

    private fun updateValues() {
        titleText.text = "Fragen-ID: ${currentState.id}"
        titleText.isVisible = currentState.id != -1

        nameField.value = currentState.name
        rightSelection.value = currentState.rights.toSet()

        // update editable rights
        rightSelection.itemEnabledProvider = SerializablePredicate { editingUser hasRight it }
    }
}