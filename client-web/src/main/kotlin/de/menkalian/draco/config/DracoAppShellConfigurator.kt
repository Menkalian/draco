package de.menkalian.draco.config

import com.vaadin.flow.component.page.AppShellConfigurator
import com.vaadin.flow.component.page.Push
import com.vaadin.flow.shared.communication.PushMode
import com.vaadin.flow.theme.Theme
import com.vaadin.flow.theme.material.Material
import org.springframework.stereotype.Component

@Push(PushMode.AUTOMATIC)
@Theme(themeClass = Material::class)
@Component
class DracoAppShellConfigurator : AppShellConfigurator
