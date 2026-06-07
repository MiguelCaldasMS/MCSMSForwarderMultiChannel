package com.miguelcaldas.mcsmsforwardermultichannel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.miguelcaldas.mcsmsforwardermultichannel.util.LogUtils

// Wakes the package after a reboot so the SMS receiver is "warm" before the first
// SMS arrives. Does no real work — its existence is the point: a manifest receiver
// for BOOT_COMPLETED forces the framework to load this app at boot, which on some
// OEMs reduces the chance of the package staying in the not-yet-launched state.
class BootReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                LogUtils.addToLog(context, "BOOT → ready to forward")
            }
        }
    }
}
