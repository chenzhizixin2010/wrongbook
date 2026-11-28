package com.chenzixin.wrongbook.feature.detail

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.chenzixin.wrongbook.core.ImageUtils.imageProxyToBitmap
import com.chenzixin.wrongbook.core.awaitResult
import com.chenzixin.wrongbook.ui.component.WbIcon
import com.chenzixin.wrongbook.ui.component.WbText
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward

/**
 * 答案页相机：拍一张答案/解析，OCR 后由用户校对入库。
 *
 * 为什么单独做一个而不是复用扫描页：扫描页是「整页 → 切题 → 多选」的流程，
 * 答案只需要「一张照片 → 一段文字」，走扫描页会把用户绕进切题界面。
 * 相机本身与扫描页同款（CameraX Preview + ImageCapture），只是取景提示不同。
 */
@Composable
fun AnswerCameraDialog(
    scanning: Boolean,
    onCaptured: (android.graphics.Bitmap) -> Unit,
    onPickGallery: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val c = MiuixTheme.colorScheme
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val askPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { ok -> granted = ok }

    LaunchedEffect(Unit) {
        if (!granted) askPermission.launch(Manifest.permission.CAMERA)
    }

    Dialog(
        onDismissRequest = { if (!scanning) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(c.background),
        ) {
            if (granted) {
                CameraLayer(
                    scanning = scanning,
                    onCaptured = onCaptured,
                    onPickGallery = onPickGallery,
                    onDismiss = onDismiss,
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    WbText(
                        text = "需要相机权限才能拍答案",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = c.onSurface,
                    )
                    Spacer(Modifier.height(10.dp))
                    WbText(
                        text = "也可以从相册选一张答案照片。",
                        fontSize = 13.sp,
                        color = c.onSurface.copy(alpha = 0.7f),
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CameraChip(text = "重新授权") {
                            askPermission.launch(Manifest.permission.CAMERA)
                        }
                        CameraChip(text = "从相册选", enabled = !scanning) {
                            if (!scanning) onPickGallery()
                        }
                    }
                }
            }
        }
    }
}

/** 取景 + 快门 + 顶部返回；相机不可用时静默失败（用户还能走相册）。 */
@Composable
private fun CameraLayer(
    scanning: Boolean,
    onCaptured: (android.graphics.Bitmap) -> Unit,
    onPickGallery: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val c = MiuixTheme.colorScheme
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
    }
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    LaunchedEffect(Unit) {
        val executor = ContextCompat.getMainExecutor(context)
        val bound = runCatching {
            ProcessCameraProvider.getInstance(context).awaitResult(executor)
        }.getOrNull()
        provider = bound
        if (bound != null) {
            runCatching {
                bound.unbindAll()
                bound.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) },
                    imageCapture,
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { runCatching { provider?.unbindAll() } }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        // 取景框：四角可见，提示整页入框
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 26.dp, vertical = 96.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 1.dp,
                        color = c.onSurface.copy(alpha = if (scanning) 0.2f else 0.45f),
                        shape = RoundedCornerShape(18.dp),
                    ),
            )
        }

        // 顶部：关闭 + 标题 + 相册
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(c.background.copy(alpha = 0.38f))
                    .clickable(enabled = !scanning, onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                WbIcon(
                    imageVector = MiuixIcons.ChevronBackward,
                    contentDescription = "关闭",
                    modifier = Modifier.size(18.dp),
                    tint = c.onSurface,
                )
            }
            Spacer(Modifier.weight(1f))
            WbText(
                text = "拍答案页",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = c.onSurface,
            )
            Spacer(Modifier.weight(1f))
            CameraChip(text = "相册", enabled = !scanning) {
                if (!scanning) onPickGallery()
            }
        }

        // 底部：提示 + 快门
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WbText(
                text = if (scanning) "识别中…" else "把答案写的那一页拍进来，尽量填满取景框",
                fontSize = 12.sp,
                color = c.onSurface.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(14.dp))
            ShutterButton(enabled = !scanning) {
                imageCapture.takePicture(
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            val bitmap = imageProxyToBitmap(image)
                            image.close()
                            if (bitmap != null) onCaptured(bitmap)
                        }

                        override fun onError(exception: ImageCaptureException) {
                            // 拍失败不弹窗：用户可以直接重按快门，或改用相册
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ShutterButton(enabled: Boolean, onClick: () -> Unit) {
    val c = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .size(78.dp)
            .clip(CircleShape)
            .border(4.dp, c.onSurface.copy(alpha = if (enabled) 0.95f else 0.4f), CircleShape)
            .padding(7.dp)
            .clip(CircleShape)
            .background(c.onSurface.copy(alpha = if (enabled) 1f else 0.4f))
            .clickable(enabled = enabled, onClick = onClick),
    )
}

@Composable
private fun CameraChip(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    val c = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(c.background.copy(alpha = 0.38f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        WbText(
            text = text,
            fontSize = 12.sp,
            color = c.onSurface.copy(alpha = if (enabled) 1f else 0.45f),
        )
    }
}
