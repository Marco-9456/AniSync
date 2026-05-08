package com.anisync.android

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import com.anisync.android.ui.theme.AppTheme
import com.anisync.android.util.AppInfo
import kotlin.system.exitProcess

class CrashReportActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val stackTrace = intent.getStringExtra(EXTRA_STACK_TRACE).orEmpty()
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Unknown Error"

        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CrashReportScreen(
                        title = title,
                        stackTrace = stackTrace,
                        onCopyAndExit = {
                            copyReport(stackTrace)
                            finishAndKill()
                        }
                    )
                }
            }
        }
    }

    private fun copyReport(stackTrace: String) {
        val payload = buildString {
            append(AppInfo.formatted())
            append("\n\n--- Stack trace ---\n")
            append(stackTrace)
        }
        val clipboard = getSystemService<ClipboardManager>() ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText("AniSync crash", payload))
    }

    private fun finishAndKill() {
        finishAffinity()
        Process.killProcess(Process.myPid())
        exitProcess(10)
    }

    @Deprecated("Use finishAndKill directly", ReplaceWith("finishAndKill()"))
    override fun onBackPressed() {
        finishAndKill()
    }

    companion object {
        const val EXTRA_STACK_TRACE = "extra_stack_trace"
        const val EXTRA_TITLE = "extra_title"

        fun newIntent(
            context: Context,
            throwable: Throwable,
            title: String = throwable::class.java.simpleName ?: "Unknown Error"
        ): Intent {
            val stack = throwable.stackTraceToString()
            return Intent(context, CrashReportActivity::class.java)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            or Intent.FLAG_ACTIVITY_CLEAR_TOP
                )
                .putExtra(EXTRA_STACK_TRACE, stack)
                .putExtra(EXTRA_TITLE, title)
        }
    }
}

@Composable
private fun CrashReportScreen(
    title: String,
    stackTrace: String,
    onCopyAndExit: () -> Unit
) {
    val systemBars = WindowInsets.systemBars.asPaddingValues()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = systemBars.calculateTopPadding() + 32.dp,
                    bottom = systemBars.calculateBottomPadding() + 96.dp,
                    start = 24.dp,
                    end = 24.dp
                )
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = stackTrace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }

        CopyAndExitButton(
            onClick = onCopyAndExit,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = systemBars.calculateBottomPadding() + 16.dp
                )
                .fillMaxWidth()
        )
    }
}

@Composable
private fun CopyAndExitButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Outlined.BugReport,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Copy and Exit",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
