/**
 * MIT License
 *
 * Copyright (c) 2020, 2022 Mark Schmieder
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * This file is part of the ChartToolkit Library
 *
 * You should have received a copy of the MIT License along with the
 * ChartToolkit Library. If not, see <https://opensource.org/licenses/MIT>.
 *
 * Project: https://github.com/mhschmieder/charttoolkit
 */
package com.mhschmieder.charttoolkit.chart;

/**
 * {@code ChartType} is an enumeration of basic Chart Types; primarily to
 * distinguish charts that connect data points via lines (i.e. Data Vectors) vs.
 * ones that compute a Center Band in both directions from the given data points
 * (as is frequently the case with certain types of Bar Charts and Line Charts).
 *
 * @version 1.0
 *
 * @author Mark Schmieder
 */
public enum ChartType {
    /**
     * Data Vector charts are the most common, and simply connect the data
     * points with individual lines.
     */
    DATA_VECTOR,
    /**
     * Center Bands are common in Bar Charts and Line Charts, where the given
     * data points often need to be drawn out in both directions until the
     * mid-point between neighboring data points.
     */
    CENTER_BAND;

    /**
     * Returns the default Chart Type, for safe initialization and for clients
     * that don't care or don't know what they want or need and thus probably
     * will be doing standard Data Vector charts.
     *
     * @return The most common preferred Chart Type, which is Data Vector
     *
     * @since 1.0
     */
    public static ChartType defaultValue() {
        return DATA_VECTOR;
    }

}