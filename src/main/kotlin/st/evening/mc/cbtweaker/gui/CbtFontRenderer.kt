package st.evening.mc.cbtweaker.gui

import net.minecraft.client.Minecraft
import net.minecraft.client.resources.IReloadableResourceManager
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.render.EnhancedFontRenderer

@ClientSide.Physical
object CbtFontRenderer {
    lateinit var renderer: EnhancedFontRenderer
        private set

    var ICON_MOUSE_LEFT: Char = 'L'
        private set
    var ICON_MOUSE_RIGHT: Char = 'R'
        private set
    var ICON_MOUSE_MIDDLE: Char = 'M'
        private set
    var ICON_MODKEY_CTRL: Char = 'C'
        private set
    var ICON_MODKEY_ALT: Char = 'A'
        private set
    var ICON_MODKEY_SHIFT: Char = 'S'
        private set
    var ICON_ING_KEEP: Char = 'K'
        private set
    var ICON_ING_DAMAGE: Char = 'D'
        private set
    var ICON_ING_CHANCE: Char = 'C'
        private set

    internal fun init() {
        renderer = EnhancedFontRenderer.fromDefault()
        ICON_MOUSE_LEFT = renderer.addIcon(CbtGuiResources.MOUSE_BUTTON_LEFT)
        ICON_MOUSE_RIGHT = renderer.addIcon(CbtGuiResources.MOUSE_BUTTON_RIGHT)
        ICON_MOUSE_MIDDLE = renderer.addIcon(CbtGuiResources.MOUSE_BUTTON_MIDDLE)
        ICON_MODKEY_CTRL = renderer.addIcon(CbtGuiResources.MODIFIER_KEY_CTRL)
        ICON_MODKEY_ALT = renderer.addIcon(CbtGuiResources.MODIFIER_KEY_ALT)
        ICON_MODKEY_SHIFT = renderer.addIcon(CbtGuiResources.MODIFIER_KEY_SHIFT)
        ICON_ING_KEEP = renderer.addIcon(CbtGuiResources.ICON_ING_KEEP)
        ICON_ING_DAMAGE = renderer.addIcon(CbtGuiResources.ICON_ING_DAMAGE)
        ICON_ING_CHANCE = renderer.addIcon(CbtGuiResources.ICON_ING_CHANCE)
        (Minecraft.getMinecraft().resourceManager as IReloadableResourceManager).registerReloadListener(renderer)
    }
}
