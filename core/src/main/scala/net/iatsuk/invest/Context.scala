/*
  Copyright 2021 Andrei Iatsuk

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */
package net.iatsuk.invest

import java.io.{InputStream, Reader}
import java.util.Properties

import net.iatsuk.invest.Context.{FETCHER_PROVIDER, STORAGE_PROVIDER}
import net.iatsuk.invest.fetcher.{Fetcher, FetcherOps}
import net.iatsuk.invest.storage.{Storage, StorageOps}
import org.reflections.Reflections

import scala.collection.mutable

class Context {

  private val storageProviders: mutable.HashMap[String, String] = new mutable.HashMap()

  private val fetcherProviders: mutable.HashMap[String, String] = new mutable.HashMap()

  val conf: Properties = new Properties()

  private var _storage: StorageOps = _

  private var _fetcher: FetcherOps = _

  def storage: StorageOps = _storage

  def fetcher: FetcherOps = _fetcher

  def updateConfiguration(in: InputStream): Unit = synchronized {
    conf.load(in)
    init()
  }

  def updateConfiguration(reader: Reader): Unit = {
    conf.load(reader)
    init()
  }

  def updateConfiguration(properties: Properties): Unit = synchronized {
    conf.clear()
    conf.putAll(properties)
    init()
  }

  private def init(): Unit = {
    Option(_fetcher).foreach(_.stop())
    Option(_storage).foreach(_.close())

    registerStorages()
    registerFetchers()

    val storageProvider = storageProviders(conf.getProperty(STORAGE_PROVIDER))
    _storage = Class.forName(storageProvider).newInstance().asInstanceOf[StorageOps]

    val fetcherProvider = fetcherProviders(conf.getProperty(FETCHER_PROVIDER))
    _fetcher = Class.forName(fetcherProvider).newInstance().asInstanceOf[FetcherOps]
  }

  private def registerStorages(): Unit = {
    val ref = new Reflections(getClass.getPackage.getName)
    ref.getTypesAnnotatedWith(classOf[Storage]).forEach { cl =>
      val findable = cl.getAnnotation(classOf[Storage])
      println(f"Found storage '${findable.name()}' in '${cl.getName}'")
      storageProviders.put(findable.name(), cl.getName)
    }
  }

  private def registerFetchers(): Unit = {
    val ref = new Reflections(getClass.getPackage.getName)
    ref.getTypesAnnotatedWith(classOf[Fetcher]).forEach { cl =>
      val findable = cl.getAnnotation(classOf[Fetcher])
      println(f"Found fetcher '${findable.name()}' in '${cl.getName}'")
      fetcherProviders.put(findable.name(), cl.getName)
    }
  }
}

object Context {
  val FETCHER_PROVIDER = "fetch.provider"
  val STORAGE_PROVIDER = "db.provider"
}