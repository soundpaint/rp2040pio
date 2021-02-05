/*
 * @(#)FIFO.java 1.00 21/02/03
 *
 * Copyright (C) 2021 Jürgen Reuter
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * For updates and more info or contacting the author, visit:
 * <https://github.com/soundpaint/rp2040pio>
 *
 * Author's web site: www.juergen-reuter.de
 */
package org.soundpaint.rp2040pio;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * A pair of an RX FIFO and a TX FIFO, each having a capacity of 4
 * words of 32 bits.  One of the FIFOs' capacity can be reconfigured
 * to be joined with the capacity of the other FIFO, thus resulting in
 * 8 words of capacity for that FIFO and leaving no capacity left for
 * the other FIFO.
 */
public class FIFO
{

  /**
   * RX queue from state machine to system.
   */
  private final Queue<Integer> rx;

  private int rxCapacity;

  /**
   * TX queue from system to state machine.
   */
  private final Queue<Integer> tx;

  private int txCapacity;

  public FIFO()
  {
    rx = new ArrayDeque<Integer>();
    tx = new ArrayDeque<Integer>();
    reset();
  }

  public void reset()
  {
    rx.clear();
    rxCapacity = 4;
    tx.clear();
    txCapacity = 4;
  }

  public void rxJoin()
  {
    if (rxCapacity > 4)
      throw new InternalError("rx already joind");
    rx.clear();
    rxCapacity = 8;
    tx.clear();
    txCapacity = 0;
  }

  public boolean fstatRxFull()
  {
    return rx.size() >= rxCapacity;
  }

  public boolean fstatRxEmpty()
  {
    return rx.size() == 0;
  }

  public void rxPush(final int value)
  {
    if (!fstatRxFull())
      rx.add(value);
  }

  public void txJoin()
  {
    if (txCapacity > 4)
      throw new InternalError("tx already joind");
    tx.clear();
    txCapacity = 8;
    rx.clear();
    rxCapacity = 0;
  }

  public boolean fstatTxFull()
  {
    return tx.size() >= txCapacity;
  }

  public boolean fstatTxEmpty()
  {
    return tx.size() == 0;
  }

  public int txPull()
  {
    if (!fstatTxEmpty())
      return rx.remove();
    return 0;
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
