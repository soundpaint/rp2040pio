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
 * A pair of an RX FIFO and a TX FIFO, each having a capacity of DEPTH
 * words of 32 bits.  One of the FIFOs' capacity can be reconfigured
 * to be joined with the capacity of the other FIFO, thus resulting in
 * 8 words of capacity for that FIFO and leaving no capacity left for
 * the other FIFO.
 */
public class FIFO
{
  public static final int DEPTH = 4;

  /**
   * RX queue from state machine to system.
   */
  private final Queue<Integer> rx;

  /**
   * TX queue from system to state machine.
   */
  private final Queue<Integer> tx;

  private boolean regSHIFTCTRL_FJOIN_RX; // bit 31 of SHIFTCTRL
  private boolean regSHIFTCTRL_FJOIN_TX; // bit 30 of SHIFTCTRL

  public FIFO()
  {
    rx = new ArrayDeque<Integer>();
    tx = new ArrayDeque<Integer>();
    reset();
  }

  public void reset()
  {
    rx.clear();
    tx.clear();
    regSHIFTCTRL_FJOIN_RX = false;
    regSHIFTCTRL_FJOIN_TX = false;
  }

  public void setJoinRX(final boolean join)
  {
    if (join == regSHIFTCTRL_FJOIN_RX)
      return;
    if (join)
      regSHIFTCTRL_FJOIN_TX = false;
    rx.clear();
    tx.clear();
  }

  public boolean getJoinRX()
  {
    return regSHIFTCTRL_FJOIN_RX;
  }

  public int getRXLevel()
  {
    return rx.size();
  }

  public boolean fstatRxFull()
  {
    // bit 0, 1, 2 or 3 (for SM_0 .. SM_3) of FSTAT
    return rx.size() >= (regSHIFTCTRL_FJOIN_RX ? 2 * DEPTH : DEPTH);
  }

  public boolean fstatRxEmpty()
  {
    // bit 8, 9, 10 or 11 (for SM_0 .. SM_3) of FSTAT
    return rx.size() == 0;
  }

  public void rxPush(final int value)
  {
    if (!fstatRxFull())
      rx.add(value);
  }

  public int rxDMARead()
  {
    if (!fstatRxEmpty())
      return rx.remove();
    else
      throw new IllegalStateException("RX FIFO empty");
  }

  public void setJoinTX(final boolean join)
  {
    if (join == regSHIFTCTRL_FJOIN_TX)
      return;
    if (join)
      regSHIFTCTRL_FJOIN_RX = false;
    rx.clear();
    tx.clear();
  }

  public boolean getJoinTX()
  {
    return regSHIFTCTRL_FJOIN_TX;
  }

  public int getTXLevel()
  {
    return tx.size();
  }

  public boolean fstatTxFull()
  {
    // bit 16, 17, 18 or 19 (for SM_0 .. SM_3) of FSTAT
    return tx.size() >= (regSHIFTCTRL_FJOIN_TX ? 2 * DEPTH : DEPTH);
  }

  public boolean fstatTxEmpty()
  {
    // bit 24, 25, 26 or 27 (for SM_0 .. SM_3) of FSTAT
    return tx.size() == 0;
  }

  public int txPull()
  {
    if (!fstatTxEmpty())
      return rx.remove();
    return 0;
  }

  public void txDMAWrite(final int value)
  {
    if (!fstatTxFull())
      tx.add(value);
    else
      throw new IllegalStateException("TX FIFO full");
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
