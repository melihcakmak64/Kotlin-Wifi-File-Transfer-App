package com.example.wififiletransfer.utils

import java.io.*
import java.util.zip.*

object ZipUtil {
    fun zipFiles(rootDir: File, relativePaths: List<String>, outputFile: File) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(outputFile))).use { zos ->
            relativePaths.forEach { relativePath ->
                val file = File(rootDir, relativePath)
                if (file.exists()) {
                    addFileToZip(zos, file, relativePath)
                }
            }
        }
    }

    private fun addFileToZip(zos: ZipOutputStream, file: File, entryName: String) {
        if (file.isDirectory) {
            file.listFiles()?.forEach {
                addFileToZip(zos, it, "$entryName/${it.name}")
            }
        } else {
            zos.putNextEntry(ZipEntry(entryName))
            file.inputStream().use { input ->
                input.copyTo(zos)
            }
            zos.closeEntry()
        }
    }
}
