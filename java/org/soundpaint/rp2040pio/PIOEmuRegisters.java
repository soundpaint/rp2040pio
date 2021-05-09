/*
 * @(#)PIOEmuRegisters.java 1.00 21/03/06
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
import java.util.function.LongSupplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.soundpaint.rp2040pio.doctool.RegistersDocs;

/**
 * Facade to additonal emulator properties of the internal subsystems
 * of a PIO that are not available via the PIORegisters facade.  This
 * facade is in particular intended for use by software that wants to
 * exploit the emulator's debug facilities.
 */
public abstract class PIOEmuRegisters extends AbstractRegisters
  implements Constants
{
  public enum Regs implements RegistersDocs<Regs> {
    SM0_REGX("Direct read / write access to the SM's%n" +
             "scratch register X.",
             new BitsInfo[] {
               new BitsInfo(31, 0, null, null, BitsType.RW, 0)
             }),
    SM0_REGY("Direct read / write access to the SM's%n" +
             "scratch register Y.",
             new BitsInfo[] {
               new BitsInfo(31, 0, null, null, BitsType.RW, 0)
             }),
    SM0_PC("Direct read-only access to the SM's%n" +
           "instruction pointer / program counter.",
           new BitsInfo[] {
             new BitsInfo(31, 0, null, null, BitsType.RW, 0)
           }),
    SM0_ISR("Direct read / write access to the SM's%n" +
            "input shift register.",
            new BitsInfo[] {
              new BitsInfo(31, 0, null, null, BitsType.RW, 0)
             }),
    SM0_ISR_SHIFT_COUNT("Direct read / write access to the SM's%n" +
                        "input shift count register.",
                        new BitsInfo[] {
                          new BitsInfo(31, 0, null, null, BitsType.RW, 0)
                        }),
    SM0_OSR("Direct read / write access to all of the SM's%n" +
            "output shift register.",
            new BitsInfo[] {
              new BitsInfo(31, 0, null, null, BitsType.RW, 0)
             }),
    SM0_OSR_SHIFT_COUNT("Direct read / write access to the SM's%n" +
                        "output shift count register.",
                        new BitsInfo[] {
                          new BitsInfo(31, 0, null, null, BitsType.RW, 0)
                        }),
    SM0_FIFO_MEM0("Read / write access to FIFO memory word.",
                  new BitsInfo[] {
                    new BitsInfo(31, 0, null, null, BitsType.RW, 0)
                  }),
    SM0_FIFO_MEM1(Regs.SM0_FIFO_MEM0),
    SM0_FIFO_MEM2(Regs.SM0_FIFO_MEM0),
    SM0_FIFO_MEM3(Regs.SM0_FIFO_MEM0),
    SM0_FIFO_MEM4(Regs.SM0_FIFO_MEM0),
    SM0_FIFO_MEM5(Regs.SM0_FIFO_MEM0),
    SM0_FIFO_MEM6(Regs.SM0_FIFO_MEM0),
    SM0_FIFO_MEM7(Regs.SM0_FIFO_MEM0),
    SM0_INSTR_ORIGIN("Direct read-only access to the origin of the SM's%n" +
                     "currently executed instruction.  The mode bits%n" +
                     "determine the origin category.  If the origin%n" +
                     "category is memory address, the memory address bits%n" +
                     "will contain the memory instruction's address.%n" +
                     "Otherwise, the bits of the memory address are%n" +
                     "undefined.%n" +
                     "Note that for memory instructions, the address may%n" +
                     "differ from the value of the instruction pointer PC,%n" +
                     "if the PC has already been updated while the%n" +
                     "instruction is still in progress.",
                     new BitsInfo[] {
                       new BitsInfo(31, 7, null, null, BitsType.RESERVED, null),
                       new BitsInfo(6, 5, "CATEGORY",
                                    "For forced instructions,%n" +
                                    "this is the value " +
                                    (INSTR_ORIGIN_FORCED & 0x3) +
                                    ".%n" +
                                    "For EXEC'd instructions,%n" +
                                    "this is the value " +
                                    (INSTR_ORIGIN_EXECED & 0x3) +
                                    ".%n" +
                                    "Otherwise (e.g. after reset),%n" +
                                    "this is the value " +
                                    (INSTR_ORIGIN_UNKNOWN & 0x3) +
                                    ".%n",
                                    BitsType.RO, INSTR_ORIGIN_UNKNOWN & 0x3),
                       new BitsInfo(4, 0, "MEMORY_ADDRESS",
                                    "memory address value (0x00…0x1f)",
                                    BitsType.RO, 0)
                     }),
    SM0_DELAY("Direct read-only access to the SM's%n" +
              "currently executed instruction's number of delay cycles.",
              new BitsInfo[] {
                new BitsInfo(31, 5, null, null, BitsType.RESERVED, null),
                new BitsInfo(4, 0, null, null, BitsType.RO, 0)
              }),
    SM0_DELAY_CYCLE("Read-only access to the SM's delay status.",
                    new BitsInfo[] {
                      new BitsInfo(31, 1, null, null, BitsType.RESERVED, null),
                      new BitsInfo(0, 0, "DELAY_CYCLE",
                                   "0x1, if the currently executed cycles%n" +
                                   "is a delay cycle.", BitsType.RO, 0)
                    }),
    SM0_PENDING_DELAY("Direct read-only access to the SM's%n" +
                      "number of pending delay cycles.",
                      new BitsInfo[] {
                        new BitsInfo(31, 5, null, null, BitsType.RESERVED, null),
                        new BitsInfo(4, 0, "PENDING_DELAY",
                                     "Number (0x00…0x1f) of pending delays%n" +
                                     "of the currently executed instruction.",
                                     BitsType.RO, 0)
                      }),
    SM0_FORCED_INSTR("Direct read-only access to the op-code of a forced%n" +
                      "instruction.",
                      new BitsInfo[] {
                        new BitsInfo(31, 17, null, null,
                                     BitsType.RESERVED, null),
                        new BitsInfo(16, 16, "PENDING",
                                     "0x1, if a forced instruction is%n" +
                                     "awaiting execution, otherwise 0x0.",
                                     BitsType.RO, 0),
                        new BitsInfo(15, 0, "INSTR",
                                     "Instruction op-code, if any;%n" +
                                     "otherwise, 0x0000.",
                                     BitsType.RO, 0)
                      }),
    SM0_CLK_ENABLE("Read-only access to the SM's delay status.",
                   new BitsInfo[] {
                     new BitsInfo(31, 1, null, null, BitsType.RESERVED, null),
                     new BitsInfo(0, 0, "DELAY_CYCLE",
                                  "0x1, if in the current cycle the clock%n" +
                                  "enable signal evaluates to 0x1.",
                                  BitsType.RO, 0)
                   }),
    SM0_BREAKPOINTS("Each bit of this values corresponds to each of the%n" +
                    "32 memory locations of the PIO instruction memory%n" +
                    "(with the LSB of the word corresponding to the lowest%n" +
                    "memory address).  Setting a bit to 1 marks the%n" +
                    "corresponding memory address as location of a%n" +
                    "breakpoint.  Setting a bit to 0 removes the%n" +
                    "breakpoint.%n" +
                    "%n" +
                    "As soon as the program counter of the state machine%n" +
                    "reaches an address that is marked as a breakpoint,%n" +
                    "master clock MASTERCLK_MODE will be automatically set%n" +
                    "to single step mode.",
                    IntStream.rangeClosed(0, 31).boxed()
                    .map(n -> new BitsInfo(31 - n, 31 - n, "BP_MEM" + (31 - n),
                                           "0x1, if the memory address is " +
                                           "marked as breakpoint.",
                                           BitsType.RW, 0))
                    .collect(Collectors.toList())),
    SM0_TRACEPOINTS("Tracepoints work like breakpoints with the difference%n" +
                    "that master clock MASTERCLK_MODE it not automatically%n" +
                    "set to single step mode, but instead a message is%n" +
                    "typically printed to console output (depending on%n" +
                    "the specific client application).  The message may,%n" +
                    "for example, caontain the state machine's number and%n" +
                    "disassembled instruction with prefixed instruction%n" +
                    "memory address.  Tracepoints work in all master clock%n" +
                    "MASTERCLK_MODE modes.",
                    IntStream.rangeClosed(0, 31).boxed()
                    .map(n -> new BitsInfo(31 - n, 31 - n, "TP_MEM" + (31 - n),
                                           "0x1, if the memory address is " +
                                           "marked as tracepoint.",
                                           BitsType.RW, 0))
                    .collect(Collectors.toList())),
    SM1_REGX(Regs.SM0_REGX),
    SM1_REGY(Regs.SM0_REGY),
    SM1_PC(Regs.SM0_PC),
    SM1_ISR(Regs.SM0_ISR),
    SM1_ISR_SHIFT_COUNT(Regs.SM0_ISR_SHIFT_COUNT),
    SM1_OSR(Regs.SM0_OSR),
    SM1_OSR_SHIFT_COUNT(Regs.SM0_OSR_SHIFT_COUNT),
    SM1_FIFO_MEM0(Regs.SM0_FIFO_MEM0),
    SM1_FIFO_MEM1(Regs.SM0_FIFO_MEM0),
    SM1_FIFO_MEM2(Regs.SM0_FIFO_MEM0),
    SM1_FIFO_MEM3(Regs.SM0_FIFO_MEM0),
    SM1_FIFO_MEM4(Regs.SM0_FIFO_MEM0),
    SM1_FIFO_MEM5(Regs.SM0_FIFO_MEM0),
    SM1_FIFO_MEM6(Regs.SM0_FIFO_MEM0),
    SM1_FIFO_MEM7(Regs.SM0_FIFO_MEM0),
    SM1_INSTR_ORIGIN(Regs.SM0_INSTR_ORIGIN),
    SM1_DELAY(Regs.SM0_DELAY),
    SM1_DELAY_CYCLE(Regs.SM0_DELAY_CYCLE),
    SM1_PENDING_DELAY(Regs.SM0_PENDING_DELAY),
    SM1_FORCED_INSTR(Regs.SM0_FORCED_INSTR),
    SM1_CLK_ENABLE(Regs.SM0_CLK_ENABLE),
    SM1_BREAKPOINTS(Regs.SM0_BREAKPOINTS),
    SM1_TRACEPOINTS(Regs.SM0_TRACEPOINTS),
    SM2_REGX(Regs.SM0_REGX),
    SM2_REGY(Regs.SM0_REGY),
    SM2_PC(Regs.SM0_PC),
    SM2_ISR(Regs.SM0_ISR),
    SM2_ISR_SHIFT_COUNT(Regs.SM0_ISR_SHIFT_COUNT),
    SM2_OSR(Regs.SM0_OSR),
    SM2_OSR_SHIFT_COUNT(Regs.SM0_OSR_SHIFT_COUNT),
    SM2_FIFO_MEM0(Regs.SM0_FIFO_MEM0),
    SM2_FIFO_MEM1(Regs.SM0_FIFO_MEM0),
    SM2_FIFO_MEM2(Regs.SM0_FIFO_MEM0),
    SM2_FIFO_MEM3(Regs.SM0_FIFO_MEM0),
    SM2_FIFO_MEM4(Regs.SM0_FIFO_MEM0),
    SM2_FIFO_MEM5(Regs.SM0_FIFO_MEM0),
    SM2_FIFO_MEM6(Regs.SM0_FIFO_MEM0),
    SM2_FIFO_MEM7(Regs.SM0_FIFO_MEM0),
    SM2_INSTR_ORIGIN(Regs.SM0_INSTR_ORIGIN),
    SM2_DELAY(Regs.SM0_DELAY),
    SM2_DELAY_CYCLE(Regs.SM0_DELAY_CYCLE),
    SM2_PENDING_DELAY(Regs.SM0_PENDING_DELAY),
    SM2_FORCED_INSTR(Regs.SM0_FORCED_INSTR),
    SM2_CLK_ENABLE(Regs.SM0_CLK_ENABLE),
    SM2_BREAKPOINTS(Regs.SM0_BREAKPOINTS),
    SM2_TRACEPOINTS(Regs.SM0_TRACEPOINTS),
    SM3_REGX(Regs.SM0_REGX),
    SM3_REGY(Regs.SM0_REGY),
    SM3_PC(Regs.SM0_PC),
    SM3_ISR(Regs.SM0_ISR),
    SM3_ISR_SHIFT_COUNT(Regs.SM0_ISR_SHIFT_COUNT),
    SM3_OSR(Regs.SM0_OSR),
    SM3_OSR_SHIFT_COUNT(Regs.SM0_OSR_SHIFT_COUNT),
    SM3_FIFO_MEM0(Regs.SM0_FIFO_MEM0),
    SM3_FIFO_MEM1(Regs.SM0_FIFO_MEM0),
    SM3_FIFO_MEM2(Regs.SM0_FIFO_MEM0),
    SM3_FIFO_MEM3(Regs.SM0_FIFO_MEM0),
    SM3_FIFO_MEM4(Regs.SM0_FIFO_MEM0),
    SM3_FIFO_MEM5(Regs.SM0_FIFO_MEM0),
    SM3_FIFO_MEM6(Regs.SM0_FIFO_MEM0),
    SM3_FIFO_MEM7(Regs.SM0_FIFO_MEM0),
    SM3_INSTR_ORIGIN(Regs.SM0_INSTR_ORIGIN),
    SM3_DELAY(Regs.SM0_DELAY),
    SM3_DELAY_CYCLE(Regs.SM0_DELAY_CYCLE),
    SM3_PENDING_DELAY(Regs.SM0_PENDING_DELAY),
    SM3_FORCED_INSTR(Regs.SM0_FORCED_INSTR),
    SM3_CLK_ENABLE(Regs.SM0_CLK_ENABLE),
    SM3_BREAKPOINTS(Regs.SM0_BREAKPOINTS),
    SM3_TRACEPOINTS(Regs.SM0_TRACEPOINTS),
    INSTR_MEM0("Read / write access to instruction memory word.",
               new BitsInfo[] {
                 new BitsInfo(31, 0, null, null, BitsType.RW, 0)
               }),
    INSTR_MEM1(Regs.INSTR_MEM0),
    INSTR_MEM2(Regs.INSTR_MEM0),
    INSTR_MEM3(Regs.INSTR_MEM0),
    INSTR_MEM4(Regs.INSTR_MEM0),
    INSTR_MEM5(Regs.INSTR_MEM0),
    INSTR_MEM6(Regs.INSTR_MEM0),
    INSTR_MEM7(Regs.INSTR_MEM0),
    INSTR_MEM8(Regs.INSTR_MEM0),
    INSTR_MEM9(Regs.INSTR_MEM0),
    INSTR_MEM10(Regs.INSTR_MEM0),
    INSTR_MEM11(Regs.INSTR_MEM0),
    INSTR_MEM12(Regs.INSTR_MEM0),
    INSTR_MEM13(Regs.INSTR_MEM0),
    INSTR_MEM14(Regs.INSTR_MEM0),
    INSTR_MEM15(Regs.INSTR_MEM0),
    INSTR_MEM16(Regs.INSTR_MEM0),
    INSTR_MEM17(Regs.INSTR_MEM0),
    INSTR_MEM18(Regs.INSTR_MEM0),
    INSTR_MEM19(Regs.INSTR_MEM0),
    INSTR_MEM20(Regs.INSTR_MEM0),
    INSTR_MEM21(Regs.INSTR_MEM0),
    INSTR_MEM22(Regs.INSTR_MEM0),
    INSTR_MEM23(Regs.INSTR_MEM0),
    INSTR_MEM24(Regs.INSTR_MEM0),
    INSTR_MEM25(Regs.INSTR_MEM0),
    INSTR_MEM26(Regs.INSTR_MEM0),
    INSTR_MEM27(Regs.INSTR_MEM0),
    INSTR_MEM28(Regs.INSTR_MEM0),
    INSTR_MEM29(Regs.INSTR_MEM0),
    INSTR_MEM30(Regs.INSTR_MEM0),
    INSTR_MEM31(Regs.INSTR_MEM0),
    TXF0("Direct read access to the TX FIFO for the corresponding state%n" +
         "machine.  Each read pops one word from the FIFO. Attempting to%n" +
         "read from an empty FIFO has no effect on the FIFO state,%n" +
         "and sets the sticky FDEBUG_TXUNDER error flag for this FIFO.%n" +
         "The data returned to the system on a read from an empty FIFO%n" +
         "is undefined.",
         new BitsInfo[] {
           new BitsInfo(31, 0, null, null, BitsType.RF, null)
         }),
    TXF1(Regs.TXF0),
    TXF2(Regs.TXF0),
    TXF3(Regs.TXF0),
    RXF0("Direct write access to the RX FIFO for the corresponding state%n" +
         "machine.  Each write pushes one word to the FIFO.  Attempting to%n" +
         "write to a full FIFO has no effect on the FIFO state or contents,%n" +
         "and sets the sticky FDEBUG_RXOVER error flag for this FIFO.",
         new BitsInfo[] {
           new BitsInfo(31, 0, null, null, BitsType.WF, 0)
         }),
    RXF1(Regs.RXF0),
    RXF2(Regs.RXF0),
    RXF3(Regs.RXF0),
    FREAD_PTR("Read pointers of all of the SM's TX and RX FIFOs.",
              IntStream.rangeClosed(0, 7).boxed()
              .map(n -> new BitsInfo(31 - (n << 2),
                                     28 - (n << 2),
                                     ((n & 0x1) == 0 ? "TX" : "RX") + "F" +
                                     (n >> 1) + "_READ_PTR",
                                     "Offset (0…7) within FIFO memory for%n" +
                                     "the next FIFO read operation",
                                     BitsType.RO, 0))
              .collect(Collectors.toList())),
    GPIO_PINS("Direct read / write access to all of the 32 GPIO pins.",
              IntStream.rangeClosed(0, 31).boxed()
              .map(n -> new BitsInfo(31 - n, 31 - n, "GPIO_PIN" + (31 - n),
                                     "0x1 for HIGH or 0x0 for LOW",
                                     BitsType.RW, 0))
              .collect(Collectors.toList())),
    GPIO_PINDIRS("Direct read / write access to all of the 32 GPIO pin%n" +
                 "directions.",
                 IntStream.rangeClosed(0, 31).boxed()
                 .map(n -> new BitsInfo(31 - n, 31 - n,
                                        "GPIO_PINDIR" + (31 - n),
                                        "0x1 for pin direction out or%n" +
                                        "0x0 for pin direction in",
                                        BitsType.RW, 0))
                 .collect(Collectors.toList()));

    public static String getRegisterSetLabel()
    {
      return "Emulator PIO Registers";
    }

    public static String getRegisterSetDescription()
    {
      return
        "The PIO emulator provides registers in addition to those%n" +
        "of the PIO as specified in the RP2040 datasheet to allow%n" +
        "for inspection of more details of the PIO's internal state%n" +
        "such as its scratch registers X and Y, its shift registers%n" +
        "ISR, OSR, FIFO memory, and read access to PIO instruction%n" +
        "memory for enhanced debugging of programs.%n" +
        "Base address for the two emulator PIO register sets (one %n" +
        "register set for each of the two PIOs) is%n" +
        String.format("0x%08x and 0x%08x for PIO0 and PIO1, respectively.%n",
                      PIO0_EMU_BASE, PIO1_EMU_BASE);
    }

    private final String info;
    private final RegisterDetails registerDetails;

    private Regs()
    {
      throw new UnsupportedOperationException("unsupported empty constructor");
    }

    private Regs(final Regs ref)
    {
      this(ref.getInfo(), ref.getRegisterDetails());
    }

    private Regs(final String info, final BitsInfo[] bitsInfos)
    {
      this(info,
           bitsInfos == null ?
           (RegisterDetails)null :
           new RegisterDetails(info, bitsInfos));
    }

    private Regs(final String info, final List<BitsInfo> bitsInfos)
    {
      this(info,
           bitsInfos == null ?
           (RegisterDetails)null :
           new RegisterDetails(info, bitsInfos));
    }

    private Regs(final String info, final RegisterDetails registerDetails)
    {
      this.info = info;
      this.registerDetails = registerDetails;
    }

    @Override
    public String getInfo()
    {
      return info;
    }

    @Override
    public RegisterDetails getRegisterDetails()
    {
      return registerDetails;
    }
  }

  protected static final Regs[] REGS = Regs.values();

  @Override
  @SuppressWarnings("unchecked")
  protected <T extends Enum<T>> T[] getRegs() { return (T[])REGS; }

  protected static final int SM_SIZE =
    Regs.SM1_REGX.ordinal() - Regs.SM0_REGX.ordinal();

  public static int getAddress(final int pioNum,
                               final PIOEmuRegisters.Regs register)
  {
    Constants.checkPioNum(pioNum, "PIO index number");
    if (register == null) {
      throw new NullPointerException("register");
    }
    return Constants.getPIOEmuBaseAddress(pioNum) + 0x4 * register.ordinal();
  }

  public static int getSMAddress(final int pioNum,
                                 final int smNum,
                                 final PIOEmuRegisters.Regs register)
  {
    Constants.checkPioNum(pioNum, "PIO index number");
    Constants.checkSmNum(smNum);
    if (register == null) {
      throw new NullPointerException("register");
    }
    switch (register) {
    case SM0_REGX:
    case SM0_REGY:
    case SM0_PC:
    case SM0_ISR:
    case SM0_ISR_SHIFT_COUNT:
    case SM0_OSR:
    case SM0_OSR_SHIFT_COUNT:
    case SM0_FIFO_MEM0:
    case SM0_FIFO_MEM1:
    case SM0_FIFO_MEM2:
    case SM0_FIFO_MEM3:
    case SM0_FIFO_MEM4:
    case SM0_FIFO_MEM5:
    case SM0_FIFO_MEM6:
    case SM0_FIFO_MEM7:
    case SM0_INSTR_ORIGIN:
    case SM0_DELAY:
    case SM0_DELAY_CYCLE:
    case SM0_PENDING_DELAY:
    case SM0_FORCED_INSTR:
    case SM0_CLK_ENABLE:
    case SM0_BREAKPOINTS:
    case SM0_TRACEPOINTS:
      break; // ok
    default:
      throw new IllegalArgumentException("register not one of SM0_*: " +
                                         register);
    }
    return
      Constants.getPIOEmuBaseAddress(pioNum) +
      0x4 * (register.ordinal() + smNum * SM_SIZE);
  }

  public static int getFIFOMemAddress(final int pioNum, final int smNum,
                                      final int address)
  {
    Constants.checkPioNum(pioNum, "PIO index number");
    Constants.checkSmNum(smNum);
    Constants.checkFIFOAddr(address, "FIFO address");
    return
      Constants.getPIOEmuBaseAddress(pioNum) +
      0x4 * (Regs.SM0_FIFO_MEM0.ordinal() + smNum * SM_SIZE + address);
  }

  public static int getMemoryAddress(final int pioNum,
                                     final int memoryAddress)
  {
    Constants.checkPioNum(pioNum, "PIO index number");
    Constants.checkSmMemAddr(memoryAddress, "memory address");
    return
      Constants.getPIOEmuBaseAddress(pioNum) +
      0x4 * (Regs.INSTR_MEM0.ordinal() + memoryAddress);
  }

  public static int getTXFAddress(final int pioNum, final int smNum)
  {
    Constants.checkPioNum(pioNum, "PIO index number");
    Constants.checkSmNum(smNum);
    return
      Constants.getPIOEmuBaseAddress(pioNum) +
      0x4 * (Regs.TXF0.ordinal() + smNum);
  }

  public static int getRXFAddress(final int pioNum, final int smNum)
  {
    Constants.checkPioNum(pioNum, "PIO index number");
    Constants.checkSmNum(smNum);
    return
      Constants.getPIOEmuBaseAddress(pioNum) +
      0x4 * (Regs.RXF0.ordinal() + smNum);
  }

  public PIOEmuRegisters(final int pioNum, final LongSupplier wallClockSupplier)
  {
    super(Constants.getPIOEmuBaseAddress(pioNum), (short)REGS.length,
          wallClockSupplier);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
