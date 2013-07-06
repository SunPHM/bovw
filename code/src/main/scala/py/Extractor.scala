package py

import com.googlecode.javacv.cpp.opencv_highgui._
import com.googlecode.javacv.cpp.opencv_core._
import com.googlecode.javacv.cpp.opencv_core.CvMat
import com.googlecode.javacv.cpp.opencv_features2d._
import java.io.File
import com.googlecode.javacv.CanvasFrame
import com.googlecode.javacv.cpp.opencv_nonfree.SIFT
import com.googlecode.javacv.cpp.opencv_imgproc._
import com.googlecode.javacv.CanvasFrame
import com.googlecode.javacv.cpp.opencv_highgui._
import javax.swing.JFrame._
import OpenCVUtils._
import java.io.PrintWriter
import java.util.ArrayList
import java.util.Date

object Extractor {
  
  // parameters used to detect SIFT key points
  val nFeatures = 0
  val nOctaveLayers = 3
  val contrastThreshold = 0.03
  val edgeThreshold = 10
  val sigma = 1.6
  val sift = new SIFT(nFeatures, nOctaveLayers, contrastThreshold, edgeThreshold, sigma)
  val des = sift.getDescriptorExtractor()
  
  // open a file to store all the statistics
  val stat = new PrintWriter("resources/stat.txt")
  
  
  def main(args : Array[String]){
    //val file = new File("resources/oxbuild_images/ashmolean_000214.jpg")
    //extractFeatures(file)
    //run
    //println(sift.info())
    //println(des.info())
    drawFromFile("resources/oxbuild_images/all_souls_000001.jpg")
  }
  
  def run(){
    println(new Date().toGMTString() + " " + this.getClass().getName() + " " + "start of run over the Oxford Building 5K dataset")
    // for each file in the directory, extract features, store into files
    val folder = new File("resources/oxbuild_images")
    val files  = folder.list()
    files.foreach(file => extractFeatures("resources/oxbuild_images/" + file))
    println(new Date().toGMTString() + " " + this.getClass().getName() + " " + "end of run over the Oxford Building 5K dataset")
  }
  
  // read one file and then output the features to one file
  def extractFeatures(filename: String){
    //println(new Date().toGMTString() + " " + this.getClass().getName() + " " + file.getName() + ": processing")
    val begin = System.nanoTime()
    // load image
    var file = new File(filename)
    var image = cvLoadImageM(file.getAbsolutePath(), CV_LOAD_IMAGE_GRAYSCALE)
    var keyPoints = new KeyPoint()  
    sift.detect(image, null, keyPoints)
    var points = toArray(keyPoints)
    var cv_mat = new CvMat(null)
    des.compute(image, keyPoints, cv_mat)
    
    // used to count the number of points in different scales
    var s = Array(0,0,0,0,0,0,0,0); 
    
    if (cv_mat.isNull()) {println(new Date().toGMTString() + " " + this.getClass().getName() + " " + "empty cv_mat");return} // handling exception
    // write the cv_mat into a file
    val pw = new PrintWriter("resources/features/" + file.getName() + ".txt")
    for(i <- 0 until cv_mat.rows())
    {
      val row = new ArrayList[Double]
      for(j<- 0 until cv_mat.cols())
        row.add(cv_mat.get(i, j))
      // write the key point information as well as the 128B feature into file  
      pw.print(points(i).position() + " " + points(i).pt() + " " + points(i).response() + " " + points(i).angle() + " " + points(i).size() + " ")
      val paras = getSIFTParameters(points(i)) // result as a tuple
      pw.print((paras._1 + 1) + " " + paras._2 + " " + paras._3 * 0.5f + " " + paras._4  + " ")
      pw.println(row.toArray().mkString(" "))
      pw.flush()
      // add the scale number information
      s(paras._1 + 1) = s(paras._1 + 1) + 1
      
    }
    
    val end = System.nanoTime()
    // write the statistics about this image
    stat.println(file.getName() + " " + (end - begin) / 1000000.0 + " " + cv_mat.rows() + " " + s.mkString(" "))
    stat.flush()
    println(new Date().toGMTString() + " " + this.getClass().getName() + " " + file.getName() + ": processed")
    
    // clear memory and garbage collection
    keyPoints.deallocate()
    cv_mat.release()
    file = null
    image = null
    keyPoints = null
    points = null
    cv_mat = null
    System.gc()
    
  }
  
  def getSIFTParameters(point : KeyPoint) = {
    // get octave
	var octave = point.octave & 255;
    octave = if (octave < 128) octave else octave | -128
    // get layer
    var layer = (point.octave >> 8) & 255;
    // get scale
    var scale = if (octave >= 0)  1.0f/(1 << octave) else (1 << -octave).asInstanceOf[Float]
    // multiply the point's radius by the calculated scale
    var scl = point.size * 0.5f * scale;
    // determines the size of a single descriptor orientation histogram
    var histWidth = 3.0f * scl;
    // descWidth is the number of histograms on one side of the descriptor
    val radius = (histWidth * 1.4142135623730951f * (4 + 1) * 0.5f).toInt
    (octave, layer, scale, radius) 
  }
  
  
  def draw(keyPoints:KeyPoint, image:CvMat){
    // Draw keyPoints
    val featureImage = cvCreateImage(cvGetSize(image), image.depth(), 3)
    drawKeypoints(image, keyPoints, featureImage, CvScalar.WHITE, DrawMatchesFlags.DRAW_RICH_KEYPOINTS)
    show(featureImage, "SIFT Features")
  }
  
  def drawFromFile(filename:String){
    // to draw an arbitrary image file
       // Read input image
      // parameters used to detect SIFT key points
    val nFeatures = 0
    val nOctaveLayers = 3
    val contrastThreshold = 0.03
    val edgeThreshold = 10
    val sigma = 1.6
    val sift = new SIFT(nFeatures, nOctaveLayers, contrastThreshold, edgeThreshold, sigma)
    val image = loadAndShowOrExit(new File(filename))
    var keyPoints = new KeyPoint()
    sift.detect(image, null, keyPoints)
    var points = toArray(keyPoints)
    points.slice(5000, 5400).foreach(point => {
      // output the getSIFTParameters and other key point information
      val paras = getSIFTParameters(point)
      //print(point.position() + " " + point.pt() + " " + point.response() + " " + point.angle() + " ")  
      //println(paras._1 + " " + paras._2 + " " + paras._3 + " " + paras._4  + " " + point.size())
      println(point.position() + " " + point.pt() + " " + paras._1)
    })
        
    println("the last keypoint: angle " + points(points.size - 1).angle() + " octave " + points(points.size - 1).octave() + 
        " pt " + points(points.size - 1).pt() + " size " + points(points.size - 1).size() + " capacity " + points(points.size - 1).capacity() + 
        " limit " + points(points.size - 1).limit() + " position " + points(points.size - 1).position() + " response " + points(points.size - 1).response())
        
    println("key points number " + points.size)
    // Draw keyPoints
    val featureImage = cvCreateImage(cvGetSize(image), image.depth(), 3)
    drawKeypoints(image, points(24), featureImage, CvScalar.RED, DrawMatchesFlags.DRAW_RICH_KEYPOINTS)
    show(featureImage, "SIFT Features")
    save(new File("ex.jpg"), featureImage)
  }
  
  def drawScales(image : IplImage, point : KeyPoint){
    // divide the key points by the scales
    val marks = Array(-1, -1, -1, -1, -1, -1, -1, -1)
    val points = toArray(point)
    for(i <-0 until points.size){
      var octave = point.octave & 255;
      octave = if (octave < 128) octave else octave | -128
      if (marks(octave + 1) == -1) marks(octave) = i
    }
    val psa = new Array[KeyPoint](8)
    for(i<-0 unitl marks.size){
      // create a new key point vector
      
      // show image
    }
  }
  
  def pointMatch(image0 : IplImage, mat0 : CvMat, point0 : KeyPoint, image1 : IplImage, mat1 : CvMat, point1 : KeyPoint){
    
    val matcher = new BFMatcher(NORM_L2, false)
    val matches = new DMatch()

    matcher.`match`(mat0, mat1, matches, null)
    println("Matched: " + matches.capacity)

    // Select only 25 best matches
    val bestMatches = selectBest(matches, 25)

    // Draw best matches
    val imageMatches = cvCreateImage(new CvSize(image0.width + image1.width, image0.height), image0.depth, 3)
    drawMatches(image0, point0, image1, point1,
        bestMatches, imageMatches, CvScalar.BLUE, CvScalar.RED, null, DrawMatchesFlags.DEFAULT)
    show(imageMatches, "Best SURF Feature Matches")

  }
      /** Select only the best matches from the list. Return new list. */
  def selectBest(matches: DMatch, numberToSelect: Int): DMatch = {
        // Convert to Scala collection, and sort
        val sorted = toArray(matches).sortWith(_ compare _)
        //sortWith(_ compare _)

        // Select the best, and return in native vector
        toNativeVector(sorted.take(numberToSelect))
    }
}
