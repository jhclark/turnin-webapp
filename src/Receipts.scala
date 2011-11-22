package turnin

import java.security._
import javax.crypto._
import javax.crypto.spec._
import java.io._
import java.util._
import sys.process._

object Receipts {
  val DATE_CONST = 1319722893834L // make dates smaller (relative to a different date than 1970)

  def generate(file: String, keyFile: String): String = {
    val md5all = "md5sum %s".format(file) !!
    val md5 = md5all.trim.split("""\s+""")(0)
    val time = new Date()
    // Only use a few digits of the MD5 to keep the receipt length managable
    val str = md5.slice(0,4) + (time.getTime-DATE_CONST)

    System.err.println("Plaintext receipt: " + str)

    val cipher: Cipher = Cipher.getInstance("DES")

    val in = new FileInputStream(keyFile)
    val raw: Array[Byte] = collection.Iterator.continually(in.read).takeWhile(_ != -1).map(_.toByte).toArray
    in.close

    val skeySpec: SecretKeySpec = new SecretKeySpec(raw, "DES")
    cipher.init(Cipher.ENCRYPT_MODE, skeySpec)

    val encrypted: Array[Byte] = cipher.doFinal(str.getBytes)
    val builder = new StringBuilder
    for(b <- encrypted) builder.append("%02x".format(b))
    val receiptStr = builder.toString
    System.err.println("Generated receipt with %d bytes => %d characters".format(encrypted.length, receiptStr.length))
    System.err.println("Encrypted receipt: " + receiptStr)
    receiptStr
  }

  def view(receipt: String, keyFile: String): (String, Date) = {
    val decipher: Cipher = Cipher.getInstance("DES")

    val in = new FileInputStream(keyFile)
    val raw: Array[Byte] = collection.Iterator.continually(in.read).takeWhile(_ != -1).map(_.toByte).toArray
    in.close

    val skeySpec: SecretKeySpec = new SecretKeySpec(raw, "DES")
    decipher.init(Cipher.DECRYPT_MODE, skeySpec)

    val bytes = receipt.sliding(2, 2).map(java.lang.Integer.valueOf(_, 16)).map(_.toByte).toArray
    System.err.println("Got receipt of %d characters => %d bytes".format(receipt.length, bytes.length))

    val original = decipher.doFinal(bytes)
    val originalStr = new String(original)

    val md5 = originalStr.slice(0,4)
    val dateStr = originalStr.substring(4)
    val date = new Date(java.lang.Long.parseLong(dateStr) + DATE_CONST)
    (md5, date)
  }

  def keygen(outFile: String) = {
    val kgen: KeyGenerator = KeyGenerator.getInstance("DES")
    //kgen.init(128) // 192 and 256 bits may not be available (AES)
    kgen.init(56)

    val skey: SecretKey = kgen.generateKey
    val raw: Array[Byte] = skey.getEncoded

    val out = new FileOutputStream(outFile)
    out.write(raw)
    out.close
  }
}
