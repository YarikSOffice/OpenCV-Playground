package com.yariksoffice.javaopencvplaygroung
import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
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
import pub.devrel.easypermissions.EasyPermissions
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.github.chrisbanes.photoview.PhotoView
import com.mukesh.image_processing.ImageProcessor
import android.os.AsyncTask


import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.Picasso

class EditActivity : AppCompatActivity() {
    // on below line we are creating variables.
    lateinit var oneIV: ImageView
    lateinit var twoIV: ImageView
    lateinit var threeIV: ImageView
    lateinit var fourIV: ImageView
    lateinit var fiveIV: ImageView
    lateinit var sixIV: ImageView
    lateinit var sevenIV: ImageView
    lateinit var eightIV: ImageView
    lateinit var tenIV: ImageView
    lateinit var image_con: ImageView

    lateinit var bmp: Bitmap
    lateinit var onebmp: Bitmap
    lateinit var twobmp: Bitmap
    lateinit var threebmp: Bitmap
    lateinit var fourbmp: Bitmap
    lateinit var fivebmp: Bitmap
    lateinit var sixbmp: Bitmap
    lateinit var sevenbmp: Bitmap
    lateinit var eightbmp: Bitmap
    lateinit var tenbmp: Bitmap

    private lateinit var galeryButton: ImageButton

    private val CHOOSE_IMAGE = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)
        image_con = findViewById(R.id.image);
        image_con = findViewById(R.id.image);
        galeryButton = findViewById(R.id.gallery)

        val processor = ImageProcessor()
        bmp = BitmapFactory.decodeResource(resources, R.drawable.flower)

        galeryButton.setOnClickListener {
            openGallery()
        }


        // on below line we are initializing
        // our variable with their ids.
        oneIV = findViewById(R.id.idIVOne);
        twoIV = findViewById(R.id.idIVTwo);
        threeIV = findViewById(R.id.idIVThree);
        fourIV = findViewById(R.id.idIVFour);
        fiveIV = findViewById(R.id.idIVFive);
        sixIV = findViewById(R.id.idIVSix);
        sevenIV = findViewById(R.id.idIVSeven);
        eightIV = findViewById(R.id.idIVEight);
        tenIV = findViewById(R.id.idIVTen);


        // below line is use to add tint effect to our original
        // image bitmap and storing that in one bitmap.
        onebmp = processor.tintImage(bmp, 90)

        // after storing it to one bitmap
        // we are setting it to imageview.
        oneIV.setImageBitmap(onebmp)

        // below line is use to apply gaussian blur effect
        // to our original image bitmap.
        twobmp = processor.applyGaussianBlur(bmp);
        twoIV.setImageBitmap(twobmp);

        // below line is use to add sepia toning effect
        // to our original image bitmap.
        threebmp = processor.createSepiaToningEffect(bmp, 1, 2.0, 1.0, 5.0);
        threeIV.setImageBitmap(threebmp);

        // below line is use to apply saturation
        // filter to our original image bitmap.
        fourbmp = processor.applySaturationFilter(bmp, 3);
        fourIV.setImageBitmap(fourbmp);

        // below line is use to apply snow effect
        // to our original image bitmap.
        fivebmp = processor.applySnowEffect(bmp);
        fiveIV.setImageBitmap(fivebmp);

        // below line is use to add gray scale
        // to our image view.
        sixbmp = processor.doGreyScale(bmp);
        sixIV.setImageBitmap(sixbmp);

        // below line is use to add engrave effect
        // to our image view.
        sevenbmp = processor.engrave(bmp);
        sevenIV.setImageBitmap(sevenbmp);

        // below line is use to create a contrast
        // effect to our image view.
        eightbmp = processor.createContrast(bmp, 1.5);
        eightIV.setImageBitmap(eightbmp);

        // below line is use to add shadow effect
        // to our original bitmap

        // below line is use to add flea
        // effect to our image view.
        tenbmp = processor.applyFleaEffect(bmp);
        tenIV.setImageBitmap(tenbmp);

        // on below line we are adding click listeners
        // for all image views.
        oneIV.setOnClickListener {
            // on clicking on each filter we are
            // setting that filter to our original image.
            image_con.setImageBitmap(onebmp)
        }

        twoIV.setOnClickListener {
            // on clicking on each filter we are
            // setting that filter to our original image.
            image_con.setImageBitmap(twobmp)
        }

        threeIV.setOnClickListener {
            // on clicking on each filter we are
            // setting that filter to our original image.
            image_con.setImageBitmap(threebmp)
        }
        fourIV.setOnClickListener {
            // on clicking on each filter we are
            // setting that filter to our original image.
            image_con.setImageBitmap(fourbmp)
        }
        fiveIV.setOnClickListener {
            // on clicking on each filter we are
            // setting that filter to our original image.
            image_con.setImageBitmap(fivebmp)
        }
        sixIV.setOnClickListener {
            // on clicking on each filter we are
            // setting that filter to our original image.
            image_con.setImageBitmap(sixbmp)
        }
        sevenIV.setOnClickListener {
            // on clicking on each filter we are
            // setting that filter to our original image.
            image_con.setImageBitmap(sevenbmp)
        }
        eightIV.setOnClickListener {
            // on clicking on each filter we are
            // setting that filter to our original image.
            image_con.setImageBitmap(eightbmp)
        }


        tenIV.setOnClickListener {
            // on clicking on each filter we are
            // setting that filter to our original image.
            image_con.setImageBitmap(tenbmp)
        }


    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
                .setType("image/*")
        startActivityForResult(intent, CHOOSE_IMAGE)
    }
    // startActivityForResult sonuç işlemi
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CHOOSE_IMAGE && resultCode == android.app.Activity.RESULT_OK && data != null) {
            val selectedImageUri = data.data
            processSelectedImage(selectedImageUri)
        }
    }

    // Seçilen resmi işleme
    private fun processSelectedImage(uri: Uri?) {
        if (uri != null) {
            // Load the selected image
            val selectedBitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
            image_con.setImageBitmap(selectedBitmap)
            bmp = selectedBitmap
        } else {
            Toast.makeText(this, "Error loading selected image", Toast.LENGTH_SHORT).show()
            return
        }

        // Filtreleri uygula, sadece image_con üzerinde işlem yap
        val processor = ImageProcessor()

        onebmp = processor.tintImage(bmp, 90)
        twoIV.setImageBitmap(onebmp)

        twobmp = processor.applyGaussianBlur(bmp)
        twoIV.setImageBitmap(twobmp)

        threebmp = processor.createSepiaToningEffect(bmp, 1, 2.0, 1.0, 5.0)
        threeIV.setImageBitmap(threebmp)

        fourbmp = processor.applySaturationFilter(bmp, 3)
        fourIV.setImageBitmap(fourbmp)

        fivebmp = processor.applySnowEffect(bmp)
        fiveIV.setImageBitmap(fivebmp)

        sixbmp = processor.doGreyScale(bmp)
        sixIV.setImageBitmap(sixbmp)

        sevenbmp = processor.engrave(bmp)
        sevenIV.setImageBitmap(sevenbmp)

        eightbmp = processor.createContrast(bmp, 1.5)
        eightIV.setImageBitmap(eightbmp)

        tenbmp = processor.applyFleaEffect(bmp)
        tenIV.setImageBitmap(tenbmp)
    }





}



