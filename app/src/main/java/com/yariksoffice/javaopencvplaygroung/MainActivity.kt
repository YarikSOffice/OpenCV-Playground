package com.yariksoffice.javaopencvplaygroung

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.Toast

import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.Picasso

import org.bytedeco.javacpp.opencv_stitching.Stitcher

import androidx.appcompat.app.AppCompatActivity
import com.yariksoffice.javaopencvplaygroung.StitcherOutput.Failure
import com.yariksoffice.javaopencvplaygroung.StitcherOutput.Success
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var radioGroup: RadioGroup

    private lateinit var imageStitcher: ImageStitcher
    private lateinit var disposable: Disposable

    private val stitcherInputRelay = PublishSubject.create<StitcherInput>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setUpViews()
        setUpStitcher()
    }

    private fun setUpViews() {
        imageView = findViewById(R.id.image)
        radioGroup = findViewById(R.id.radio_group)
        findViewById<View>(R.id.button).setOnClickListener { chooseImages() }
    }

    @Suppress("DEPRECATION")
    private fun setUpStitcher() {
        imageStitcher = ImageStitcher(FileUtil(applicationContext))
        val dialog = ProgressDialog(this).apply {
            setMessage(getString(R.string.processing_images))
            setCancelable(false)
        }

        disposable = stitcherInputRelay.switchMapSingle {
            imageStitcher.stitchImages(it)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe { dialog.show() }
                    .doOnSuccess { dialog.dismiss() }
        }
                .subscribe({ processResult(it) }, { processError(it) })
    }

    private fun chooseImages() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
                .setType(INTENT_IMAGE_TYPE)
                .putExtra(EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(intent, CHOOSE_IMAGES)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CHOOSE_IMAGES && resultCode == Activity.RESULT_OK && data != null) {
            val clipData = data.clipData
            val images = if (clipData != null) {
                List(clipData.itemCount) { clipData.getItemAt(it).uri }
            } else {
                listOf(data.data!!)
            }
            processImages(images)
        }
    }

    private fun processImages(uris: List<Uri>) {
        imageView.setImageDrawable(null) // reset preview
        val isScansChecked = radioGroup.checkedRadioButtonId == R.id.radio_scan
        val stitchMode = if (isScansChecked) Stitcher.SCANS else Stitcher.PANORAMA
        stitcherInputRelay.onNext(StitcherInput(uris, stitchMode))
    }

    private fun processError(e: Throwable) {
        Log.e(TAG, "", e)
        Toast.makeText(this, e.message + "", Toast.LENGTH_LONG).show()
    }

    private fun processResult(output: StitcherOutput) {
        when (output) {
            is Success -> showImage(output.file)
            is Failure -> processError(output.e)
        }
    }

    private fun showImage(file: File) {
        Picasso.with(this).load(file)
                .memoryPolicy(MemoryPolicy.NO_STORE, MemoryPolicy.NO_CACHE)
                .into(imageView)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
    }

    companion object {
        private const val TAG = "TAG"
        private const val EXTRA_ALLOW_MULTIPLE = "android.intent.extra.ALLOW_MULTIPLE"
        private const val INTENT_IMAGE_TYPE = "image/*"
        private const val CHOOSE_IMAGES = 777
    }
}
