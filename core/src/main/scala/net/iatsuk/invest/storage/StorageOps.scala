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

import net.iatsuk.invest.Context

/**
 * Key valued storage DAO.
 */
trait StorageOps {

  /**
   * @return all persisted keys in storage.
   */
  def keys(implicit ctx: Context): Set[Array[Byte]]

  /**
   * Put new data into storage. If key exists it will be override.
   *
   * @return [[Unit]] if success or [[Throwable]].
   */
  def put(key: Array[Byte], value: Array[Byte])(implicit ctx: Context): Either[Throwable, Unit]

  /**
   * @return data by key or [[Throwable]] instance.
   */
  def get(key: Array[Byte])(implicit ctx: Context): Either[Throwable, Array[Byte]]

  /**
   * Remove data from storage by key.
   *
   * @return remove boolean status or [[Throwable]] in case of error.
   */
  def remove(key: Array[Byte])(implicit ctx: Context): Either[Throwable, Boolean]

  /**
   * Close connection to storage.
   */
  def close(): Unit
}
