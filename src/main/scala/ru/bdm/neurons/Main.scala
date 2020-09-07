package ru.bdm.neurons

import java.io.{File, PrintWriter}

import org.json4s._
import org.json4s.native.Serialization

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.io.{Source, StdIn}


object Main {


  def main(args: Array[String]): Unit = {
    implicit val formats = Serialization.formats(NoTypeHints)

    println(-1 % 30)

    val alf = "ёйцукенгшщзхъфывапролджэячсмитьбю.,!? "
    val loadFile = "result.json"
    val s1 = Source.fromFile(loadFile)
    //val ns = Serialization.read[NeuronSystemWrite](s1.getLines().mkString(" ")).create()


    val l1 = RecurLayer(alf.length, 100, alf.length)

    val ns = NeuronSystem.create(l1)
    val bpa = new BackpropagationAlgorithm(ns, 0.1)

    def genInputs(in: Seq[Char]): Array[Double] = {
      val arr = Array.fill(alf.length)(.1)
      for (i <- in.indices) {
        arr(i) = alf.indexOf(in(i)).toDouble / alf.length
      }
      arr
    }

    def genOuts(char: Char): Array[Double] = {
      val arr = Array.fill(alf.length)(.1)
      arr(alf.indexOf(char)) = 1.0
      arr
    }

    def getOut(out: Seq[Double]): Char = {
      alf(out.indexOf(out.max))
    }

    val source = Source.fromFile("pushkin.txt")
    val reg = "[ .,]+".r
    var str = source.getLines().mkString(" ").toLowerCase.filter(alf.contains(_))
    str = reg.replaceAllIn(str, " ")
    var noEnd = true
    var index = 0
    var circle = 0
    val future = Future {
      println("teach started")
      var minError = Double.MaxValue
      val startTime = System.currentTimeMillis()
      while (noEnd) {
        index += 1
        if (index > str.length - 5) {
          index = 0
          circle += 1
        }

        bpa.teach(genInputs(str.slice(index, index + 5)), genOuts(str.charAt(index + 5)))

        val endTime = (1 - (index.toDouble / str.length)) / ((index.toDouble / str.length) / ((System.currentTimeMillis() - startTime) / 1000.0))
        minError = Math.min(minError, bpa.sumError)
        if (index % 100 == 0)
          println(minError + " " + index.toFloat / str.length * 100 + 100 * circle + "% осталось времени " + (endTime / 60 toInt) + " min " + (((endTime * 1000) % 60 toInt) / 10)  + " sec (" + bpa.sumError + ")")
      }
    }


    var in = StdIn.readLine()
    noEnd = false
    Await.ready(future, Duration.Inf)
    val wr = new PrintWriter(new File(loadFile))

    wr.write(Serialization.writePretty(ns.write()))
    wr.close()
    while (in != "exit") {
      print("enter start words 5 symbols:")
      in = StdIn.readLine()
      for (_ <- 1 to 20)
        in += getOut(ns.work(genInputs(in.slice(in.length - 5, in.length))))
      println(s"'$in'")
    }
  }

  def printTime[T](name: String = "no name")(fun: => T): T = {
    val st = System.currentTimeMillis()
    val t = fun
    println(s"$name end in ${(System.currentTimeMillis() - st) / 1000d} sec")
    t
  }
}
