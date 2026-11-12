package com.jaat.keyboard

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.provider.Settings
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.*

class MainActivity : Activity() {

    private val green = 0xFF55E6A5.toInt()
    private val muted = 0xFF91A39C.toInt()
    private val bg = 0xFF080C10.toInt()
    private val panel = 0xFF111A20.toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    private fun render() {
        val prefs = getSharedPreferences("jaat", MODE_PRIVATE)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 30, 22, 24)
            setBackgroundColor(bg)
        }

        fun text(value: String, size: Float, color: Int) =
            TextView(this).apply {
                this.text = value
                textSize = size
                setTextColor(color)
                typeface = android.graphics.Typeface.MONOSPACE
            }

        root.addView(text("JAAT / KEYBOARD", 25f, green))
        root.addView(text("PERSONAL INPUT SYSTEM  •  VERSION 1.0", 10f, muted))
        root.addView(Space(this), LinearLayout.LayoutParams(1, 24))

        root.addView(text("SYSTEM STATUS", 13f, green))
        root.addView(text("Native Android keyboard engine", 15f, -1))
        root.addView(text("Offline-first • Local preferences • No account required", 11f, muted))

        root.addView(Space(this), LinearLayout.LayoutParams(1, 25))
        root.addView(text("APPEARANCE", 14f, green))

        val themes = listOf(
            "Hacker Green" to "green",
            "Cyber Blue" to "blue",
            "Amber Terminal" to "amber"
        )

        val group = RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
        }

        themes.forEach { (label, value) ->
            val radio = RadioButton(this).apply {
                text = label
                textSize = 15f
                setTextColor(-1)
                isChecked = prefs.getString("theme", "green") == value
                setOnClickListener {
                    prefs.edit().putString("theme", value).apply()
                }
            }
            group.addView(radio)
        }
        root.addView(group)

        root.addView(Space(this), LinearLayout.LayoutParams(1, 22))
        root.addView(text("ACTIVATION", 14f, green))

        fun action(label: String, callback: () -> Unit) {
            val button = Button(this).apply {
                text = label
                textSize = 13f
                setTextColor(green)
                setBackgroundColor(panel)
                isAllCaps = false
                setOnClickListener { callback() }
            }
            root.addView(button, LinearLayout.LayoutParams(-1, 54).apply {
                bottomMargin = 9
            })
        }

        action("01  /  Enable JAAT Keyboard") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }

        action("02  /  Select JAAT Keyboard") {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }

        action("03  /  Test Keyboard") {
            startActivity(Intent(this, TestActivity::class.java))
        }

        root.addView(Space(this), LinearLayout.LayoutParams(1, 15))
        root.addView(text("PRIVACY", 13f, green))
        root.addView(text(
            "JAAT does not upload typed text. Clipboard content is accessed only when you tap its button. Avoid entering passwords into untrusted apps.",
            12f, muted
        ))

        setContentView(ScrollView(this).apply { addView(root) })
    }
}

class TestActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 30, 20, 20)
            setBackgroundColor(0xFF080C10.toInt())
        }

        root.addView(TextView(this).apply {
            text = "JAAT / INPUT TEST"
            textSize = 22f
            setTextColor(0xFF55E6A5.toInt())
        })

        root.addView(EditText(this).apply {
            hint = "Type something here..."
            textSize = 17f
            setTextColor(-1)
            setHintTextColor(0xFF71847D.toInt())
            setSingleLine(false)
            minLines = 4
            setBackgroundColor(0xFF18232A.toInt())
            setPadding(12, 12, 12, 12)
        }, LinearLayout.LayoutParams(-1, 180))

        setContentView(root)
    }
}
