package pro.darc.park.utils

import Main
import ai.djl.Device
import ai.djl.modality.cv.BufferedImageFactory
import ai.djl.modality.cv.Image
import ai.djl.modality.cv.output.DetectedObjects
import ai.djl.modality.cv.transform.Resize
import ai.djl.modality.cv.transform.ToTensor
import ai.djl.modality.cv.translator.YoloV5Translator
import ai.djl.repository.zoo.Criteria
import ai.djl.translate.Pipeline
import ai.djl.translate.Translator
import org.bytedeco.javacpp.Pointer
import org.bytedeco.javacv.Java2DFrameUtils
import org.bytedeco.javacv.OpenCVFrameConverter
import org.opencv.core.Mat
import org.opencv.core.Rect
import java.awt.image.BufferedImage
import java.nio.file.Path
import java.nio.file.Paths


fun locatePlate(frame: Mat): Mat? {
    val pipeline = Pipeline().apply {
        add(Resize(640))
        add(ToTensor())
    }
    val translator: Translator<Image, DetectedObjects> = YoloV5Translator.builder()
        .optSynsetArtifactName("names")
        .optRescaleSize(640.0, 640.0)
        .setPipeline(pipeline)
        .optThreshold(0.7F)
        .build()
    val criteria = Criteria.builder()
        .setTypes(Image::class.java, DetectedObjects::class.java)
        .optDevice(Device.cpu())
//        .optModelUrls(Main::class.java.getResource("/yolov5s")!!.path)
//        .optModelPath(Main::class.java.getResource("/yolov5s")!!)
        .optModelPath(Paths.get("D:/Codes/Github/DarcJC/EZParkClient/src/main/resources/yolov5s"))
        .optModelName("best.torchscript.pt")
        .optTranslator(translator)
        .optEngine("PyTorch")
        .build()

    val model = try {
        criteria.loadModel()
    } catch (e: Throwable) {e.printStackTrace(); null}

    return model?.let {
        val temp = mat2BuffedImage(frame) ?: return@let null
        val img = BufferedImageFactory.getInstance().fromImage(temp)
        val predictor = try { it.newPredictor() } catch (e: Throwable) { e.printStackTrace(); null }!!
        val results = predictor.predict(img)
        for (obj in results.items<DetectedObjects.DetectedObject>()) {
            val bbox = obj.boundingBox
            val clsName = obj.className
            val prob = obj.probability
            if (prob > .75f && clsName == "plate") {
                val rect = bbox.bounds
                println(rect)
                return@let Mat(frame, Rect(rect.x.toInt(), rect.y.toInt(), rect.width.toInt(), rect.height.toInt()))
            }
        }
        null
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
