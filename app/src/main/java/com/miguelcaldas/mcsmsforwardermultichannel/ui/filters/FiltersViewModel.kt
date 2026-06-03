package com.miguelcaldas.mcsmsforwardermultichannel.ui.filters

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import com.miguelcaldas.mcsmsforwardermultichannel.util.RegexListStore
import com.miguelcaldas.mcsmsforwardermultichannel.util.SenderListStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FiltersViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("mc_sms_fwd_wa", Context.MODE_PRIVATE)

    private val _senders = MutableStateFlow(SenderListStore.load(prefs))
    val senders: StateFlow<List<String>> = _senders.asStateFlow()

    private val _rules = MutableStateFlow(RegexListStore.load(prefs))
    val rules: StateFlow<List<String>> = _rules.asStateFlow()

    private val _template = MutableStateFlow(prefs.getString(KEY_TEMPLATE, "").orEmpty())
    val template: StateFlow<String> = _template.asStateFlow()

    fun refresh() {
        _senders.value = SenderListStore.load(prefs)
        _rules.value = RegexListStore.load(prefs)
        _template.value = prefs.getString(KEY_TEMPLATE, "").orEmpty()
    }

    fun addSender(raw: String): String? {
        val value = raw.trim()
        if (value.isEmpty()) {
            return "Enter a sender first"
        }
        if (_senders.value.any { it.equals(value, ignoreCase = true) }) {
            return "Already in the list"
        }
        val updated = _senders.value + value
        _senders.value = updated
        SenderListStore.save(prefs, updated)
        return null
    }

    fun removeSender(value: String) {
        val updated = _senders.value.filterNot { it == value }
        _senders.value = updated
        SenderListStore.save(prefs, updated)
    }

    fun addRule() {
        _rules.value = _rules.value + ""
    }

    fun updateRule(index: Int, value: String) {
        val updated = _rules.value.toMutableList()
        if (index in updated.indices) {
            // RegexListStore does not trim — leading/trailing whitespace can be part of a pattern.
            updated[index] = value
            _rules.value = updated
            RegexListStore.save(prefs, updated)
        }
    }

    fun removeRule(index: Int) {
        val updated = _rules.value.toMutableList()
        if (index in updated.indices) {
            updated.removeAt(index)
            _rules.value = updated
            RegexListStore.save(prefs, updated)
        }
    }

    fun setTemplate(value: String) {
        _template.value = value
        prefs.edit {
            putString(KEY_TEMPLATE, value)
        }
    }

    private companion object {
        const val KEY_TEMPLATE = "forwardTemplate"
    }
}
