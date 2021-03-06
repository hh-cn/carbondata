/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.carbondata.examples

import java.io.File

import org.apache.spark.sql.SparkSession

import org.apache.carbondata.core.constants.CarbonCommonConstants
import org.apache.carbondata.core.util.CarbonProperties

object CarbonSortColumnsExample {

  def main(args: Array[String]) {
    val rootPath = new File(this.getClass.getResource("/").getPath
                            + "../../../..").getCanonicalPath
    val storeLocation = s"$rootPath/examples/spark2/target/store"
    val warehouse = s"$rootPath/examples/spark2/target/warehouse"
    val metastoredb = s"$rootPath/examples/spark2/target"

    CarbonProperties.getInstance()
      .addProperty(CarbonCommonConstants.CARBON_TIMESTAMP_FORMAT, "yyyy/MM/dd HH:mm:ss")
      .addProperty(CarbonCommonConstants.CARBON_DATE_FORMAT, "yyyy/MM/dd")

    import org.apache.spark.sql.CarbonSession._
    val spark = SparkSession
      .builder()
      .master("local")
      .appName("CarbonSortColumnsExample")
      .config("spark.sql.warehouse.dir", warehouse)
      .config("spark.driver.host", "localhost")
      .getOrCreateCarbonSession(storeLocation)

    spark.sparkContext.setLogLevel("ERROR")

    spark.sql("DROP TABLE IF EXISTS no_sort_columns_table")

    // Create table with no sort columns
    spark.sql(
      s"""
         | CREATE TABLE no_sort_columns_table(
         | shortField SHORT,
         | intField INT,
         | bigintField LONG,
         | doubleField DOUBLE,
         | stringField STRING,
         | timestampField TIMESTAMP,
         | decimalField DECIMAL(18,2),
         | dateField DATE,
         | charField CHAR(5),
         | floatField FLOAT,
         | complexData ARRAY<STRING>
         | )
         | STORED BY 'carbondata'
         | TBLPROPERTIES('SORT_COLUMNS'='')
       """.stripMargin)

    // Create table with sort columns
    // you can specify any columns to sort columns for building MDX index, remark: currently
    // sort columns don't support "FLOAT, DOUBLE, DECIMAL"
    spark.sql("DROP TABLE IF EXISTS sort_columns_table")
    spark.sql(
      s"""
         | CREATE TABLE sort_columns_table(
         | shortField SHORT,
         | intField INT,
         | bigintField LONG,
         | doubleField DOUBLE,
         | stringField STRING,
         | timestampField TIMESTAMP,
         | decimalField DECIMAL(18,2),
         | dateField DATE,
         | charField CHAR(5),
         | floatField FLOAT,
         | complexData ARRAY<STRING>
         | )
         | STORED BY 'carbondata'
         | TBLPROPERTIES('SORT_COLUMNS'='intField, stringField, charField')
       """.stripMargin)

    val path = s"$rootPath/examples/spark2/src/main/resources/data.csv"

    // scalastyle:off
    spark.sql(
      s"""
         | LOAD DATA LOCAL INPATH '$path'
         | INTO TABLE no_sort_columns_table
         | OPTIONS('FILEHEADER'='shortField,intField,bigintField,doubleField,stringField,timestampField,decimalField,dateField,charField,floatField,complexData',
         | 'COMPLEX_DELIMITER_LEVEL_1'='#')
       """.stripMargin)
    spark.sql(
      s"""
         | LOAD DATA LOCAL INPATH '$path'
         | INTO TABLE sort_columns_table
         | OPTIONS('FILEHEADER'='shortField,intField,bigintField,doubleField,stringField,timestampField,decimalField,dateField,charField,floatField,complexData',
         | 'COMPLEX_DELIMITER_LEVEL_1'='#')
       """.stripMargin)
    // scalastyle:on

    spark.sql(
      s"""SELECT * FROM no_sort_columns_table""".stripMargin).show()

    spark.sql(
      s"""SELECT * FROM sort_columns_table""".stripMargin).show()

    // Drop table
    spark.sql("DROP TABLE IF EXISTS no_sort_columns_table")
    spark.sql("DROP TABLE IF EXISTS sort_columns_table")

    spark.stop()
  }

}
