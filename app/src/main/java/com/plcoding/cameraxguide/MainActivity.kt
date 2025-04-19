@file:OptIn(ExperimentalMaterial3Api::class)

package com.plcoding.cameraxguide

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.plcoding.cameraxguide.ui.theme.CameraXGuideTheme
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.compose.foundation.background
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.camera.core.TorchState
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasRequiredPermissions()) {
            ActivityCompat.requestPermissions(
                this, CAMERAX_PERMISSIONS, 0
            )
        }
        
        cameraExecutor = Executors.newSingleThreadExecutor()

        setContent {
            CameraXGuideTheme {
                val scope = rememberCoroutineScope()
                val scaffoldState = rememberBottomSheetScaffoldState()
                val recognizedText = remember { mutableStateOf("") }
                val torchState = remember { mutableStateOf(TorchState.OFF) }
                var showManualEntryDialog by remember { mutableStateOf(false) }
                var manualCode by remember { mutableStateOf("") }
                var language by remember { mutableStateOf("hr") }
                
                val controller = remember {
                    LifecycleCameraController(applicationContext).apply {
                        setEnabledUseCases(
                            CameraController.IMAGE_CAPTURE or
                                    CameraController.VIDEO_CAPTURE or
                                    CameraController.IMAGE_ANALYSIS
                        )
                        
                        setImageAnalysisAnalyzer(
                            cameraExecutor,
                            TextAnalyzer { text ->
                                recognizedText.value = text
                            }
                        )

                        torchState.value = if (torchState.value == TorchState.ON) TorchState.ON else TorchState.OFF
                    }
                }
                val viewModel = viewModel<MainViewModel>()
                val bitmaps by viewModel.bitmaps.collectAsState()

                BottomSheetScaffold(
                    scaffoldState = scaffoldState,
                    sheetPeekHeight = 0.dp,
                    sheetContent = {
                        PhotoBottomSheetContent(
                            bitmaps = bitmaps,
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                    }
                ) { padding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        CameraPreview(
                            controller = controller,
                            modifier = Modifier
                                .fillMaxSize()
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = {
                                    controller.cameraSelector =
                                        if (controller.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                                            CameraSelector.DEFAULT_FRONT_CAMERA
                                        } else CameraSelector.DEFAULT_BACK_CAMERA
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cameraswitch,
                                    contentDescription = "Switch camera",
                                    tint = Color.White
                                )
                            }

                            IconButton(
                                onClick = {
                                    language = if (language == "hr") "en" else "hr"
                                    
                                    if (recognizedText.value.isNotEmpty()) {
                                        val lines = recognizedText.value.split("\n")
                                        val codePrefix = if (language == "hr") "Kod: " else "Code: "
                                        val altCodePrefix = if (language == "hr") "Code: " else "Kod: "
                                        
                                        val codeLine = lines.firstOrNull { it.startsWith(codePrefix) || it.startsWith(altCodePrefix) } ?: ""
                                        val code = codeLine.substringAfter(": ").trim()
                                        
                                        if (code.isNotEmpty()) {
                                            if (code.length == 8 && code.endsWith("00000")) {
                                                val yearDigit = code[0].toString()
                                                val weekNumber = code.substring(1, 3).toInt()
                                                
                                                val year = "202$yearDigit"
                                                val date = TextAnalyzer.calculateDateFromYearAndWeek(year.toInt(), weekNumber)
                                                
                                                val formatter = DateTimeFormatter.ofPattern("MM/yyyy")
                                                val expirationDate = date.plusMonths(30)
                                                val now = LocalDate.now()
                                                val isExpired = expirationDate.isBefore(now)
                                                
                                                val dateInfo = TextAnalyzer.Companion.DateInfo(
                                                    code = code,
                                                    year = year,
                                                    weekNumber = weekNumber,
                                                    manufactureDate = date.format(formatter),
                                                    expirationDate = expirationDate.format(formatter),
                                                    isExpired = isExpired
                                                )
                                                
                                                recognizedText.value = TextAnalyzer.formatDateInfo(dateInfo, language)
                                            } else if (TextAnalyzer.CODE_PATTERN.matches(code)) {
                                                val dateInfo = TextAnalyzer.calculateDatesFromCode(code)
                                                recognizedText.value = TextAnalyzer.formatDateInfo(dateInfo, language)
                                            }
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = if (language == "hr") "Switch to English" else "Prebaci na Hrvatski",
                                    tint = Color.White
                                )
                            }

                            IconButton(
                                onClick = {
                                    val newTorchState = if (torchState.value == TorchState.ON) TorchState.OFF else TorchState.ON
                                    controller.enableTorch(newTorchState == TorchState.ON)
                                    torchState.value = newTorchState
                                }
                            ) {
                                Icon(
                                    imageVector = if (torchState.value == TorchState.ON) {
                                        Icons.Default.FlashOn
                                    } else {
                                        Icons.Default.FlashOff
                                    },
                                    contentDescription = "Toggle flash",
                                    tint = Color.White
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        scaffoldState.bottomSheetState.expand()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Photo,
                                    contentDescription = "Open gallery"
                                )
                            }
                            IconButton(
                                onClick = {
                                    takePhoto(
                                        controller = controller,
                                        onPhotoTaken = viewModel::onTakePhoto
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "Take photo"
                                )
                            }
                            IconButton(
                                onClick = {
                                    showManualEntryDialog = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Manual entry"
                                )
                            }
                        }

                        if (showManualEntryDialog) {
                            AlertDialog(
                                onDismissRequest = { showManualEntryDialog = false },
                                title = { Text(if (language == "hr") "Ručni unos koda" else "Enter Code Manually") },
                                text = {
                                    OutlinedTextField(
                                        value = manualCode,
                                        onValueChange = { manualCode = it },
                                        label = { Text(if (language == "hr") "Unesite godinu i tjedan (3 znamenke)" else "Enter Year and Week (3 digits)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            val pattern = """(\d{3})""".toRegex()
                                            if (pattern.matches(manualCode)) {
                                                val yearDigit = manualCode[0].toString()
                                                val weekNumber = manualCode.substring(1, 3).toInt()
                                                val year = "202$yearDigit"
                                                
                                                val date = TextAnalyzer.calculateDateFromYearAndWeek(year.toInt(), weekNumber)
                                                
                                                val placeholderCode = manualCode + "00000"
                                                
                                                val formatter = DateTimeFormatter.ofPattern("MM/yyyy")
                                                val expirationDate = date.plusMonths(30)
                                                val now = LocalDate.now()
                                                val isExpired = expirationDate.isBefore(now)
                                                
                                                val dateInfo = TextAnalyzer.Companion.DateInfo(
                                                    code = placeholderCode,
                                                    year = year,
                                                    weekNumber = weekNumber,
                                                    manufactureDate = date.format(formatter),
                                                    expirationDate = expirationDate.format(formatter),
                                                    isExpired = isExpired
                                                )
                                                
                                                val formattedText = TextAnalyzer.formatDateInfo(dateInfo, language)
                                                
                                                recognizedText.value = formattedText
                                                showManualEntryDialog = false
                                                manualCode = ""
                                            }
                                        }
                                    ) {
                                        Text(if (language == "hr") "Potvrdi" else "Confirm")
                                    }
                                },
                                dismissButton = {
                                    TextButton(
                                        onClick = { 
                                            showManualEntryDialog = false
                                            manualCode = ""
                                        }
                                    ) {
                                        Text(if (language == "hr") "Odustani" else "Cancel")
                                    }
                                }
                            )
                        }

                        if (recognizedText.value.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(16.dp)
                                    .background(
                                        color = Color.Black.copy(alpha = 0.6f),
                                        shape = MaterialTheme.shapes.medium
                                    )
                                    .padding(16.dp)
                            ) {
                                val lines = recognizedText.value.split("\n")
                                val lastLine = lines.lastOrNull()?.trim() ?: ""
                                val isExpired = lastLine.length >= 9 && 
                                              lastLine.startsWith("EXPIRED:") &&
                                              lastLine.contains("true")
                                val textLines = if (lines.isNotEmpty()) lines.dropLast(1) else emptyList()

                                Text(
                                    text = buildAnnotatedString {
                                        textLines.take(3).forEachIndexed { index, line ->
                                            withStyle(SpanStyle(fontSize = 14.sp, color = Color.White)) {
                                                append(line.takeIf { it.isNotEmpty() } ?: "-")
                                                if (index < 2) append("\n")
                                            }
                                        }
                                        
                                        if (textLines.size > 3) {
                                            append("\n")
                                            withStyle(SpanStyle(
                                                fontSize = 26.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isExpired) Color.Red else Color.White
                                            )) {
                                                append(textLines[3].takeIf { it.isNotEmpty() } ?: "-")
                                            }
                                        }
                                        
                                        if (textLines.size > 4) {
                                            append("\n")
                                            withStyle(SpanStyle(
                                                fontSize = 16.sp,
                                                color = Color.Red,
                                                fontWeight = FontWeight.Bold
                                            )) {
                                                append(if (isExpired) "Istekao rok trajanja" else textLines[4])
                                            }
                                        }
                                    },
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun takePhoto(
        controller: LifecycleCameraController,
        onPhotoTaken: (Bitmap) -> Unit
    ) {
        controller.takePicture(
            ContextCompat.getMainExecutor(applicationContext),
            object : OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)

                    val matrix = Matrix().apply {
                        postRotate(image.imageInfo.rotationDegrees.toFloat())
                    }
                    val rotatedBitmap = Bitmap.createBitmap(
                        image.toBitmap(),
                        0,
                        0,
                        image.width,
                        image.height,
                        matrix,
                        true
                    )

                    onPhotoTaken(rotatedBitmap)
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                    Log.e("Camera", "Couldn't take photo: ", exception)
                }
            }
        )
    }

    private fun hasRequiredPermissions(): Boolean {
        return CAMERAX_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                applicationContext,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    companion object {
        private val CAMERAX_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
        )
    }
}