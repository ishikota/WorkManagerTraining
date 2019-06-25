package example.com.workmanagertraining

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.work.*
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Exception
import java.util.*
import java.util.concurrent.TimeUnit

val taskA: () -> String = {
    Thread.sleep(1000)
    if (Random().nextBoolean()) {
        Log.d("WM", "taskA is succeeded !!")
        "Work"
    } else {
        Log.d("WM", "taskA is failed...")
        throw Exception("taskA is failed...")
    }
}

val taskB: () -> String = {
    Thread.sleep(2000)
    if (Random().nextBoolean()) {
        Log.d("WM", "taskB is succeeded !!")
        "Manager"
    } else {
        Log.d("WM", "taskB is failed...")
        throw Exception("taskB is failed...")
    }
}

// ようこそ Work Manager !!
val taskC: (String, String) -> Unit = { dataA, dataB ->
    if (Random().nextBoolean()) {
        Log.d("WM", "ようこそ $dataA $dataB !!")
    } else {
        Log.d("WM", "taskC is failed...")
        throw Exception("taskC is failed...")
    }
}

/**
 * TaskA --
 *         |--> TaskC -> [ Welcome Work Manager !! ]
 * TaskB --
 */

// Create Worker classes
class WorkerA(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        Log.d("WM", "WorkerA started")
        try {
            val dataA = taskA()
            val outData: Data = workDataOf(
                KEY_DATA_A to dataA
            )
            return Result.success(outData)
        } catch (e: Exception) {
            Log.e("WM", e.message)
            return Result.retry()
        }
    }

    companion object {
        const val KEY_DATA_A = "key_data_a"
    }
}

class WorkerB(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        Log.d("WM", "WorkerB started")
        try {
            val dataB = taskB()
            val outData: Data = workDataOf(
                KEY_DATA_B to dataB
            )
            return Result.success(outData)
        } catch (e: Exception) {
            Log.e("WM", e.message)
            return Result.retry()
        }
    }

    companion object {
        const val KEY_DATA_B = "key_data_b"
    }
}

class WorkerC(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        Log.d("WM", "WorkerC started")
        val dataA = inputData.getString(WorkerA.KEY_DATA_A)
        val dataB = inputData.getString(WorkerB.KEY_DATA_B)
        return if (dataA != null && dataB != null) {
            try {
                taskC(dataA, dataB)
                Result.success()
            } catch (e: Exception) {
                Log.e("WM", e.message)
                Result.retry()
            }
        } else {
            Result.failure()
        }
    }
}

fun executeTaskA() {
    Log.d("WM", "executeTaskA")
    val constraints = Constraints.Builder()
        .setRequiresCharging(true)
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .build()
    val req = OneTimeWorkRequestBuilder<WorkerA>()
        .setConstraints(constraints)
        .setBackoffCriteria(
            BackoffPolicy.LINEAR,  // or BackoffPolicy.EXPONENTIAL
            OneTimeWorkRequest.MIN_BACKOFF_MILLIS,  // 10000L
            TimeUnit.MILLISECONDS
        )
        .addTag("executeTaskA")
        .build()

    val workManager = WorkManager.getInstance()
    workManager.enqueue(req)

    // How to Cancel
    // workManager.cancelAllWorkByTag("executeTaskA")
}

fun executeTaskAandB() {
    Log.d("WM", "executeTaskAandB")
    val constraints = Constraints.Builder()
        .setRequiresCharging(true)
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .build()
    val reqA = OneTimeWorkRequestBuilder<WorkerA>()
        .setConstraints(constraints)
        .setBackoffCriteria(
            BackoffPolicy.LINEAR,  // or BackoffPolicy.EXPONENTIAL
            OneTimeWorkRequest.MIN_BACKOFF_MILLIS,  // 10000L
            TimeUnit.MILLISECONDS
        )
        .build()
    val reqB = OneTimeWorkRequestBuilder<WorkerB>()
        .setConstraints(constraints)
        .setBackoffCriteria(
            BackoffPolicy.LINEAR,  // or BackoffPolicy.EXPONENTIAL
            OneTimeWorkRequest.MIN_BACKOFF_MILLIS,  // 10000L
            TimeUnit.MILLISECONDS
        )
        .build()

    val workManager = WorkManager.getInstance()
    workManager.beginUniqueWork(
        "executeTaskAandB",
        ExistingWorkPolicy.REPLACE,
        listOf(reqA, reqB)
    )
        .enqueue()

    // How to cancel
    // workManager.cancelUniqueWork("executeTaskAandB")
}

fun executeAllTasks() {
    Log.d("WM", "executeAllTasks")
    val constraints = Constraints.Builder()
        .setRequiresCharging(true)
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .build()
    val reqA = OneTimeWorkRequestBuilder<WorkerA>()
        .setConstraints(constraints)
        .setBackoffCriteria(
            BackoffPolicy.LINEAR,  // or BackoffPolicy.EXPONENTIAL
            OneTimeWorkRequest.MIN_BACKOFF_MILLIS,  // 10000L
            TimeUnit.MILLISECONDS
        )
        .build()
    val reqB = OneTimeWorkRequestBuilder<WorkerB>()
        .setConstraints(constraints)
        .setBackoffCriteria(
            BackoffPolicy.LINEAR,  // or BackoffPolicy.EXPONENTIAL
            OneTimeWorkRequest.MIN_BACKOFF_MILLIS,  // 10000L
            TimeUnit.MILLISECONDS
        )
        .build()
    val reqC = OneTimeWorkRequestBuilder<WorkerC>()
        .setConstraints(constraints)
        .setBackoffCriteria(
            BackoffPolicy.LINEAR,  // or BackoffPolicy.EXPONENTIAL
            OneTimeWorkRequest.MIN_BACKOFF_MILLIS,  // 10000L
            TimeUnit.MILLISECONDS
        )
        .build()

    val workManager = WorkManager.getInstance()
    workManager.beginUniqueWork(
        "executeAllTasks",
        ExistingWorkPolicy.REPLACE,
        listOf(reqA, reqB)
    )
        .then(reqC)
        .enqueue()
}

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        button1.setOnClickListener { executeTaskA() }
        button2.setOnClickListener { executeTaskAandB() }
        button3.setOnClickListener { executeAllTasks() }
    }
}
