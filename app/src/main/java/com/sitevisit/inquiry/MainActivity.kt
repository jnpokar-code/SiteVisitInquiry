package com.sitevisit.inquiry

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.MediaStore
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {

    private val fields = linkedMapOf<String, EditText>()
    private val choices = linkedMapOf<String, String>()
    private val prefs by lazy {
        getSharedPreferences("site_visit_data", MODE_PRIVATE)
    }

    private val pdfRequest = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Site Visit Inquiry"
        buildForm()
    }

    private fun buildForm() {
        val scroll = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 24, 28, 32)
            setBackgroundColor(Color.WHITE)
        }

        scroll.addView(layout)
        setContentView(scroll)

        val heading = TextView(this).apply {
            text = "SITE VISIT INQUIRY"
            textSize = 25f
            setTextColor(Color.rgb(25, 75, 130))
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 24)
        }
        layout.addView(heading)

        addSection(layout, "1. CUSTOMER DETAIL")

        addTextField(layout, "Full Name")
        addTextField(layout, "Age", true)
        addTextField(layout, "Contact Number", true)
        addTextField(layout, "WhatsApp Number", true)
        addTextField(layout, "Job / Business")
        addTextField(layout, "Address", false, true)

        addSection(layout, "2. REQUIREMENT DETAIL")

        addChoice(layout, "Configuration", listOf("1 BHK", "2 BHK"))
        addTextField(layout, "Project Interested In")
        addTextField(layout, "Budget Range")
        addTextField(layout, "Loan Amount Required")

        addSection(layout, "3. CUSTOMER FEEDBACK")

        addChoice(layout, "Property Liked", listOf("Yes", "No"))
        addChoice(layout, "Interested for Booking", listOf("Yes", "No"))
        addTextField(layout, "Remark", false, true)

        addButton(layout, "SAVE INQUIRY") {
            saveInquiry()
        }

        addButton(layout, "VIEW SAVED INQUIRIES") {
            viewInquiries()
        }

        addButton(layout, "GENERATE PDF") {
            createPdf()
        }

        addButton(layout, "CLEAR FORM") {
            clearForm()
        }
    }

    private fun addSection(layout: LinearLayout, title: String) {
        val text = TextView(this).apply {
            this.text = title
            textSize = 19f
            setTextColor(Color.rgb(25, 75, 130))
            setPadding(0, 22, 0, 8)
        }
        layout.addView(text)
    }

    private fun addTextField(
        layout: LinearLayout,
        label: String,
        numeric: Boolean = false,
        multiline: Boolean = false
    ) {
        val input = EditText(this).apply {
            hint = label
            textSize = 16f
            setSingleLine(!multiline)

            if (numeric) {
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
            } else if (label == "Budget Range" ||
                label == "Loan Amount Required") {
                inputType =
                    android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            } else {
                inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            }

            if (multiline) {
                minLines = 2
                gravity = Gravity.TOP
            }

            setPadding(16, 12, 16, 12)
        }

        fields[label] = input
        layout.addView(input, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
    }

    private fun addChoice(
        layout: LinearLayout,
        label: String,
        options: List<String>
    ) {
        val title = TextView(this).apply {
            text = label
            textSize = 15f
            setTextColor(Color.DKGRAY)
            setPadding(0, 12, 0, 4)
        }
        layout.addView(title)

        val spinner = Spinner(this)
        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            options
        )
        layout.addView(spinner)

        choices[label] = options[0]

        spinner.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    choices[label] = options[position]
                }

                override fun onNothingSelected(
                    parent: android.widget.AdapterView<*>?
                ) {}
            }
    }

    private fun addButton(
        layout: LinearLayout,
        label: String,
        action: () -> Unit
    ) {
        val button = Button(this).apply {
            text = label
            isAllCaps = false
        }

        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 12, 0, 0)

        layout.addView(button, params)
        button.setOnClickListener { action() }
    }

    private fun currentInquiry(): JSONObject {
        val obj = JSONObject()

        fields.forEach { (label, input) ->
            obj.put(label, input.text.toString().trim())
        }

        choices.forEach { (label, value) ->
            obj.put(label, value)
        }

        return obj
    }

    private fun saveInquiry() {
        val inquiry = currentInquiry()

        if (inquiry.optString("Full Name").isBlank()) {
            fields["Full Name"]?.error = "Enter customer name"
            return
        }

        val all = try {
            JSONArray(prefs.getString("inquiries", "[]"))
        } catch (_: Exception) {
            JSONArray()
        }

        all.put(inquiry)

        prefs.edit()
            .putString("inquiries", all.toString())
            .apply()

        Toast.makeText(
            this,
            "Inquiry saved successfully",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun viewInquiries() {
        val all = JSONArray(prefs.getString("inquiries", "[]"))

        if (all.length() == 0) {
            Toast.makeText(this, "No saved inquiries", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val items = Array(all.length()) { index ->
            val obj = all.getJSONObject(index)
            "${index + 1}. ${obj.optString("Full Name")} - " +
                obj.optString("Contact Number")
        }

        AlertDialog.Builder(this)
            .setTitle("Saved Inquiries")
            .setItems(items) { _, index ->
                val obj = all.getJSONObject(index)
                val details = obj.keys().asSequence().joinToString("\n") {
                    "$it: ${obj.optString(it)}"
                }

                AlertDialog.Builder(this)
                    .setTitle(obj.optString("Full Name"))
                    .setMessage(details)
                    .setPositiveButton("OK", null)
                    .show()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun createPdf() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(
                Intent.EXTRA_TITLE,
                "SiteVisitInquiry.pdf"
            )
        }

        startActivityForResult(intent, pdfRequest)
    }

    @Deprecated("Uses the document picker to save the PDF")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == pdfRequest &&
            resultCode == RESULT_OK &&
            data?.data != null) {
            writePdf(data.data!!)
        }
    }

    private fun writePdf(uri: Uri) {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 42f
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
        }

        val lines = mutableListOf<Pair<String, Boolean>>()
        lines.add("SITE VISIT INQUIRY FORM" to true)
        lines.add("" to false)
        lines.add("CUSTOMER DETAIL" to true)

        fields.forEach { (label, input) ->
            if (label in listOf(
                    "Full Name", "Age", "Contact Number",
                    "WhatsApp Number", "Job / Business", "Address"
                )) {
                lines.add("$label: ${input.text}" to false)
            }
        }

        lines.add("" to false)
        lines.add("REQUIREMENT DETAIL" to true)

        fields.forEach { (label, input) ->
            if (label in listOf(
                    "Project Interested In",
                    "Budget Range",
                    "Loan Amount Required"
                )) {
                lines.add("$label: ${input.text}" to false)
            }
        }

        choices.forEach { (label, value) ->
            if (label == "Configuration") {
                lines.add("$label: $value" to false)
            }
        }

        lines.add("" to false)
        lines.add("CUSTOMER FEEDBACK" to true)

        choices.forEach { (label, value) ->
            if (label != "Configuration") {
                lines.add("$label: $value" to false)
            }
        }

        fields["Remark"]?.let {
            lines.add("Remark: ${it.text}" to false)
        }

        try {
            var pageNumber = 1
            var page = document.startPage(
                PdfDocument.PageInfo.Builder(
                    pageWidth, pageHeight, pageNumber
                ).create()
            )
            var canvas = page.canvas
            var y = 55f

            for ((text, bold) in lines) {
                if (y > pageHeight - 45f) {
                    document.finishPage(page)
                    pageNumber++
                    page = document.startPage(
                        PdfDocument.PageInfo.Builder(
                            pageWidth, pageHeight, pageNumber
                        ).create()
                    )
                    canvas = page.canvas
                    y = 55f
                }

                paint.textSize = if (bold) 15f else 11f
                paint.isFakeBoldText = bold

                val maxWidth = pageWidth - margin * 2
                val chunks = if (text.isEmpty()) {
                    listOf("")
                } else {
                    text.chunked(75)
                }

                for (chunk in chunks) {
                    canvas.drawText(chunk, margin, y, paint)
                    y += if (bold) 25f else 20f
                }
            }

            document.finishPage(page)

            contentResolver.openOutputStream(uri)?.use { output ->
                document.writeTo(output)
            } ?: throw Exception("Unable to open PDF destination")

            Toast.makeText(
                this,
                "PDF created. Open it to print or share.",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {
            Toast.makeText(
                this,
                "PDF error: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        } finally {
            document.close()
        }
    }

    private fun clearForm() {
        fields.values.forEach { it.text.clear() }

        Toast.makeText(
            this,
            "Form cleared",
            Toast.LENGTH_SHORT
        ).show()
    }
}
