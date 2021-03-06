package net.officefloor.benchmark;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.text.StringEscapeUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.officefloor.server.RequestHandler;
import net.officefloor.server.http.HttpHeaderValue;
import net.officefloor.server.http.HttpResponse;
import net.officefloor.server.http.ServerHttpConnection;
import net.officefloor.server.http.impl.ProcessAwareServerHttpConnectionManagedObject;
import net.officefloor.server.http.parse.HttpRequestParser;
import net.officefloor.server.stream.ServerWriter;

/**
 * Sends the Fortunes response.
 * 
 * @author Daniel Sagenschneider
 */
public class FortunesSendResponse extends AbstractSendResponse {

	private static final HttpHeaderValue TEXT_HTML = new HttpHeaderValue("text/html;charset=utf-8");

	private static final byte[] TEMPLATE_START = "<!DOCTYPE html><html><head><title>Fortunes</title></head><body><table><tr><th>id</th><th>message</th></tr>"
			.getBytes(ServerHttpConnection.DEFAULT_HTTP_ENTITY_CHARSET);

	private static final byte[] FORTUNE_START = "<tr><td>".getBytes(ServerHttpConnection.DEFAULT_HTTP_ENTITY_CHARSET);

	private static final byte[] FORTUNE_MIDDLE = "</td><td>".getBytes(ServerHttpConnection.DEFAULT_HTTP_ENTITY_CHARSET);

	private static final byte[] FORTUNE_END = "</td></tr>".getBytes(ServerHttpConnection.DEFAULT_HTTP_ENTITY_CHARSET);

	private static final byte[] TEMPLATE_END = "</table></body></html>"
			.getBytes(ServerHttpConnection.DEFAULT_HTTP_ENTITY_CHARSET);

	private static Comparator<Fortune> SORT_FORTUNE = (a, b) -> a.message.compareTo(b.message);

	public FortunesSendResponse(RequestHandler<HttpRequestParser> requestHandler,
			ProcessAwareServerHttpConnectionManagedObject<ByteBuffer> connection, ObjectMapper objectMapper) {
		super(requestHandler, connection, objectMapper);
	}

	public void sendFortunes(List<Fortune> fortunes) {
		this.requestHandler.execute(() -> {
			try {
				// Additional fortunes
				fortunes.add(new Fortune(0, "Additional fortune added at request time."));
				Collections.sort(fortunes, SORT_FORTUNE);

				// Send response
				HttpResponse response = this.connection.getResponse();
				response.setContentType(TEXT_HTML, null);
				ServerWriter writer = response.getEntityWriter();
				writer.write(TEMPLATE_START);
				for (Fortune fortune : fortunes) {
					writer.write(FORTUNE_START);
					int id = fortune.id;
					writer.write(Integer.valueOf(id).toString());
					writer.write(FORTUNE_MIDDLE);
					StringEscapeUtils.ESCAPE_HTML4.translate(fortune.message, writer);
					writer.write(FORTUNE_END);
				}
				writer.write(TEMPLATE_END);
				send(this.connection);
			} catch (CancelledKeyException | ClosedChannelException ex) {
				// Ignore as disconnecting client
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		});
	}

}