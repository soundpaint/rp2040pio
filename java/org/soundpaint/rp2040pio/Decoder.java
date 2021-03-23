/*
 * @(#)Decoder.java 1.00 21/01/31
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

import java.util.List;

/**
 * Instruction Decoder
 */
public class Decoder
{
  public static class DecodeException extends Exception
  {
    private static final long serialVersionUID = -3754988538292081517L;

    private final Instruction instruction;
    private final int opCode;

    private DecodeException()
    {
      throw new UnsupportedOperationException("unsupported empty constructor");
    }

    public DecodeException(final Instruction instruction, final int opCode)
    {
      super("decode failed: unsupported op-code: " +
            String.format("%04x", opCode));
      this.instruction = instruction;
      this.opCode = opCode;
    }

    public Instruction getInstruction() { return instruction; }

    public int getOpCode() { return opCode; }
  }

  private final Instructions instructions;

  private class Instructions
  {
    private final Instruction.Jmp jmp;
    private final Instruction.Wait wait;
    private final Instruction.In in;
    private final Instruction.Out out;
    private final Instruction.Push push;
    private final Instruction.Pull pull;
    private final Instruction.Mov mov;
    private final Instruction.Irq irq;
    private final Instruction.Set set;
    private final Instruction[] instructionSet;

    public Instructions()
    {
      jmp = new Instruction.Jmp();
      wait = new Instruction.Wait();
      in = new Instruction.In();
      out = new Instruction.Out();
      push = new Instruction.Push();
      pull = new Instruction.Pull();
      mov = new Instruction.Mov();
      irq = new Instruction.Irq();
      set = new Instruction.Set();
      instructionSet =
        new Instruction[] { jmp, wait, in, out, push, pull, mov, irq, set };
    }
  }

  public Decoder()
  {
    instructions = new Instructions();
  }

  public void reset()
  {
    for (final Instruction instruction : instructions.instructionSet) {
      instruction.reset();
    }
  }

  public Instruction decode(final short word,
                            final int pinCtrlSidesetCount,
                            final boolean execCtrlSideEn)
    throws DecodeException
  {
    final Instruction instruction;
    switch ((word >>> 13) & 0x7) {
    case 0b000:
      instruction = instructions.jmp;
      break;
    case 0b001:
      instruction = instructions.wait;
      break;
    case 0b010:
      instruction = instructions.in;
      break;
    case 0b011:
      instruction = instructions.out;
      break;
    case 0b100:
      if ((word & 0x80) == 0)
        instruction = instructions.push;
      else
        instruction = instructions.pull;
      break;
    case 0b101:
      instruction = instructions.mov;
      break;
    case 0b110:
      instruction = instructions.irq;
      break;
    case 0b111:
      instruction = instructions.set;
      break;
    default:
      throw new InternalError("unexpected case fall-through");
    }
    return instruction.decode(word, pinCtrlSidesetCount, execCtrlSideEn);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
