// using System;
// using System.Security.Cryptography;
// using System.Text;
using ikvm.runtime;
// using MonoTouch.Foundation;

using playn.core;

namespace everything
{
  public class IOSUtil {
    /** Invokes the supplied delegate on the client's main thread. */
    public static void invokeLater (Delegates.RunnableDelegate action) {
      PlayN.invokeLater(Delegates.toRunnable(action));
    }

  //   /** Returns the supplied bytes as a hex-encoded string. */
  //   public static string hexlate (byte[] data) {
  //     StringBuilder buf = new StringBuilder();
  //     for (int ii = 0; ii < data.Length; ii++) buf.Append(data[ii].ToString("x2"));
  //     return buf.ToString().ToUpper();
  //   }

  //   /** Returns the supplied binary data as a hex-encoded string. */
  //   public static string hexlate (NSData data) {
  //     StringBuilder buf = new StringBuilder();
  //     for (int ii = 0; ii < data.Length; ii++) buf.Append(data[ii].ToString("x2"));
  //     return buf.ToString().ToUpper();
  //   }

  //   /** Returns the SHA1 hash of the supplied bytes as a hex-encoded string. */
  //   public static string sha1hex (byte[] data) {
  //     using (SHA1 sha1 = SHA1.Create()) {
  //       return hexlate(sha1.ComputeHash(data));
  //     }
  //   }

  //   /** Returns the SHA1 hash of the UTF-8 bytes of the supplied string as a hex-encoded string. */
  //   public static string sha1hex (string data) {
  //     return sha1hex(Encoding.UTF8.GetBytes(data));
  //   }
  }
}
