package pro.darc.park.utils

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.bytedeco.javacv.Java2DFrameUtils
import org.bytedeco.javacv.OpenCVFrameConverter
import org.bytedeco.opencv.opencv_core.IplImage
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileInputStream
import java.util.*

fun Mat.max(): Double {
    var res = Double.MIN_VALUE;
    for (i in 0..width()) {
        for (j in 0..height()) {
            if ((get(i, j)?.maxOrNull() ?: Double.MIN_VALUE) > res) res = get(i, j).maxOrNull()!!
        }
    }
    return res
}

fun Mat.min(): Double {
    var res = Double.MAX_VALUE;
    for (i in 0..width()) {
        for (j in 0..height()) {
            if ((get(i, j)?.maxOrNull() ?: Double.MAX_VALUE) < res) res = get(i, j).maxOrNull()!!
        }
    }
    return res
}

fun Mat.stretch(): Mat {
    val max = this.max()
    val min = this.min()

    for (i in 0..width()) {
        for (j in 0..height()) {
            this[i, j]?.forEachIndexed { index, d ->
                this[i, j][index] = (255 / (max - min)) * d - (255 * min) / (max - min)
            }
        }
    }
    return this
}

fun Mat.binary(): Mat {
    val max = this.max()
    val min = this.min()

    val x = max - ((max - min) / 1.5)
    Imgproc.threshold(this, this, x, 255.0, Imgproc.THRESH_BINARY)
    return this
}

fun locatePlate(frame: Mat): Mat? {
    if (frame.empty()) return null
    val ratio = frame.width() / frame.height().coerceAtLeast(1) // 防止除0
    Imgproc.resize(frame, frame, Size(600.0, (600 * ratio).toDouble())) // Image compress
    val rawFrame = frame.clone()

    val classifier = CascadeClassifier("D:/Codes/Github/DarcJC/EZParkClient/src/main/resources/cascades/plate.xml")
    val detectedObjects = MatOfRect()
    classifier.detectMultiScale(frame, detectedObjects)

    val arr = detectedObjects.toArray()
    println("${arr.size} plate(s) detected")

//    detectedObjects.toArray().forEach {
//        Imgproc.rectangle(rawFrame, it, Scalar(255.0, .0, 255.0))
//    }

    return if (arr.isNotEmpty()) postProcess(Mat(rawFrame, detectedObjects.toArray().first()))
        else null
}

fun locatePlate1(frame: Mat): Mat? {
    if (frame.empty()) return null
    val ratio = frame.width() / frame.height().coerceAtLeast(1) // 防止除0
    Imgproc.resize(frame, frame, Size(400.0, (400 * ratio).toDouble())) // Image compress
    val rawFrame = frame.clone()
    Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY) // convert to gray image
    frame.stretch()

    val kernel = Mat.zeros(33, 33, 0)
    val openImg = Mat().apply {
        Imgproc.morphologyEx(frame, this, Imgproc.MORPH_OPEN, kernel)
    }
    val strtImg = openImg.apply {
        Core.absdiff(frame, openImg, this)
//        Imgproc.Laplacian(openImg, this, openImg.depth())
        binary()
        Imgproc.Canny(this, this, 70.0, 255.0)
        val kernel2 = Mat.ones(10, 19, 0)
        Imgproc.morphologyEx(this, this, Imgproc.MORPH_CLOSE, kernel2)
        Imgproc.morphologyEx(this, this, Imgproc.MORPH_OPEN, kernel2)
        val kernel3 = Mat.ones(11, 5, 0)
        Imgproc.morphologyEx(this, this, Imgproc.MORPH_OPEN, kernel3)
    }

    val resList = mutableListOf<MatOfPoint>()
    Imgproc.findContours(strtImg, resList, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

    resList.sortBy { it.size().area() }

    resList.forEachIndexed { _, d ->
        Imgproc.rectangle(rawFrame, Imgproc.boundingRect(d), Scalar(.0, .0, 255.0))
    }

//    return rawFrame
    return Mat(rawFrame, Imgproc.boundingRect(resList.last()))
}

fun postProcess(img: Mat): Mat {
    return img.apply {
        if (img.channels() == 3) Imgproc.cvtColor(this, this, Imgproc.COLOR_BGR2GRAY)
        Imgproc.GaussianBlur(this, this, Size(5.0, 5.0), .0)
        Imgproc.threshold(this, this, .0, 255.0, Imgproc.THRESH_OTSU + Imgproc.THRESH_BINARY)
    }
}

fun mat2BuffedImage(mat: Mat): BufferedImage? {
    if (mat.empty()) return null
    val converter = OpenCVFrameConverter.ToOrgOpenCvCoreMat()
    val converter2 = OpenCVFrameConverter.ToMat()
    val mat3 = converter.convert(mat)
    val mat2: org.bytedeco.opencv.opencv_core.Mat = converter2.convert(mat3)
    return Java2DFrameUtils.toBufferedImage(mat2)
}

val httpClient = HttpClient(CIO) {
    install(JsonFeature)
}

@Serializable
data class PlateItem(val license_plate_number: String, val confidence: Int, val bound: Array<Int>) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PlateItem

        if (license_plate_number != other.license_plate_number) return false
        if (confidence != other.confidence) return false
        if (!bound.contentEquals(other.bound)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = license_plate_number.hashCode()
        result = 31 * result + confidence
        result = 31 * result + bound.contentHashCode()
        return result
    }
}

@Serializable
data class PlateResponse(val results: Array<PlateItem>) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PlateResponse

        if (!results.contentEquals(other.results)) return false

        return true
    }

    override fun hashCode(): Int {
        return results.contentHashCode()
    }
}

const val BASE_URL = "http://localhost:5000"

suspend fun detectPlateFromServer(imageFile: File): PlateResponse {
    val data = withContext(Dispatchers.IO) {
        val inputStream = FileInputStream(imageFile)
        inputStream.readAllBytes()
    }
    val resp: PlateResponse = httpClient.post("$BASE_URL/client/detect_plate") {
        body = "\"${Base64.getEncoder().encodeToString(data)}\""
    }
    return resp
}
