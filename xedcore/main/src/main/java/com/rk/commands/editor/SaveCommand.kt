package com.rk.commands.editor

import android.view.KeyEvent
import com.example.core.FileTools
import com.example.core.db.AppDataBase
import com.rk.DefaultScope
import com.rk.commands.EditorActionContext
import com.rk.commands.EditorCommand
import com.rk.commands.EditorNonActionContext
import com.rk.commands.KeyCombination
import com.rk.icons.Icon
import com.rk.resources.drawables
import com.rk.resources.getString
import com.rk.resources.strings
import com.rk.settings.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

class SaveCommand : EditorCommand() {

    override val id: String = "editor.save"

    override fun getLabel(): String = strings.save.getString()

    override fun execute(context: EditorActionContext) {
        DefaultScope.launch(Dispatchers.IO) { context.editorTab.save() }
    }

    override fun isEnabled(context: EditorNonActionContext): Boolean {
        return !context.editorTab.isReadOnly && (context.editorTab.editorState.isDirty || Settings.auto_save)
    }

    override fun getIcon(): Icon = Icon.ResourceIcon(drawables.save)

    override val defaultKeybinds: KeyCombination = KeyCombination(keyCode = KeyEvent.KEYCODE_S, ctrl = true)
}
