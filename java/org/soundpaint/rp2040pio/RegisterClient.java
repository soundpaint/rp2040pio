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
public class RegisterClient
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
  }

  private final Socket socket;
  private final PrintWriter out;
  private final BufferedReader in;

  private RegisterClient()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public RegisterClient(final int port) throws IOException
  {
    this("localhost", port);
  }

  public RegisterClient(final String host, final int port) throws IOException
  {
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
    return response.getMessage();
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
