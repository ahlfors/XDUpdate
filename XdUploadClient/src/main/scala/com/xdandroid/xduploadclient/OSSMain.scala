package com.xdandroid.xduploadclient

import java.io._

import com.aliyun.oss._
import com.aliyun.oss.common.auth._
import com.aliyun.oss.model._
import rx.lang.scala.schedulers._
import rx.lang.scala.{Observable, Subscriber}

/**
  * Created by xingda on 16-11-4.
  */
object OSSMain {

  def main(args: Array[String]): Unit = {
    val xdUpdateBean = Utils.extractApkInfo(args)
    val json = Utils.sGson.toJson(xdUpdateBean)
    Utils.saveJsonToFile(json)
    doUpload()
    Thread.sleep(15 * 60 * 1000)
  }

  def doUpload() {
    val credentialsProvider = new DefaultCredentialProvider(Environment.sAccessKeyId, Environment.sAccessKeySecret)
    val oss = new OSSClient(Environment.sEndpoint, credentialsProvider)
    val jsonObsrv = Observable((s: Subscriber[Boolean]) => {putObject("json", oss, s)}).subscribeOn(IOScheduler())
    val apkObsrv = Observable((s: Subscriber[Boolean]) => {putObject("apk", oss, s)}).subscribeOn(IOScheduler())
    Observable.combineLatest(Iterable(jsonObsrv, apkObsrv)) ((_: Seq[Boolean]) => true)
      .observeOn(ImmediateScheduler())
      .subscribe((_: Boolean) => {
        oss.shutdown()
        System.exit(0)
      }, (t: Throwable) => t.printStackTrace())
  }

  def putObject(theType: String, oss: OSS, subscriber: Subscriber[_ >: Boolean]) {
    val request = theType match {
      case "json" =>
        new PutObjectRequest(Environment.sBucketName,
          Environment.sPathPrefix + Environment.sPackageName + ".json",
          new File(Environment.sPackageName + ".json"))
      case "apk" =>
        new PutObjectRequest(Environment.sBucketName,
          Environment.sPathPrefix + Environment.sPackageName + ".apk",
          new File(Environment.sPackageName + ".apk"))
    }
    try {
      oss.putObject(request.withProgressListener(new PutObjectProgressListener()))
      subscriber.onNext(true)
      subscriber.onCompleted()
    } catch {
      case e: Exception => subscriber.onError(e)
    }
  }
}
