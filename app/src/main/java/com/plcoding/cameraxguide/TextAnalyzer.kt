package com.plcoding.cameraxguide

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.time.format.DateTimeFormatter
import java.util.Locale

class TextAnalyzer(private val onTextFound: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    // Search for codes in the text
                    val matches = CODE_PATTERN.findAll(visionText.text)
                    
                    matches.forEach { match ->
                        val code = match.value
                        // Use the static methods for date calculations
                        val dateInfo = calculateDatesFromCode(code)
                        
                        val formattedText = buildString {
                            appendLine("Kod: ${dateInfo.code}")
                            appendLine("Godina: ${dateInfo.year} Tjedan: ${dateInfo.weekNumber}")
                            appendLine("Proizvedeno: ${dateInfo.manufactureDate}")
                            appendLine("Rok uporabe: ${dateInfo.expirationDate}")
                            if (dateInfo.isExpired) {
                                appendLine("Istekao rok trajanja!")
                            }
                            append("EXPIRED:${dateInfo.isExpired}") // This line will be used for color control
                        }.trimIndent()
                        onTextFound(formattedText)
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
    
    companion object {
        /**
         * Date information extracted from a product code
         */
        data class DateInfo(
            val code: String,
            val year: String,
            val weekNumber: Int,
            val manufactureDate: String,
            val expirationDate: String,
            val isExpired: Boolean
        )
        
        /**
         * Pattern to match product codes
         */
        val CODE_PATTERN = """(\d{8})[A-Z]{2}""".toRegex()
        
        /**
         * Pattern for manual code entry (without the characters after digits)
         */
        val MANUAL_CODE_PATTERN = """(\d{8})""".toRegex()
        
        /**
         * Calculates dates from a product code
         * @param code The product code (8 digits)
         * @return DateInfo object containing all date information
         */
        fun calculateDatesFromCode(code: String): DateInfo {
            // First digit is year (4 = 2024), next two digits are week (01-52)
            val yearDigit = code[0].toString()
            val weekNumber = code.substring(1, 3).toInt()
            val year = "202$yearDigit"
            
            // Calculate date from week number
            val date = calculateDateFromYearAndWeek(year.toInt(), weekNumber)
            
            // Calculate expiration date (30 months from manufacture date)
            val expirationDate = date.plusMonths(30)
            // Use numeric format for months (01-12)
            val formatter = DateTimeFormatter.ofPattern("MM/yyyy")
            
            val now = LocalDate.now()
            val isExpired = expirationDate.isBefore(now)
            
            return DateInfo(
                code = code,
                year = year,
                weekNumber = weekNumber,
                manufactureDate = date.format(formatter),
                expirationDate = expirationDate.format(formatter),
                isExpired = isExpired
            )
        }
        
        /**
         * Calculates a date from year and week number
         * @param year Full year (e.g., 2024)
         * @param weekNumber Week number (1-52)
         * @return LocalDate representing the first day of that week
         */
        fun calculateDateFromYearAndWeek(year: Int, weekNumber: Int): LocalDate {
            val weekFields = WeekFields.of(Locale.getDefault())
            return LocalDate.of(year, 1, 1)
                .with(weekFields.weekOfWeekBasedYear(), weekNumber.toLong())
        }
        
        /**
         * Formats the extracted information into a display string
         * @param dateInfo The date information
         * @param language The language to use ("hr" for Croatian, "en" for English)
         * @return Formatted text for display
         */
        fun formatDateInfo(dateInfo: DateInfo, language: String = "hr"): String {
            return if (language.equals("en", ignoreCase = true)) {
                // English format
                buildString {
                    appendLine("Code: ${dateInfo.code}")
                    appendLine("Year: ${dateInfo.year} Week: ${dateInfo.weekNumber}")
                    appendLine("Manufactured: ${dateInfo.manufactureDate}")
                    appendLine("Expiration date: ${dateInfo.expirationDate}")
                    if (dateInfo.isExpired) {
                        appendLine("Product has expired!")
                    }
                    append("EXPIRED:${dateInfo.isExpired}") // This line will be used for color control
                }.trimIndent()
            } else {
                // Croatian format (default)
                buildString {
                    appendLine("Kod: ${dateInfo.code}")
                    appendLine("Godina: ${dateInfo.year} Tjedan: ${dateInfo.weekNumber}")
                    appendLine("Proizvedeno: ${dateInfo.manufactureDate}")
                    appendLine("Rok uporabe: ${dateInfo.expirationDate}")
                    if (dateInfo.isExpired) {
                        appendLine("Istekao rok trajanja!")
                    }
                    append("EXPIRED:${dateInfo.isExpired}") // This line will be used for color control
                }.trimIndent()
            }
        }
    }
} 