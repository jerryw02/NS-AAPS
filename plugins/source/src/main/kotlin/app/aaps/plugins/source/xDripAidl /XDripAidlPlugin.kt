//package app.aaps.plugins.source.xDripAidl

//import app.aaps.core.interfaces.plugin.PluginBase
//import app.aaps.core.interfaces.plugin.PluginDescription
//import app.aaps.core.interfaces.plugin.PluginType

//class XDripPlugin : PluginBase(
//    PluginDescription()
//        .mainType(PluginType.BG_SOURCE)
//        .pluginName("xDrip AIDL")
//        .shortName("xDrip")
//        .description("Receive glucose data from xDrip via AIDL")
//        .visible(true)
//        .enableByDefault(false)
//) {
//    // 空类体，确保编译通过
//    override fun shouldBeEnabled(): Boolean = false
//}

package app.aaps.plugins.source.xDripAidl

/**
 * 最简单的插件类 - 无依赖，无继承
 * 只为了通过编译测试
 * 等编译通过后，我们再实现正确的AAPS插件接口
 */
class XDripPlugin {
    // 空类体 - 最简单的定义
    // 不继承PluginBase，不使用任何AAPS框架类
    
    companion object {
        const val PLUGIN_NAME = "xDrip AIDL Integration"
    }
    
    fun getDescription(): String {
        return "Receives glucose data from xDrip via AIDL"
    }
}
