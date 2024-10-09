package net.xprogrammer.ide.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(name = "RevisionSettings", storages = [Storage("YuDaoIdeSettings.xml")])
@Service
class RevisionSettings : PersistentStateComponent<RevisionSettings.State> {
    data class State(var revision: String = "")

    private var state = State()

    override fun getState(): State? = state
    override fun loadState(state: State) {
        this.state = state
    }

    fun getRevision(): String = state.revision
    fun setRevision(revision: String) {
        state.revision = revision
    }
}