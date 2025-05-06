package com.example.calc

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.io.File
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import android.graphics.Color
import android.util.Log
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация элементов интерфейса
        val lineChart = findViewById<LineChart>(R.id.lineChart)
        val initialAmountEditText = findViewById<EditText>(R.id.initialAmountEditText)
        val annualRateEditText = findViewById<EditText>(R.id.annualRateEditText)
        val yearsEditText = findViewById<EditText>(R.id.yearsEditText)
        val monthlyContributionEditText = findViewById<EditText>(R.id.monthlyContributionEditText)
        val calculateButton = findViewById<Button>(R.id.calculateButton)
        val resultTextView = findViewById<TextView>(R.id.resultTextView)
        val themeSwitch = findViewById<Switch>(R.id.themeSwitch)
        val exportPdfButton = findViewById<Button>(R.id.exportPdfButton)
        val exportCsvButton = findViewById<Button>(R.id.exportCsvButton)
        val exportExcelButton = findViewById<Button>(R.id.exportExcelButton)
        val clearButton = findViewById<Button>(R.id.clearButton)

        clearButton.setOnClickListener {
            initialAmountEditText.text.clear()
            annualRateEditText.text.clear()
            yearsEditText.text.clear()
            monthlyContributionEditText.text.clear()
            resultTextView.text = "Результат появится здесь"
            lineChart.clear()
            exportPdfButton.visibility = Button.GONE
        }

        exportCsvButton.setOnClickListener {
            val result = resultTextView.text.toString()
            exportToCSV(result)
        }

        exportExcelButton.setOnClickListener {
            val result = resultTextView.text.toString()
            exportToExcel(result)
        }


        // Настройка переключателя темы
        setupThemeSwitch(themeSwitch)

        // Обработчик кнопки "Рассчитать"
        calculateButton.setOnClickListener {
            val initialAmount = initialAmountEditText.text.toString().toDoubleOrNull() ?: 0.0
            val annualRate = annualRateEditText.text.toString().toDoubleOrNull() ?: 0.0
            val years = yearsEditText.text.toString().toIntOrNull() ?: 0
            val monthlyContribution = monthlyContributionEditText.text.toString().toDoubleOrNull() ?: 0.0

            Log.d("DEBUG", "initial=$initialAmount, rate=$annualRate, years=$years, monthly=$monthlyContribution")


            val finalAmount = calculateCompoundInterest(initialAmount, annualRate, years, monthlyContribution)
            val symbols = DecimalFormatSymbols(Locale.getDefault()).apply {
                groupingSeparator = ' '
                decimalSeparator = ','
            }
            val formatter = DecimalFormat("#,##0.00", symbols)
            val formattedAmount = formatter.format(finalAmount)
            resultTextView.text = "Итоговая сумма: $formattedAmount ₽"

            // Генерация данных для графика
            val dataPoints = generateGraphData(initialAmount, annualRate, years, monthlyContribution)
            displayGraph(lineChart, dataPoints)

            // Показать кнопку экспорта
            exportPdfButton.visibility = Button.VISIBLE
        }

        // Обработчик кнопки "Экспортировать в PDF"
        exportPdfButton.setOnClickListener {
            val resultText = resultTextView.text.toString()
            if (resultText.isNotBlank() && resultText != "Результат появится здесь") {
                exportToPdf(resultText)
            } else {
                Toast.makeText(this, "Нет данных для экспорта!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Расчёт сложных процентов
    private fun calculateCompoundInterest(
        initialAmount: Double,
        annualRate: Double,
        years: Int,
        monthlyContribution: Double
    ): Double {
        var totalAmount = initialAmount
        val monthlyRate = annualRate / 100 / 12
        val months = years * 12

        for (i in 1..months) {
            totalAmount += monthlyContribution
            totalAmount *= 1 + monthlyRate
        }

        return totalAmount
    }

    // Генерация данных для графика
    private fun generateGraphData(
        initialAmount: Double,
        annualRate: Double,
        years: Int,
        monthlyContribution: Double
    ): List<Entry> {
        val data = mutableListOf<Entry>()
        var totalAmount = initialAmount
        val monthlyRate = annualRate / 100 / 12
        val months = years * 12

        for (i in 0..months) {
            data.add(Entry(i.toFloat() / 12, totalAmount.toFloat()))
            totalAmount += monthlyContribution
            totalAmount *= 1 + monthlyRate
        }

        return data
    }

    // Отображение графика
    private fun displayGraph(lineChart: LineChart, dataPoints: List<Entry>) {
        val isDarkTheme = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES

        val dataSet = LineDataSet(dataPoints, "Прирост инвестиций").apply {
            color = if (isDarkTheme) Color.CYAN else Color.BLUE
            valueTextSize = 10f
            setDrawCircles(true)
            circleRadius = 3f
            setCircleColor(color)
            lineWidth = 2f
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawValues(false)
        }

        lineChart.apply {
            data = LineData(dataSet)
            description.text = "График роста инвестиций"
            description.textSize = 12f
            description.textColor = if (isDarkTheme) Color.WHITE else Color.BLACK

            xAxis.apply {
                textColor = if (isDarkTheme) Color.LTGRAY else Color.DKGRAY
                labelRotationAngle = -45f
                gridColor = if (isDarkTheme) Color.GRAY else Color.LTGRAY
            }

            axisLeft.apply {
                textColor = if (isDarkTheme) Color.LTGRAY else Color.DKGRAY
                gridColor = if (isDarkTheme) Color.GRAY else Color.LTGRAY
            }

            axisRight.isEnabled = false
            legend.isEnabled = false
            setBackgroundColor(if (isDarkTheme) Color.BLACK else Color.WHITE)
            invalidate()
        }
    }



    // Настройка переключателя темы
    private fun setupThemeSwitch(themeSwitch: Switch) {
        val sharedPreferences = getSharedPreferences("ThemePrefs", MODE_PRIVATE)
        val isDarkMode = sharedPreferences.getBoolean("isDarkMode", false)

        themeSwitch.isChecked = isDarkMode

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            val editor = sharedPreferences.edit()
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                editor.putBoolean("isDarkMode", true)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                editor.putBoolean("isDarkMode", false)
            }
            editor.apply()
        }
    }

    // Экспорт данных в PDF
    private fun exportToPdf(result: String) {
        val fileName = "InvestmentReport.pdf"
        val filePath = File(getExternalFilesDir(null), fileName)

        try {
            // Создаем PDF
            val pdfWriter = PdfWriter(filePath)
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument)

            // Добавляем текст в PDF
            document.add(Paragraph("Инвестиционный отчёт"))
            document.add(Paragraph("Результат: $result"))

            // Закрываем документ
            document.close()

            Toast.makeText(this, "PDF сохранён в ${filePath.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Ошибка создания PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Экспорт в CSV
    private fun exportToCSV(result: String) {
        val fileName = "InvestmentData.csv"
        val dir = getExternalFilesDir(null)

        if (dir == null) {
            Toast.makeText(this, "Невозможно получить доступ к хранилищу", Toast.LENGTH_LONG).show()
            return
        }

        val file = File(dir, fileName)

        try {
            val sanitizedResult = result.replace("\"", "\"\"") // экранирование кавычек
            file.writeText("Отчёт,Сумма\n\"Результат\",\"$sanitizedResult\"\n", Charsets.UTF_8)
            Toast.makeText(this, "CSV сохранён: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Ошибка при сохранении CSV: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }


    // Экспорт в Excel
    private fun exportToExcel(result: String) {
        val fileName = "InvestmentData.xls" // Excel откроет как таблицу
        val dir = getExternalFilesDir(null)

        if (dir == null) {
            Toast.makeText(this, "Невозможно получить доступ к хранилищу", Toast.LENGTH_LONG).show()
            return
        }

        val file = File(dir, fileName)

        try {
            val sanitizedResult = result.replace("\"", "\"\"")
            file.writeText("Отчёт\tСумма\nРезультат\t$sanitizedResult\n", Charsets.UTF_8)
            Toast.makeText(this, "Excel-файл сохранён: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Ошибка при сохранении Excel: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }



}
