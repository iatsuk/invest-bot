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
package net.iatsuk.invest.app

import java.util.Properties

import net.iatsuk.invest.Context
import net.iatsuk.invest.storage.FileBasedStorage

/**
 * Application entry point. Defined in build.gradle.
 */
object App {

  def main(args: Array[String]): Unit = {
    val properties = new Properties()
    properties.put(Context.STORAGE_PROVIDER, "fs")
    properties.put(FileBasedStorage.DIR, "spb-pdf")
    properties.put(Context.FETCHER_PROVIDER, "spb-pdf")

    implicit val ctx: Context = new Context()
    ctx.updateConfiguration(properties)

    ctx.fetcher.sync()
  }
}
