package com.example.wifihot.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.wifihot.BleServer
import com.example.wifihot.BleServer.socket
import com.example.wifihot.databinding.FragmentClientBinding
import com.example.wifihot.databinding.FragmentMainBinding
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.Socket
import java.net.UnknownHostException

class ClientFragment:Fragment() {
    lateinit var binding: FragmentClientBinding
    lateinit var wifiManager: WifiManager
    var wifiState = 0


    private fun isWifiConnected(): Boolean {
        val connectivityManager =
            requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wifiNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        return wifiNetworkInfo!!.isConnected
    }

    private fun getConnectWifiSsid(): String? {
        val wifiInfo = wifiManager!!.connectionInfo
        Log.d("wifiInfo", wifiInfo.toString())
        Log.d("SSID", wifiInfo.ssid)
        return wifiInfo.ssid
    }

    private val wifiBroadcast: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // TODO Auto-generated method stub
            if (intent.action == WifiManager.RSSI_CHANGED_ACTION) {
                //signal strength changed
            } else if (intent.action == WifiManager.WIFI_STATE_CHANGED_ACTION) { //wifi打开与否
                val wifistate = intent.getIntExtra(
                    WifiManager.EXTRA_WIFI_STATE,
                    WifiManager.WIFI_STATE_DISABLED
                )
                if (wifistate == WifiManager.WIFI_STATE_DISABLED) {
                    println("系统关闭wifi")

                    wifiState = 0
                } else if (wifistate == WifiManager.WIFI_STATE_ENABLED) {
                    println("系统开启wifi")
                    wifiState = if (isWifiConnected()) {
                        if (getConnectWifiSsid() == "\"wifisocket\"") {
                            3
                        } else {
                            4
                        }
                    } else {
                        1
                    }
                }
            } else if (intent.action == WifiManager.NETWORK_STATE_CHANGED_ACTION) { //wifi连接上与否
                println("网络状态改变")
                val info = intent.getParcelableExtra<NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO)
                if (info!!.state == NetworkInfo.State.DISCONNECTED) {
                    println("wifi网络连接断开")
                    wifiState = 2
                } else if (info.state == NetworkInfo.State.CONNECTED) {
                    val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    val wifiInfo = wifiManager.connectionInfo

                    //获取当前wifi名称
                    println("连接到网络 " + wifiInfo.ssid)
                    wifiState = if (wifiInfo.ssid == "\"wifisocket\"") {
                        3
                    } else {
                        4
                    }
                }
            }
        }
    }

    private fun makeGattUpdateIntentFilter(): IntentFilter? {
        val filter = IntentFilter()
        filter.addAction(WifiManager.RSSI_CHANGED_ACTION)
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        return filter
    }

    fun initClientSocket() {
        try {

        } catch (e: UnknownHostException) {
            // TODO Auto-generated catch block
            println("请检查端口号是否为服务器IP")
            e.printStackTrace()
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            println("服务器未开启")
            e.printStackTrace()
        }
        // output.println("this is the message from client");
    }


    private fun intToIp(paramInt: Int): String? {
        return ((paramInt.and(255)).toString() + "." + (paramInt.shr(8).and(255)) + "." + (paramInt.shr(16).and(255)) + "."
                + (paramInt.shr(24).and(255)))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        wifiManager =requireActivity().getSystemService(Context.WIFI_SERVICE) as WifiManager


        val gate=intToIp(wifiManager.dhcpInfo.gateway)
        if (gate != null) {
            BleServer.dataScope.launch {
                try {
                    val buffer = ByteArray(64)
                    socket = Socket(gate, 9999)
                    val input= socket.getInputStream()
                    while(true){
                        val bytes=input.read(buffer)
                        if(bytes>0){
                            Log.e("fuckNet", String(buffer.copyOfRange(0,bytes)))
                        }
                    }

                } catch (e: UnknownHostException) {
                    println("请检查端口号是否为服务器IP")
                    e.printStackTrace()
                } catch (e: IOException) {
                    println("服务器未开启")
                    e.printStackTrace()
                }
            }

        }

        requireContext().registerReceiver(wifiBroadcast,makeGattUpdateIntentFilter())
        binding= FragmentClientBinding.inflate(inflater,container,false)
        return binding.root
    }

}