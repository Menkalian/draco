package de.menkalian.draco.view.content

import com.vaadin.flow.component.html.Hr
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import de.menkalian.draco.view.MainView

@PageTitle("Willkommen")
@Route(value = "", layout = MainView::class)
class WelcomeContent : VerticalLayout() {
    init {
        add("Hier ist alles noch etwas in Bearbeitung, aber Vorschläge sind bereits jetzt gerne gesehen ;-).")
        add(Hr())
        add("Falls euch Probleme auffallen gebt mir gerne Bescheid.")
    }
}