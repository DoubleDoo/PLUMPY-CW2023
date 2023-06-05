package com.kkozhakin.ryvok1

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.Tensor
import java.io.*
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.ArrayList

var sleepMLModule: Module? = null
var activityMLModule: Module? = null
var activityModelPath: String? = null
var sleepModelPath: String? = null
var activityResult: MutableMap<String, Int> = mutableMapOf()
var sleepResult: MutableMap<String, Int> = mutableMapOf()


@Throws(IOException::class)
fun assetFilePath(context: Context, assetName: String): String? {
    val file = File(context.filesDir, assetName)
    if (file.exists() && file.length() > 0) {
        return file.absolutePath
    }
    context.assets.open(assetName).use { `is` ->
        FileOutputStream(file).use { os ->
            val buffer = ByteArray(4 * 1024)
            var read: Int
            while (`is`.read(buffer).also { read = it } != -1) {
                os.write(buffer, 0, read)
            }
            os.flush()
        }
        return file.absolutePath
    }
}

fun getTopClass(scores: FloatArray): Int {
    var maxScore = scores[0]
    var maxScoreIdx = 0
    for (i in scores.indices) {
        if (scores[i] > maxScore) {
            maxScore = scores[i]
            maxScoreIdx = i
        }
    }
    return maxScoreIdx
}

fun initModuls(){
    try {
        activityMLModule = LiteModuleLoader.load(activityModelPath)
        sleepMLModule = LiteModuleLoader.load(sleepModelPath)
    } catch (e: IOException) {
        Log.e("PytorchHelloWorld", "Error reading assets", e)
    }
}

fun normalize(x: Float, min: Float, max: Float): Float {
    return (x - min) / (max - min)
}

@RequiresApi(Build.VERSION_CODES.O)
fun resolveData(data: List<ArrayList<String>>, type: String): ArrayList<Map<String, Any>>? {

    if ((activityMLModule == null) or (sleepMLModule == null)) {
        initModuls()
    }

    val ml_data = mutableListOf<Float>()

    for (data_string in data.reversed()) {
        ml_data.addAll(
            when(type) {
                "activity" -> mutableListOf(
                    normalize(data_string[1].toFloat(), -32764f, 12694f),
                    normalize(data_string[2].toFloat(), -32679f, 22360f),
                    normalize(data_string[3].toFloat(), -14423f, 9242f),
                    normalize(data_string[4].toFloat(), -11072f, 10497f),
                    normalize(data_string[5].toFloat(), -5857f, 5605f),
                    normalize(data_string[6].toFloat(), -12018f, 14266f),
                )
                "sleep" -> mutableListOf(
                    normalize(data_string[1].toFloat(), -8188f, 10079f),
                    normalize(data_string[2].toFloat(), -14114f, 9052f),
                    normalize(data_string[3].toFloat(), -7509f, 10987f),
                    normalize(data_string[4].toFloat(), -9380f, 9310f),
                    normalize(data_string[5].toFloat(), -4681f, 4887f),
                    normalize(data_string[6].toFloat(), -5269f, 5716f),
                )
                else -> mutableListOf()
            }
        )
    }

    val shape = longArrayOf(64, 6 * 39)
    val inputTensor: Tensor = Tensor.fromBlob(ml_data.toFloatArray(), shape)

    val outputTensor: Tensor = when (type) {
        "activity" ->  activityMLModule!!.forward(IValue.from(inputTensor)).toTensor()
        "sleep" -> sleepMLModule!!.forward(IValue.from(inputTensor)).toTensor()
        else -> return null
    }

    val scores: FloatArray = outputTensor.dataAsFloatArray
    var time = data[0][8]
    val result = arrayListOf<Map<String, Any>>()

    var i = 0
    while(i < scores.size) {
        when (type) {
            "activity" -> {
                result.add(mapOf("result" to getTopClass(floatArrayOf(scores[i], scores[i+1], scores[i+2])), "time" to time))
                i += 3
            }
            "sleep" -> {
                result.add(mapOf("result" to getTopClass(floatArrayOf(scores[i], scores[i+1])), "time" to time))
                i += 2
            }
        }
        try {
            time = LocalDateTime.parse(time).plusSeconds(3).toString()
        }
        catch (e: Exception){
            continue
        }
    }

    return result
}