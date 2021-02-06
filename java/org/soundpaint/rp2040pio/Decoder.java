/*
 * @(#)SM.java 1.00 21/01/31
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

  public static class MultiDecodeException extends Exception
  {
    private static final long serialVersionUID = 1631236078957230846L;

    private final List<DecodeException> causes;

    private MultiDecodeException()
    {
      throw new UnsupportedOperationException("unsupported empty constructor");
    }

    public MultiDecodeException(final List<DecodeException> causes)
    {
      super("decode failed: unsupported op-code(s) found");
      this.causes = causes;
    }
    public List<DecodeException> getCauses() { return causes; }
  }

  private final SM sm;
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

    public Instructions()
    {
      jmp = new Instruction.Jmp(sm);
      wait = new Instruction.Wait(sm);
      in = new Instruction.In(sm);
      out = new Instruction.Out(sm);
      push = new Instruction.Push(sm);
      pull = new Instruction.Pull(sm);
      mov = new Instruction.Mov(sm);
      irq = new Instruction.Irq(sm);
      set = new Instruction.Set(sm);
    }
  }

  private Decoder()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public Decoder(final SM sm)
  {
    if (sm == null) throw new NullPointerException("sm");
    this.sm = sm;
    instructions = new Instructions();
  }

  public Instruction decode(final short word) throws DecodeException
  {
    switch ((word >>> 13) & 0x7) {
    case 0b000: return instructions.jmp.decode(word);
    case 0b001: return instructions.wait.decode(word);
    case 0b010: return instructions.in.decode(word);
    case 0b011: return instructions.out.decode(word);
    case 0b100:
      if ((word & 0x80) == 0)
        return instructions.push.decode(word);
      else
        return instructions.pull.decode(word);
    case 0b101: return instructions.mov.decode(word);
    case 0b110: return instructions.irq.decode(word);
    case 0b111: return instructions.set.decode(word);
    default:
      throw new InternalError("unexpected case fall-through");
    }
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
