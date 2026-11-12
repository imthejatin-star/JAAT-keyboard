package com.jaat.keyboard

import android.inputmethodservice.InputMethodService
import android.view.*
import android.view.inputmethod.*
import android.content.*
import android.graphics.Typeface
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.*
import android.content.ClipData
import kotlin.math.abs

class KeyboardService : InputMethodService() {

    private val prefs by lazy { getSharedPreferences("jaat", MODE_PRIVATE) }

    private var hindi = false
    private var symbols = false
    private var caps = false
    private var currentWord = ""
    private var swipePath = ""
    private var swipeMoved = false

    private var suggestionRow: LinearLayout? = null
    private var keyViews = mutableListOf<Pair<View, String>>()

    private val dictionary = listOf(
        "about", "above", "after", "again", "always", "android",
        "application", "because", "beautiful", "before", "better",
        "build", "business", "can", "code", "computer", "create",
        "development", "different", "doing", "everything", "example",
        "financial", "first", "future", "good", "great", "hello",
        "help", "important", "information", "keyboard", "language",
        "learn", "learning", "make", "money", "more", "never",
        "people", "personal", "please", "project", "programming",
        "really", "should", "something", "system", "technology",
        "thank", "that", "their", "there", "these", "think",
        "this", "through", "today", "understand", "want", "what",
        "when", "where", "which", "with", "work", "world",
        "would", "you", "your", "yourself"
    )

    private val green = Color.rgb(85, 230, 165)
    private val blue = Color.rgb(80, 190, 255)
    private val amber = Color.rgb(255, 190, 90)
    private val bg = Color.rgb(8, 12, 16)
    private val keyBg = Color.rgb(22, 32, 39)

    private val accent: Int
        get() = when (prefs.getString("theme", "green")) {
            "blue" -> blue
            "amber" -> amber
            else -> green
        }

    override fun onCreateInputView(): View {
        currentWord = ""
        return buildKeyboard()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        currentWord = ""
        updateSuggestions()
    }

    private fun buildKeyboard(): View {
        keyViews.clear()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(5, 5, 5, 8)
            setBackgroundColor(bg)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val logo = label("JAAT", 14, accent)
        header.addView(logo, LinearLayout.LayoutParams(0, 40, 1f))

        listOf("⌘", "SYM", "EN/हि", "CLIP").forEach { item ->
            val b = smallButton(item)
            header.addView(b, LinearLayout.LayoutParams(
                if (item == "EN/हि") 58 else 48, 38
            ).apply { setMargins(2, 0, 2, 0) })

            b.setOnClickListener {
                when (item) {
                    "⌘" -> showWritingTools()
                    "SYM" -> {
                        symbols = !symbols
                        setInputView(buildKeyboard())
                    }
                    "EN/हि" -> {
                        hindi = !hindi
                        currentWord = ""
                        setInputView(buildKeyboard())
                    }
                    "CLIP" -> pasteClipboard()
                }
            }
        }

        root.addView(header)

        val suggestions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        suggestionRow = suggestions
        root.addView(suggestions, LinearLayout.LayoutParams(-1, 39))

        val rows = if (symbols) {
            listOf(
                listOf("1","2","3","4","5","6","7","8","9","0"),
                listOf("@","#","$","%","&","*","-","+","/","="),
                listOf("(",")","[","]","{","}","<",">","_","!"),
                listOf(":",";","'","\"","\\","|","~","`","?","⌫")
            )
        } else if (hindi) {
            listOf(
                listOf("क","ख","ग","घ","च","छ","ज","झ","ञ"),
                listOf("ट","ठ","ड","ढ","ण","त","थ","द","ध"),
                listOf("न","प","फ","ब","भ","म","य","र","ल"),
                listOf("व","श","ष","स","ह","ा","ि","ी","⌫")
            )
        } else {
            val first = if (caps) "QWERTYUIOP" else "qwertyuiop"
            val second = if (caps) "ASDFGHJKL" else "asdfghjkl"
            val third = if (caps) "ZXCVBNM" else "zxcvbnm"
            listOf(first.map { it.toString() }, second.map { it.toString() },
                listOf("⇧") + third.map { it.toString() } + listOf("⌫"))
        }

        rows.forEach { row ->
            val line = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }

            row.forEach { key ->
                val view = keyView(key)
                line.addView(view, LinearLayout.LayoutParams(
                    0, 47, 1f
                ).apply {
                    setMargins(2, 3, 2, 3)
                })

                if (key.length == 1 && key[0].isLetter()) {
                    keyViews.add(view to key)
                    attachSwipe(view)
                } else {
                    view.setOnClickListener { processKey(key) }
                }
            }

            root.addView(line)
        }

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val controls = listOf(
            "123" to 0.9f,
            "," to 0.6f,
            "SPACE" to 3.5f,
            "." to 0.6f,
            "ENTER" to 1.1f
        )

        controls.forEach { (key, weight) ->
            val v = keyView(key)
            bottom.addView(v, LinearLayout.LayoutParams(
                0, 48, weight
            ).apply { setMargins(2, 3, 2, 3) })
            v.setOnClickListener {
                when (key) {
                    "123" -> {
                        symbols = !symbols
                        setInputView(buildKeyboard())
                    }
                    "SPACE" -> processKey(" ")
                    "," -> processKey(",")
                    "." -> processKey(".")
                    "ENTER" -> processKey("ENTER")
                }
            }
        }

        root.addView(bottom)
        updateSuggestions()
        return root
    }

    private fun label(text: String, size: Int, color: Int) =
        TextView(this).apply {
            this.text = text
            textSize = size.toFloat()
            setTextColor(color)
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER_VERTICAL
            setPadding(8, 0, 8, 0)
        }

    private fun smallButton(text: String): TextView {
        return keyView(text).apply { textSize = 10f }
    }

    private fun keyView(text: String): TextView {
        val view = TextView(this).apply {
            this.text = text
            textSize = when (text) {
                "SPACE", "ENTER", "123" -> 11f
                else -> 17f
            }
            setTextColor(if (text in listOf("⌫","⇧","ENTER","SYM","CLIP","⌘"))
                accent else Color.WHITE)
            gravity = Gravity.CENTER
            typeface = Typeface.MONOSPACE
            background = GradientDrawable().apply {
                setColor(keyBg)
                cornerRadius = 8f
                setStroke(1, Color.rgb(42, 62, 69))
            }
        }
        return view
    }

    private fun attachSwipe(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    swipePath = (v as TextView).text.toString().lowercase()
                    swipeMoved = false
                    false
                }

                MotionEvent.ACTION_MOVE -> {
                    val x = event.rawX
                    val y = event.rawY
                    val found = keyViews.firstOrNull { (candidate, _) ->
                        val loc = IntArray(2)
                        candidate.getLocationOnScreen(loc)
                        x >= loc[0] && x <= loc[0] + candidate.width &&
                            y >= loc[1] && y <= loc[1] + candidate.height
                    }
                    if (found != null) {
                        val letter = found.second.lowercase()
                        if (swipePath.lastOrNull()?.toString() != letter) {
                            swipePath += letter
                            swipeMoved = true
                        }
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    if (swipeMoved && swipePath.length >= 3) {
                        val result = matchSwipe(swipePath)
                        if (result.isNotBlank()) {
                            currentInputConnection?.commitText(result, 1)
                            currentInputConnection?.commitText(" ", 1)
                            currentWord = ""
                            updateSuggestions()
                        }
                        true
                    } else {
                        processKey((v as TextView).text.toString())
                        true
                    }
                }

                else -> false
            }
        }
    }

    private fun matchSwipe(path: String): String {
        val matches = dictionary.filter { word ->
            var index = 0
            for (ch in word) {
                if (index < path.length && ch == path[index]) index++
            }
            index >= minOf(3, path.length - 1)
        }
        return matches.minByOrNull { abs(it.length - path.length) }
            ?: path
    }

    private fun processKey(key: String) {
        val ic = currentInputConnection ?: return

        when (key) {
            "⌫" -> {
                if (currentWord.isNotEmpty()) {
                    currentWord = currentWord.dropLast(1)
                }
                ic.deleteSurroundingText(1, 0)
            }

            "⇧" -> {
                caps = !caps
                setInputView(buildKeyboard())
            }

            " " -> {
                ic.commitText(" ", 1)
                currentWord = ""
            }

            "ENTER" -> {
                val action = currentInputEditorInfo?.imeOptions?.and(
                    EditorInfo.IME_MASK_ACTION
                ) ?: EditorInfo.IME_ACTION_NONE

                if (action != EditorInfo.IME_ACTION_NONE &&
                    action != EditorInfo.IME_ACTION_UNSPECIFIED) {
                    ic.performEditorAction(action)
                } else {
                    ic.commitText("\n", 1)
                }
                currentWord = ""
            }

            else -> {
                val actual = if (caps && !hindi && !symbols)
                    key.uppercase() else key
                ic.commitText(actual, 1)
                if (actual.length == 1 && actual[0].isLetter()) {
                    currentWord += actual.lowercase()
                } else {
                    currentWord = ""
                }
                updateSuggestions()
            }
        }
    }

    private fun updateSuggestions() {
        val row = suggestionRow ?: return
        row.removeAllViews()

        val options = if (currentWord.isBlank()) {
            listOf("JAAT", "SYM", "TOOLS")
        } else {
            dictionary.filter { it.startsWith(currentWord.lowercase()) }
                .take(3).ifEmpty { listOf(currentWord) }
        }

        options.forEach { word ->
            val suggestion = TextView(this).apply {
                text = word
                textSize = 13f
                setTextColor(accent)
                gravity = Gravity.CENTER
                typeface = Typeface.MONOSPACE
                setPadding(10, 0, 10, 0)
                setOnClickListener {
                    val ic = currentInputConnection ?: return@setOnClickListener
                    if (currentWord.isNotEmpty()) {
                        ic.deleteSurroundingText(currentWord.length, 0)
                    }
                    ic.commitText(word, 1)
                    currentWord = word
                }
            }
            row.addView(suggestion, LinearLayout.LayoutParams(0, 38, 1f))
        }
    }

    private fun pasteClipboard() {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        if (!clipboard.hasPrimaryClip()) return
        val clip = clipboard.primaryClip ?: return
        val item = clip.getItemAt(0)
        val text = item.coerceToText(this)?.toString() ?: return
        currentInputConnection?.commitText(text, 1)
        currentWord = ""
        updateSuggestions()
    }

    private fun showWritingTools() {
        val options = arrayOf(
            "CAPITALIZE SELECTED TEXT",
            "LOWERCASE SELECTED TEXT",
            "ADD FULL STOP",
            "INSERT ARROW  →",
            "INSERT CODE BLOCK"
        )

        android.app.AlertDialog.Builder(this)
            .setTitle("JAAT / WRITING TOOLS")
            .setItems(options) { _, which ->
                val ic = currentInputConnection ?: return@setItems
                when (which) {
                    0 -> transformSelection { it.uppercase() }
                    1 -> transformSelection { it.lowercase() }
                    2 -> ic.commitText(".", 1)
                    3 -> ic.commitText(" → ", 1)
                    4 -> ic.commitText("```\n\n```", 1)
                }
            }
            .setNegativeButton("CLOSE", null)
            .show()
    }

    private fun transformSelection(transform: (String) -> String) {
        val ic = currentInputConnection ?: return
        val selected = ic.getSelectedText(0)?.toString() ?: return
        if (selected.isNotEmpty()) {
            ic.commitText(transform(selected), 1)
        }
    }
}
