package com.yariksoffice.javaopencvplaygroung

import android.content.Context
import android.net.Uri
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class FileUtil(private val context: Context) {

    @Throws(IOException::class)
    fun urisToFiles(uris: List<Uri>): List<File> {
        val files = ArrayList<File>(uris.size)
        for (uri in uris) {
            val file = createTempFile()
            writeUriToFile(uri, file)
            files.add(file)
        }
        return files
    }

    @Throws(IOException::class)
    private fun createTempFile(): File {
        // don't need read/write permission for this directory starting from android 19
        val root = requirePicturesDirectory()
        root.mkdirs() // make sure that directory exists

        val date = SimpleDateFormat(DATE_FORMAT_TEMPLATE, Locale.getDefault()).format(Date())
        val filePrefix = IMAGE_NAME_TEMPLATE.format(date)
        return File.createTempFile(filePrefix, JPG_EXTENSION, root)
    }

    @Throws(IOException::class)
    private fun writeUriToFile(target: Uri, destination: File) {
        val inputStream = context.contentResolver.openInputStream(target)!!
        val outputStream = FileOutputStream(destination)
        inputStream.use { input ->
            outputStream.use { out ->
                input.copyTo(out)
            }
        }
    }

    fun createResultFile(): File {
        val pictures = requirePicturesDirectory()
        //noinspection ConstantConditions,ResultOfMethodCallIgnored
        pictures.mkdirs()
        return File("${pictures.absolutePath}$RESULT_FILE_NAME")
    }

    private fun requirePicturesDirectory(): File {
        return context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: throw IOException(
                "Can't access folder")
    }

    fun cleanUpWorkingDirectory() {
        deleteFile(requirePicturesDirectory())
    }

    // there is no build in function for deleting folders. <3
    private fun deleteFile(file: File) {
        if (file.isDirectory) {
            val entries = file.listFiles()
            if (entries != null) {
                for (entry in entries) {
                    deleteFile(entry)
                }
            }
        }
        file.delete()
    }

    companion object {
        private const val RESULT_FILE_NAME = "/result.jpg"
        private const val DATE_FORMAT_TEMPLATE = "yyyyMMdd_HHmmss"
        private const val IMAGE_NAME_TEMPLATE = "IMG_%s_"
        private const val JPG_EXTENSION = ".jpg"
    }
}
