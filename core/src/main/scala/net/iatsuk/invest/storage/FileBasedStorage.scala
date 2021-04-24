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
package net.iatsuk.invest.storage

import java.io.{File, FileOutputStream}
import java.nio.file.{Files, Paths}
import java.util.Properties

import net.iatsuk.invest.Context
import net.iatsuk.invest.storage.FileBasedStorage.DIR

import scala.util.Try

@Storage(name = "fs")
class FileBasedStorage extends StorageOps {

  implicit class Configuration(val properties: Properties) {
    def dir: String = properties.getProperty(DIR)
  }

  override def keys(implicit ctx: Context): Set[Array[Byte]] = {
    new File(ctx.conf.dir).listFiles().map(_.getName.getBytes()).toSet
  }

  override def put(key: Array[Byte], value: Array[Byte])(implicit ctx: Context): Either[Throwable, Unit] = {
    Try {
      val fileName = new String(key)
      val path = new File(ctx.conf.dir, fileName)

      val out = new FileOutputStream(path)
      out.write(value)
      out.close()
    }.toEither
  }

  override def get(key: Array[Byte])(implicit ctx: Context): Either[Throwable, Array[Byte]] = {
    Try {
      val fileName = new String(key)
      val path = Paths.get(ctx.conf.dir, fileName)
      Files.readAllBytes(path)
    }.toEither
  }

  override def remove(key: Array[Byte])(implicit ctx: Context): Either[Throwable, Boolean] = {
    Try {
      val fileName = new String(key)
      val path = Paths.get(ctx.conf.dir, fileName)
      Files.deleteIfExists(path)
    }.toEither
  }

  override def close(): Unit = {
    // Nothing to do
  }
}

object FileBasedStorage {
  val DIR = "db.dir"
}