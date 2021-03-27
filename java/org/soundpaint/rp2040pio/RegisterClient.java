/*
 * @(#)RegisterClient.java 1.00 21/03/17
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
import java.io.PrintWriter;
import java.net.Socket;
import org.soundpaint.rp2040pio.sdk.SDK;

/**
 * Remote client that connects to a RegisterServer via TCP/IP socket
 * connection.
 */
public class RegisterClient extends AbstractRegisters
{
  private static class Response
  {
    private final int statusCode;
    private final String statusId;
    private final String message;

    private Response()
    {
      throw new UnsupportedOperationException("unsupported empty constructor");
    }

    private Response(final int statusCode, final String statusId,
                     final String message)
    {
      this.statusCode = statusCode;
      this.statusId = statusId;
      this.message = message;
    }

    public int getStatusCode()
    {
      return statusCode;
    }

    public String getStatusId()
    {
      return statusId;
    }

    public String getMessage()
    {
      return message;
    }

    public boolean isOk()
    {
      return statusCode == 101; // TODO: Use global constant.
    }

    public String toString()
    {
      return
        statusCode + " " + statusId + (message != null ? ": " + message : "");
    }
  }

  private final Socket socket;
  private final PrintWriter out;
  private final BufferedReader in;

  public RegisterClient() throws IOException
  {
    this(Constants.REGISTER_SERVER_DEFAULT_PORT_NUMBER);
  }

  public RegisterClient(final int port) throws IOException
  {
    this("localhost", port);
  }

  public RegisterClient(final String host, final int port) throws IOException
  {
    super(0x0, (short)0x0);
    socket = new Socket(host, port);
    out = new PrintWriter(socket.getOutputStream(), true);
    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
  }

  private Response getResponse(final String request) throws IOException
  {
    out.println(request);
    final String response = in.readLine();
    if (response == null) {
      return null;
    }
    final int colonPos = response.indexOf(':');
    if (colonPos < 0) {
      throw new IOException("failed parsing server response: " + response);
    }
    final String statusDisplay = response.substring(0, colonPos - 1);
    final int spacePos = statusDisplay.indexOf(' ');
    if (spacePos < 0) {
      throw new IOException("failed parsing server response status: " +
                            statusDisplay);
    }
    final String statusCodeAsString = statusDisplay.substring(0, spacePos - 1);
    final int statusCode;
    try {
      statusCode = Integer.parseInt(statusCodeAsString);
    } catch (final NumberFormatException e) {
      throw new IOException("failed parsing server response status code: " +
                            statusCodeAsString);
    }
    final String statusId = statusDisplay.substring(spacePos);
    final String message = response.substring(colonPos);
    return new Response(statusCode, statusId, message);
  }

  public synchronized String getVersion() throws IOException
  {
    final Response response = getResponse("v");
    if (response == null) {
      return null;
    }
    final String message = response.getMessage();
    if (!response.isOk()) {
      throw new IOException("failed retrieving version: " + message);
    }
    return message;
  }

  public synchronized String getHelp() throws IOException
  {
    final Response response = getResponse("h");
    if (response == null) {
      return null;
    }
    final String message = response.getMessage();
    if (!response.isOk()) {
      throw new IOException("failed retrieving help: " + message);
    }
    return message;
  }

  public synchronized void quit() throws IOException
  {
    final Response response = getResponse("q");
    if (response != null) {
      throw new IOException("unexpected response on quit: " + response);
    }
  }

  @Override
  public int getBaseAddress() { return 0; }

  @Override
  public boolean providesAddress(final int address) throws IOException
  {
    final Response response = getResponse("p " + address);
    if (response == null) {
      throw new IOException("missing response for address " + address);
    }
    final String message = response.getMessage();
    if (!response.isOk()) {
      throw new IOException("failed retrieving provision info " +
                            "for address " + address + ": " + message);
    }
    if (message == null) {
      throw new IOException("missing provision info for address " + address);
    }
    final boolean provided;
    try {
      provided = Boolean.parseBoolean(message);
    } catch (final NumberFormatException e) {
      throw new IOException("failed parsing provision info for address " +
                            address + ": " + message);
    }
    return provided;
  }

  @Override
  protected String getRegisterLabel(final int regNum)
  {
    throw new InternalError("method not applicable for this class");
  }

  @Override
  public String getAddressLabel(final int address) throws IOException
  {
    final Response response = getResponse("l " + address);
    if (response == null) {
      throw new IOException("missing response for address " + address);
    }
    final String message = response.getMessage();
    if (!response.isOk()) {
      throw new IOException("failed retrieving label " +
                            "for address " + address + ": " + message);
    }
    if (message == null) {
      throw new IOException("missing label for address " + address);
    }
    return message;
  }

  @Override
  protected void writeRegister(final int regNum,
                               final int bits, final int mask,
                               final boolean xor)
  {
    throw new InternalError("method not applicable for this class");
  }

  @Override
  public synchronized void writeAddress(final int address,
                                        final int value) throws IOException
  {
    final Response response = getResponse("w " + address + " " + value);
    if (response == null) {
      throw new IOException("missing response for address " + address);
    }
    final String message = response.getMessage();
    if (!response.isOk()) {
      throw new IOException("failed writing value " + value +
                            "to address " + address + ": " + message);
    }
  }

  private int parseIntResult(final int address, final String message)
    throws IOException
  {
    if (message == null) {
      throw new IOException("missing value for address " + address);
    }
    final int value;
    try {
      value = Integer.parseInt(message);
    } catch (final NumberFormatException e) {
      throw new IOException("failed parsing value for address " +
                            address + ": " + message);
    }
    return value;
  }

  @Override
  protected int readRegister(final int regNum)
  {
    throw new InternalError("method not applicable for this class");
  }

  @Override
  public synchronized int readAddress(final int address) throws IOException
  {
    final Response response = getResponse("r " + address);
    if (response == null) {
      throw new IOException("missing response for address " + address);
    }
    final String message = response.getMessage();
    if (!response.isOk()) {
      throw new IOException("failed retrieving value " +
                            "for address " + address + ": " + message);
    }
    return parseIntResult(address, message);
  }

  @Override
  public synchronized int wait(final int address,
                               final int expectedValue, final int mask,
                               final long cyclesTimeout,
                               final long millisTimeout)
    throws IOException
  {
    final StringBuffer query = new StringBuffer();
    final Response response = getResponse("i " + address + " " +
                                          expectedValue + " " +
                                          mask + " " +
                                          cyclesTimeout + " " +
                                          millisTimeout);
    if (response == null) {
      throw new IOException("missing response for address " + address);
    }
    final String message = response.getMessage();
    if (!response.isOk()) {
      throw new IOException("failed waiting for IRQ " +
                            "on address " + address + ": " + message);
    }
    return parseIntResult(address, message);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
