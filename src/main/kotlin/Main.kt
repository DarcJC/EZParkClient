// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.material.MaterialTheme
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.bytedeco.javacv.CanvasFrame
import org.bytedeco.javacv.OpenCVFrameConverter
import org.opencv.core.Core
import pro.darc.park.utils.WebcamCapture
import pro.darc.park.utils.locatePlate
import org.bytedeco.opencv.global.opencv_imgcodecs
import org.bytedeco.opencv.opencv_core.IplImage
import org.opencv.core.Mat
import pro.darc.park.utils.mat2BuffedImage

@Composable
@Preview
fun App() {
    var text by remember { mutableStateOf("Hello, World!") }

    MaterialTheme {
        Button(onClick = {
            text = "Hello, Desktop!"
        }) {
            Text(text)
        }
    }
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "EZPark Client",
    ) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME)

        val webcam = WebcamCapture(deviceNumber = 0, framePerSecond = 1)
        val canvas = CanvasFrame("Webcam").apply {
            setCanvasSize(1024, 768)
        }
        val converter = OpenCVFrameConverter.ToIplImage()
        val converter2 = OpenCVFrameConverter.ToMat()

        webcam.afterUpdateHooks.add { _, eventArgs ->
            val result: IplImage = converter.convert(converter.convert(locatePlate(converter.convertToOrgOpenCvCoreMat(eventArgs.currentImage)))) ?: converter.convertToIplImage(eventArgs.currentImage)
            if (eventArgs.currentImage.image.size > 0L) canvas.showImage(
                mat2BuffedImage(converter2.convertToOrgOpenCvCoreMat(converter.convert(result)))
            )
        }

        webcam.run()
        App()
    }
}

class Main
