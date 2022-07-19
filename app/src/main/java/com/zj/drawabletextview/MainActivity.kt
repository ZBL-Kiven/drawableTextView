package com.zj.drawabletextview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.zj.dtv.DrawableTextView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<DrawableTextView>(R.id.dtv)?.let {d->
            d.setOnBadgeClickListener {
                it.isSelected = !it.isSelected
            }
        }
    }
}