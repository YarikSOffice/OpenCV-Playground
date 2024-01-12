package com.yariksoffice.javaopencvplaygroung
import com.yariksoffice.javaopencvplaygroung.StitcherOutput.Failure
import com.yariksoffice.javaopencvplaygroung.StitcherOutput.Success
import org.bytedeco.javacpp.opencv_stitching.Stitcher

import android.Manifest

import android.app.Activity
import android.app.ProgressDialog

import android.content.ContentValues
import android.content.Intent

import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log

import android.view.View

import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast

import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.Picasso

import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.yariksoffice.javaopencvplaygroung.Constants.REQUEST_CODE_CAMERA_PERMISSION
import com.yariksoffice.javaopencvplaygroung.Constants.TAG
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.bytedeco.javacpp.opencv_stitching
import pub.devrel.easypermissions.EasyPermissions
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


typealias LumaListener = (luma: Double) -> Unit
private const val CHOOSE_PANORAMA_IMAGES = 778
class CameraActivity : AppCompatActivity(){
    private lateinit var cameraPreview: PreviewView
    private lateinit var captureButton: ImageButton
    private lateinit var panoramaButton: ImageButton
    private lateinit var editButton: ImageButton

    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    private lateinit var imageView: ImageView
    private lateinit var imageStitcher: ImageStitcher
    private lateinit var disposable: Disposable
    private val stitcherInputRelay = PublishSubject.create<StitcherInput>()

    private lateinit var progressDialog: ProgressDialog


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        cameraPreview = findViewById(R.id.cameraPreview)
        captureButton = findViewById(R.id.capture)
        panoramaButton = findViewById(R.id.panorama)
        editButton = findViewById(R.id.edit)

        // editButton'a tıklandığında EditActivity'ye geçiş yap
        editButton.setOnClickListener {
            // Open the CameraActivity when buttonapp is clicked
            val intent = Intent(this, EditActivity::class.java)
            startActivity(intent)
        }


        imageCapture = ImageCapture.Builder().build()

        // PANORAMA
        panoramaButton.setOnClickListener {
            openGalleryForPanorama()
        }
        imageStitcher = ImageStitcher(FileUtil(applicationContext))
        val dialog = ProgressDialog(this).apply {
            setMessage(getString(R.string.processing_images))
            setCancelable(false)
        }

        progressDialog = ProgressDialog(this).apply {
            setMessage(getString(R.string.processing_images))
            setCancelable(false)
        }

        disposable = stitcherInputRelay.switchMapSingle { input ->
            Single.fromCallable {
                val output = imageStitcher.stitchImages(input).blockingGet()
                progressDialog.dismiss()
                output
            }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
        }.subscribe({ processResult(it) }, { processError(it) })

        cameraPreview.post { startCamera() }
        captureButton.setOnClickListener { takePhoto() }
        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()

    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        val timestamp = System.currentTimeMillis()

        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, timestamp)
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")

        // Set up image capture listener, which is triggered after photo has been taken
        imageCapture.takePicture(
                ImageCapture.OutputFileOptions.Builder(
                        contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
        ).build(),
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                        Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {

                        val msg = "Photo capture succeeded: "
                        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                        Log.d(TAG, msg)
                    }
                })
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            try {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                // Set up image capture use case
                imageCapture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setTargetRotation(windowManager.defaultDisplay.rotation).build()

                // Set up preview use case
                val preview = Preview.Builder().build()
                        .also {
                            it.setSurfaceProvider(cameraPreview.surfaceProvider)
                        }

                // Set up image analysis use case if needed
                val imageAnalyzer = ImageAnalysis.Builder()
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
                                // Do something with the luma value
                                // Log.d(TAG, "Average luminosity: $luma")
                            })
                        }

                // Get a list of available cameras
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                //cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalyzer)
                // Request camera permissions
                if (EasyPermissions.hasPermissions(this, Manifest.permission.CAMERA)) {
                    // Permission granted, bind use cases to camera
                    //cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalyzer)
                    Log.e(TAG, "Binding successful: $camera")
                } else {
                    // Ask for camera permission
                    EasyPermissions.requestPermissions(
                            this,
                            "Camera permission is needed to take pictures.",
                            REQUEST_CODE_CAMERA_PERMISSION,
                            Manifest.permission.CAMERA
                    )
                }

            } catch (exc: Exception) {
                Log.e(TAG, "Error starting camera", exc)
                Toast.makeText(this, "Error starting camera: $exc", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider, cameraSelector: CameraSelector) {
        // Unbind use cases before rebinding
        cameraProvider.unbindAll()

        // Bind use cases to camera

        val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(cameraPreview.surfaceProvider)
                }

        val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
                        // Do something with the luma value
                        //Log.d(TAG, "Average luminosity: $luma")
                    })
                }

        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalyzer)
    }

    private fun getOutputDirectory(): File {
        val saveToGallery = true // Set this to true to save to gallery

        return if (saveToGallery) {
            // Create a new MediaStore content values object
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "${System.currentTimeMillis()}.jpg")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            }

            // Insert the content values into the MediaStore
            val resolver = contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            // Get the file path from the URI
            val photoFile = File(uri?.path!!)

            // Create the directory if it doesn't exist
            photoFile.mkdirs()

            photoFile
        } else {
            // Get the external media directory
            val mediaDir = externalMediaDirs.firstOrNull()?.let {
                File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
            }

            // Return the directory, or the device's files directory if it doesn't exist
            mediaDir ?: filesDir
        }
    }


    private fun openGalleryForPanorama() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
                .setType("image/*")
                .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(intent, CHOOSE_PANORAMA_IMAGES)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CHOOSE_PANORAMA_IMAGES && resultCode == Activity.RESULT_OK && data != null) {
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
        stitcherInputRelay.onNext(StitcherInput(uris, opencv_stitching.Stitcher.PANORAMA))
    }
    private fun processResult(output: StitcherOutput) {
        when (output) {
            is Success -> {
                Log.e(TAG, "cameraActivity ${Uri.fromFile(output.file)}")
                savePanoramaToGallery(output.file)
            }
            is Failure -> processError(output.e)
        }
    }

    // Panorama'nın sonucunu galeriye kaydetme fonksiyonu
    private fun savePanoramaToGallery(file: File) {
        // MediaStore üzerinden galeriye kaydetme işlemi
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }

        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)?.let { uri ->
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                java.io.FileInputStream(file).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            Toast.makeText(this, "Panorama saved to Gallery", Toast.LENGTH_SHORT).show()
        } ?: run {
            Toast.makeText(this, "Error saving Panorama to Gallery", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processError(e: Throwable) {
        Log.e(TAG, "", e)
        Toast.makeText(this, e.message + "", Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }


    private class LuminosityAnalyzer(private val listener: LumaListener) : ImageAnalysis.Analyzer {

        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Rewind the buffer to zero
            val data = ByteArray(remaining())
            get(data)   // Copy the buffer into a byte array
            return data // Return the byte array
        }

        override fun analyze(image: ImageProxy) {

            val buffer = image.planes[0].buffer
            val data = buffer.toByteArray()
            val pixels = data.map { it.toInt() and 0xFF }
            val luma = pixels.average()

            listener(luma)

            image.close()
        }
    }
    companion object {
        private const val TAG = "TAG"
        private const val EXTRA_ALLOW_MULTIPLE = "android.intent.extra.ALLOW_MULTIPLE"
        private const val INTENT_IMAGE_TYPE = "image/*"
        private const val CHOOSE_IMAGES = 777
    }
// class end line

}


