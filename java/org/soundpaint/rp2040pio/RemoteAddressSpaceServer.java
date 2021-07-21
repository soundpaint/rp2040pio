/*
 * @(#)RemoteAddressSpaceServer.java 1.00 21/03/03
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * The idea of the RemoteAddressSpaceServer class is to provide access
 * to the PIO emulator applicable even for processes other than the
 * JVM instance that hosts the PIO emulator, and potential integration
 * with other languages such as C/C++ or Python.  Effectively, this
 * class adds an architectural layer that provides the PIO emulator as
 * software as a service (SaaS).  Access is provided via a standard
 * TC/IP socket with a simple protocol for accessing the PIO
 * emulator's pseudo-memory-mapped registers, including the additional
 * emulator-specific extended set of registers (such as for accessing
 * the internal X and Y register or FIFO values).  For example, even
 * an ordinary C program (like one created with the pioasm tool) may
 * make access the PIO emulator by compiling it against a special
 * extended version of the Pico C SDK, such that e.g. set up and
 * control of the PIO Emulator can be done directly from the C code
 * injected in a .pio file.  Similarly, a Python library may be
 * developed that replaces the standard Pico Python libary with one
 * that accesses the emulator instead of real Pico hardware.
 *
 * The Pico Host SDL shows a specific example that draws the general
 * idea of how to extend the Pico C SDK in such a manner (see:
 * https://github.com/raspberrypi/pico-host-sdl).  For this PIO
 * emulator, the SDK is to be extended in a way similar to the Pico
 * Host SDL, such that access to the PIO's registers is not performed
 * via direct memory access (as the default implementation of the C
 * SDK does), but via the socket interface that this
 * RemoteAddressSpaceServer class provides.
 */
public class RemoteAddressSpaceServer
{
  private static final String[] NULL_ARGS = new String[0];

  private final PrintStream console;
  private final AddressSpace memory;
  private final int portNumber;
  private final ServerSocket serverSocket;
  private int connectionCounter;

  private RemoteAddressSpaceServer()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public RemoteAddressSpaceServer(final PrintStream console,
                                  final AddressSpace memory)
    throws IOException
  {
    this(console, memory, Constants.REGISTER_SERVER_DEFAULT_PORT_NUMBER);
  }

  public RemoteAddressSpaceServer(final PrintStream console,
                                  final AddressSpace memory,
                                  final int portNumber)
    throws IOException
  {
    if (console == null) {
      throw new NullPointerException("console");
    }
    if (memory == null) {
      throw new NullPointerException("memory");
    }
    this.console = console;
    this.memory = memory;
    this.portNumber = portNumber;
    serverSocket = new ServerSocket(portNumber);
    connectionCounter = 0;
    // TODO: Maybe introduce pool of reusable client threads to limit
    // maximum number of simultaneously open connections.
    new Thread(() -> listen(),
               "RemoteAddressSpaceServer Client Thread").start();
  }

  private void listen()
  {
    while (true) {
      try {
        final Socket clientSocket = serverSocket.accept();
        new Thread(() -> serve(clientSocket),
                   "RemoteAddressSpaceServer Server Thread").start();
      } catch (final IOException e) {
        // establishing connection failed => abort connection
      }
    }
  }

  private String getHelp()
  {
    final String ls = System.lineSeparator();
    return "available commands: " + ls +
      "h                   (help)" + ls +
      "v                   (version)" + ls +
      "q                   (quit)" + ls +
      "r <addr>            (read address)" + ls +
      "w <addr> <value> <mask> <xor>" + ls +
      "                    (write address)" + ls +
      "i <addr> <value> [<mask> [<timeout cycles> [<timeout millis>]]]" + ls +
      "                    (await value)" + ls +
      "s <addr>            (show address register set id)" + ls +
      "l <addr>            (show address label)" + ls +
      "p <addr>            (check address validity)";
  }

  private enum ResponseStatus
  {
    BYE("bye", 100),
    OK("ok", 101),
    ERR_UNKNOWN_COMMAND("unknown command", 400),
    ERR_MISSING_OPERAND("missing operand", 401),
    ERR_UNPARSED_INPUT("unparsed input", 402),
    ERR_INVALID_NUMBER("invalid number", 403),
    ERR_INVALID_BOOL("invalid Boolean value", 404),
    ERR_IO("input / output error", 405),
    ERR_UNEXPECTED("unexpected error", 406);

    private final String id;
    private final int code;

    private ResponseStatus(final String id, final int code)
    {
      if (id == null) {
        throw new NullPointerException("id");
      }
      this.id = id;
      this.code = code;
    }

    public String getId() { return id; }

    public int getCode() { return code; }

    public String getDisplayValue()
    {
      return code + " " + id.toUpperCase();
    }
  };

  private String createResponse(final ResponseStatus status)
  {
    return createResponse(status, null);
  }

  private String createResponse(final ResponseStatus status,
                                final String message)
  {
    if (status == null) {
      throw new NullPointerException("status");
    }
    final String statusDisplay = status.getDisplayValue();
    return statusDisplay + (message != null ? ": " + message : "");
  }

  private boolean parseBoolean(final String unparsed)
  {
    if (unparsed.equals("t") ||
        unparsed.equals("T")) {
      return true;
    } else if (unparsed.equals("f") ||
        unparsed.equals("F")) {
      return false;
    } else {
      final String message =
        String.format("expected Boolean value 't' or 'f': %s", unparsed);
      throw new IllegalArgumentException(message);
    }
  }

  private int parseInt(final String unparsed)
  {
    if (unparsed.startsWith("0x") ||
        unparsed.startsWith("0X")) {
      return Integer.parseUnsignedInt(unparsed.substring(2), 16);
    } else {
      return Integer.parseInt(unparsed);
    }
  }

  private int parseAddress(final String unparsed)
  {
    final int address = parseInt(unparsed);
    if ((address & 0x3) != 0x0) {
      final String message =
        String.format("address not word-aligned: 0x%08x", address);
      throw new NumberFormatException(message);
    }
    return address;
  }

  private String handleGetVersion(final String[] args) throws IOException
  {
    if (args.length > 0) {
      return createResponse(ResponseStatus.ERR_UNPARSED_INPUT, args[0]);
    }
    return createResponse(ResponseStatus.OK, memory.getEmulatorInfo());
  }

  private String handleGetHelp(final String[] args)
  {
    if (args.length > 0) {
      return createResponse(ResponseStatus.ERR_UNPARSED_INPUT, args[0]);
    }
    return createResponse(ResponseStatus.OK, getHelp());
  }

  private String handleQuit(final String[] args)
  {
    if (args.length > 0) {
      return createResponse(ResponseStatus.ERR_UNPARSED_INPUT, args[0]);
    }
    return null;
  }

  private String handleProvidesAddress(final String[] args) throws IOException
  {
    if (args.length < 1) {
      return createResponse(ResponseStatus.ERR_MISSING_OPERAND, null);
    }
    if (args.length > 1) {
      return createResponse(ResponseStatus.ERR_UNPARSED_INPUT, args[1]);
    }
    final int address;
    try {
      address = parseAddress(args[0]);
    } catch (final NumberFormatException e) {
      return createResponse(ResponseStatus.ERR_INVALID_NUMBER, e.getMessage());
    }
    final boolean providesAddress = memory.providesAddress(address);
    return createResponse(ResponseStatus.OK, String.valueOf(providesAddress));
  }

  private String handleGetRegisterSetId(final String[] args) throws IOException
  {
    if (args.length < 1) {
      return createResponse(ResponseStatus.ERR_MISSING_OPERAND, null);
    }
    if (args.length > 1) {
      return createResponse(ResponseStatus.ERR_UNPARSED_INPUT, args[1]);
    }
    final int address;
    try {
      address = parseAddress(args[0]);
    } catch (final NumberFormatException e) {
      return createResponse(ResponseStatus.ERR_INVALID_NUMBER, e.getMessage());
    }
    final String id = memory.getRegisterSetId(address);
    return createResponse(ResponseStatus.OK, id);
  }

  private String handleGetLabel(final String[] args) throws IOException
  {
    if (args.length < 1) {
      return createResponse(ResponseStatus.ERR_MISSING_OPERAND, null);
    }
    if (args.length > 1) {
      return createResponse(ResponseStatus.ERR_UNPARSED_INPUT, args[1]);
    }
    final int address;
    try {
      address = parseAddress(args[0]);
    } catch (final NumberFormatException e) {
      return createResponse(ResponseStatus.ERR_INVALID_NUMBER, e.getMessage());
    }
    final String label = memory.getAddressLabel(address);
    return createResponse(ResponseStatus.OK, label);
  }

  private String handleWriteAddress(final String[] args) throws IOException
  {
    if (args.length < 4) {
      return createResponse(ResponseStatus.ERR_MISSING_OPERAND, null);
    }
    if (args.length > 4) {
      return createResponse(ResponseStatus.ERR_UNPARSED_INPUT, args[4]);
    }
    final int address;
    try {
      address = parseAddress(args[0]);
    } catch (final NumberFormatException e) {
      return createResponse(ResponseStatus.ERR_INVALID_NUMBER, e.getMessage());
    }
    final int value;
    try {
      value = parseInt(args[1]);
    } catch (final NumberFormatException e) {
      return createResponse(ResponseStatus.ERR_INVALID_NUMBER, e.getMessage());
    }
    final int mask;
    try {
      mask = parseInt(args[2]);
    } catch (final NumberFormatException e) {
      return createResponse(ResponseStatus.ERR_INVALID_NUMBER, e.getMessage());
    }
    final boolean xor;
    try {
      xor = parseBoolean(args[3]);
    } catch (final IllegalArgumentException e) {
      return createResponse(ResponseStatus.ERR_INVALID_BOOL, e.getMessage());
    }
    memory.writeAddressMasked(address, value, mask, xor);
    return createResponse(ResponseStatus.OK);
  }

  private String handleReadAddress(final String[] args) throws IOException
  {
    if (args.length < 1) {
      return createResponse(ResponseStatus.ERR_MISSING_OPERAND, null);
    }
    if (args.length > 1) {
      return createResponse(ResponseStatus.ERR_UNPARSED_INPUT, args[1]);
    }
    final int address;
    try {
      address = parseAddress(args[0]);
    } catch (final NumberFormatException e) {
      return createResponse(ResponseStatus.ERR_INVALID_NUMBER, e.getMessage());
    }
    final int value = memory.readAddress(address);
    return createResponse(ResponseStatus.OK, String.valueOf(value));
  }

  private String handleWait(final String[] args) throws IOException
  {
    if (args.length < 2) {
      return createResponse(ResponseStatus.ERR_MISSING_OPERAND, null);
    }
    if (args.length > 5) {
      return createResponse(ResponseStatus.ERR_UNPARSED_INPUT, args[5]);
    }
    final int address;
    try {
      address = parseAddress(args[0]);
    } catch (final NumberFormatException e) {
      return createResponse(ResponseStatus.ERR_INVALID_NUMBER, e.getMessage());
    }
    final int expectedValue;
    try {
      expectedValue = parseInt(args[1]);
    } catch (final NumberFormatException e) {
      return createResponse(ResponseStatus.ERR_INVALID_NUMBER, e.getMessage());
    }
    final int mask;
    if (args.length > 2) {
      try {
        mask = parseInt(args[2]);
      } catch (final NumberFormatException e) {
        return createResponse(ResponseStatus.ERR_INVALID_NUMBER, e.getMessage());
      }
    } else {
      mask = 0xffffffff;
    }
    final int cyclesTimeout;
    if (args.length > 3) {
      try {
        cyclesTimeout = parseInt(args[3]);
      } catch (final NumberFormatException e) {
        return createResponse(ResponseStatus.ERR_INVALID_NUMBER, e.getMessage());
      }
    } else {
      cyclesTimeout = 0x0;
    }
    final int millisTimeout;
    if (args.length > 4) {
      try {
        millisTimeout = parseInt(args[4]);
      } catch (final NumberFormatException e) {
        return createResponse(ResponseStatus.ERR_INVALID_NUMBER, e.getMessage());
      }
    } else {
      millisTimeout = 0x0;
    }
    final int value =
      memory.waitAddress(address, expectedValue, mask,
                         ((long)cyclesTimeout) & 0xffffffffL,
                         ((long)millisTimeout) & 0xffffffffL);
    return createResponse(ResponseStatus.OK, String.valueOf(value));
  }

  private String handleRequest(final String request) throws IOException
  {
    if (request.isEmpty()) {
      return null;
    }
    final char command = request.charAt(0);
    final String unparsedArgs = request.substring(1).trim();
    final String[] args =
      unparsedArgs.length() > 0 ? unparsedArgs.split(" ") : NULL_ARGS;
    /*
     * TODO: Idea: Introduce another command 's' for waiting (or
     * "sleeping") until the emulator runs idle (in MasterClock
     * SINGLE_STEP mode).  This way, an external application like
     * TimingDiagram does not need to busy wait (via periodic polling
     * with 'r' read command) until a triggered clock phase has been
     * completed.
     */
    switch (command) {
    case 'v':
      return handleGetVersion(args);
    case 'h':
    case '?':
      return handleGetHelp(args);
    case 'q':
      return handleQuit(args);
    case 'p':
      return handleProvidesAddress(args);
    case 's':
      return handleGetRegisterSetId(args);
    case 'l':
      return handleGetLabel(args);
    case 'w':
      return handleWriteAddress(args);
    case 'r':
      return handleReadAddress(args);
    case 'i':
      return handleWait(args);
    default:
      return createResponse(ResponseStatus.ERR_UNKNOWN_COMMAND,
                            String.valueOf(command));
    }
  }

  private void handleThrowable(final PrintWriter clientOut, final Throwable t,
                               final ResponseStatus responseStatus,
                               final int id)
  {
    if (clientOut != null) {
      try {
        clientOut.println(createResponse(responseStatus, t.getMessage()));
      } catch (final Throwable s) {
        // ignore
      }
    }
    t.printStackTrace(console);
    console.printf("connection #%d aborted: %s%n", id, t);
  }

  private void serve(final Socket clientSocket)
  {
    final int id = connectionCounter++;
    console.printf("connection #%d opened%n", id);
    PrintWriter clientOut = null;
    try {
      clientOut = new PrintWriter(clientSocket.getOutputStream(), true);
      final BufferedReader in =
        new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      String request;
      while ((request = in.readLine()) != null) {
        final String response = handleRequest(request.trim());
        if (response == null) {
          break;
        }
        clientOut.println(response);
      }
    } catch (final IOException e) {
      handleThrowable(clientOut, e, ResponseStatus.ERR_IO, id);
    } catch (final Throwable t) {
      handleThrowable(clientOut, t, ResponseStatus.ERR_UNEXPECTED, id);
    } finally {
      console.printf("connection #%d closed%n", id);
      try {
        clientSocket.close();
      } catch (final IOException e) {
        console.println("warning: failed closing client socket: " + e);
      }
    }
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
