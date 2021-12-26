package pro.darc.park.utils

import kotlinx.coroutines.*
import org.bytedeco.javacv.Frame
import org.bytedeco.javacv.OpenCVFrameConverter
import org.bytedeco.javacv.OpenCVFrameConverter.ToIplImage
import org.bytedeco.javacv.OpenCVFrameConverter.ToMat
import org.bytedeco.javacv.VideoInputFrameGrabber
import org.bytedeco.opencv.global.opencv_core.cvFlip
import org.bytedeco.opencv.opencv_core.IplImage
import org.bytedeco.opencv.opencv_core.Mat
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.measureTimeMillis

class WebcamCapture(
    private val framePerSecond: Int = 3,
    private val deviceNumber: Int = 0,
) {

    private val imageBuffer = arrayOf(Frame(), Frame()) // make buffer not null but empty first
    private var currentBufferIndex: Int = 0
    val isRunning = AtomicBoolean(false)

    private val currentImage: Frame get() = imageBuffer[currentBufferIndex]

    data class HookArgs(val currentImage: Frame): EventArgs
    val beforeUpdateHooks = EventHandler<HookArgs>()
    val afterUpdateHooks = EventHandler<HookArgs>()
    val beforeStopHooks = EventHandler<HookArgs>()


    private fun swapBuffer() {
        currentBufferIndex = (currentBufferIndex + 1) % 2
    }

    fun run() {
        isRunning.set(true)
        val grabber = VideoInputFrameGrabber(deviceNumber)
        val sleepDelta: Long = 1000 / framePerSecond.toLong()
        GlobalScope.launch(context = Dispatchers.IO) {
            grabber.start()
            while (isRunning.get()) {
                val elapsed = measureTimeMillis {
                    val frame = grabber.grab()
//                    cvFlip(tempImage, tempImage, 1) // re-flip the image
                    beforeUpdateHooks(this, HookArgs(currentImage))
                    imageBuffer[(currentBufferIndex + 1) % 2].close()
                    imageBuffer[(currentBufferIndex + 1) % 2] = frame
                    swapBuffer()
                    afterUpdateHooks(this, HookArgs(currentImage))
                }
                delay(maxOf(sleepDelta - elapsed, 0))
            }
            beforeStopHooks(this, HookArgs(currentImage))
            grabber.stop()
            this.cancel()
        }
    }

    fun stop() {
        isRunning.set(false)
    }
}

fun Mat.toIplImage(): IplImage? {
    val iplConverter = ToIplImage()
    val matConverter = ToMat()
    val frame: Frame = matConverter.convert(this)
    val img: IplImage = iplConverter.convert(frame)
    val result = img.clone()
    img.release()
    return result
}
