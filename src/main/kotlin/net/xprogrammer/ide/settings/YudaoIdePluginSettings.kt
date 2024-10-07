package net.xprogrammer.ide.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.components.service
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.Cell

class YudaoIdePluginSettings : Configurable {
    private var myTextField: Cell<JBTextField>? = null
    private val settings: RevisionSettings = service()

    override fun createComponent() = panel {
        row("Revision:") {
            myTextField = textField().apply {
                component.text = settings.getRevision() // 设置默认值
            }
        }
    }

    override fun isModified(): Boolean {
        return settings.getRevision() != myTextField?.component?.text
    }

    override fun apply() {
        settings.setRevision(myTextField?.component?.text ?: "")
    }

    override fun reset() {
        myTextField?.component?.text = settings.getRevision()
    }

    override fun disposeUIResources() {
        myTextField = null
    }

    override fun getDisplayName(): String {
        return "Yudao IDE"
    }

}
