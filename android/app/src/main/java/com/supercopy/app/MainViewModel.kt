package com.supercopy.app

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.supercopy.app.core.Filter
import com.supercopy.app.core.Processor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("supercopy", Context.MODE_PRIVATE)

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    private val _output = MutableStateFlow("")
    val output: StateFlow<String> = _output.asStateFlow()

    private val _removedInfo = MutableStateFlow("")
    val removedInfo: StateFlow<String> = _removedInfo.asStateFlow()

    private val _processing = MutableStateFlow(false)
    val processing: StateFlow<Boolean> = _processing.asStateFlow()

    private val _activeFilters = MutableStateFlow(loadFilters())
    val activeFilters: StateFlow<Set<Filter>> = _activeFilters.asStateFlow()

    private var processJob: Job? = null

    private fun loadFilters(): Set<Filter> {
        val saved = prefs.getStringSet("filters", null)
            ?: return setOf(Filter.EXTRACT_URL, Filter.EXPAND, Filter.TRACKING) // 默认开启链接三件套
        return saved.mapNotNull { name -> Filter.entries.find { it.name == name } }.toSet()
    }

    fun setInput(text: String) {
        _input.value = text
        scheduleProcess()
    }

    fun toggleFilter(filter: Filter) {
        val next = _activeFilters.value.toMutableSet()
        if (!next.remove(filter)) next.add(filter)
        _activeFilters.value = next
        prefs.edit().putStringSet("filters", next.map { it.name }.toSet()).apply()
        scheduleProcess()
    }

    fun useOutputAsInput() {
        if (_output.value.isNotBlank()) setInput(_output.value)
    }

    fun clear() {
        processJob?.cancel()
        _input.value = ""
        _output.value = ""
        _removedInfo.value = ""
        _processing.value = false
    }

    /** 防抖 + 取消旧任务：慢的短链展开不会用旧结果覆盖新输入 */
    private fun scheduleProcess() {
        processJob?.cancel()
        processJob = viewModelScope.launch {
            delay(150)
            val raw = _input.value
            if (raw.isEmpty()) {
                _output.value = ""
                _removedInfo.value = ""
                return@launch
            }
            _processing.value = true
            try {
                val result = Processor.process(raw, _activeFilters.value)
                _output.value = result.output
                _removedInfo.value = result.removedParts.joinToString("  ")
            } finally {
                _processing.value = false
            }
        }
    }
}
