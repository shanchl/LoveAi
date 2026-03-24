package com.loveai.ui

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.loveai.R
import java.io.File

class CoverPreviewActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_IMAGE_PATH = "image_path"
        const val EXTRA_TITLE = "title"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cover_preview)

        val tvTitle = findViewById<TextView>(R.id.tvCoverPreviewTitle)
        val ivCover = findViewById<ImageView>(R.id.ivCoverPreview)
        findViewById<Button>(R.id.btnBackCoverPreview).setOnClickListener { finish() }

        tvTitle.text = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH)
        val bitmap = imagePath?.let { path ->
            runCatching { BitmapFactory.decodeFile(File(path).absolutePath) }.getOrNull()
        }
        if (bitmap != null) {
            ivCover.setImageBitmap(bitmap)
        }
    }
}
