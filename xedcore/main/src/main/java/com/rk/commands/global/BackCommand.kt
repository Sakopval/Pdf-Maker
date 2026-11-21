package com.rk.commands.global

import android.view.KeyEvent
import com.rk.commands.ActionContext
import com.rk.commands.GlobalCommand
import com.rk.commands.KeyCombination
import com.rk.icons.Back
import com.rk.icons.Icon
import com.rk.icons.Menu_book
import com.rk.icons.XedIcons
import com.rk.resources.getString
import com.rk.resources.strings
import com.rk.utils.openUrl

class BackCommand : GlobalCommand() {
    //NOTE: ids like this used to define the button and add it to a view
    override val id: String = "global.back"

    override fun getLabel(): String = "Back to main app"

    override fun getIcon(): Icon = Icon.VectorIcon(XedIcons.Back)

    override fun execute(context: ActionContext) {
        context.currentActivity.finish()
    }

    override val defaultKeybinds: KeyCombination = KeyCombination(keyCode = KeyEvent.KEYCODE_F1)
}
