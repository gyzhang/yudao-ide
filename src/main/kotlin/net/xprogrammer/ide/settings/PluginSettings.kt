package net.xprogrammer.ide.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(name = "PluginSettings", storages = [Storage("YuDaoIdePluginSettings.xml")])
@Service
class PluginSettings : PersistentStateComponent<PluginSettings.State> {

    companion object {
        const val DEFAULT_REVISION = "2.2.0-snapshot"
        const val DEFAULT_API_MODULE_PACKAGES = "api,enums"
        const val DEFAULT_BIZ_MODULE_PACKAGES = "controller,dal,service"
        const val DEFAULT_MODULE_POM = ""
        const val DEFAULT_API_MODULE_POM = ""
        const val DEFAULT_BIZ_MODULE_POM = ""
    }

    data class State(
        var revision: String = DEFAULT_REVISION,
        var apiModulePackages: String = DEFAULT_API_MODULE_PACKAGES,
        var bizModulePackages: String = DEFAULT_BIZ_MODULE_PACKAGES,
        var modulePom: String = DEFAULT_MODULE_POM,
        var apiModulePom: String = DEFAULT_API_MODULE_POM,
        var bizModulePom: String = DEFAULT_BIZ_MODULE_POM
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    fun getRevision(): String = state.revision
    fun setRevision(value: String) {
        state.revision = value
    }

    fun getApiModulePackages(): String = state.apiModulePackages
    fun setApiModulePackages(value: String) {
        state.apiModulePackages = value
    }

    fun getBizModulePackages(): String = state.bizModulePackages
    fun setBizModulePackages(value: String) {
        state.bizModulePackages = value
    }

    fun getModulePom(): String = state.modulePom
    fun setModulePom(value: String) {
        state.modulePom = value
    }

    fun getApiModulePom(): String = state.apiModulePom
    fun setApiModulePom(value: String) {
        state.apiModulePom = value
    }

    fun getBizModulePom(): String = state.bizModulePom
    fun setBizModulePom(value: String) {
        state.bizModulePom = value
    }
}
