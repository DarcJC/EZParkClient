// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.sourceforge.tess4j.ITessAPI
import net.sourceforge.tess4j.ITesseract
import net.sourceforge.tess4j.Tesseract
import org.bytedeco.javacv.CanvasFrame
import org.bytedeco.javacv.OpenCVFrameConverter
import org.bytedeco.opencv.opencv_core.IplImage
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import pro.darc.park.utils.*
import java.awt.FileDialog
import java.io.File


@Composable
@Preview
fun App() {
    var text by remember { mutableStateOf("Hello, World!") }
    var plateImage by remember { mutableStateOf(ImageBitmap(300, 200)) }
    var webcam: WebcamCapture = WebcamCapture(deviceNumber = 0, framePerSecond = 4)

    val tessApi = Tesseract().apply {
        setDatapath("D:/Codes/Github/DarcJC/EZParkClient/src/main/resources/tess")
        setLanguage("chi_sim+eng")
    }

    System.loadLibrary(Core.NATIVE_LIBRARY_NAME)

    MaterialTheme {
        Box {
            Column {
                Row {
                    Image(plateImage, "Preprocessed plate")
                    Text(text)
                }
                Button(
                    onClick = {
                        webcam.stop()
                        val dialog = FileDialog(ComposeWindow())
                        dialog.isVisible = true
                        val filename = "${dialog.directory}${dialog.file}"
                        val file = File(filename)
                        if (!file.exists()) return@Button
                        GlobalScope.launch(Dispatchers.IO) {
                            val result = detectPlateFromServer(file)
                            val matImg = Imgcodecs.imread(filename)
                            result.results.forEach {
                                text = it.license_plate_number
                                Imgproc.rectangle(
                                    matImg,
                                    Point(it.bound[0].toDouble(), it.bound[1].toDouble()),
                                    Point(it.bound[2].toDouble(), it.bound[3].toDouble()),
                                    Scalar(255.0, .0, 255.0)
                                )
                            }
                            plateImage = mat2BuffedImage(matImg)?.toComposeImageBitmap() ?: plateImage
                        }
                    }
                ) {
                    Text("图像")
                }
                Button(
                    onClick = {
                        webcam.stop()
                        webcam = WebcamCapture(deviceNumber = 0, framePerSecond = 4)
                        val converter = OpenCVFrameConverter.ToIplImage()
                        webcam.afterUpdateHooks.add { _, eventArgs ->
                            var result: Mat = converter.convertToOrgOpenCvCoreMat(eventArgs.currentImage)
                            locatePlate(converter.convertToOrgOpenCvCoreMat(eventArgs.currentImage))?.let {
                                result = it
                            }
                            plateImage = mat2BuffedImage(result)?.toComposeImageBitmap() ?: plateImage
                        }
                        webcam.run()
                    }
                ) {
                    Text("视频")
                }
            }
        }
    }

}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "EZPark Client",
    ) {
        App()
    }
}

class Main
