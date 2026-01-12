
package app.aaps.plugins.source.xDripAidl

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import com.eveningoutpost.dexdrip.BgData
import com.eveningoutpost.dexdrip.IBgDataCallback
import com.eveningoutpost.dexdrip.IBgDataService
import timber.log.Timber

/**
 * 完全无Hilt依赖的xDrip AIDL服务
 * 不引用任何AAPS内部注入依赖，只做基础连接和日志
 */
class XDripAidlService: Service() {
    
    // 移除所有@Inject字段
    // 不依赖：AAPSLogger, AppRepository, DateUtil
    
    private var bgDataService: IBgDataService? = null
    private var isBound = false
    
    // 使用Timber替代AAPSLogger（AAPS已集成Timber）
    private val TAG = "xDripAidl"
    
    private val callback = object : IBgDataCallback.Stub() {
        override fun onNewBgData(data: BgData?) {
            data?.let { bg ->
                // 使用Timber记录
                Timber.tag(TAG).d("Received BG ${bg.value} mg/dL @ ${bg.timestamp}")
                
                // 仅记录，不处理数据（第一阶段只验证连接）
                logToSimpleFile(bg) // 可选：记录到文件
            }
        }
    }
    
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            bgDataService = IBgDataService.Stub.asInterface(service)
            isBound = true
            Timber.tag(TAG).i("Connected to xDrip service")
            
            try {
                bgDataService?.registerCallback(callback)
                Timber.tag(TAG).i("Callback registered successfully")
            } catch (e: RemoteException) {
                Timber.tag(TAG).e(e, "Failed to register callback")
            }
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            bgDataService = null
            Timber.tag(TAG).w("Disconnected from xDrip")
            
            // 简单重连逻辑（5秒后）
            android.os.Handler(mainLooper).postDelayed({
                if (!isBound) {
                    bindToXdrip()
                }
            }, 5000)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Timber.tag(TAG).i("Service created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.tag(TAG).i("Service started")
        bindToXdrip()
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun bindToXdrip() {
        if (isBound) {
            Timber.tag(TAG).d("Already bound, skipping")
            return
        }
        
        Timber.tag(TAG).i("Attempting to bind to xDrip...")
        
        val intent = Intent().apply {
            component = ComponentName(
                "com.eveningoutpost.dexdrip",
                "com.eveningoutpost.dexdrip.BgDataService" // 注意：使用你的实际服务名
            )
        }
        
        try {
            val bindResult = bindService(intent, connection, Context.BIND_AUTO_CREATE)
            if (bindResult) {
                Timber.tag(TAG).i("Bind request sent successfully")
            } else {
                Timber.tag(TAG).e("Bind service returned false")
                // 备选服务名尝试（如果第一个失败）
                tryAlternativeServiceName()
            }
        } catch (e: SecurityException) {
            Timber.tag(TAG).e(e, "Security exception. Missing permission?")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to bind to xDrip")
        }
    }
    
    private fun tryAlternativeServiceName() {
        Timber.tag(TAG).w("Trying alternative service name...")
        
        val alternativeIntent = Intent().apply {
            component = ComponentName(
                "com.eveningoutpost.dexdrip",
                "com.eveningoutpost.dexdrip.BgDataService" // 你的原始服务名
            )
        }
        
        try {
            val bindResult = bindService(alternativeIntent, connection, Context.BIND_AUTO_CREATE)
            Timber.tag(TAG).i("Alternative bind result: $bindResult")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Alternative bind also failed")
        }
    }
    
    /**
     * 简单文件日志（替代数据库存储，验证阶段使用）
     */
    private fun logToSimpleFile(data: BgData) {
        // 创建简单日志条目
        val logEntry = "xDripAIDL: ${System.currentTimeMillis()}, ${data.value}, ${data.timestamp}, ${data.trend ?: "N/A"}\n"
        
        // 可选：写入应用私有目录（不需要权限）
        try {
            openFileOutput("xdrip_aidl_log.txt", Context.MODE_APPEND).use {
                it.write(logEntry.toByteArray())
            }
            Timber.tag(TAG).v("Logged to file: ${data.value}")
        } catch (e: Exception) {
            // 忽略文件写入错误
        }
    }
    
    override fun onDestroy() {
        Timber.tag(TAG).i("Service destroying")
        
        if (isBound) {
            try {
                bgDataService?.unregisterCallback(callback)
                Timber.tag(TAG).i("Callback unregistered")
            } catch (e: RemoteException) {
                Timber.tag(TAG).w("Unregister callback failed", e)
            }
            
            try {
                unbindService(connection)
                Timber.tag(TAG).i("Service unbound")
            } catch (e: Exception) {
                Timber.tag(TAG).w("Unbind failed", e)
            }
            
            isBound = false
            bgDataService = null
        }
        
        super.onDestroy()
    }
    
    companion object {
        /**
         * 启动服务的静态方法（从其他类调用）
         */
        fun startService(context: Context) {
            val intent = Intent(context, XDripAidlService::class.java)
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                Timber.tag("xDripAidl").i("Starting foreground service")
                context.startForegroundService(intent)
            } else {
                Timber.tag("xDripAidl").i("Starting service")
                context.startService(intent)
            }
        }
        
        /**
         * 停止服务的静态方法
         */
        fun stopService(context: Context) {
            val intent = Intent(context, XDripAidlService::class.java)
            context.stopService(intent)
        }
    }
}
