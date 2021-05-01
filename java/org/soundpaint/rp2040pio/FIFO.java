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

import java.util.LinkedList;
import java.util.Collections;

/**
 * A pair of an RX FIFO and a TX FIFO, each having a capacity of DEPTH
 * words of 32 bits.  One of the FIFOs' capacity can be reconfigured
 * to be joined with the capacity of the other FIFO, thus resulting in
 * 8 words of capacity for that FIFO and leaving no capacity left for
 * the other FIFO.
 */
public class FIFO implements Constants
{
  private static final Integer[] INTEGER_PROTOTYPE_ARRAY = new Integer[0];

  /**
   * RX queue from state machine to system.
   */
  private final LinkedList<Integer> rx;

  /**
   * TX queue from system to state machine.
   */
  private final LinkedList<Integer> tx;

  private boolean regSHIFTCTRL_FJOIN_RX; // bit 31 of SHIFTCTRL
  private boolean regSHIFTCTRL_FJOIN_TX; // bit 30 of SHIFTCTRL
  private boolean regFDEBUG_TXSTALL; // one of bits 27:24 of FDEBUG
  private boolean regFDEBUG_TXOVER; // one of bits 19:16 of FDEBUG
  private boolean regFDEBUG_RXUNDER; // one of bits 11:8 of FDEBUG
  private boolean regFDEBUG_RXSTALL; // one of bits 3:0 of FDEBUG

  public FIFO()
  {
    rx = new LinkedList<Integer>();
    tx = new LinkedList<Integer>();
    reset();
  }

  public synchronized void reset()
  {
    clear();
    regSHIFTCTRL_FJOIN_RX = false;
    regSHIFTCTRL_FJOIN_TX = false;
    regFDEBUG_TXSTALL = false;
    regFDEBUG_TXOVER = false;
    regFDEBUG_RXUNDER = false;
    regFDEBUG_RXSTALL = false;
  }

  public synchronized void clear()
  {
    rx.clear();
    tx.clear();
    notifyAll();
  }

  public synchronized void setJoinRX(final boolean join)
  {
    if (join == regSHIFTCTRL_FJOIN_RX)
      return;
    regSHIFTCTRL_FJOIN_RX = join;
    rx.clear();
    tx.clear();
    notifyAll();
  }

  public boolean getJoinRX()
  {
    return regSHIFTCTRL_FJOIN_RX;
  }

  public synchronized int getRXReadPointer()
  {
    return getJoinRX() ? 0 : 4; // TODO
  }

  public synchronized int getRXLevel()
  {
    return rx.size();
  }

  public synchronized boolean fstatRxFull()
  {
    // bit 0, 1, 2 or 3 (for SM_0…SM_3) of FSTAT
    return tx.size() >=
      (regSHIFTCTRL_FJOIN_TX ? 0 :
       (regSHIFTCTRL_FJOIN_RX ? 2 : 1 ) * FIFO_DEPTH);
  }

  public synchronized boolean fstatRxEmpty()
  {
    // bit 8, 9, 10 or 11 (for SM_0…SM_3) of FSTAT
    return rx.size() == 0;
  }

  /**
   * @return &lt;code&gt;true&lt;/code&gt; if the operation succeeded.
   */
  public synchronized boolean rxPush(final int value, final boolean stallIfFull)
  {
    if (!fstatRxFull()) {
      rx.add(value);
      notifyAll();
      return true;
    }
    if (stallIfFull) {
      regFDEBUG_RXSTALL = true;
    }
    notifyAll();
    return false;
  }

  public synchronized int rxDMARead()
  {
    final int value;
    if (!fstatRxEmpty()) {
      value = rx.remove();
      notifyAll();
    } else {
      regFDEBUG_RXUNDER = true;
      value = 0;
    }
    return value;
  }

  public boolean isRXUnder()
  {
    return regFDEBUG_RXUNDER;
  }

  public void clearRXUnder()
  {
    regFDEBUG_RXUNDER = false;
  }

  public boolean isRXStall()
  {
    return regFDEBUG_RXSTALL;
  }

  public void clearRXStall()
  {
    regFDEBUG_RXSTALL = false;
  }

  public synchronized void setJoinTX(final boolean join)
  {
    if (join == regSHIFTCTRL_FJOIN_TX)
      return;
    regSHIFTCTRL_FJOIN_TX = join;
    rx.clear();
    tx.clear();
    notifyAll();
  }

  public boolean getJoinTX()
  {
    return regSHIFTCTRL_FJOIN_TX;
  }

  public synchronized int getTXReadPointer()
  {
    return 0; // TODO
  }

  public synchronized int getTXLevel()
  {
    return tx.size();
  }

  public synchronized boolean fstatTxFull()
  {
    // bit 16, 17, 18 or 19 (for SM_0…SM_3) of FSTAT
    return tx.size() >=
      (regSHIFTCTRL_FJOIN_RX ? 0 :
       (regSHIFTCTRL_FJOIN_TX ? 2 : 1 ) * FIFO_DEPTH);
  }

  public synchronized boolean fstatTxEmpty()
  {
    // bit 24, 25, 26 or 27 (for SM_0…SM_3) of FSTAT
    return tx.size() == 0;
  }

  public synchronized int txPull(final boolean stallIfEmpty)
  {
    final int value;
    if (!fstatTxEmpty()) {
      value = tx.remove();
    } else {
      value = 0;
      if (stallIfEmpty) {
        regFDEBUG_TXSTALL = true;
      }
    }
    notifyAll();
    return value;
  }

  public synchronized void txDMAWrite(final int value)
  {
    if (!fstatTxFull()) {
      tx.add(value);
    } else {
      // overwrite most recent value
      final Integer[] values = tx.toArray(INTEGER_PROTOTYPE_ARRAY);
      if (values.length > 0) values[0] = value;
      tx.clear();
      Collections.addAll(tx, values);
      regFDEBUG_TXOVER = true;
    }
    notifyAll();
  }

  public boolean isTXOver()
  {
    return regFDEBUG_TXOVER;
  }

  public void clearTXOver()
  {
    regFDEBUG_TXOVER = false;
  }

  public boolean isTXStall()
  {
    return regFDEBUG_TXSTALL;
  }

  public void clearTXStall()
  {
    regFDEBUG_TXSTALL = false;
  }

  public int getMemValue(final int address)
  {
    if (address < 0) {
      throw new IllegalArgumentException("address < 0: " + address);
    }
    if (address > 2 * FIFO_DEPTH - 1) {
      throw new IllegalArgumentException("address > " +
                                         (2 * FIFO_DEPTH - 1) + ": " +
                                         address);
    }
    if (regSHIFTCTRL_FJOIN_RX && regSHIFTCTRL_FJOIN_TX)
      return 0;
    if (regSHIFTCTRL_FJOIN_RX) {
      return address < rx.size() ? rx.get(address) : 0;
    }
    if (regSHIFTCTRL_FJOIN_TX || (address < FIFO_DEPTH)) {
      return address < tx.size() ? tx.get(address) : 0;
    }
    return
      address - FIFO_DEPTH < rx.size() ? rx.get(address - FIFO_DEPTH) : 0;
  }

  public void setMemValue(final int address, final int value)
  {
    // TODO: Replace LinkedList FIFOs with static buffer, to be able
    // to implement this method.
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
