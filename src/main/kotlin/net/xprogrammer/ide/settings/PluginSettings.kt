package net.xprogrammer.ide.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(name = "PluginSettings", storages = [Storage("YuDaoIdePluginSettings.xml")])
@Service
class PluginSettings : PersistentStateComponent<PluginSettings.State> {

    data class State(
        var revision: String = "2.2.0-snapshot",
        var apiModulePackages: String = "api,enums",
        var bizModulePackages: String = "controller,convert,job,mq,service",
        var apiModulePom: String = "",  // 新增字段
        var bizModulePom: String = ""   // 新增字段
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    fun getRevision(): String = state.revision
    fun setRevision(revision: String) {
        state.revision = revision
    }

    fun getApiModulePackages(): String = state.apiModulePackages
    fun setApiModulePackages(value: String) {
        state.apiModulePackages = value
    }

    fun getBizModulePackages(): String = state.bizModulePackages
    fun setBizModulePackages(value: String) {
        state.bizModulePackages = value
    }

    fun getApiModulePom(): String = state.apiModulePom  // 新增getter
    fun setApiModulePom(value: String) {
        state.apiModulePom = value
    }

    fun getBizModulePom(): String = state.bizModulePom  // 新增getter
    fun setBizModulePom(value: String) {
        state.bizModulePom = value
    }
}
