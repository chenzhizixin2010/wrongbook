package com.chenzixin.wrongbook.feature.scan

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.LocalActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.chenzixin.wrongbook.core.ImageUtils
import com.chenzixin.wrongbook.core.awaitResult
import com.chenzixin.wrongbook.ui.component.WbAlertDialog
import com.chenzixin.wrongbook.ui.component.WbIcons
import com.chenzixin.wrongbook.ui.component.WbPrimaryButton
import com.chenzixin.wrongbook.ui.component.WbTopBar
import com.chenzixin.wrongbook.ui.theme.wbColors
import io.github.robinpcrd.cupertino.CupertinoButton
import io.github.robinpcrd.cupertino.CupertinoIcon
import io.github.robinpcrd.cupertino.CupertinoText
import io.github.robinpcrd.cupertino.icons.CupertinoIcons
import io.github.robinpcrd.cupertino.icons.outlined.ChevronBackward
import io.github.robinpcrd.cupertino.icons.outlined.RectanglePortraitAndArrowForward
import kotlin.math.min

/**
 * 拍照 → 本地 OCR → 题目切分 → 矩形多选 → 入库。
 *
 * P3 负责前两步，P4 负责多选与入库；两个阶段共用这一个页面。
 */
@Composable
fun CameraCaptureScreen(
    onDone: (Int) -> Unit,
    onBack: () -> Unit,
    vm: ScanViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    CameraSystemBars()
    var cropBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> permissionGranted = granted }

    LaunchedEffect(state.stage) {
        if (state.stage == ScanViewModel.Stage.Done) {
            onDone(state.savedCount)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        val pendingCrop = cropBitmap
        when {
            pendingCrop != null -> PerspectiveCropScreen(
                bitmap = pendingCrop,
                onCancel = { cropBitmap = null },
                onConfirm = { warped ->
                    cropBitmap = null
                    vm.onCaptured(warped)
                },
            )

            !permissionGranted -> PermissionRequest(onRequest = {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            })

            state.stage == ScanViewModel.Stage.Idle ||
                state.stage == ScanViewModel.Stage.Recognizing -> CameraStage(
                stage = state.stage,
                onCaptured = { bmp -> cropBitmap = bmp },
                onError = vm::onError,
                onBack = onBack,
            )

            state.stage == ScanViewModel.Stage.Selecting ||
                state.stage == ScanViewModel.Stage.Saving -> SelectionStage(
                state = state,
                onToggle = vm::toggleRegion,
                onSelectAll = vm::selectAll,
                onInvert = vm::invertSelection,
                onToggleTag = vm::toggleTag,
                onCreateTag = vm::createTag,
                onSave = vm::save,
                onDiscard = vm::discard,
            )

            else -> Unit
        }

        // 顶栏（非 Idle 时也保留返回入口）
        if (state.stage == ScanViewModel.Stage.Selecting) {
            // 相机页固定黑底，不能复用 WbTopBar（浅色主题下标题会是深色）。
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent),
                            endY = 220f,
                        ),
                    )
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f))
                        .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                        .clickable(onClick = vm::discard),
                    contentAlignment = Alignment.Center,
                ) {
                    CupertinoIcon(
                        imageVector = CupertinoIcons.Outlined.ChevronBackward,
                        contentDescription = "返回",
                        modifier = Modifier.size(17.dp),
                        tint = Color.White,
                    )
                }
                CupertinoText(
                    text = "选择要入库的题目",
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                )
                CupertinoText(
                    text = "已选 ${state.selected.size}",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.65f),
                )
            }
        }

        state.message?.let { msg ->
            WbAlertDialog(
                visible = true,
                title = "提示",
                message = msg,
                confirmText = "知道了",
                onConfirm = vm::clearMessage,
                onDismiss = vm::clearMessage,
            )
        }

        if (state.stage == ScanViewModel.Stage.Saving) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000)),
                contentAlignment = Alignment.Center,
            ) {
                CupertinoText(
                    text = "正在保存 ${state.progress ?: ""}",
                    fontSize = 16.sp,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun PermissionRequest(onRequest: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0A84FF), Color(0xFF6C5CE7)),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                CupertinoIcon(
                    imageVector = WbIcons.Camera,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                    tint = Color.White,
                )
            }
            Spacer(Modifier.height(20.dp))
            CupertinoText(
                text = "需要相机权限",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            Spacer(Modifier.height(7.dp))
            CupertinoText(
                text = "只用于拍摄错题；识别在本机完成，不会上传图片。",
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                color = Color.White.copy(alpha = 0.68f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(22.dp))
            WbPrimaryButton(text = "允许使用相机", onClick = onRequest)
        }
    }
}

/** 相机页是黑底，系统状态栏图标要强制用浅色。 */
@Composable
private fun CameraSystemBars() {
    val activity = LocalActivity.current
    DisposableEffect(activity) {
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        val previousStatus = controller?.isAppearanceLightStatusBars ?: false
        val previousNav = controller?.isAppearanceLightNavigationBars ?: false
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            controller?.isAppearanceLightStatusBars = previousStatus
            controller?.isAppearanceLightNavigationBars = previousNav
        }
    }
}

@Composable
private fun CameraStage(
    stage: ScanViewModel.Stage,
    onCaptured: (android.graphics.Bitmap) -> Unit,
    onError: (String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    LaunchedEffect(Unit) {
        val executor = ContextCompat.getMainExecutor(context)
        val provider = ProcessCameraProvider.getInstance(context).awaitResult(executor)
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        runCatching {
            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture,
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        // 顶部压暗，保证状态栏与提示可读
        Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .align(Alignment.TopCenter)
            .background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent),
                ),
            ),
    )
    Box(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.38f))
                    .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                CupertinoIcon(
                    imageVector = CupertinoIcons.Outlined.ChevronBackward,
                    contentDescription = "返回",
                    modifier = Modifier.size(17.dp),
                    tint = Color.White,
                )
            }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Color.Black.copy(alpha = 0.38f))
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                CupertinoText(
                    text = "整页入框 · 四角可见",
                    fontSize = 11.5.sp,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.size(40.dp))
        }
    }

    CameraFrameOverlay()

    // 取景框提示
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(bottom = 26.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (stage == ScanViewModel.Stage.Recognizing) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                ) {
                    CupertinoText(text = "识别中…", fontSize = 12.sp, color = Color.White)
                }
                Spacer(Modifier.height(16.dp))
            }
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .border(4.dp, Color.White.copy(alpha = 0.92f), CircleShape)
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .pointerInput(Unit) {
                        detectTapGestures {
                            if (stage != ScanViewModel.Stage.Idle) return@detectTapGestures
                            imageCapture.takePicture(
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(image: ImageProxy) {
                                        val bitmap = ImageUtils.imageProxyToBitmap(image)
                                        image.close()
                                        if (bitmap != null) onCaptured(bitmap) else onError("拍照失败")
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        onError(exception.message ?: "拍照失败")
                                    }
                                },
                            )
                        }
                    },
            )
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Color.Black.copy(alpha = 0.38f))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                CupertinoText(
                    text = "点按拍摄",
                    fontSize = 11.5.sp,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
    }
    }
}

/** 取景框四角括号。 */
@Composable
private fun CameraFrameOverlay() {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 96.dp, bottom = 150.dp),
    ) {
        val len = 26.dp.toPx()
        val stroke = 2.5.dp.toPx()
        val color = Color.White.copy(alpha = 0.8f)
        val left = 0f
        val top = 0f
        val right = size.width
        val bottom = size.height

        fun corner(x: Float, y: Float, dx: Float, dy: Float) {
            drawLine(color, Offset(x, y), Offset(x + dx * len, y), stroke)
            drawLine(color, Offset(x, y), Offset(x, y + dy * len), stroke)
        }
        corner(left, top, 1f, 1f)
        corner(right, top, -1f, 1f)
        corner(left, bottom, 1f, -1f)
        corner(right, bottom, -1f, -1f)
    }
}

@Composable
private fun SelectionStage(
    state: ScanViewModel.UiState,
    onToggle: (Int) -> Unit,
    onSelectAll: () -> Unit,
    onInvert: () -> Unit,
    onToggleTag: (Long) -> Unit,
    onCreateTag: (String) -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
) {
    val bitmap = state.bitmap ?: return
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    val colors = wbColors()
    var showTagDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithImageOverlay(
            image = image,
            bitmapWidth = bitmap.width,
            bitmapHeight = bitmap.height,
            regions = state.regions,
            selected = state.selected,
            onToggle = onToggle,
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color(0xE6000000))
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                com.chenzixin.wrongbook.ui.component.WbChip(
                    label = "全选",
                    selected = false,
                    onClick = onSelectAll,
                    onLight = true,
                )
                com.chenzixin.wrongbook.ui.component.WbChip(
                    label = "反选",
                    selected = false,
                    onClick = onInvert,
                    onLight = true,
                )
                com.chenzixin.wrongbook.ui.component.WbChip(
                    label = "重拍",
                    selected = false,
                    onClick = onDiscard,
                    onLight = true,
                )
                Spacer(Modifier.weight(1f))
                CupertinoText(
                    text = "已选 ${state.selected.size} 题",
                    fontSize = 11.5.sp,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }
            if (state.tags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.tags.forEach { tag ->
                        com.chenzixin.wrongbook.ui.component.WbChip(
                            label = tag.name,
                            selected = tag.id in state.selectedTagIds,
                            onClick = { onToggleTag(tag.id) },
                            onLight = true,
                        )
                    }
                    com.chenzixin.wrongbook.ui.component.WbChip(
                        label = "＋ 新建标签",
                        selected = false,
                        onClick = { showTagDialog = true },
                        onLight = true,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            WbPrimaryButton(
                text = "存入题库（${state.selected.size} 题）",
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.selected.isNotEmpty() && state.stage != ScanViewModel.Stage.Saving,
            )
        }
    }

    com.chenzixin.wrongbook.ui.component.WbInputDialog(
        visible = showTagDialog,
        title = "新建标签",
        placeholder = "标签名，例如 函数",
        onConfirm = { name ->
            showTagDialog = false
            onCreateTag(name)
        },
        onDismiss = { showTagDialog = false },
    )
}

@Composable
private fun BoxWithImageOverlay(
    image: androidx.compose.ui.graphics.ImageBitmap,
    bitmapWidth: Int,
    bitmapHeight: Int,
    regions: List<com.chenzixin.wrongbook.domain.scan.QuestionRegion>,
    selected: Set<Int>,
    onToggle: (Int) -> Unit,
) {
    val accent = wbColors().tint
    Box(modifier = Modifier.fillMaxSize()) {
        var canvasSize by remember { mutableStateOf(Size.Zero) }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { canvasSize = Size(it.width.toFloat(), it.height.toFloat()) }
                .pointerInput(regions, canvasSize) {
                    detectTapGestures { tap ->
                        if (canvasSize.width == 0f) return@detectTapGestures
                        val scale = min(canvasSize.width / bitmapWidth, canvasSize.height / bitmapHeight)
                        val dw = bitmapWidth * scale
                        val dh = bitmapHeight * scale
                        val ox = (canvasSize.width - dw) / 2f
                        val oy = (canvasSize.height - dh) / 2f
                        val nx = (tap.x - ox) / dw
                        val ny = (tap.y - oy) / dh
                        val hit = regions.indexOfFirst { r ->
                            nx >= r.rect.left && nx <= r.rect.right && ny >= r.rect.top && ny <= r.rect.bottom
                        }
                        if (hit >= 0) onToggle(hit)
                    }
                },
        ) {
            val scale = min(size.width / bitmapWidth, size.height / bitmapHeight)
            val dw = bitmapWidth * scale
            val dh = bitmapHeight * scale
            val ox = (size.width - dw) / 2f
            val oy = (size.height - dh) / 2f
            drawImage(
                image = image,
                dstOffset = IntOffset(ox.toInt(), oy.toInt()),
                dstSize = androidx.compose.ui.unit.IntSize(dw.toInt(), dh.toInt()),
            )
            regions.forEachIndexed { index, region ->
                val isSelected = index in selected
                if (region.isSectionHeader) return@forEachIndexed
                val left = ox + region.rect.left * dw
                val top = oy + region.rect.top * dh
                val w = region.rect.width * dw
                val h = region.rect.height * dh
                drawRect(
                    color = if (isSelected) accent else Color.White,
                    topLeft = Offset(left, top),
                    size = Size(w, h),
                    style = Stroke(width = if (isSelected) 6f else 3f),
                )
                if (isSelected) {
                    drawRect(
                        color = accent.copy(alpha = 0.15f),
                        topLeft = Offset(left, top),
                        size = Size(w, h),
                    )
                }
            }
        }

        // 题号角标：用 Compose 文本叠在框左上角（Canvas 里画文字太啰嗦）
        val canvasW = canvasSize.width
        val canvasH = canvasSize.height
        if (canvasW > 0f && canvasH > 0f) {
            val scale = min(canvasW / bitmapWidth, canvasH / bitmapHeight)
            val dw = bitmapWidth * scale
            val dh = bitmapHeight * scale
            val ox = (canvasW - dw) / 2f
            val oy = (canvasH - dh) / 2f
            regions.forEachIndexed { index, region ->
                if (region.isSectionHeader) return@forEachIndexed
                val left = ox + region.rect.left * dw
                val top = oy + region.rect.top * dh
                Box(
                    modifier = Modifier
                        .offset { IntOffset(left.toInt().coerceAtLeast(0), (top - 22.dp.toPx()).toInt().coerceAtLeast(0)) }
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (index in selected) accent else Color(0x99000000))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    CupertinoText(
                        text = region.index.takeIf { it > 0 }?.toString() ?: "题",
                        fontSize = 12.sp,
                        color = Color.White,
                    )
                }
            }
        }
    }
}
