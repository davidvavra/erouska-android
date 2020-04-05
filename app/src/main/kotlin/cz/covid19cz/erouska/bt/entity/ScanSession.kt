package cz.covid19cz.erouska.bt.entity

import arch.livedata.SafeMutableLiveData
import java.util.*
import kotlin.collections.ArrayList


open class ScanSession(var deviceId: String = DEFAULT_BUID, val mac: String) {

    companion object{
        const val DEFAULT_BUID = "UNKNOWN"
    }

    private val rssiList = ArrayList<Rssi>()

    var avgRssi = 0
    var medRssi = 0
    val timestampStart: Long
        get() = rssiList.firstOrNull()?.timestamp ?: 0L
    val timestampEnd: Long
        get() = rssiList.lastOrNull()?.timestamp ?: 0L
    val rssiCount: Int
        get() = rssiList.size

    open fun addRssi(rssiVal: Int) {
        rssiList.add(Rssi(rssiVal))
    }

    private fun addRssi(rssi: Rssi) {
        rssiList.add(rssi)
    }

    fun calculate() {
        var sum = 0

        for (rssi in rssiList) {
            sum += rssi.rssi
        }
        if (rssiList.size != 0) {
            if (sum != 0) {
                avgRssi = sum / rssiList.size
            }
            medRssi = median(rssiList.map { it.rssi }.toIntArray())
        }
    }

    private fun median(values: IntArray): Int {
        Arrays.sort(values)
        val middle = values.size / 2
        return if (values.size % 2 == 0) {
            val left = values[middle - 1]
            val right = values[middle]
            (left + right) / 2
        } else {
            values[middle]
        }
    }

    fun reset(){
        rssiList.clear()
        avgRssi = 0
        medRssi = 0
    }

    fun fold(interval: Long): List<ScanSession> {
        return rssiList.toList().fold(mutableListOf()) { acc, item ->
            acc.lastOrNull()?.apply {
                if (item.timestamp - timestampStart < interval) {
                    addRssi(item)
                }
            } ?: run {
                ScanSession(deviceId, mac).apply {
                    acc.add(this)
                    addRssi(item)
                }
            }
            acc
        }
    }
}

class ObservableScanSession(deviceId: String = DEFAULT_BUID, mac: String): ScanSession(deviceId, mac) {
    val currRssi = SafeMutableLiveData(Int.MAX_VALUE)

    override fun addRssi(rssiVal: Int) {
        super.addRssi(rssiVal)
        currRssi.postValue(rssiVal)
    }
}