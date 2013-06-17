//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import com.google.gson.Gson
import org.junit.Assert._
import org.junit.Test

import com.threerings.everything.data._

class GsonTest {

  case class CaseTest (one :String, two :Int, three :Long)

  @Test def testToFromJson {
    val gson = new Gson
    def testMarshal (obj :AnyRef) {
      val json = gson.toJson(obj)
      // println(s"JSON $json")
      val pojo = gson.fromJson(json, obj.getClass)
      // println(s"POJO $pojo")
      assertFieldsEqual(obj.getClass.getName, obj, pojo)
    }
    testMarshal(FakeData.mikeD)
    testMarshal(FakeData.mythology)
    testMarshal(FakeData.yanluo)
    testMarshal(FakeData.maltesersCard(0L))
    testMarshal(CaseTest("one", 2, 3L))
  }

  def assertFieldsEqual (msg :String, a :AnyRef, b :AnyRef) {
    val ac = a.getClass
    if (ac.isPrimitive || ac.isEnum || ac.getName.startsWith("java.")) assertEquals(msg, a, b)
    else ac.getFields foreach { f =>
      val fmsg = s"$msg.${f.getName}"
      if (!f.getType.isArray) assertFieldsEqual(fmsg, f.get(a), f.get(b))
      else {
        val (as, bs) = (f.get(a).asInstanceOf[Array[AnyRef]], f.get(b).asInstanceOf[Array[AnyRef]])
        as.zip(bs) foreach { case (a, b) => assertFieldsEqual(fmsg, a, b) }
      }
    }
  }
}
