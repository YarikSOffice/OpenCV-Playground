package com.yariksoffice.javaopencvplaygroung

import android.net.Uri
import android.util.Log

import org.bytedeco.javacpp.opencv_core.Mat
import org.bytedeco.javacpp.opencv_core.MatVector
import org.bytedeco.javacpp.opencv_stitching.Stitcher
import org.bytedeco.javacpp.opencv_imgcodecs.imread
import org.bytedeco.javacpp.opencv_imgcodecs.imwrite
import org.bytedeco.javacpp.opencv_imgproc.cvtColor
import org.bytedeco.javacpp.opencv_imgproc.resize

import java.io.File

import io.reactivex.Single
import org.bytedeco.javacpp.opencv_core
import org.bytedeco.javacpp.opencv_imgcodecs.imread

import org.bytedeco.javacpp.opencv_imgcodecs.imwrite
import org.bytedeco.javacpp.opencv_stitching.Stitcher.ERR_CAMERA_PARAMS_ADJUST_FAIL
import org.bytedeco.javacpp.opencv_stitching.Stitcher.ERR_HOMOGRAPHY_EST_FAIL
import org.bytedeco.javacpp.opencv_stitching.Stitcher.ERR_NEED_MORE_IMGS
import org.opencv.imgproc.Imgproc
import java.lang.Exception

class StitcherInput(val uris: List<Uri>, val stitchMode: Int)

sealed class StitcherOutput {
    class Success(val file: File) : StitcherOutput()
    class Failure(val e: Exception) : StitcherOutput()
}

class ImageStitcher(private val fileUtil: FileUtil) {
    fun stitchImages(input: StitcherInput): Single<StitcherOutput> {
        return Single.fromCallable {
            val files = fileUtil.urisToFiles(input.uris)
            val vector = filesToMatVector(files)
            stitch(vector, input.stitchMode)// return 1
        }
    }

    private fun stitch(vector: MatVector, stitchMode: Int): StitcherOutput {
        val result = Mat()
        val stitcher = Stitcher.create(stitchMode)
        Log.e(Constants.TAG, "stiching basladi")

        val status = stitcher.stitch(vector, result)
        Log.e(Constants.TAG, "stiching bitti")
        Log.e(Constants.TAG, "status: ${status}")
        Log.e(Constants.TAG, "Stitcher.OK: ${Stitcher.OK}")
        Log.e(Constants.TAG, "vector: ${vector}")
        Log.e(Constants.TAG, "result: ${result}")


        fileUtil.cleanUpWorkingDirectory()
        return if (status == Stitcher.OK) {
            Log.e(Constants.TAG, "BURADA")
            val resultFile = fileUtil.createResultFile()
            imwrite(resultFile.absolutePath, result)
            StitcherOutput.Success(resultFile)
        } else {
            val e = RuntimeException("Can't stitch images: " + getStatusDescription(status))
            StitcherOutput.Failure(e)
        }
    }

    @Suppress("SpellCheckingInspection")
    private fun getStatusDescription(status: Int): String {
        return when (status) {
            ERR_NEED_MORE_IMGS -> "ERR_NEED_MORE_IMGS"
            ERR_HOMOGRAPHY_EST_FAIL -> "ERR_HOMOGRAPHY_EST_FAIL"
            ERR_CAMERA_PARAMS_ADJUST_FAIL -> "ERR_CAMERA_PARAMS_ADJUST_FAIL"
            else -> "UNKNOWN"
        }
    }

    private fun filesToMatVector(files: List<File>): MatVector {
        val images = MatVector(files.size.toLong())
        for (i in files.indices) {
            var img = imread(files[i].absolutePath)

            images.put(i.toLong(), img)
        }
        Log.e(Constants.TAG, "transformation to mat list bitti")
        return images
    }
}
