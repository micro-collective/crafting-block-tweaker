package st.evening.mc.cbtweaker.event

import net.minecraftforge.fml.common.eventhandler.Event
import st.evening.mc.cbtweaker.template.TemplateRegistry

class CbtLoadTemplatesEvent(val registry: TemplateRegistry<*>) : Event()

class CbtLoadDataEvent : Event()
