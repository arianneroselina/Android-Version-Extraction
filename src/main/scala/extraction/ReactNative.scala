package extraction

import play.api.libs.json._
import vulnerability.ReactNative.getVulnerabilities

import java.io.{BufferedReader, IOException, InputStreamReader}
import java.nio.file.{Files, Paths}
import scala.reflect.io.Path._
import scala.util.control.Breaks.{break, breakable}

class ReactNative(var reactNativeVersion: Array[String] = Array()) {

  /**
   * Extract the React Native version from the given APK, if React Native is used.
   *
   * @param folderPath the path to the extracted APK folder
   * @return the mapping of the React Native version
   */
  def extractReactNativeVersion(folderPath: String): (String, JsValue) = {
    try {
      // search for libreact*.so
      val fileName = """libreact.*.so"""
      val filePaths = findInLib(folderPath, fileName)

      // no libreact*.so found
      if (filePaths == null || filePaths.isEmpty) {
        println(Console.YELLOW + s"$fileName is not found in $folderPath lib directory")
        return null
      }
      println(Console.GREEN + "React Native implementation found")

      // check which lib is the returned libreact*.so in
      var libType = ""
      if (filePaths(0).contains("arm64-v8a")) libType = "arm64-v8a"
      else if (filePaths(0).contains("armeabi-v7a")) libType = "armeabi-v7a"
      else if (filePaths(0).contains("x86")) libType = "x86"
      else if (filePaths(0).contains("x86_64")) libType = "x86_64"

      // run certutil
      for (filePath <- filePaths) {
        val f = filePath.split(Array('\\', '/'))
        val fileName = f(f.length-1)

        val processBuilder = new ProcessBuilder("certutil", "-hashfile", filePath, "SHA256")
        val process = processBuilder.start

        // prepare to read the output
        val stdout = process.getInputStream
        val reader = new BufferedReader(new InputStreamReader(stdout))

        // extract the React Native version
        extractReactNativeVersion(reader, libType, fileName)
      }
      println(Console.GREEN + "React Native version extraction finished")

      // return it as a JSON value
      createJson()
    } catch {
      case e: IOException => println(Console.RED + e.getMessage)
        null
    }
  }

  /**
   * Find the path of the given file in lib directory.
   *
   * @param folderPath the path to the extracted APK folder
   * @param fileName the filename in regex (e.g. libreact*,so)
   * @return the path
   */
  def findInLib(folderPath: String, fileName: String): Array[String] = {
    val libDirs = folderPath.toDirectory.dirs.map(_.path).filter(name => name matches """.*lib""")
    for (libDir <- libDirs) {
      val inLibs = libDir.toDirectory.dirs.map(_.path)
      for (lib <- inLibs) {
        try {
          val filePaths = lib.toDirectory.files
            .filter(file => file.name matches fileName)
            .map(_.path)
          val pathArray = filePaths.toArray

          var found = true
          for (filePath <- filePaths) {
            if (!Files.exists(Paths.get(filePath))) {
              found = false
              break
            }
          }
          if (found) {
            return pathArray
          }
        } catch {
          case _: Exception => // do nothing
        }
      }
    }
    null // file not found
  }

  /**
   * Extract the React Native version from a buffered reader
   *
   * @param reader the buffered reader of the output from certutil execution
   * @param libType the lib directory type arm64-v8a, armeabi-v7a, x86, or x86_64
   */
  def extractReactNativeVersion(reader: BufferedReader, libType: String, fileName: String): Unit = {
    try {
      var line = reader.readLine
      breakable {
        while (line != null) {
          if (!line.contains("SHA256") && !line.contains("CertUtil")) {
            val fileHash = line

            // check which version the hash belongs to
            val bufferedSource = io.Source.fromFile(
              Paths.get(".").toAbsolutePath + "/src/files/hashes/react_native/" + libType + ".csv")
            for (csvLine <- bufferedSource.getLines) {
              val cols = csvLine.split(',').map(_.trim)
              if (cols(1).equals(fileName) && cols(2).equals(fileHash) && !reactNativeVersion.contains(cols(0))) {
                reactNativeVersion = reactNativeVersion :+ cols(0)
              }
            }
            bufferedSource.close
          }

          line = reader.readLine
        }
      }
    } catch {
      case e: IOException => println(Console.RED + e.getMessage)
    }
  }

  /**
   * Create a JSON from this class' object
   *
   * @return the mapping of the React Native version
   */
  def createJson(): (String, JsValue) = {
    var versions = Json.obj()
    var writeVersion = ""

    if (reactNativeVersion.nonEmpty) {
      for (i <- 0 until reactNativeVersion.length) {
        if (i == 0) {
          writeVersion = reactNativeVersion(i)
        } else {
          writeVersion += ", " + reactNativeVersion(i)
        }
        val links = getVersionVulnerability(reactNativeVersion(i))
        versions = versions + (reactNativeVersion(i) -> Json.toJson(links))
      }
    }

    "React Native" -> Json.obj("Version" -> writeVersion, "Vulnerabilities" -> versions)
  }

  def getVersionVulnerability(version: String): Array[String] = {
    getVulnerabilities(version)
  }
}