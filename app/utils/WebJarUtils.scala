package utils

import java.io.{ByteArrayOutputStream, OutputStream, FileOutputStream}
import java.util.jar.JarOutputStream
import java.util.zip.{ZipEntry, ZipInputStream}

import scala.annotation.tailrec

object WebJarUtils {

  def createWebJar(zip: ZipInputStream, pom: String, name: String, version: String): Array[Byte] = {

    def createDir(dir: String, jar: JarOutputStream): Unit = {
      val ze = new ZipEntry(dir)
      jar.putNextEntry(ze)
      jar.closeEntry()
    }

    def createFileEntry(path: String, jar: JarOutputStream, contents: String): Unit = {
      val ze = new ZipEntry(path)
      jar.putNextEntry(ze)
      jar.write(contents.getBytes)
      jar.closeEntry()
    }

    val byteArrayOutputStream = new ByteArrayOutputStream()

    val jar = new JarOutputStream(byteArrayOutputStream) //new FileOutputStream("/tmp/foo.jar"))

    createDir(s"META-INF/", jar)
    createDir(s"META-INF/maven/", jar)
    createDir(s"META-INF/maven/org.webjars.bower/", jar)
    createDir(s"META-INF/maven/org.webjars.bower/$name/", jar)

    createFileEntry(s"META-INF/maven/org.webjars.bower/$name/pom.xml", jar, pom)

    val properties = s"""
                        |#Generated by WebJar Bower Sync
                        |version=$version
        |groupId=org.webjars.bower
        |artifactId=$name
       """.stripMargin

    createFileEntry(s"META-INF/maven/org.webjars.bower/$name/pom.properties", jar, properties)

    val webJarPrefix = s"META-INF/resources/webjars/$name/$version/"

    createDir(s"META-INF/resources/", jar)
    createDir(s"META-INF/resources/webjars/", jar)
    createDir(s"META-INF/resources/webjars/$name/", jar)
    createDir(webJarPrefix, jar)

    // copy zip to jar, excluding .bower.json
    val zipEntryTraverable = new ZipEntryTraversableClass(zip)

    zipEntryTraverable.foreach { ze =>
      if (ze.getName != ".bower.json") {
        val path = webJarPrefix + ze.getName
        val nze = new ZipEntry(path)
        jar.putNextEntry(nze)
        zipEntryTraverable.writeCurrentEntryTo(jar)
      }
    }

    zip.close()
    jar.close()

    byteArrayOutputStream.toByteArray
  }

  def emptyJar(): Array[Byte] = {
    val byteArrayOutputStream = new ByteArrayOutputStream()

    val jar = new JarOutputStream(byteArrayOutputStream)
    jar.close()

    byteArrayOutputStream.toByteArray
  }

}


// from: http://harrah.github.io/browse/samples/compiler/scala/tools/nsc/io/ZipArchive.scala.html
class ZipEntryTraversableClass(zis: ZipInputStream) extends Traversable[ZipEntry] {

  def foreach[U](f: ZipEntry => U) {
    @tailrec
    def loop(x: ZipEntry): Unit = if (x != null) {
      f(x)
      zis.closeEntry()
      loop(zis.getNextEntry)
    }
    loop(zis.getNextEntry)
  }

  def writeCurrentEntryTo(os: OutputStream) {
    val bytes = new Array[Byte](1024)
    Iterator
      .continually(zis.read(bytes))
      .takeWhile(_ != -1)
      .foreach(read => os.write(bytes, 0, read))
    zis.closeEntry()
  }
}