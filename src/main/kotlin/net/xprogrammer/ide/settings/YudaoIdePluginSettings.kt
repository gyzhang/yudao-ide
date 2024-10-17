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
    private var modulePomTextField: Cell<JBTextField>? = null
    private var apiModulePomTextField: Cell<JBTextField>? = null
    private var bizModulePomTextField: Cell<JBTextField>? = null

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

        row("Module POM Path:") {
            modulePomTextField = textField().apply {
                component.text = settings.getModulePom()
                align(Align.FILL)
            }
        }

        row("API Module POM Path:") {
            apiModulePomTextField = textField().apply {
                component.text = settings.getApiModulePom()
                align(Align.FILL)
            }
        }

        row("Biz Module POM Path:") {
            bizModulePomTextField = textField().apply {
                component.text = settings.getBizModulePom()
                align(Align.FILL)
            }
        }
    }

    override fun isModified(): Boolean {
        return settings.getRevision() != revisionTextField?.component?.text ||
                settings.getApiModulePackages() != apiModuleTextField?.component?.text ||
                settings.getBizModulePackages() != bizModuleTextField?.component?.text ||
                settings.getModulePom() != modulePomTextField?.component?.text ||
                settings.getBizModulePom() != bizModulePomTextField?.component?.text ||
                settings.getApiModulePom() != apiModulePomTextField?.component?.text
    }

    override fun apply() {
        settings.setRevision(revisionTextField?.component?.text ?: "2.2.0-snapshot")
        settings.setApiModulePackages(apiModuleTextField?.component?.text ?: "api,enums")
        settings.setBizModulePackages(bizModuleTextField?.component?.text ?: "controller,convert,job,mq,service")
        settings.setModulePom(modulePomTextField?.component?.text ?: "")
        settings.setApiModulePom(apiModulePomTextField?.component?.text ?: "")
        settings.setBizModulePom(bizModulePomTextField?.component?.text ?: "")
    }

    override fun reset() {
        revisionTextField?.component?.text = settings.getRevision()
        apiModuleTextField?.component?.text = settings.getApiModulePackages()
        bizModuleTextField?.component?.text = settings.getBizModulePackages()
        modulePomTextField?.component?.text = settings.getModulePom()
        apiModulePomTextField?.component?.text = settings.getApiModulePom()
        bizModulePomTextField?.component?.text = settings.getBizModulePom()
    }

    override fun disposeUIResources() {
        revisionTextField = null
        apiModuleTextField = null
        bizModuleTextField = null
        modulePomTextField = null
        apiModulePomTextField = null
        bizModulePomTextField = null
    }

    override fun getDisplayName(): String {
        return "Yudao IDE Settings"
    }
}
