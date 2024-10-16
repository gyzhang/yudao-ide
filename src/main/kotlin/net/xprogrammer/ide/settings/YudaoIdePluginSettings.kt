package net.xprogrammer.ide.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.panel
import javax.swing.JLabel
import javax.swing.ImageIcon

class YudaoIdePluginSettings : Configurable {
    private var revisionTextField: Cell<JBTextField>? = null
    private var apiModuleTextField: Cell<JBTextField>? = null
    private var bizModuleTextField: Cell<JBTextField>? = null

    private val settings: PluginSettings = service()

    override fun createComponent() = panel {
        row {
            val icon = ImageIcon(YudaoIdePluginSettings::class.java.classLoader.getResource("elephant.png"))
            cell(JLabel(icon)).align(Align.CENTER)
        }

        row("Revision:") {
            revisionTextField = textField().apply {
                component.text = settings.getRevision()
                align(Align.FILL)
            }
        }

        row("API Module Packages:") {
            apiModuleTextField = textField().apply {
                component.text = settings.getApiModulePackages()
                align(Align.FILL)
            }
        }

        row("Biz Module Packages:") {
            bizModuleTextField = textField().apply {
                component.text = settings.getBizModulePackages()
                align(Align.FILL)
            }
        }
    }

    override fun isModified(): Boolean {
        return settings.getRevision() != revisionTextField?.component?.text ||
                settings.getApiModulePackages() != apiModuleTextField?.component?.text ||
                settings.getBizModulePackages() != bizModuleTextField?.component?.text
    }

    override fun apply() {
        settings.setRevision(revisionTextField?.component?.text ?: "")
        settings.setApiModulePackages(apiModuleTextField?.component?.text ?: "")
        settings.setBizModulePackages(bizModuleTextField?.component?.text ?: "")
    }

    override fun reset() {
        revisionTextField?.component?.text = settings.getRevision()
        apiModuleTextField?.component?.text = settings.getApiModulePackages()
        bizModuleTextField?.component?.text = settings.getBizModulePackages()
    }

    override fun disposeUIResources() {
        revisionTextField = null
        apiModuleTextField = null
        bizModuleTextField = null
    }

    override fun getDisplayName(): String {
        return "Yudao IDE Settings"
    }
}
