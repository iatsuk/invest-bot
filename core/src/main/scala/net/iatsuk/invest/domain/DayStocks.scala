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
package net.iatsuk.invest.domain

/**
 * Represent day stock meta.
 *
 * @example DayStocks(
 *          date=2019-08-13,
 *          code="AAPL",
 *          title="Apple Inc.",
 *          category="Акции иностранного эмитента обыкновенные",
 *          isin="US0378331005",
 *          currency="USD",
 *          priceMin=198.83,
 *          priceMax=212.1,
 *          priceLast=208.73)
 */
case class DayStocks(date: String,
                     code: String,
                     title: String,
                     category: String,
                     isin: String,
                     currency: String,
                     priceMin: Float,
                     priceMax: Float,
                     priceLast: Float)
