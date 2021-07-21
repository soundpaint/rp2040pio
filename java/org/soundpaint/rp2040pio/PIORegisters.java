/*
 * @(#)PIORegisters.java 1.00 21/02/25
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.soundpaint.rp2040pio.doctool.RegistersDocs;

/**
 * Facade to the internal subsystems of a PIO.  The layout of
 * registers follows the list of registers in Sect. 3.7 of the RP2040
 * datasheet.  The facade is in particular intended for use by the
 * SDK.
 */
public abstract class PIORegisters extends RegisterSet
{
  public enum Regs implements RegistersDocs<Regs>
  {
    CTRL("PIO control register.",
         new BitsInfo[] {
           new BitsInfo(null, 31, 12, null, BitsType.RESERVED, null),
           new BitsInfo("CLKDIV3_RESTART", 11, 11, null, BitsType.SC, 0),
           new BitsInfo("CLKDIV2_RESTART", 10, 10, null, BitsType.SC, 0),
           new BitsInfo("CLKDIV1_RESTART", 9, 9, null, BitsType.SC, 0),
           new BitsInfo("CLKDIV0_RESTART", 8, 8, null, BitsType.SC, 0),
           new BitsInfo("SM3_RESTART", 7, 7, null, BitsType.SC, 0),
           new BitsInfo("SM2_RESTART", 6, 6, null, BitsType.SC, 0),
           new BitsInfo("SM1_RESTART", 5, 5, null, BitsType.SC, 0),
           new BitsInfo("SM0_RESTART", 4, 4, null, BitsType.SC, 0),
           new BitsInfo("SM3_ENABLE", 3, 3, null, BitsType.RW, 0),
           new BitsInfo("SM2_ENABLE", 2, 2, null, BitsType.RW, 0),
           new BitsInfo("SM1_ENABLE", 1, 1, null, BitsType.RW, 0),
           new BitsInfo("SM0_ENABLE", 0, 0, null, BitsType.RW, 0)
         }),
    FSTAT("FIFO status register.",
          new BitsInfo[] {
            new BitsInfo(null, 31, 28, null, BitsType.RESERVED, null),
            new BitsInfo("SM3_TXEMPTY", 27, 27, null, BitsType.RO, 1),
            new BitsInfo("SM2_TXEMPTY", 26, 26, null, BitsType.RO, 1),
            new BitsInfo("SM1_TXEMPTY", 25, 25, null, BitsType.RO, 1),
            new BitsInfo("SM0_TXEMPTY", 24, 24, null, BitsType.RO, 1),
            new BitsInfo(null, 23, 20, null, BitsType.RESERVED, null),
            new BitsInfo("SM3_TXFULL", 19, 19, null, BitsType.RO, 0),
            new BitsInfo("SM2_TXFULL", 18, 18, null, BitsType.RO, 0),
            new BitsInfo("SM1_TXFULL", 17, 17, null, BitsType.RO, 0),
            new BitsInfo("SM0_TXFULL", 16, 16, null, BitsType.RO, 0),
            new BitsInfo(null, 15, 12, null, BitsType.RESERVED, null),
            new BitsInfo("SM3_RXEMPTY", 11, 11, null, BitsType.RO, 1),
            new BitsInfo("SM2_RXEMPTY", 10, 10, null, BitsType.RO, 1),
            new BitsInfo("SM1_RXEMPTY", 9, 9, null, BitsType.RO, 1),
            new BitsInfo("SM0_RXEMPTY", 8, 8, null, BitsType.RO, 1),
            new BitsInfo(null, 7, 4, null, BitsType.RESERVED, null),
            new BitsInfo("SM3_RXFULL", 3, 3, null, BitsType.RO, 0),
            new BitsInfo("SM2_RXFULL", 2, 2, null, BitsType.RO, 0),
            new BitsInfo("SM1_RXFULL", 1, 1, null, BitsType.RO, 0),
            new BitsInfo("SM0_RXFULL", 0, 0, null, BitsType.RO, 0)
          }),
    FDEBUG("FIFO debug register.",
          new BitsInfo[] {
            new BitsInfo(null, 31, 28, null, BitsType.RESERVED, null),
            new BitsInfo("SM3_TXSTALL", 27, 27, null, BitsType.WC, 0),
            new BitsInfo("SM2_TXSTALL", 26, 26, null, BitsType.WC, 0),
            new BitsInfo("SM1_TXSTALL", 25, 25, null, BitsType.WC, 0),
            new BitsInfo("SM0_TXSTALL", 24, 24, null, BitsType.WC, 0),
            new BitsInfo(null, 23, 20, null, BitsType.RESERVED, null),
            new BitsInfo("SM3_TXOVER", 19, 19, null, BitsType.WC, 0),
            new BitsInfo("SM2_TXOVER", 18, 18, null, BitsType.WC, 0),
            new BitsInfo("SM1_TXOVER", 17, 17, null, BitsType.WC, 0),
            new BitsInfo("SM0_TXOVER", 16, 16, null, BitsType.WC, 0),
            new BitsInfo(null, 15, 12, null, BitsType.RESERVED, null),
            new BitsInfo("SM3_RXUNDER", 11, 11, null, BitsType.WC, 0),
            new BitsInfo("SM2_RXUNDER", 10, 10, null, BitsType.WC, 0),
            new BitsInfo("SM1_RXUNDER", 9, 9, null, BitsType.WC, 0),
            new BitsInfo("SM0_RXUNDER", 8, 8, null, BitsType.WC, 0),
            new BitsInfo(null, 7, 4, null, BitsType.RESERVED, null),
            new BitsInfo("SM3_RXSTALL", 3, 3, null, BitsType.WC, 0),
            new BitsInfo("SM2_RXSTALL", 2, 2, null, BitsType.WC, 0),
            new BitsInfo("SM1_RXSTALL", 1, 1, null, BitsType.WC, 0),
            new BitsInfo("SM0_RXSTALL", 0, 0, null, BitsType.WC, 0)
          }),
    FLEVEL("FIFO levels.",
           IntStream.rangeClosed(0, 7).boxed()
           .map(n -> new BitsInfo(((n & 1) == 0 ? "R" : "T") +
                                  "X" + (3 - (n / 2)), 31 - (n << 2),
                                  28 - (n << 2), null, BitsType.RO, 0))
           .collect(Collectors.toList())),
    TXF0("Direct write access to the TX FIFO for state machine *N*.", 0,
         new BitsInfo[] {
           new BitsInfo(null, 31, 0, null, BitsType.WF, 0)
         }),
    TXF1(Regs.TXF0, 1),
    TXF2(Regs.TXF0, 2),
    TXF3(Regs.TXF0, 3),
    RXF0("Direct read access to the RX FIFO for state machine *N*.", 0,
         new BitsInfo[] {
           new BitsInfo(null, 31, 0, null, BitsType.RF, 0)
         }),
    RXF1(Regs.RXF0, 1),
    RXF2(Regs.RXF0, 2),
    RXF3(Regs.RXF0, 3),
    IRQ("State machine IRQ flags register.  Write 1 to clear.",
        new BitsInfo[] {
          new BitsInfo(null, 31, 8, null, BitsType.RESERVED, null),
          new BitsInfo("IRQ7", 7, 7, null, BitsType.WC, 0),
          new BitsInfo("IRQ6", 6, 6, null, BitsType.WC, 0),
          new BitsInfo("IRQ5", 5, 5, null, BitsType.WC, 0),
          new BitsInfo("IRQ4", 4, 4, null, BitsType.WC, 0),
          new BitsInfo("IRQ3", 3, 3, null, BitsType.WC, 0),
          new BitsInfo("IRQ2", 2, 2, null, BitsType.WC, 0),
          new BitsInfo("IRQ1", 1, 1, null, BitsType.WC, 0),
          new BitsInfo("IRQ0", 0, 0, null, BitsType.WC, 0),
        }),
    IRQ_FORCE("Writing a 1 to each the bit will forcibly assert " +
              "the corresponding IRQ.",
        new BitsInfo[] {
          new BitsInfo(null, 31, 8, null, BitsType.RESERVED, null),
          new BitsInfo("IRQ7", 7, 7, null, BitsType.WF, 0),
          new BitsInfo("IRQ6", 6, 6, null, BitsType.WF, 0),
          new BitsInfo("IRQ5", 5, 5, null, BitsType.WF, 0),
          new BitsInfo("IRQ4", 4, 4, null, BitsType.WF, 0),
          new BitsInfo("IRQ3", 3, 3, null, BitsType.WF, 0),
          new BitsInfo("IRQ2", 2, 2, null, BitsType.WF, 0),
          new BitsInfo("IRQ1", 1, 1, null, BitsType.WF, 0),
          new BitsInfo("IRQ0", 0, 0, null, BitsType.WF, 0),
        }),
    INPUT_SYNC_BYPASS("GPIO input synchronizer policy.",
                      IntStream.rangeClosed(0, 31).boxed()
                      .map(n -> new BitsInfo("GPIO" + (31 - n),
                                             31 - n, 31 - n, null,
                                             BitsType.RW, 0))
                      .collect(Collectors.toList())),
    DBG_PADOUT("Read to sample pad output value from PIO.",
               IntStream.rangeClosed(0, 31).boxed()
               .map(n -> new BitsInfo("GPIO" + (31 - n),
                                      31 - n, 31 - n, null,
                                      BitsType.RO, 0))
               .collect(Collectors.toList())),
    DBG_PADOE("Read to sample pad output enable from PIO.",
              IntStream.rangeClosed(0, 31).boxed()
              .map(n -> new BitsInfo("GPIO" + (31 - n),
                                     31 - n, 31 - n, null,
                                     BitsType.RO, 0))
              .collect(Collectors.toList())),
    DBG_CFGINFO("PIO hardware free parameters.",
               new BitsInfo[] {
                 new BitsInfo(null, 31, 22, null, BitsType.RESERVED, null),
                 new BitsInfo("IMEM_SIZE", 21, 16, null, BitsType.RO, null),
                 new BitsInfo(null, 15, 12, null, BitsType.RESERVED, null),
                 new BitsInfo("SM_COUNT", 11, 8, null, BitsType.RO, null),
                 new BitsInfo(null, 7, 6, null, BitsType.RESERVED, null),
                 new BitsInfo("FIFO_DEPTH", 5, 0, null, BitsType.RO, null)
               }),
    INSTR_MEM0("Write-only access to instruction memory location *N*.",
               new BitsInfo[] {
                 new BitsInfo(null, 31, 16, null, BitsType.RESERVED, null),
                 new BitsInfo(null, 15, 0, null, BitsType.RO, 0)
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
    SM0_CLKDIV("Clock divisor register for state machine *N*.", 0,
               new BitsInfo[] {
                 new BitsInfo("INT", 31, 16, null, BitsType.RW, 1),
                 new BitsInfo("FRAC", 15, 8, null, BitsType.RW, 0),
                 new BitsInfo(null, 7, 0, null, BitsType.RESERVED, null)
               }),
    SM0_EXECCTRL("Execution/behavioural settings for state machine *N*.", 0,
                 new BitsInfo[] {
                   new BitsInfo("EXEC_STALLED", 31, 31, null, BitsType.RO, 0),
                   new BitsInfo("SIDE_EN", 30, 30, null, BitsType.RW, 0),
                   new BitsInfo("SIDE_PINDIR", 29, 29, null, BitsType.RW, 0),
                   new BitsInfo("JMP_PIN", 28, 24, null, BitsType.RW, 0),
                   new BitsInfo("OUT_EN_SEL", 23, 19, null, BitsType.RW, 0),
                   new BitsInfo("INLINE_OUT_EN", 18, 18, null, BitsType.RW, 0),
                   new BitsInfo("OUT_STICKY", 17, 17, null, BitsType.RW, 0),
                   new BitsInfo("WRAP_TOP", 16, 12, null, BitsType.RW, 0x1f),
                   new BitsInfo("WRAP_BOTTOM", 11, 7, null, BitsType.RW, 0),
                   new BitsInfo(null, 6, 5, null, BitsType.RESERVED, null),
                   new BitsInfo("STATUS_SEL", 4, 4, null, BitsType.RW, 0),
                   new BitsInfo("STATUS_N", 3, 0, null, BitsType.RW, 0)
                 }),
    SM0_SHIFTCTRL("Control behaviour of the input/output shift registers " +
                  "for state machine *N*.", 0,
                  new BitsInfo[] {
                    new BitsInfo("FJOIN_RX", 31, 31, null, BitsType.RW, 0),
                    new BitsInfo("FJOIN_TX", 30, 30, null, BitsType.RW, 0),
                    new BitsInfo("PULL_THRESH", 29, 25, null, BitsType.RW, 0),
                    new BitsInfo("PUSH_THRESH", 24, 20, null, BitsType.RW, 0),
                    new BitsInfo("OUT_SHIFTDIR", 19, 19, null, BitsType.RW, 1),
                    new BitsInfo("IN_SHIFTDIR", 18, 18, null, BitsType.RW, 1),
                    new BitsInfo("AUTOPULL", 17, 17, null, BitsType.RW, 0),
                    new BitsInfo("AUTOPUSH", 16, 16, null, BitsType.RW, 0),
                    new BitsInfo(null, 15, 0, null, BitsType.RESERVED, null)
                  }),
    SM0_ADDR("Current instruction address of state machine *N*.", 0,
             new BitsInfo[] {
               new BitsInfo(null, 31, 5, null, BitsType.RESERVED, null),
               new BitsInfo(null, 4, 0, null, BitsType.RO, 0)
             }),
    SM0_INSTR("Read to see current instruction on state machine *N*.  Write " +
              "to execute instruction immediately on state machine *N*.", 0,
              new BitsInfo[] {
                new BitsInfo(null, 31, 16, null, BitsType.RESERVED, null),
                new BitsInfo(null, 15, 0, null, BitsType.RW, null)
              }),
    SM0_PINCTRL("State machine pin control for state machine *N*.", 0,
                new BitsInfo[] {
                  new BitsInfo("SIDESET_COUNT", 31, 29, null, BitsType.RW, 0),
                  new BitsInfo("SET_COUNT", 28, 26, null, BitsType.RW, 5),
                  new BitsInfo("OUT_COUNT", 25, 20, null, BitsType.RW, 0),
                  new BitsInfo("IN_BASE", 19, 15, null, BitsType.RW, 0),
                  new BitsInfo("SIDESET_BASE", 14, 10, null, BitsType.RW, 0),
                  new BitsInfo("SET_BASE", 9, 5, null, BitsType.RW, 0),
                  new BitsInfo("OUT_BASE", 4, 0, null, BitsType.RW, 0)
                }),
    SM1_CLKDIV(Regs.SM0_CLKDIV, 1),
    SM1_EXECCTRL(Regs.SM0_EXECCTRL, 1),
    SM1_SHIFTCTRL(Regs.SM0_SHIFTCTRL, 1),
    SM1_ADDR(Regs.SM0_ADDR, 1),
    SM1_INSTR(Regs.SM0_INSTR, 1),
    SM1_PINCTRL(Regs.SM0_PINCTRL, 1),
    SM2_CLKDIV(Regs.SM0_CLKDIV, 2),
    SM2_EXECCTRL(Regs.SM0_EXECCTRL, 2),
    SM2_SHIFTCTRL(Regs.SM0_SHIFTCTRL, 2),
    SM2_ADDR(Regs.SM0_ADDR, 2),
    SM2_INSTR(Regs.SM0_INSTR, 2),
    SM2_PINCTRL(Regs.SM0_PINCTRL, 2),
    SM3_CLKDIV(Regs.SM0_CLKDIV, 3),
    SM3_EXECCTRL(Regs.SM0_EXECCTRL, 3),
    SM3_SHIFTCTRL(Regs.SM0_SHIFTCTRL, 3),
    SM3_ADDR(Regs.SM0_ADDR, 3),
    SM3_INSTR(Regs.SM0_INSTR, 3),
    SM3_PINCTRL(Regs.SM0_PINCTRL, 3),
    INTR("Raw Interrupts.",
         new BitsInfo[] {
           new BitsInfo(null, 31, 12, null, BitsType.RESERVED, null),
           new BitsInfo("SM3", 11, 11, null, BitsType.RO, 0),
           new BitsInfo("SM2", 10, 10, null, BitsType.RO, 0),
           new BitsInfo("SM1", 9, 9, null, BitsType.RO, 0),
           new BitsInfo("SM0", 8, 8, null, BitsType.RO, 0),
           new BitsInfo("SM3_TXNFULL", 7, 7, null, BitsType.RO, 0),
           new BitsInfo("SM2_TXNFULL", 6, 6, null, BitsType.RO, 0),
           new BitsInfo("SM1_TXNFULL", 5, 5, null, BitsType.RO, 0),
           new BitsInfo("SM0_TXNFULL", 4, 4, null, BitsType.RO, 0),
           new BitsInfo("SM3_RXNEMPTY", 3, 3, null, BitsType.RO, 0),
           new BitsInfo("SM2_RXNEMPTY", 2, 2, null, BitsType.RO, 0),
           new BitsInfo("SM1_RXNEMPTY", 1, 1, null, BitsType.RO, 0),
           new BitsInfo("SM0_RXNEMPTY", 0, 0, null, BitsType.RO, 0)
         }),
    IRQ0_INTE("Interrupt enable for IRQ0.",
              new BitsInfo[] {
                new BitsInfo(null, 31, 12, null, BitsType.RESERVED, null),
                new BitsInfo("SM3", 11, 11, null, BitsType.RW, 0),
                new BitsInfo("SM2", 10, 10, null, BitsType.RW, 0),
                new BitsInfo("SM1", 9, 9, null, BitsType.RW, 0),
                new BitsInfo("SM0", 8, 8, null, BitsType.RW, 0),
                new BitsInfo("SM3_TXNFULL", 7, 7, null, BitsType.RW, 0),
                new BitsInfo("SM2_TXNFULL", 6, 6, null, BitsType.RW, 0),
                new BitsInfo("SM1_TXNFULL", 5, 5, null, BitsType.RW, 0),
                new BitsInfo("SM0_TXNFULL", 4, 4, null, BitsType.RW, 0),
                new BitsInfo("SM3_RXNEMPTY", 3, 3, null, BitsType.RW, 0),
                new BitsInfo("SM2_RXNEMPTY", 2, 2, null, BitsType.RW, 0),
                new BitsInfo("SM1_RXNEMPTY", 1, 1, null, BitsType.RW, 0),
                new BitsInfo("SM0_RXNEMPTY", 0, 0, null, BitsType.RW, 0)
         }),
    IRQ0_INTF("Interrupt force for IRQ0.",
              new BitsInfo[] {
                new BitsInfo(null, 31, 12, null, BitsType.RESERVED, null),
                new BitsInfo("SM3", 11, 11, null, BitsType.RW, 0),
                new BitsInfo("SM2", 10, 10, null, BitsType.RW, 0),
                new BitsInfo("SM1", 9, 9, null, BitsType.RW, 0),
                new BitsInfo("SM0", 8, 8, null, BitsType.RW, 0),
                new BitsInfo("SM3_TXNFULL", 7, 7, null, BitsType.RW, 0),
                new BitsInfo("SM2_TXNFULL", 6, 6, null, BitsType.RW, 0),
                new BitsInfo("SM1_TXNFULL", 5, 5, null, BitsType.RW, 0),
                new BitsInfo("SM0_TXNFULL", 4, 4, null, BitsType.RW, 0),
                new BitsInfo("SM3_RXNEMPTY", 3, 3, null, BitsType.RW, 0),
                new BitsInfo("SM2_RXNEMPTY", 2, 2, null, BitsType.RW, 0),
                new BitsInfo("SM1_RXNEMPTY", 1, 1, null, BitsType.RW, 0),
                new BitsInfo("SM0_RXNEMPTY", 0, 0, null, BitsType.RW, 0)
         }),
    IRQ0_INTS("Interrupt status after masking & forcing for IRQ0.",
              new BitsInfo[] {
                new BitsInfo(null, 31, 12, null, BitsType.RESERVED, null),
                new BitsInfo("SM3", 11, 11, null, BitsType.RO, 0),
                new BitsInfo("SM2", 10, 10, null, BitsType.RO, 0),
                new BitsInfo("SM1", 9, 9, null, BitsType.RO, 0),
                new BitsInfo("SM0", 8, 8, null, BitsType.RO, 0),
                new BitsInfo("SM3_TXNFULL", 7, 7, null, BitsType.RO, 0),
                new BitsInfo("SM2_TXNFULL", 6, 6, null, BitsType.RO, 0),
                new BitsInfo("SM1_TXNFULL", 5, 5, null, BitsType.RO, 0),
                new BitsInfo("SM0_TXNFULL", 4, 4, null, BitsType.RO, 0),
                new BitsInfo("SM3_RXNEMPTY", 3, 3, null, BitsType.RO, 0),
                new BitsInfo("SM2_RXNEMPTY", 2, 2, null, BitsType.RO, 0),
                new BitsInfo("SM1_RXNEMPTY", 1, 1, null, BitsType.RO, 0),
                new BitsInfo("SM0_RXNEMPTY", 0, 0, null, BitsType.RO, 0)
         }),
    IRQ1_INTE("Interrupt enable for IRQ1.",
              new BitsInfo[] {
                new BitsInfo(null, 31, 12, null, BitsType.RESERVED, null),
                new BitsInfo("SM3", 11, 11, null, BitsType.RW, 0),
                new BitsInfo("SM2", 10, 10, null, BitsType.RW, 0),
                new BitsInfo("SM1", 9, 9, null, BitsType.RW, 0),
                new BitsInfo("SM0", 8, 8, null, BitsType.RW, 0),
                new BitsInfo("SM3_TXNFULL", 7, 7, null, BitsType.RW, 0),
                new BitsInfo("SM2_TXNFULL", 6, 6, null, BitsType.RW, 0),
                new BitsInfo("SM1_TXNFULL", 5, 5, null, BitsType.RW, 0),
                new BitsInfo("SM0_TXNFULL", 4, 4, null, BitsType.RW, 0),
                new BitsInfo("SM3_RXNEMPTY", 3, 3, null, BitsType.RW, 0),
                new BitsInfo("SM2_RXNEMPTY", 2, 2, null, BitsType.RW, 0),
                new BitsInfo("SM1_RXNEMPTY", 1, 1, null, BitsType.RW, 0),
                new BitsInfo("SM0_RXNEMPTY", 0, 0, null, BitsType.RW, 0)
         }),
    IRQ1_INTF("Interrupt force for IRQ1.",
              new BitsInfo[] {
                new BitsInfo(null, 31, 12, null, BitsType.RESERVED, null),
                new BitsInfo("SM3", 11, 11, null, BitsType.RW, 0),
                new BitsInfo("SM2", 10, 10, null, BitsType.RW, 0),
                new BitsInfo("SM1", 9, 9, null, BitsType.RW, 0),
                new BitsInfo("SM0", 8, 8, null, BitsType.RW, 0),
                new BitsInfo("SM3_TXNFULL", 7, 7, null, BitsType.RW, 0),
                new BitsInfo("SM2_TXNFULL", 6, 6, null, BitsType.RW, 0),
                new BitsInfo("SM1_TXNFULL", 5, 5, null, BitsType.RW, 0),
                new BitsInfo("SM0_TXNFULL", 4, 4, null, BitsType.RW, 0),
                new BitsInfo("SM3_RXNEMPTY", 3, 3, null, BitsType.RW, 0),
                new BitsInfo("SM2_RXNEMPTY", 2, 2, null, BitsType.RW, 0),
                new BitsInfo("SM1_RXNEMPTY", 1, 1, null, BitsType.RW, 0),
                new BitsInfo("SM0_RXNEMPTY", 0, 0, null, BitsType.RW, 0)
         }),
    IRQ1_INTS("Interrupt status after masking & forcing for IRQ1.",
              new BitsInfo[] {
                new BitsInfo(null, 31, 12, null, BitsType.RESERVED, null),
                new BitsInfo("SM3", 11, 11, null, BitsType.RO, 0),
                new BitsInfo("SM2", 10, 10, null, BitsType.RO, 0),
                new BitsInfo("SM1", 9, 9, null, BitsType.RO, 0),
                new BitsInfo("SM0", 8, 8, null, BitsType.RO, 0),
                new BitsInfo("SM3_TXNFULL", 7, 7, null, BitsType.RO, 0),
                new BitsInfo("SM2_TXNFULL", 6, 6, null, BitsType.RO, 0),
                new BitsInfo("SM1_TXNFULL", 5, 5, null, BitsType.RO, 0),
                new BitsInfo("SM0_TXNFULL", 4, 4, null, BitsType.RO, 0),
                new BitsInfo("SM3_RXNEMPTY", 3, 3, null, BitsType.RO, 0),
                new BitsInfo("SM2_RXNEMPTY", 2, 2, null, BitsType.RO, 0),
                new BitsInfo("SM1_RXNEMPTY", 1, 1, null, BitsType.RO, 0),
                new BitsInfo("SM0_RXNEMPTY", 0, 0, null, BitsType.RO, 0)
         });

    public static String getRegisterSetLabel()
    {
      return "PIO Registers";
    }

    public static String getRegisterSetDescription()
    {
      return
        "The PIO registers as described in Sect. 3.7 of the RP2040%n" +
        "datasheet.%n" +
        "Base address for the two emulator PIO register sets (one %n" +
        "register set for each of the two PIOs) is%n" +
        String.format("0x%08x and 0x%08x for PIO0 and PIO1, respectively.%n",
                      PIO0_BASE, PIO1_BASE);
    }

    private final RegisterDetails registerDetails;

    private Regs()
    {
      throw new UnsupportedOperationException("unsupported empty constructor");
    }

    private Regs(final Regs ref)
    {
      this(ref.registerDetails);
    }

    private Regs(final Regs ref, final int smNum)
    {
      this(ref.registerDetails.createCopyForDifferentSm(smNum));
    }

    private Regs(final String info, final BitsInfo[] bitsInfos)
    {
      this(new RegisterDetails(info, bitsInfos));
    }

    private Regs(final String info, final List<BitsInfo> bitsInfos)
    {
      this(new RegisterDetails(info, bitsInfos));
    }

    private Regs(final String info, final int smNum,
                 final BitsInfo[] bitsInfos)
    {
      this(new RegisterDetails(info, smNum, bitsInfos));
    }

    private Regs(final String info, final int smNum,
                 final List<BitsInfo> bitsInfos)
    {
      this(new RegisterDetails(info, smNum, bitsInfos));
    }

    private Regs(final RegisterDetails registerDetails)
    {
      this.registerDetails = registerDetails;
    }

    @Override
    public String getInfo()
    {
      return registerDetails.getInfo();
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
    Regs.SM1_CLKDIV.ordinal() - Regs.SM0_CLKDIV.ordinal();

  public static int getAddress(final int pioNum,
                               final PIORegisters.Regs register)
  {
    Constants.checkPioNum(pioNum, "PIO index number");
    if (register == null) {
      throw new NullPointerException("register");
    }
    return Constants.getPIOBaseAddress(pioNum) + 0x4 * register.ordinal();
  }

  public static int getSMAddress(final int pioNum,
                                 final int smNum,
                                 final PIORegisters.Regs register)
  {
    Constants.checkPioNum(pioNum, "PIO index number");
    Constants.checkSmNum(smNum);
    if (register == null) {
      throw new NullPointerException("register");
    }
    switch (register) {
    case SM0_CLKDIV:
    case SM0_EXECCTRL:
    case SM0_SHIFTCTRL:
    case SM0_ADDR:
    case SM0_INSTR:
    case SM0_PINCTRL:
      break; // ok
    default:
      throw new IllegalArgumentException("register not one of SM0_*: " +
                                         register);
    }
    return
      Constants.getPIOBaseAddress(pioNum) +
      0x4 * (register.ordinal() + smNum * SM_SIZE);
  }

  public static int getMemoryAddress(final int pioNum,
                                     final int memoryAddress)
  {
    Constants.checkPioNum(pioNum, "PIO index number");
    Constants.checkSmMemAddr(memoryAddress, "memory address");
    return
      Constants.getPIOBaseAddress(pioNum) +
      0x4 * (Regs.INSTR_MEM0.ordinal() + memoryAddress);
  }

  public static int getTXFAddress(final int pioNum, final int smNum)
  {
    Constants.checkPioNum(pioNum, "PIO index number");
    Constants.checkSmNum(smNum);
    return
      Constants.getPIOBaseAddress(pioNum) +
      0x4 * (Regs.TXF0.ordinal() + smNum);
  }

  public static int getRXFAddress(final int pioNum, final int smNum)
  {
    Constants.checkPioNum(pioNum, "PIO index number");
    Constants.checkSmNum(smNum);
    return
      Constants.getPIOBaseAddress(pioNum) +
      0x4 * (Regs.RXF0.ordinal() + smNum);
  }

  public PIORegisters(final String id, final int baseAddress)
  {
    super(id, baseAddress);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
