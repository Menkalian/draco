package de.menkalian.draco.view.content

import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.formlayout.FormLayout
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.EmailField
import com.vaadin.flow.component.textfield.TextArea
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.value.ValueChangeMode
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import de.menkalian.draco.data.telemetrie.LogReport
import de.menkalian.draco.data.telemetrie.TelemetrieReport
import de.menkalian.draco.data.user.EntitledUser
import de.menkalian.draco.services.IAuthenticationService
import de.menkalian.draco.services.ISharedServiceFactory
import de.menkalian.draco.view.MainView
import org.springframework.boot.info.BuildProperties
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@PageTitle("Fehler melden")
@Route(value = "/report", layout = MainView::class)
class ReportProblemContent(
    private val serviceFactory: ISharedServiceFactory,
    private val buildProperties: BuildProperties
) : VerticalLayout(), IAuthenticationService.IAuthenticationListener {
    private val innerLayout = FormLayout()
    private val nameField = TextField()
    private val emailField = EmailField()
    private val reportText = TextArea()
    private val submit = Button()

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

    private fun initBehaviour() {
        reportText.valueChangeMode = ValueChangeMode.EAGER

        submit.addClickListener {
            serviceFactory
                .getSharedAuthService(ui.get())
                .clientWithAuthentication
                .telemetrie
                .startUpload(
                    buildTelemetrieReport(),
                    onFinished = {
                        ui.get().access {
                            Notification.show(
                                "Der Fehlerbericht wurde erfolgreich übertragen.",
                                2000, Notification.Position.BOTTOM_CENTER
                            )
                            reportText.clear()
                        }
                    },
                    onError = {
                        ui.get().access {
                            Notification.show(
                                "Ein Fehler ist bei der Übertragung der Fehlermeldung aufgetreten: $it",
                                5000, Notification.Position.MIDDLE
                            )
                        }
                    }
                )
        }

        submit.isEnabled = reportText.value.isNotBlank()
        reportText.addValueChangeListener {
            submit.isEnabled = it.value.isNotBlank()
        }
    }

    private fun initAppearance() {
        isPadding = true

        nameField.isRequired = false
        nameField.label = "Name"

        emailField.isRequiredIndicatorVisible = false
        emailField.label = "Email-Adresse"

        reportText.isRequired = true
        reportText.isRequiredIndicatorVisible = true
        reportText.label = "Fehlerbeschreibung"

        submit.addThemeVariants(ButtonVariant.MATERIAL_CONTAINED)
        submit.text = "Bericht senden"
    }

    private fun initLayout() {
        innerLayout.add(nameField, 2)
        innerLayout.add(emailField, 2)
        innerLayout.add(reportText, 2)
        innerLayout.add(Div(), 1)
        innerLayout.add(submit, 1)
        add(innerLayout)
    }

    private fun buildTelemetrieReport() = TelemetrieReport(
        nameField.value ?: "",
        emailField.value ?: "",
        reportText.value!!,
        listOf(
            LogReport(
                "${buildProperties.group}:${buildProperties.name}:${buildProperties.version}", "Webclient",
                OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                ""
            )
        )
    )
}