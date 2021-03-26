/*
 * @(#)Monitor.java 1.00 21/02/02
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

import java.io.IOException;
import java.io.PrintStream;
import org.soundpaint.rp2040pio.sdk.GPIOSDK;
import org.soundpaint.rp2040pio.sdk.PIOSDK;
import org.soundpaint.rp2040pio.sdk.SDK;
import org.soundpaint.rp2040pio.sdk.Program;
import org.soundpaint.rp2040pio.sdk.ProgramParser;

/**
 * Program Execution Monitor And Control
 */
public class Monitor
{
  private final PrintStream console;
  private final SDK sdk;
  private final PIOSDK pioSdk;
  private final GPIOSDK gpioSdk;

  private Monitor()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public Monitor(final SDK sdk)
  {
    if (sdk == null) {
      throw new NullPointerException("sdk");
    }
    this.sdk = sdk;
    console = sdk.getConsole();
    pioSdk = sdk.getPIO0SDK();
    gpioSdk = sdk.getGPIOSDK();
    printAbout();
  }

  private void printAbout()
  {
    console.println(sdk.getAbout());
  }

  public void addProgram(final String programResourcePath)
    throws IOException
  {
    final Program program = ProgramParser.parse(programResourcePath);
    pioSdk.addProgram(program);
  }

  public void dumpProgram() throws IOException
  {
    for (int address = 0; address < Constants.MEMORY_SIZE; address++) {
      final PIOSDK.InstructionInfo instructionInfo =
        pioSdk.getMemoryInstruction(0, address, true);
      System.out.println(instructionInfo.getToolTipText());
    }
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
