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

/**
 * A pair of an RX FIFO and a TX FIFO, each having a capacity of DEPTH
 * words of 32 bits.  One of the FIFOs' capacity can be reconfigured
 * to be joined with the capacity of the other FIFO, thus resulting in
 * 8 words of capacity for that FIFO and leaving no capacity left for
 * the other FIFO.
 */
public class FIFO implements Constants
{
  private static int JOINED_FIFO_DEPTH = FIFO_DEPTH + FIFO_DEPTH;

  private static enum Mode
  {
    JoinNone(false, false, FIFO_DEPTH, FIFO_DEPTH),
    JoinTX(true, false, JOINED_FIFO_DEPTH, 0),
    JoinRX(false, true, 0, JOINED_FIFO_DEPTH),
    JoinBoth(true, true, 0, 0);

    private final boolean joinTX;
    private final boolean joinRX;
    private final int txSize;
    private final int rxSize;

    private Mode(final boolean joinTX, final boolean joinRX,
                 final int txSize, final int rxSize)
    {
      this.joinTX = joinTX;
      this.joinRX = joinRX;
      this.txSize = txSize;
      this.rxSize = rxSize;
    }

    private boolean isJoinTX() { return joinTX; }
    private boolean isJoinRX() { return joinRX; }
    private int getTXSize() { return txSize; }
    private int getRXSize() { return rxSize; }

    private int incPtrTX(final int ptr)
    {
      return
        txSize == 0 ? 0 :
        (ptr + 1) & (txSize - 1);
    }

    private int incPtrRX(final int ptr)
    {
      final int rxOffset = JOINED_FIFO_DEPTH - rxSize;
      return
        rxSize == 0 ? 0 :
        ((ptr + 1) & (rxSize - 1)) + rxOffset;
    }

    private static Mode fromJoins(final boolean joinTX, final boolean joinRX)
    {
      return
        joinTX ? (joinRX ? JoinBoth : JoinTX) : (joinRX ? JoinRX : JoinNone);
    }
  }

  private final int smNum;
  private final IRQ irq;
  private int[] memory;
  private Mode mode;
  private int txReadPtr;
  private int txWritePtr;
  private boolean txFull;
  private int rxReadPtr;
  private int rxWritePtr;
  private boolean rxFull;
  private boolean regFDEBUG_TXSTALL; // one of bits 27:24 of FDEBUG
  private boolean regFDEBUG_TXOVER; // one of bits 19:16 of FDEBUG
  private boolean regFDEBUG_RXUNDER; // one of bits 11:8 of FDEBUG
  private boolean regFDEBUG_RXSTALL; // one of bits 3:0 of FDEBUG

  public FIFO(final int smNum, final IRQ irq)
  {
    Constants.checkSmNum(smNum);
    if (irq == null) {
      throw new NullPointerException("irq");
    }
    this.smNum = smNum;
    this.irq = irq;
    memory = new int[JOINED_FIFO_DEPTH];
    reset();
  }

  public synchronized void reset()
  {
    reset(false, false);
  }

  private void reset(final boolean joinTX, final boolean joinRX)
  {
    for (int index = 0; index < memory.length; index++) {
      memory[index] = 0;
    }
    mode = Mode.fromJoins(joinTX, joinRX);
    regFDEBUG_TXSTALL = false;
    regFDEBUG_TXOVER = false;
    regFDEBUG_RXUNDER = false;
    regFDEBUG_RXSTALL = false;
    txReadPtr = joinTX && joinRX ? -1 : 0;
    txWritePtr = txReadPtr;
    txFull = joinRX;
    rxReadPtr = joinTX && joinRX ? -1 : (joinRX ? 0 : FIFO_DEPTH);
    rxWritePtr = rxReadPtr;
    rxFull = joinTX;
    irq.setRxNEmpty(smNum, !fstatRxEmpty());
    irq.setTxNFull(smNum, !fstatTxFull());
    notifyAll();
  }

  public synchronized void setJoinRX(final boolean join)
  {
    if (mode.isJoinRX() == join) return;
    reset(mode.isJoinTX(), join);
  }

  public boolean getJoinRX()
  {
    return mode.isJoinRX();
  }

  private int getRXSize()
  {
    return mode.getRXSize();
  }

  public synchronized int getRXReadPointer()
  {
    return rxReadPtr;
  }

  public synchronized boolean fstatRxFull()
  {
    // bit 0, 1, 2 or 3 (for SM_0…SM_3) of FSTAT
    return rxFull;
  }

  public synchronized boolean fstatRxEmpty()
  {
    // bit 8, 9, 10 or 11 (for SM_0…SM_3) of FSTAT
    return (rxReadPtr == rxWritePtr) && !rxFull;
  }

  public synchronized int getRXLevel()
  {
    final int rxSize = mode.getRXSize();
    return
      rxFull ? rxSize : (rxSize + rxWritePtr - rxReadPtr) & (rxSize - 1);
  }

  /**
   * @return &lt;code&gt;true&lt;/code&gt; if the operation succeeded.
   */
  public synchronized boolean rxPush(final int value, final boolean stallIfFull)
  {
    final boolean modified;
    if (!fstatRxFull()) {
      memory[rxWritePtr] = value;
      rxWritePtr = mode.incPtrRX(rxWritePtr);
      rxFull = rxWritePtr == rxReadPtr;
      modified = true;
    } else {
      if (stallIfFull) {
        regFDEBUG_RXSTALL = true;
      }
      modified = false;
    }
    irq.setRxNEmpty(smNum, !fstatRxEmpty());
    notifyAll();
    return modified;
  }

  public synchronized int rxDMARead()
  {
    final int value;
    if (!fstatRxEmpty()) {
      value = memory[rxReadPtr];
      rxReadPtr = mode.incPtrRX(rxReadPtr);
      rxFull = false;
    } else {
      regFDEBUG_RXUNDER = true;
      value = 0;
    }
    irq.setRxNEmpty(smNum, !fstatRxEmpty());
    notifyAll();
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
    if (mode.isJoinTX() == join) return;
    reset(join, mode.isJoinRX());
  }

  public boolean getJoinTX()
  {
    return mode.isJoinTX();
  }

  public synchronized int getTXReadPointer()
  {
    return txReadPtr;
  }

  public synchronized boolean fstatTxFull()
  {
    // bit 16, 17, 18 or 19 (for SM_0…SM_3) of FSTAT
    return txFull;
  }

  public synchronized boolean fstatTxEmpty()
  {
    // bit 24, 25, 26 or 27 (for SM_0…SM_3) of FSTAT
    return (txReadPtr == txWritePtr) && !txFull;
  }

  public synchronized int getTXLevel()
  {
    final int txSize = mode.getTXSize();
    return
      txFull ? txSize : (txSize + txWritePtr - txReadPtr) & (txSize - 1);
  }

  public synchronized int txPull(final boolean stallIfEmpty)
  {
    final int value;
    if (!fstatTxEmpty()) {
      value = memory[txReadPtr];
      txReadPtr = mode.incPtrTX(txReadPtr);
      txFull = false;
    } else {
      value = 0;
      if (stallIfEmpty) {
        regFDEBUG_TXSTALL = true;
      }
    }
    irq.setTxNFull(smNum, !fstatTxFull());
    notifyAll();
    return value;
  }

  public synchronized void txDMAWrite(final int value)
  {
    if (!fstatTxFull()) {
      memory[txWritePtr] = value;
      txWritePtr = mode.incPtrTX(txWritePtr);
      txFull = txWritePtr == txReadPtr;
    } else {
      // overwrite most recent value
      if (txWritePtr >= 0) {
        memory[txWritePtr] = value;
      }
      regFDEBUG_TXOVER = true;
    }
    irq.setTxNFull(smNum, !fstatTxFull());
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
    Constants.checkFIFOAddr(address, "address");
    return memory[address];
  }

  public void setMemValue(final int address, final int value)
  {
    Constants.checkFIFOAddr(address, "address");
    memory[address] = value;
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
