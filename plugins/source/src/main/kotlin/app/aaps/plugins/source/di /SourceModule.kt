package app.aaps.plugins.source.di

import app.aaps.core.interfaces.source.DexcomBoyda
import app.aaps.core.interfaces.source.NSClientSource
import app.aaps.core.interfaces.source.XDripSource
import app.aaps.plugins.source.DexcomPlugin
import app.aaps.plugins.source.NSClientSourcePlugin
import app.aaps.plugins.source.XdripSourcePlugin
import plugins.source.src.main.kotlin.app.aaps.plugins.source.xDripAidl.XDripPlugin

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// 主模块：只包含 @Binds，安装到 SingletonComponent
@Module
@InstallIn(SingletonComponent::class)
interface SourceModuleBindings {

    @Binds
    fun bindNSClientSource(nsClientSourcePlugin: NSClientSourcePlugin): NSClientSource

    @Binds
    fun bindDexcomBoyda(dexcomPlugin: DexcomPlugin): DexcomBoyda

    @Binds
    fun bindXDrip(xdripSourcePlugin: XdripSourcePlugin): XDripSource

    // 2. 添加新的绑定方法，将你的 XDripPlugin 绑定到 XDripSource 接口
    @Binds
    fun bindXDripAidl(xDripAidlPlugin: XDripPlugin): XDripSource // 注意：方法名需要唯一，这里用了 bindXDripAidl

}
