import java.util.Date

object View extends App {
  val keyFile = args(0)
  val receipt = args(1)

  val (md5: String, date: Date) = turnin.Receipts.view(receipt, keyFile)

  println("First 4 MD5: %s (This can be verified by running md5sum on the student's file)".format(md5))
  println("Turnin date: %s".format(date.toString))
}
