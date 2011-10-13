/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datastax.drivers.jdbc.pool.cassandra.exceptions;


/**
 * @author Ran Tavory (rantav@gmail.com)
 */
public final class HUnavailableException extends HectorException {

  private static final String ERR_MSG = 
    ": May not be enough replicas present to handle consistency level.";
  private static final long serialVersionUID = 1971498442136497970L;

  public HUnavailableException(String s) {
    super(s + ERR_MSG);
  }

  public HUnavailableException(Throwable t) {
    super(ERR_MSG,t);
  }
}
