package turnin

import collection._
import org.scalatra._
import java.io._
import java.util.zip._
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import scalate.ScalateSupport

import org.scalatra.fileupload.FileUploadSupport

import org.apache.commons.compress.archivers.tar._

object Files {
  def isGzip(file: File) = {
    try {
      val in = new DataInputStream(new FileInputStream(file))
      val result = in.readShort == 0x1f8b
      in.close
      result
    } catch {
      case _ => false
    }
  }

  def isTar(file: File, useGzip: Boolean) = {
    try {
      val in = new DataInputStream({
        val fis = new FileInputStream(file)
        if(useGzip) new GZIPInputStream(fis) else fis
      })
      for(i <- 0 until 257) in.readByte // discard
      val result = (in.readInt == 0x75737461 && in.readByte == 0x72)
      in.close
      result
    } catch {
      case _ => false
    }
  }
  
  def getFilesInTgz(tgzFile: File): Set[String] = {
    val in = new TarArchiveInputStream(new GZIPInputStream(new FileInputStream(tgzFile)))
    val it = Iterator.continually(in.getNextTarEntry).takeWhile(_ != null)
    val files: Set[String] = (for(entry: TarArchiveEntry <- it) yield {
      val content = new Array[Byte](entry.getSize.toInt)
      in.read(content, 0, entry.getSize.toInt)
      entry.getName
    }).toSet
    files
  }

  def copy(src: File, dest: File) {
    try {
      dest.createNewFile
    } catch {
      case e: IOException => throw new RuntimeException("Could not create file: " + dest.getAbsolutePath, e)
    }
    new FileOutputStream(dest).getChannel.transferFrom(new FileInputStream(src).getChannel, 0, Long.MaxValue)
  }
}


// see https://github.com/scalatra/scalatra
// this directory came from git clone git://github.com/scalatra/scalatra-sbt-prototype.git my-app
// can be run locally with sbt '~jetty-run'
class TurninWebApp extends ScalatraFilter with ScalateSupport with FileUploadSupport {
  val conf: Map[String, String] = {
    val confFile = "data/turnin.conf"
    val props = new java.util.Properties
    val in = new FileInputStream(confFile)
    props.load(in)
    in.close
    import collection.JavaConversions._
    props.keySet.map(key => (key.toString, props.get(key).toString) ).toMap
  }

  val uploadDir = conf("uploadDir")
  val keyFile = conf("keyFile")
  val receiptLogFile = conf("receiptLogFile")
  val manifestFile = conf("manifestFile")
  val pageTitle = conf("pageTitle")

  if(!new File(uploadDir).exists) {
    throw new RuntimeException("Upload directory not found: %s".format(new File(uploadDir).getAbsolutePath))
  }
  if(!new File(keyFile).exists) {
    throw new RuntimeException("File not found: %s".format(new File(keyFile).getAbsolutePath))
  }
  if(!new File(manifestFile).exists) {
    throw new RuntimeException("File not found: %s".format(new File(manifestFile).getAbsolutePath))
  }
  val reqFiles = io.Source.fromFile(manifestFile).getLines.toList

  // always append; must flush on each write
  val receiptLog = new PrintWriter(new FileOutputStream(receiptLogFile, true))

  get("/") {
    <html>
      <body>
        <table><tr>
	<td><img src="logo.png" /></td>
        <td><h2>{pageTitle}</h2></td>
	</tr></table>
        <form method="post" enctype="multipart/form-data">
	  <table border="0">
	    <tr><td>Real Name:</td><td><input type="text" name="name" /></td></tr>
            <tr><td>Submission (tar.gz with required files)</td><td><input type="file" name="file" /></td></tr>
            <tr><td colspan="2"><center><input type="submit" /></center></td></tr>
          </table>
        </form>
        <b>You are not done submitting your assignment until you have a receipt number. You should copy this receipt down and put it in a safe place.</b>
        <!-- Say <a href="hello-scalate">hello to Scalate</a>. -->
      </body>
    </html>
  }

  post("/") {
    import org.apache.commons.fileupload._
    import java.io._

    println(fileParams)

    var failed = false

//    val user: String = params("user")
    val realName: String = params("name").replace(' ', '_')
    val fileItem: FileItem = fileParams("file")
    val time: String = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date)
    
    val file = new File(uploadDir, "%s.tar.gz".format(realName)).getAbsoluteFile
    val allDir = new File(uploadDir, "all")
    if (!allDir.exists) {
      allDir.mkdir
    }
    val bakFile = new File(allDir, "%s-%s.tar.gz".format(realName,time)).getAbsoluteFile
    println("Receiving upload to: %s".format(file))
    fileItem.write(file)
    println("Making backup to: %s".format(bakFile))
    Files.copy(file, bakFile)

    <html>
      <body>
        <table border="0"><tr>
	<td><img src="logo.png" /></td>
        <td><h2>{pageTitle}</h2></td>
	</tr></table>


      File received. Verifying contents...<br/>
      {
	val checks = new mutable.ArrayBuffer[(String, () => Boolean)]
	checks.append( ("Is Gzip?", () => Files.isGzip(file)) )
	checks.append( ("Is Tarball?", () => Files.isTar(file, true)) )
        lazy val files = Files.getFilesInTgz(file) // important that it is lazy so we don't open non-gzips!
	for(reqFile <- reqFiles) {
          checks.append( ("Contains %s without internal directory?".format(reqFile), () => files.contains(reqFile)) )
        }
	
        for( (msg, func) <- checks) yield {
          if(!failed) func() match {
            case true => <b>{msg} </b><font color="green"><b>Yes.</b></font><br/>
            case false => {
	      failed = true
	      <b>{msg} </b><font color="red"><b>NO.</b></font><br/>
            }
          } else {
            <b>FAILED.</b><br/>
          }
        }
      }
      </body>
      {
	if(!failed) {
	  <div><b>Receipt:</b> {
	  		       val receipt = turnin.Receipts.generate(file.getAbsolutePath, keyFile)
			       receiptLog.println("%s %s".format(receipt, file.toString))
			       receiptLog.flush
			       receipt
			       }<br/>
	  You must keep this in a safe place as proof that you turned in your assignment. (Copy-paste so that you don't make transcription mistakes.)<br/></div>
        }
      }
    </html>
  }

  notFound {
    // If no route matches, then try to render a Scaml template
    val templateBase = requestPath match {
      case s if s.endsWith("/") => s + "index"
      case s => s
    }
    val templatePath = "/WEB-INF/scalate/templates/" + templateBase + ".scaml"
    servletContext.getResource(templatePath) match {
      case url: URL => 
        contentType = "text/html"
        templateEngine.layout(templatePath)
      case _ => 
        filterChain.doFilter(request, response)
    } 
  }
}
