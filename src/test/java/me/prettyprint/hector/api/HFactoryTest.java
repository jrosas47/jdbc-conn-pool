/*
 * Copyright (c) 2009 Openwave Systems Inc. All rights reserved.
 *
 * The copyright to the computer software herein is the property of
 * Openwave Systems Inc. The software may be used and/or copied only
 * with the written permission of Openwave Systems Inc. or in accordance
 * with the terms and conditions stipulated in the agreement/contract
 * under which the software has been supplied.
 *
 * $Id: $
 */
package me.prettyprint.hector.api;


import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.datastax.drivers.jdbc.pool.cassandra.ClockResolution;
import com.datastax.drivers.jdbc.pool.cassandra.factory.HFactory;
import com.datastax.drivers.jdbc.pool.cassandra.service.clock.MicrosecondsClockResolution;
import com.datastax.drivers.jdbc.pool.cassandra.service.clock.MicrosecondsSyncClockResolution;
import com.datastax.drivers.jdbc.pool.cassandra.service.clock.MillisecondsClockResolution;
import com.datastax.drivers.jdbc.pool.cassandra.service.clock.SecondsClockResolution;


/**
 * @author Patricio Echague (patricioe@gmail.com)
 *
 */
public class HFactoryTest {

  @Test
  public void testCreateClockResolution() throws Exception {
    try {
      HFactory.createClockResolution("ItDoesNotExist");
      fail();
    } catch (RuntimeException e) {
        // good!
    }

    assertTrue(HFactory.createClockResolution(ClockResolution.SECONDS) instanceof SecondsClockResolution);
    assertTrue(HFactory.createClockResolution(ClockResolution.MILLISECONDS) instanceof MillisecondsClockResolution);
    assertTrue(HFactory.createClockResolution(ClockResolution.MICROSECONDS) instanceof MicrosecondsClockResolution);
    assertTrue(HFactory.createClockResolution(ClockResolution.MICROSECONDS_SYNC)
            instanceof MicrosecondsSyncClockResolution);
  }
}