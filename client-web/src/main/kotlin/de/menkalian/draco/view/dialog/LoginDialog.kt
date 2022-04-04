package de.menkalian.draco.view.dialog

import com.vaadin.flow.component.Key
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.formlayout.FormLayout
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.textfield.PasswordField
import com.vaadin.flow.data.value.ValueChangeMode
import de.menkalian.draco.services.IAuthenticationService

class LoginDialog(private val authenticationService: IAuthenticationService) : Dialog() {
    val layout = FormLayout()
    val textInput = PasswordField("Authentication")
    val cancelButton = Button("Abbrechen")
    val loginButton = Button("Login")

    init {
        isModal = true

        addDialogCloseActionListener {
            close()
        }

        initLayout()
    }

    private fun initLayout() {
        loginButton.addThemeVariants(ButtonVariant.MATERIAL_CONTAINED)
        layout.responsiveSteps = listOf(
            FormLayout.ResponsiveStep("0", 3)
        )

        textInput.helperText =
            "Den benötigten Authentifizierungscode erhältst du von einem Administrator. Falls du keinen Code bekommen hast, brauchst du vermutlich auch keinen ;-)."

        loginButton.addClickListener {
            authenticationService.login(textInput.value)
            close()
        }

        cancelButton.addClickListener {
            close()
        }

        loginButton.isEnabled = textInput.value.isNotBlank()
        textInput.valueChangeMode = ValueChangeMode.EAGER
        textInput.addValueChangeListener {
            loginButton.isEnabled = it.value.isNotBlank()
        }

        textInput.addKeyDownListener {
            if (it.key == Key.ENTER || it.key == Key.NUMPAD_ENTER) {
                if (loginButton.isEnabled)
                    loginButton.clickInClient()
            }
        }

        layout.add(textInput, 3)
        layout.add(Div(), 1)
        layout.add(cancelButton, 1)
        layout.add(loginButton, 1)
        add(layout)
    }

    override fun open() {
        super.open()
        textInput.clear()
    }
}