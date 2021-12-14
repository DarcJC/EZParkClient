// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import net.sourceforge.tess4j.ITessAPI
import net.sourceforge.tess4j.ITesseract
import net.sourceforge.tess4j.Tesseract
import org.bytedeco.javacv.CanvasFrame
import org.bytedeco.javacv.OpenCVFrameConverter
import org.bytedeco.opencv.opencv_core.IplImage
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import pro.darc.park.utils.WebcamCapture
import pro.darc.park.utils.locatePlate
import pro.darc.park.utils.mat2BuffedImage


@Composable
@Preview
fun App() {
    var text by remember { mutableStateOf("Hello, World!") }
    var plateImage by remember { mutableStateOf(ImageBitmap(300, 200)) }
    val webcam = WebcamCapture(deviceNumber = 0, framePerSecond = 4)

    MaterialTheme {
        Image(plateImage, "Preprocessed plate")
        Button(onClick = {
            when {
                webcam.isRunning.get() -> webcam.stop()
                else -> webcam.run()
            }
        }) {
            Text(text)
        }
    }

    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

//    val tessApi = TessBaseAPI().apply {
////            println(Init(Main::class.java.getResource("/tess")!!.path, "eng"))
//        if (Init("D:/Codes/Github/DarcJC/EZParkClient/src/main/resources/tess", "eng+chi_sim") != 0) {
//            throw RuntimeException("Could not initialize tess.")
//        }
//    }
    val tessApi = Tesseract().apply {
        setDatapath("D:/Codes/Github/DarcJC/EZParkClient/src/main/resources/tess")
        setLanguage("eng+chi_sim")
    }

//    val canvas = CanvasFrame("Webcam").apply {
//        setCanvasSize(1024, 768)
//    }
    val converter = OpenCVFrameConverter.ToIplImage()
    val converter2 = OpenCVFrameConverter.ToMat()

    webcam.afterUpdateHooks.add { _, eventArgs ->
        var result: Mat = converter.convertToOrgOpenCvCoreMat(eventArgs.currentImage)
        locatePlate(converter.convertToOrgOpenCvCoreMat(eventArgs.currentImage))?.let {
            Imgproc.blur(it, it, Size(5.0, 5.0))
            Imgproc.cvtColor(it, it, Imgproc.COLOR_BGR2GRAY)
            Imgproc.Laplacian(it, it, it.depth(), 3)
            Imgproc.threshold(it, it, .0, 255.0, Imgproc.THRESH_OTSU + Imgproc.THRESH_BINARY)
            result = it
            val img = mat2BuffedImage(result)
            text = tessApi.doOCR(img)
        }
        plateImage = mat2BuffedImage(result)?.toComposeImageBitmap() ?: plateImage
//        if (eventArgs.currentImage.image.size > 0L) canvas.showImage(
//            mat2BuffedImage(converter2.convertToOrgOpenCvCoreMat(converter.convert(result)))
//        )
    }

    webcam.run()
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
