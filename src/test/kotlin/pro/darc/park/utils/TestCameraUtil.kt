package pro.darc.park.utils

import org.bytedeco.javacv.CanvasFrame
import org.bytedeco.javacv.OpenCVFrameConverter
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.opencv.core.Core

@DisplayName("CameraUtils Tests")
class TestCameraUtil {

    companion object {
        @JvmStatic lateinit var webcam: WebcamCapture
        private val canvas = CanvasFrame("Webcam").apply {
            setCanvasSize(1024, 768)
        }
        @JvmStatic private val converter = OpenCVFrameConverter.ToIplImage()

        @BeforeAll
        @JvmStatic fun init() {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
            webcam = WebcamCapture()
            webcam.afterUpdateHooks.add { _, eventArgs ->
                if (eventArgs.currentImage.image.size > 0L) canvas.showImage(converter.convert(locatePlate(converter.convertToOrgOpenCvCoreMat(eventArgs.currentImage))))
            }
        }
    }

    @DisplayName("Testing camera capture")
    @Test
    fun testCameraCapture() {
    }

}
