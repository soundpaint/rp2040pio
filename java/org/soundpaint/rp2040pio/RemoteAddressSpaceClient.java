/*
 * @(#)RemoteAddressSpaceClient.java 1.00 21/03/17
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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import org.soundpaint.rp2040pio.sdk.SDK;

/**
 * TCP/IP Client that connects to a RemoteAddressSpaceServer via
 * socket.
 */
public class RemoteAddressSpaceClient extends AddressSpace
{
  private static final String MSG_NO_CONNECTION = "no connection";

  private static class Response
  {
    private final PrintStream console;
    private final int statusCode;
    private final String statusId;
    private final String result;

    private Response()
    {
      throw new UnsupportedOperationException("unsupported empty constructor");
    }

    private Response(final PrintStream console,
                     final int statusCode, final String statusId,
                     final String result)
    {
      if (console == null) {
        throw new NullPointerException("console");
      }
      this.console = console;
      this.statusCode = statusCode;
      this.statusId = statusId;
      this.result = result;
    }

    public int getStatusCode()
    {
      return statusCode;
    }

    public String getStatusId()
    {
      return statusId;
    }

    public String getResult()
    {
      return result;
    }

    public boolean isOk()
    {
      return statusCode == 101; // TODO: Use global constant.
    }

    public String getResultOrThrowOnFailure(final String errorMessage)
      throws IOException
    {
      if (!isOk()) {
        final String responseMessage = errorMessage + ": " + toString();
        /*
         * TODO: To avoid duplicate error message display, the message
         * should be logged (e.g. using log4j) separately rather than
         * just being printed to the console, since a typical client
         * application (such as the Monitor application) usually will
         * already display the message by itself.
         */
        console.printf("Remote Address Map Client: %s%n" ,responseMessage);
        throw new IOException(responseMessage);
      }
      return result;
    }

    @Override
    public String toString()
    {
      return
        statusCode + " " + statusId + (result != null ? ": " + result : "");
    }
  }

  private final PrintStream console;
  private int port;
  private String host;
  private Socket socket;

  /**
   * Creates a register client, but does not yet connect to any
   * emulation server.
   *
   * @see #connect
   */
  public RemoteAddressSpaceClient(final PrintStream console) throws IOException
  {
    if (console == null) {
      throw new NullPointerException("console");
    }
    this.console = console;
  }

  /**
   * Creates register client and connects to the default port of the
   * specified host.  If host is null, connects to localhost.
   */
  public RemoteAddressSpaceClient(final PrintStream console, final String host)
    throws IOException
  {
    this(console);
    connect(host);
  }

  /**
   * Creates register client and connects to the specified port of the
   * specified host.  If host is null, connects to localhost.
   */
  public RemoteAddressSpaceClient(final PrintStream console,
                                  final String host, final int port)
    throws IOException
  {
    this(console);
    connect(host, port);
  }

  /**
   * Return host of most recently successfully established connection.
   * Return value is undefined if no connection has been successfully
   * established so far.  To check if this is the case, use method
   * getPort() and check for return value of -1.
   */
  public String getHost() { return host; }

  /**
   * Return port number of most recently successfully established
   * connection or -1, if no connection has been successfully
   * established so far.
   */
  public int getPort() { return port; }

  /**
   * Connects this register client to the default port of the
   * specified host.  If host is null, connects to localhost.
   */
  public void connect(final String host)
    throws IOException
  {
    connect(host, Constants.REGISTER_SERVER_DEFAULT_PORT_NUMBER);
  }

  /**
   * Connects this register client to the specified port of
   * localhost.
   */
  public void connect(final int port)
    throws IOException
  {
    connect(null, Constants.REGISTER_SERVER_DEFAULT_PORT_NUMBER);
  }

  /**
   * Connects this register client to the specified port of the
   * specified host.  If host is null, connects to localhost.
   */
  public void connect(final String host, final int port)
    throws IOException
  {
    if (socket != null) {
      try {
        socket.close();
      } catch (final IOException e) {
        // ignore, we are throwing this connection away anyway
      }
    }
    socket = new Socket();
    socket.connect(host != null ?
                   new InetSocketAddress(host, port) :
                   new InetSocketAddress(InetAddress.getByName(null), port));
    this.host = host;
    this.port = port;
  }

  private synchronized Response getResponse(final String request)
    throws IOException
  {
    final PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
    final BufferedReader in =
      new BufferedReader(new InputStreamReader(socket.getInputStream()));
    out.println(request);
    final String response = in.readLine();
    if (response == null) {
      return null;
    }
    final int colonPos = response.indexOf(':');
    final String statusDisplay =
      colonPos >= 0 ? response.substring(0, colonPos) : response;
    final int spacePos = statusDisplay.indexOf(' ');
    if (spacePos < 0) {
      throw new IOException("failed parsing server response status: " +
                            statusDisplay);
    }
    final String statusCodeAsString = statusDisplay.substring(0, spacePos);
    final int statusCode;
    try {
      statusCode = Integer.parseInt(statusCodeAsString);
    } catch (final NumberFormatException e) {
      throw new IOException("failed parsing server response status code: " +
                            statusCodeAsString);
    }
    final String statusId = statusDisplay.substring(spacePos + 1).trim();
    final String result =
      colonPos >= 0 ? response.substring(colonPos + 1).trim() : null;
    return new Response(console, statusCode, statusId, result);
  }

  private void checkResponse(final Response response) throws IOException
  {
    if (response == null) {
      throw new IOException(MSG_NO_CONNECTION);
    }
  }

  @Override
  public String getEmulatorInfo() throws IOException
  {
    final Response response = getResponse("v");
    checkResponse(response);
    return response.getResultOrThrowOnFailure("failed retreiving version");
  }

  public String getHelp() throws IOException
  {
    final Response response = getResponse("h");
    checkResponse(response);
    return response.getResultOrThrowOnFailure("failed retreiving help");
  }

  public void quit() throws IOException
  {
    final Response response = getResponse("q");
    if (response != null) {
      throw new IOException("unexpected response on quit: " + response);
    }
  }

  @Override
  public boolean providesAddress(final int address) throws IOException
  {
    final String request = String.format("p 0x%08x", address);
    final Response response = getResponse(request);
    checkResponse(response);
    final String retrievalMessage =
      String.format("failed retrieving provision info for address 0x%08x",
                    address);
    final String result =
      response.getResultOrThrowOnFailure(retrievalMessage);
    if (result == null) {
      final String message =
        String.format("missing provision info for address 0x%08x", address);
      throw new IOException(message);
    }
    final boolean provided;
    try {
      provided = Boolean.parseBoolean(result);
    } catch (final NumberFormatException e) {
      final String message =
        String.format("failed parsing provision info for address 0x%08x: %s",
                      address, result);
      throw new IOException(message);
    }
    return provided;
  }

  @Override
  public String getRegisterSetId(final int address) throws IOException
  {
    final String request = String.format("s 0x%08x", address);
    final Response response = getResponse(request);
    checkResponse(response);
    final String retrievalMessage =
      String.format("failed retrieving register set for address 0x%08x",
                    address);
    final String result =
      response.getResultOrThrowOnFailure(retrievalMessage);
    if (result == null) {
      final String message =
        String.format("missing register set for address 0x%08x", address);
      throw new IOException(message);
    }
    return result;
  }

  @Override
  public String getAddressLabel(final int address) throws IOException
  {
    final String request = String.format("l 0x%08x", address);
    final Response response = getResponse(request);
    checkResponse(response);
    final String retrievalMessage =
      String.format("failed retrieving label for address 0x%08x", address);
    final String result =
      response.getResultOrThrowOnFailure(retrievalMessage);
    if (result == null) {
      final String message =
        String.format("missing label for address 0x%08x", address);
      throw new IOException(message);
    }
    return result;
  }

  @Override
  public void writeAddressMasked(final int address, final int bits,
                                 final int mask, final boolean xor)
    throws IOException
  {
    final String request = String.format("w 0x%08x 0x%08x 0x%08x %s",
                                         address, bits, mask, xor ? "t" : "f");
    final Response response = getResponse(request);
    checkResponse(response);
    final String message =
      String.format("failed writing value 0x%08x to address 0x%08x with " +
                    "mask 0x%08x and xor=%s", bits, address, mask, xor);
    response.getResultOrThrowOnFailure(message);
  }

  private int parseIntResult(final int address, final String result)
    throws IOException
  {
    if (result == null) {
      final String message =
        String.format("missing value for address 0x%08x", address);
      throw new IOException(message);
    }
    final int value;
    try {
      value = Integer.parseInt(result);
    } catch (final NumberFormatException e) {
      final String message =
        String.format("failed parsing value for address 0x%08x: %s",
                      address, result);
      throw new IOException(message);
    }
    return value;
  }

  @Override
  public int readAddress(final int address) throws IOException
  {
    final String request = String.format("r 0x%08x", address);
    final Response response = getResponse(request);
    checkResponse(response);
    final String message =
      String.format("failed retrieving value for address 0x%08x", address);
    final String result = response.getResultOrThrowOnFailure(message);
    return parseIntResult(address, result);
  }

  @Override
  public int waitAddress(final int address,
                         final int expectedValue, final int mask,
                         final long cyclesTimeout, final long millisTimeout)
    throws IOException
  {
    final StringBuffer query = new StringBuffer();
    final String request =
      String.format("i 0x%08x 0x%08x 0x%08x %d %d",
                    address, expectedValue, mask, cyclesTimeout, millisTimeout);
    final Response response = getResponse(request);
    checkResponse(response);
    final String message =
      String.format("failed waiting for IRQ on address 0x%08x", address);
    final String result =
      response.getResultOrThrowOnFailure(message);
    return parseIntResult(address, result);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
